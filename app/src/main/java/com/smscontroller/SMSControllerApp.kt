package com.smscontroller

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.smscontroller.util.PrefsManager
import com.smscontroller.service.BackgroundService

class SMSControllerApp : Application() {
    companion object {
        const val CHANNEL_ID = "sms_controller_service"
        const val MSG_CHANNEL_ID = "sms_controller_messages"
        lateinit var instance: SMSControllerApp
            private set
    }

    lateinit var prefs: PrefsManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = PrefsManager(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val nm = getSystemService(NotificationManager::class.java)
                val serviceChannel = NotificationChannel(
                    CHANNEL_ID,
                    "SMS Controller Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Background service for SMS command listening"
                    setShowBadge(false)
                }
                nm.createNotificationChannel(serviceChannel)
                val msgChannel = NotificationChannel(
                    MSG_CHANNEL_ID,
                    "SMS Messages",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications from TEXT command"
                    enableVibration(true)
                }
                nm.createNotificationChannel(msgChannel)
            } catch (_: Exception) {}
        }
    }

    fun startServiceOnBoot() {
        try {
            if (prefs.isServiceEnabled) {
                BackgroundService.start(this)
            }
        } catch (_: Exception) {}
    }
}
