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
import java.util.concurrent.atomic.AtomicLong

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private val processedMessages = ConcurrentHashMap<String, Long>()
        private val sequenceCounter = AtomicLong(0)
        private const val DEDUP_WINDOW_MS = 45000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = PrefsManager(context)
        if (!prefs.isServiceEnabled) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val messageBody = messages.firstOrNull()?.messageBody?.trim() ?: return
        val senderNumber = messages.firstOrNull()?.originatingAddress ?: return
        val timestamp = messages.firstOrNull()?.timestampMillis ?: 0L

        if (!prefs.isAuthorized(senderNumber)) return

        val now = System.currentTimeMillis()
        val dedupKey = "${senderNumber}:${messageBody}:${timestamp}"

        val previous = processedMessages.putIfAbsent(dedupKey, now)
        if (previous != null) {
            if (now - previous < DEDUP_WINDOW_MS) return
            processedMessages[dedupKey] = now
        }

        if (processedMessages.size > 200) {
            cleanupOldEntries()
        }

        val command = SmsCommand.fromMessage(messageBody) ?: return

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
