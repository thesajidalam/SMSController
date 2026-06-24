package com.smscontroller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.smscontroller.util.CommandExecutor
import com.smscontroller.util.PrefsManager
import com.smscontroller.util.RateLimiter
import com.smscontroller.util.SmsCommand
import com.smscontroller.util.SmsLogger
import com.smscontroller.util.SmsSender
import java.util.concurrent.ConcurrentHashMap

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private val processedMessages = ConcurrentHashMap<String, Long>()
        private const val DEDUP_WINDOW_MS = 60000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = PrefsManager(context)
        if (!prefs.isServiceEnabled) {
            SmsLogger.log("SMS ignored: service disabled")
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val messageBody = messages.firstOrNull()?.messageBody?.trim() ?: return
        val senderNumber = messages.firstOrNull()?.originatingAddress ?: return
        val timestamp = messages.firstOrNull()?.timestampMillis ?: 0L

        SmsLogger.log("SMS received: '$messageBody' from $senderNumber")

        if (!prefs.isAuthorized(senderNumber)) {
            SmsLogger.log("SMS rejected: unauthorized sender $senderNumber")
            return
        }

        val now = System.currentTimeMillis()
        val dedupKey = "${senderNumber}:${messageBody}:${timestamp}"

        val previous = processedMessages.putIfAbsent(dedupKey, now)
        if (previous != null) {
            if (now - previous < DEDUP_WINDOW_MS) {
                SmsLogger.log("SMS dedup: ignored duplicate $dedupKey")
                return
            }
            processedMessages[dedupKey] = now
        }

        if (processedMessages.size > 200) {
            cleanupOldEntries()
        }

        val command = SmsCommand.fromMessage(messageBody)
        if (command == null) {
            SmsLogger.log("SMS ignored: unknown command '$messageBody'")
            return
        }

        if (!isCommandEnabled(prefs, command)) {
            SmsSender.send(context, senderNumber, "Command '$command' is disabled. Enable it in app.")
            SmsLogger.log("SMS rejected: $command disabled")
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
            SmsCommand.Beep, SmsCommand.BeepStop, SmsCommand.BeepStatus -> prefs.isBeepEnabled
            SmsCommand.Gps -> prefs.isGpsEnabled
            SmsCommand.Battery -> prefs.isBatteryEnabled
            SmsCommand.Flash -> prefs.isFlashEnabled
            SmsCommand.Callme -> prefs.isCallmeEnabled
            SmsCommand.Data -> prefs.isDataEnabled
            is SmsCommand.Text -> prefs.isTextEnabled
            SmsCommand.Help -> true
        }
    }
}
