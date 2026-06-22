package com.smscontroller.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.smscontroller.AdminReceiver
import com.smscontroller.SMSControllerApp
import com.smscontroller.service.BackgroundService
import com.smscontroller.util.PrefsManager
import com.smscontroller.util.ScreenCaptureHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    private val prefs = SMSControllerApp.instance.prefs

    private val _serviceEnabled = MutableStateFlow(prefs.isServiceEnabled)
    val serviceEnabled: StateFlow<Boolean> = _serviceEnabled.asStateFlow()

    private val _lockEnabled = MutableStateFlow(prefs.isLockEnabled)
    val lockEnabled: StateFlow<Boolean> = _lockEnabled.asStateFlow()

    private val _beepEnabled = MutableStateFlow(prefs.isBeepEnabled)
    val beepEnabled: StateFlow<Boolean> = _beepEnabled.asStateFlow()

    private val _gpsEnabled = MutableStateFlow(prefs.isGpsEnabled)
    val gpsEnabled: StateFlow<Boolean> = _gpsEnabled.asStateFlow()

    private val _hiddenFromLauncher = MutableStateFlow(prefs.isHiddenFromLauncher)
    val hiddenFromLauncher: StateFlow<Boolean> = _hiddenFromLauncher.asStateFlow()

    private val _authorizedNumbers = MutableStateFlow(prefs.getAuthorizedNumbers())
    val authorizedNumbers: StateFlow<List<String>> = _authorizedNumbers.asStateFlow()

    private val _deviceAdminActive = MutableStateFlow(false)
    val deviceAdminActive: StateFlow<Boolean> = _deviceAdminActive.asStateFlow()

    private val _batteryOptimized = MutableStateFlow(true)
    val batteryOptimized: StateFlow<Boolean> = _batteryOptimized.asStateFlow()

    private val _beepDuration = MutableStateFlow(prefs.beepDurationSeconds)
    val beepDuration: StateFlow<Int> = _beepDuration.asStateFlow()

    private val _beepRingtoneUri = MutableStateFlow(prefs.beepRingtoneUri)
    val beepRingtoneUri: StateFlow<String> = _beepRingtoneUri.asStateFlow()

    private val _ownerNumber = MutableStateFlow(prefs.ownerNumber)
    val ownerNumber: StateFlow<String> = _ownerNumber.asStateFlow()

    private val _screenCaptureReady = MutableStateFlow(false)
    val screenCaptureReady: StateFlow<Boolean> = _screenCaptureReady.asStateFlow()

    private val _batteryEnabled = MutableStateFlow(prefs.isBatteryEnabled)
    val batteryEnabled: StateFlow<Boolean> = _batteryEnabled.asStateFlow()

    private val _photoEnabled = MutableStateFlow(prefs.isPhotoEnabled)
    val photoEnabled: StateFlow<Boolean> = _photoEnabled.asStateFlow()

    private val _wipeEnabled = MutableStateFlow(prefs.isWipeEnabled)
    val wipeEnabled: StateFlow<Boolean> = _wipeEnabled.asStateFlow()

    private val _flashEnabled = MutableStateFlow(prefs.isFlashEnabled)
    val flashEnabled: StateFlow<Boolean> = _flashEnabled.asStateFlow()

    private val _callmeEnabled = MutableStateFlow(prefs.isCallmeEnabled)
    val callmeEnabled: StateFlow<Boolean> = _callmeEnabled.asStateFlow()

    private val _wifiEnabled = MutableStateFlow(prefs.isWifiEnabled)
    val wifiEnabled: StateFlow<Boolean> = _wifiEnabled.asStateFlow()

    private val _dataEnabled = MutableStateFlow(prefs.isDataEnabled)
    val dataEnabled: StateFlow<Boolean> = _dataEnabled.asStateFlow()

    private val _recordEnabled = MutableStateFlow(prefs.isRecordEnabled)
    val recordEnabled: StateFlow<Boolean> = _recordEnabled.asStateFlow()

    private val _screenshotEnabled = MutableStateFlow(prefs.isScreenshotEnabled)
    val screenshotEnabled: StateFlow<Boolean> = _screenshotEnabled.asStateFlow()

    fun checkDeviceAdmin(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, AdminReceiver::class.java)
        _deviceAdminActive.value = dpm.isAdminActive(admin)
    }

    fun requestDeviceAdmin(context: Context) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(context, AdminReceiver::class.java))
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required for screen lock and wipe commands")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun checkBatteryOptimization(context: Context) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        _batteryOptimized.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            !pm.isIgnoringBatteryOptimizations(context.packageName)
        } else false
    }

    fun requestBatteryOptimization(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try { context.startActivity(intent) } catch (_: Exception) {}
        }
    }

    fun toggleService(context: Context) {
        val new = !_serviceEnabled.value
        _serviceEnabled.value = new
        prefs.isServiceEnabled = new
        if (new) BackgroundService.start(context)
        else BackgroundService.stop(context)
    }

    fun toggleLock() { val v = !_lockEnabled.value; _lockEnabled.value = v; prefs.isLockEnabled = v }
    fun toggleBeep() { val v = !_beepEnabled.value; _beepEnabled.value = v; prefs.isBeepEnabled = v }
    fun toggleGps() { val v = !_gpsEnabled.value; _gpsEnabled.value = v; prefs.isGpsEnabled = v }
    fun toggleBattery() { val v = !_batteryEnabled.value; _batteryEnabled.value = v; prefs.isBatteryEnabled = v }
    fun togglePhoto() { val v = !_photoEnabled.value; _photoEnabled.value = v; prefs.isPhotoEnabled = v }
    fun toggleWipe() { val v = !_wipeEnabled.value; _wipeEnabled.value = v; prefs.isWipeEnabled = v }
    fun toggleFlash() { val v = !_flashEnabled.value; _flashEnabled.value = v; prefs.isFlashEnabled = v }
    fun toggleCallme() { val v = !_callmeEnabled.value; _callmeEnabled.value = v; prefs.isCallmeEnabled = v }
    fun toggleWifi() { val v = !_wifiEnabled.value; _wifiEnabled.value = v; prefs.isWifiEnabled = v }
    fun toggleData() { val v = !_dataEnabled.value; _dataEnabled.value = v; prefs.isDataEnabled = v }
    fun toggleRecord() { val v = !_recordEnabled.value; _recordEnabled.value = v; prefs.isRecordEnabled = v }
    fun toggleScreenshot() { val v = !_screenshotEnabled.value; _screenshotEnabled.value = v; prefs.isScreenshotEnabled = v }

    fun setBeepDuration(seconds: Int) {
        _beepDuration.value = seconds
        prefs.beepDurationSeconds = seconds
    }

    fun setBeepRingtoneUri(uri: String) {
        _beepRingtoneUri.value = uri
        prefs.beepRingtoneUri = uri
    }

    fun setOwnerNumber(number: String) {
        _ownerNumber.value = number
        prefs.ownerNumber = number
    }

    fun setAuthorizedNumbers(numbers: List<String>) {
        _authorizedNumbers.value = numbers
        prefs.setAuthorizedNumbers(numbers)
    }

    fun toggleHidden(context: Context) {
        val new = !_hiddenFromLauncher.value
        _hiddenFromLauncher.value = new
        prefs.isHiddenFromLauncher = new
        updateLauncherState(context, new)
    }

    fun initScreenCapture(context: Context, resultCode: Int, data: Intent?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = mpm.getMediaProjection(resultCode, data!!)
            if (projection != null) {
                ScreenCaptureHelper.init(projection)
                _screenCaptureReady.value = true
            }
        }
    }

    fun checkScreenCapture(context: Context) {
        _screenCaptureReady.value = ScreenCaptureHelper.isReady()
    }

    private fun updateLauncherState(context: Context, hide: Boolean) {
        val pm = context.packageManager
        val componentName = ComponentName(context, "com.smscontroller.MainActivity")
        pm.setComponentEnabledSetting(
            componentName,
            if (hide) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    override fun onCleared() {
        super.onCleared()
    }
}
