package com.smscontroller.util

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.smscontroller.AdminReceiver
import com.smscontroller.SMSControllerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

object CommandExecutor {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var beepJob: Job? = null
    private var beepPlayer: MediaPlayer? = null
    private var beepVibrator: Vibrator? = null
    private var isBeeping = false

    private val commandTimestamps = ConcurrentHashMap<String, Long>()

    fun isDuplicate(commandType: String, cooldownMs: Long = 15000): Boolean {
        val now = System.currentTimeMillis()
        val last = commandTimestamps.putIfAbsent(commandType, now)
        if (last != null) {
            if (now - last < cooldownMs) return true
            commandTimestamps[commandType] = now
        }
        return false
    }

    fun execute(context: Context, command: SmsCommand, senderNumber: String) {
        SMSControllerApp.instance.prefs.incrementCommandCount()
        when (command) {
            SmsCommand.Lock -> lockDevice(context, senderNumber)
            SmsCommand.Beep -> beepDevice(context, senderNumber)
            SmsCommand.Gps -> GpsProvider.getLocationAndSend(context, senderNumber)
            SmsCommand.Battery -> getBattery(context, senderNumber)
            SmsCommand.Photo -> takePhoto(context, senderNumber)
            SmsCommand.Wipe -> wipeDevice(context, senderNumber)
            SmsCommand.Flash -> toggleFlash(context, senderNumber)
            SmsCommand.Callme -> callOwner(context, senderNumber)
            SmsCommand.Wifi -> enableWifi(context, senderNumber)
            SmsCommand.Data -> enableData(context, senderNumber)
            SmsCommand.RecordStart -> startRecording(context, senderNumber)
            SmsCommand.RecordStop -> stopRecording(context, senderNumber)
            SmsCommand.Screenshot -> takeScreenshot(context, senderNumber)
        }
    }

    private fun lockDevice(context: Context, senderNumber: String) {
        if (isDuplicate("lock", 30000)) return

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, AdminReceiver::class.java)

