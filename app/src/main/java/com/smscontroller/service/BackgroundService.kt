package com.smscontroller.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.smscontroller.MainActivity
import com.smscontroller.SMSControllerApp
import java.util.concurrent.atomic.AtomicBoolean

class BackgroundService : Service() {
    private val isStopping = AtomicBoolean(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isStopping.set(false)
        try {
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, SMSControllerApp.CHANNEL_ID)
            .setContentTitle("SMS Controller Active")
            .setContentText("Listening for SMS commands")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isStopping.getAndSet(true)) {
            try {
                val prefs = SMSControllerApp.instance.prefs
                if (prefs.isServiceEnabled) {
                    val intent = Intent(this, BackgroundService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001

        fun start(context: android.content.Context) {
            try {
                val intent = Intent(context, BackgroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun stop(context: android.content.Context) {
            try {
                context.stopService(Intent(context, BackgroundService::class.java))
            } catch (_: Exception) {}
        }
    }
}
