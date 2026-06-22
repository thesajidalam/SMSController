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
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "SMS Controller Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Background service for SMS command listening"
                    setShowBadge(false)
                }
                val nm = getSystemService(NotificationManager::class.java)
                nm.createNotificationChannel(channel)
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
