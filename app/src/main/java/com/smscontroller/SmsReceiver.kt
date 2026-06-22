package com.smscontroller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.smscontroller.util.CommandExecutor
import com.smscontroller.util.PrefsManager
import com.smscontroller.util.SmsCommand
import com.smscontroller.util.SmsSender
import java.util.concurrent.ConcurrentHashMap

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private val processedMessages = ConcurrentHashMap<String, Long>()
        private const val DEDUP_WINDOW_MS = 45000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = PrefsManager(context)
        if (!prefs.isServiceEnabled) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val messageBody = messages.firstOrNull()?.messageBody ?: return
        val senderNumber = messages.firstOrNull()?.originatingAddress ?: return
        val timestamp = messages.firstOrNull()?.timestampMillis

        if (!prefs.isAuthorized(senderNumber)) return

        val dedupKey = "${senderNumber}:${messageBody}:${timestamp}"
        val now = System.currentTimeMillis()

        val lastProcessed = processedMessages[dedupKey]
        if (lastProcessed != null && (now - lastProcessed) < DEDUP_WINDOW_MS) return

        processedMessages[dedupKey] = now

        cleanupOldEntries()

        val command = SmsCommand.fromMessage(messageBody) ?: return

        val cooldownMap = mapOf(
            SmsCommand.Lock to 30000L,
            SmsCommand.Beep to 60000L,
            SmsCommand.Gps to 30000L,
            SmsCommand.Battery to 10000L,
            SmsCommand.Photo to 30000L,
            SmsCommand.Wipe to 120000L,
            SmsCommand.Flash to 5000L,
            SmsCommand.Callme to 30000L,
            SmsCommand.Wifi to 30000L,
            SmsCommand.Data to 30000L,
            SmsCommand.RecordStart to 10000L,
            SmsCommand.RecordStop to 5000L,
            SmsCommand.Screenshot to 30000L
        )

        val cmdKey = command::class.java.simpleName
        val prevCmdTime = processedMessages["cmd_$cmdKey"] ?: 0L
        val cooldown = cooldownMap[command] ?: 15000L
        if (prevCmdTime != 0L && (now - prevCmdTime) < cooldown) return

        processedMessages["cmd_$cmdKey"] = now

        if (!isCommandEnabled(prefs, command)) {
            SmsSender.send(context, senderNumber, "Command is disabled. Enable it in app.")
            return
        }

        try {
            abortBroadcast()
        } catch (_: Exception) {}

        CommandExecutor.execute(context, command, senderNumber)
    }

    private fun cleanupOldEntries() {
        val cutoff = System.currentTimeMillis() - DEDUP_WINDOW_MS
        val toRemove = processedMessages.filter { it.value < cutoff }.keys
        toRemove.forEach { processedMessages.remove(it) }
    }

    private fun isCommandEnabled(prefs: PrefsManager, command: SmsCommand): Boolean {
        return when (command) {
            SmsCommand.Lock -> prefs.isLockEnabled
            SmsCommand.Beep -> prefs.isBeepEnabled
            SmsCommand.Gps -> prefs.isGpsEnabled
            SmsCommand.Battery -> prefs.isBatteryEnabled
            SmsCommand.Photo -> prefs.isPhotoEnabled
            SmsCommand.Wipe -> prefs.isWipeEnabled
            SmsCommand.Flash -> prefs.isFlashEnabled
            SmsCommand.Callme -> prefs.isCallmeEnabled
            SmsCommand.Wifi -> prefs.isWifiEnabled
            SmsCommand.Data -> prefs.isDataEnabled
            SmsCommand.RecordStart -> prefs.isRecordEnabled
            SmsCommand.RecordStop -> prefs.isRecordEnabled
            SmsCommand.Screenshot -> prefs.isScreenshotEnabled
        }
    }
}