        if (dpm.isAdminActive(admin)) {
            dpm.lockNow()
            SmsSender.send(context, senderNumber, "Device locked.")
        } else {
            SmsSender.send(context, senderNumber, "Lock Error: Device admin not enabled. Enable in Settings > Security > Device Admin.")
        }
    }

    private fun beepDevice(context: Context, senderNumber: String) {
        if (isDuplicate("beep", 60000)) {
            SmsSender.send(context, senderNumber, "Beep already playing. Wait or force stop app.")
            return
        }
        if (isBeeping) {
            SmsSender.send(context, senderNumber, "Beep already playing.")
            return
        }

        isBeeping = true
        beepJob?.cancel()
        beepJob = scope.launch {
            val prefs = SMSControllerApp.instance.prefs
            val duration = prefs.beepDurationSeconds
            val ringtoneUri = prefs.beepRingtoneUri

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            try {
                audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL)
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                    0
                )
                audioManager.setStreamVolume(
                    AudioManager.STREAM_ALARM,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                    0
                )
            } catch (_: Exception) {}

            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            var wakeLock: PowerManager.WakeLock? = null
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    wakeLock = pm.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                        "SMSController:BeepWakeLock"
                    )
                    wakeLock?.acquire(duration * 1000L + 5000)
                }
            } catch (_: Exception) {}

            try {
                val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vm.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                beepVibrator = vibrator

                val vibePattern = longArrayOf(0, 500, 300, 500, 300, 500, 300, 500, 300, 500)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(vibePattern, 0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(vibePattern, 0)
                }

                SmsSender.send(context, senderNumber, "Beep started for ${duration}s")

                val player = MediaPlayer()
                beepPlayer = player
                try {
                    player.setAudioStreamType(AudioManager.STREAM_ALARM)
                    player.isLooping = true
                    player.setVolume(1.0f, 1.0f)

                    if (ringtoneUri.isNotBlank()) {
                        try {
                            player.setDataSource(context, Uri.parse(ringtoneUri))
                        } catch (_: Exception) {
                            player.setDataSource(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                        }
                    } else {
                        player.setDataSource(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                    }

                    player.prepare()
                    player.start()

                    delay(duration * 1000L)

                    stopBeepInternal()
                } catch (e: Exception) {
                    stopBeepInternal()
                }
            } catch (e: Exception) {
                if (isBeeping) {
                    SmsSender.send(context, senderNumber, "Beep Error: ${e.message}")
                }
            } finally {
                try {
                    wakeLock?.let {
                        if (it.isHeld) it.release()
                    }
                } catch (_: Exception) {}
                isBeeping = false
            }
        }
    }

    fun stopBeep() {
        beepJob?.cancel()
        stopBeepInternal()
        isBeeping = false
    }

    private fun stopBeepInternal() {
        try {
            beepPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (_: Exception) {}
        beepPlayer = null

        try {
            beepVibrator?.cancel()
        } catch (_: Exception) {}
        beepVibrator = null
    }

    private fun getBattery(context: Context, senderNumber: String) {
        if (isDuplicate("battery", 10000)) return
        val result = BatteryHelper.getBatteryStatus(context)
        SmsSender.send(context, senderNumber, result)
    }

    private fun takePhoto(context: Context, senderNumber: String) {
        if (isDuplicate("photo", 30000)) return
        CameraHelper.takePhoto(context, senderNumber)
    }

    private fun wipeDevice(context: Context, senderNumber: String) {
        if (isDuplicate("wipe", 120000)) return

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, AdminReceiver::class.java)

        if (dpm.isAdminActive(admin)) {
            SmsSender.send(context, senderNumber, "WIPE: Factory reset initiated. Device will wipe all data.")
            dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE or DevicePolicyManager.WIPE_RESET_PROTECTION_DATA)
        } else {
            SmsSender.send(context, senderNumber, "Wipe Error: Device admin not enabled.")
        }
    }

    private fun toggleFlash(context: Context, senderNumber: String) {
        if (isDuplicate("flash", 5000)) return
        val result = FlashlightHelper.toggleFlashlight(context)
        SmsSender.send(context, senderNumber, result)
    }

    private fun callOwner(context: Context, senderNumber: String) {
        if (isDuplicate("callme", 30000)) return

        val prefs = SMSControllerApp.instance.prefs
        val owner = prefs.ownerNumber
        if (owner.isBlank()) {
            SmsSender.send(context, senderNumber, "Callme Error: No owner number set. Set it in app > Settings > Owner Number.")
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            SmsSender.send(context, senderNumber, "Callme Error: Phone call permission not granted.")
            return
        }

        try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$owner")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            SmsSender.send(context, senderNumber, "Calling owner: $owner")
        } catch (e: Exception) {
            SmsSender.send(context, senderNumber, "Callme Error: ${e.message}")
        }
    }

    private fun enableWifi(context: Context, senderNumber: String) {
        if (isDuplicate("wifi", 30000)) return
        val result = NetworkHelper.enableWifi(context)
        SmsSender.send(context, senderNumber, result)
    }

    private fun enableData(context: Context, senderNumber: String) {
        if (isDuplicate("data", 30000)) return
        val result = NetworkHelper.enableMobileData(context)
        SmsSender.send(context, senderNumber, result)
    }

    private fun startRecording(context: Context, senderNumber: String) {
        if (isDuplicate("recordstart", 10000)) return
        val result = AudioRecorderHelper.startRecording(context)
        SmsSender.send(context, senderNumber, result)
    }

    private fun stopRecording(context: Context, senderNumber: String) {
        val result = AudioRecorderHelper.stopRecording()
        SmsSender.send(context, senderNumber, result)
    }

    private fun takeScreenshot(context: Context, senderNumber: String) {
        if (isDuplicate("screenshot", 30000)) return
        if (!ScreenCaptureHelper.isReady()) {
            SmsSender.send(context, senderNumber, "Screenshot Error: Not initialized. Open app and enable Screen Capture in Advanced Features.")
            return
        }
        ScreenCaptureHelper.captureScreen(context, senderNumber)
    }
}
