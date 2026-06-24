package com.smscontroller.util

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.smscontroller.AdminReceiver
import com.smscontroller.SMSControllerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private val activeCommands = ConcurrentHashMap<String, Boolean>()
    private var gpsRequestActive = false
    private var gpsCallback: LocationCallback? = null
    private var gpsClient: FusedLocationProviderClient? = null

    fun isDuplicate(commandType: String, senderNumber: String, cooldownMs: Long = 15000): Boolean {
        val key = "$senderNumber:$commandType"
        val now = System.currentTimeMillis()
        val last = commandTimestamps.putIfAbsent(key, now)
        if (last != null) {
            if (now - last < cooldownMs) return true
            commandTimestamps[key] = now
        }
        return false
    }

    private fun acquireLock(commandType: String): Boolean {
        return activeCommands.putIfAbsent(commandType, true) == null
    }

    private fun releaseLock(commandType: String) {
        activeCommands.remove(commandType)
    }

    fun execute(context: Context, command: SmsCommand, senderNumber: String) {
        SMSControllerApp.instance.prefs.incrementCommandCount()
        SmsLogger.log("CMD: $command from $senderNumber")

        if (!RateLimiter.canSend(senderNumber)) {
            SmsLogger.log("RATE LIMITED: $senderNumber")
            SmsSender.send(context, senderNumber, "Rate limit exceeded. Try later.")
            return
        }

        try {
            when (command) {
                SmsCommand.Lock -> lockDevice(context, senderNumber)
                SmsCommand.Beep -> beepDevice(context, senderNumber)
                SmsCommand.BeepStop -> stopBeep(context, senderNumber)
                SmsCommand.BeepStatus -> beepStatus(context, senderNumber)
                SmsCommand.Gps -> getGps(context, senderNumber)
                SmsCommand.Battery -> getBattery(context, senderNumber)
                SmsCommand.Flash -> toggleFlash(context, senderNumber)
                SmsCommand.Callme -> callOwner(context, senderNumber)
                SmsCommand.Data -> enableData(context, senderNumber)
                SmsCommand.Help -> sendHelp(context, senderNumber)
            }
        } catch (e: Exception) {
            SmsLogger.log("EXEC ERROR: ${command} ${e.message}")
            SmsSender.send(context, senderNumber, "Error executing $command. Try again.")
        }
    }

    private fun sendHelp(context: Context, senderNumber: String) {
        if (isDuplicate("help", senderNumber, 15000)) return
        val prefs = SMSControllerApp.instance.prefs
        val commands = buildList {
            if (prefs.isLockEnabled) add("LOCK - Lock device instantly")
            if (prefs.isBeepEnabled) add("BEEP - Loud alarm for ${prefs.beepDurationSeconds}s (BEEP STOP / BEEP STATUS)")
            if (prefs.isGpsEnabled) add("GPS - Get GPS location")
            if (prefs.isBatteryEnabled) add("BATTERY - Battery level & status")
            if (prefs.isFlashEnabled) add("FLASH - Toggle camera flashlight")
            if (prefs.isCallmeEnabled) add("CALLME - Call owner number")
            if (prefs.isDataEnabled) add("DATA - Enable mobile data")
        }
        val msg = if (commands.isEmpty()) {
            "No commands enabled. Open app and enable features."
        } else {
            "Enabled Commands:\n${commands.joinToString("\n")}\n\nSend HELP to see this."
        }
        SmsSender.send(context, senderNumber, msg)
        SmsLogger.log("HELP sent to $senderNumber")
    }

    private fun lockDevice(context: Context, senderNumber: String) {
        if (isDuplicate("lock", senderNumber, 30000)) return

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, AdminReceiver::class.java)

        if (!dpm.isAdminActive(admin)) {
            SmsSender.send(context, senderNumber, "Lock Error: Device admin not enabled. Enable in Settings > Security > Device Admin.")
            SmsLogger.log("LOCK FAIL: admin not enabled")
            return
        }

        try {
            dpm.lockNow()
            SmsSender.send(context, senderNumber, "Device locked.")
            SmsLogger.log("LOCK success")
        } catch (e: Exception) {
            SmsSender.send(context, senderNumber, "Lock Error: ${e.message}")
            SmsLogger.log("LOCK FAIL: ${e.message}")
        }
    }

    private fun beepDevice(context: Context, senderNumber: String) {
        if (isDuplicate("beep", senderNumber, 60000)) return
        if (isBeeping) {
            SmsSender.send(context, senderNumber, "Beep already playing. Send BEEP STATUS or BEEP STOP.")
            SmsLogger.log("BEEP denied: already playing")
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
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
            } catch (_: Exception) {}

            var wakeLock: PowerManager.WakeLock? = null
            try {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
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
                    vibrator.vibrate(VibrationEffect.createWaveform(vibePattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(vibePattern, 0)
                }

                SmsSender.send(context, senderNumber, "Beep started for ${duration}s")
                SmsLogger.log("BEEP started: ${duration}s")

                val player = MediaPlayer()
                beepPlayer = player
                try {
                    player.setAudioStreamType(AudioManager.STREAM_ALARM)
                    player.isLooping = true
                    player.setVolume(1.0f, 1.0f)

                    if (ringtoneUri.isNotBlank()) {
                        try { player.setDataSource(context, Uri.parse(ringtoneUri)) }
                        catch (_: Exception) { player.setDataSource(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)) }
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
                stopBeepInternal()
            } finally {
                try { wakeLock?.let { if (it.isHeld) it.release() } } catch (_: Exception) {}
                isBeeping = false
            }
        }
    }

    fun stopBeep(context: Context, senderNumber: String) {
        if (!isBeeping) {
            SmsSender.send(context, senderNumber, "No beep is currently playing.")
            SmsLogger.log("BEEP STOP: nothing to stop")
            return
        }
        beepJob?.cancel()
        stopBeepInternal()
        isBeeping = false
        SmsSender.send(context, senderNumber, "Beep stopped.")
        SmsLogger.log("BEEP stopped remotely")
    }

    private fun beepStatus(context: Context, senderNumber: String) {
        val msg = if (isBeeping) "Beep is currently playing." else "No beep playing."
        SmsSender.send(context, senderNumber, msg)
        SmsLogger.log("BEEP STATUS: $msg")
    }

    private fun stopBeepInternal() {
        try {
            beepPlayer?.apply { if (isPlaying) stop(); release() }
        } catch (_: Exception) {}
        beepPlayer = null
        try { beepVibrator?.cancel() } catch (_: Exception) {}
        beepVibrator = null
    }

    private fun getGps(context: Context, senderNumber: String) {
        if (isDuplicate("gps", senderNumber, 30000)) return
        if (gpsRequestActive) {
            SmsSender.send(context, senderNumber, "GPS request already in progress. Wait a moment.")
            SmsLogger.log("GPS denied: already active")
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            SmsSender.send(context, senderNumber, "GPS Error: Location permission not granted.")
            SmsLogger.log("GPS FAIL: no permission")
            return
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) && !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            SmsSender.send(context, senderNumber, "GPS is OFF. Opening location settings...")
            try {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            } catch (_: Exception) {}

            SmsLogger.log("GPS disabled - tried to open settings")
            delayThenRetryGps(context, senderNumber)
            return
        }

        requestGpsLocation(context, senderNumber)
    }

    private fun delayThenRetryGps(context: Context, senderNumber: String) {
        gpsRequestActive = true
        scope.launch {
            delay(3000)
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                requestGpsLocation(context, senderNumber)
            } else {
                try {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        val lastKnown = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                            ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { null }
                            else { @Suppress("DEPRECATION") lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }

                        if (lastKnown != null) {
                            val mapsUrl = "https://maps.google.com/?q=${lastKnown.latitude},${lastKnown.longitude}"
                            SmsSender.send(context, senderNumber, buildString {
                                appendLine("Cached Location (GPS still off):")
                                appendLine("Lat: ${lastKnown.latitude}")
                                appendLine("Lng: ${lastKnown.longitude}")
                                appendLine("Accuracy: ${lastKnown.accuracy}m")
                                appendLine("Maps: $mapsUrl")
                            })
                            SmsLogger.log("GPS cached location sent")
                            gpsRequestActive = false
                            return@launch
                        }
                    }
                } catch (_: Exception) {}
                SmsSender.send(context, senderNumber, "GPS still OFF after 3s. Please enable location and send GPS again.")
                SmsLogger.log("GPS still off after retry")
                gpsRequestActive = false
            }
        }
    }

    private fun requestGpsLocation(context: Context, senderNumber: String) {
        gpsRequestActive = true
        SmsLogger.log("GPS requesting location...")

        try {
            gpsClient = LocationServices.getFusedLocationProviderClient(context)
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(1000)
                .setMaxUpdateDelayMillis(10000)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation
                    if (location != null) {
                        val mapsUrl = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
                        val msg = buildString {
                            appendLine("Live Location:")
                            appendLine("Lat: ${location.latitude}")
                            appendLine("Lng: ${location.longitude}")
                            appendLine("Accuracy: ${location.accuracy}m")
                            appendLine("Provider: ${location.provider}")
                            appendLine("Maps: $mapsUrl")
                        }
                        SmsSender.send(context, senderNumber, msg)
                        SmsLogger.log("GPS success: ${location.latitude},${location.longitude}")
                    } else {
                        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                        try {
                            val lastKnown = if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                    ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) null
                                    else { @Suppress("DEPRECATION") lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }
                            } else null

                            if (lastKnown != null) {
                                val mapsUrl = "https://maps.google.com/?q=${lastKnown.latitude},${lastKnown.longitude}"
                                SmsSender.send(context, senderNumber, buildString {
                                    appendLine("Cached Location:")
                                    appendLine("Lat: ${lastKnown.latitude}")
                                    appendLine("Lng: ${lastKnown.longitude}")
                                    appendLine("Accuracy: ${lastKnown.accuracy}m")
                                    appendLine("Maps: $mapsUrl")
                                })
                                SmsLogger.log("GPS cached fallback sent")
                            } else {
                                SmsSender.send(context, senderNumber, "GPS Error: Unable to get location. Try again in a open area.")
                                SmsLogger.log("GPS FAIL: no location")
                            }
                        } catch (_: Exception) {
                            SmsSender.send(context, senderNumber, "GPS Error: Unable to get location.")
                            SmsLogger.log("GPS FAIL: exception")
                        }
                    }
                    removeGpsUpdates()
                }
            }
            gpsCallback = callback

            gpsClient?.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())

            scope.launch {
                delay(15000)
                if (gpsRequestActive) {
                    SmsSender.send(context, senderNumber, "GPS timed out after 15s. Try again.")
                    SmsLogger.log("GPS timeout")
                    removeGpsUpdates()
                }
            }
        } catch (e: SecurityException) {
            SmsSender.send(context, senderNumber, "GPS Error: Location permission denied.")
            SmsLogger.log("GPS FAIL: security ${e.message}")
            removeGpsUpdates()
        } catch (e: Exception) {
            SmsSender.send(context, senderNumber, "GPS Error: ${e.message}")
            SmsLogger.log("GPS FAIL: ${e.message}")
            removeGpsUpdates()
        }
    }

    private fun removeGpsUpdates() {
        try { gpsCallback?.let { gpsClient?.removeLocationUpdates(it) } } catch (_: Exception) {}
        gpsCallback = null
        gpsClient = null
        gpsRequestActive = false
    }

    private fun getBattery(context: Context, senderNumber: String) {
        if (isDuplicate("battery", senderNumber, 10000)) return
        val result = BatteryHelper.getBatteryStatus(context)
        SmsSender.send(context, senderNumber, result)
        SmsLogger.log("BATTERY sent")
    }

    private fun toggleFlash(context: Context, senderNumber: String) {
        if (isDuplicate("flash", senderNumber, 5000)) return
        val result = FlashlightHelper.toggleFlashlight(context)
        SmsSender.send(context, senderNumber, result)
        SmsLogger.log("FLASH: $result")
    }

    private fun callOwner(context: Context, senderNumber: String) {
        if (isDuplicate("callme", senderNumber, 30000)) return

        val prefs = SMSControllerApp.instance.prefs
        val owner = prefs.ownerNumber
        if (owner.isBlank()) {
            SmsSender.send(context, senderNumber, "Callme Error: No owner number set. Set it in app > Settings > Owner Number.")
            SmsLogger.log("CALLME FAIL: no owner number")
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            SmsSender.send(context, senderNumber, "Callme Error: Phone call permission not granted.")
            SmsLogger.log("CALLME FAIL: no phone permission")
            return
        }

        try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$owner")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            SmsSender.send(context, senderNumber, "Calling owner: $owner")
            SmsLogger.log("CALLME: calling $owner")
        } catch (e: Exception) {
            SmsSender.send(context, senderNumber, "Callme Error: ${e.message}")
            SmsLogger.log("CALLME FAIL: ${e.message}")
        }
    }

    private fun enableData(context: Context, senderNumber: String) {
        if (isDuplicate("data", senderNumber, 30000)) return
        val result = NetworkHelper.enableMobileData(context)
        SmsSender.send(context, senderNumber, result)
        SmsLogger.log("DATA: $result")
    }
}
