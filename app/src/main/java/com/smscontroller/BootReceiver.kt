package com.smscontroller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smscontroller.service.BackgroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            try {
                val app = context.applicationContext as SMSControllerApp
                app.startServiceOnBoot()
            } catch (_: Exception) {}
        }
    }
}
