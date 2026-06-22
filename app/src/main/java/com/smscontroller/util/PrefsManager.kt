package com.smscontroller.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sms_controller_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val KEY_LOCK_ENABLED = "lock_enabled"
        private const val KEY_BEEP_ENABLED = "beep_enabled"
        private const val KEY_GPS_ENABLED = "gps_enabled"
        private const val KEY_HIDDEN = "hidden_from_launcher"
        private const val KEY_AUTHORIZED_NUMBERS = "authorized_numbers"
        private const val KEY_BEEP_DURATION = "beep_duration"
        private const val KEY_BEEP_RINGTONE_URI = "beep_ringtone_uri"
        private const val KEY_BATTERY_ENABLED = "battery_enabled"
        private const val KEY_PHOTO_ENABLED = "photo_enabled"
        private const val KEY_WIPE_ENABLED = "wipe_enabled"
        private const val KEY_FLASH_ENABLED = "flash_enabled"
        private const val KEY_CALLME_ENABLED = "callme_enabled"
        private const val KEY_WIFI_ENABLED = "wifi_enabled"
        private const val KEY_DATA_ENABLED = "data_enabled"
        private const val KEY_RECORD_ENABLED = "record_enabled"
        private const val KEY_SCREENSHOT_ENABLED = "screenshot_enabled"
        private const val KEY_OWNER_NUMBER = "owner_number"
        private const val KEY_COMMAND_COUNT = "command_count"
    }

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_SERVICE_ENABLED, value) }

    var isLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCK_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_LOCK_ENABLED, value) }

    var isBeepEnabled: Boolean
        get() = prefs.getBoolean(KEY_BEEP_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_BEEP_ENABLED, value) }

    var isGpsEnabled: Boolean
        get() = prefs.getBoolean(KEY_GPS_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_GPS_ENABLED, value) }

    var isHiddenFromLauncher: Boolean
        get() = prefs.getBoolean(KEY_HIDDEN, false)
        set(value) = prefs.edit { putBoolean(KEY_HIDDEN, value) }

    var beepDurationSeconds: Int
        get() = prefs.getInt(KEY_BEEP_DURATION, 60)
        set(value) = prefs.edit { putInt(KEY_BEEP_DURATION, value) }

    var beepRingtoneUri: String
        get() = prefs.getString(KEY_BEEP_RINGTONE_URI, "") ?: ""
        set(value) = prefs.edit { putString(KEY_BEEP_RINGTONE_URI, value) }

    var ownerNumber: String
        get() = prefs.getString(KEY_OWNER_NUMBER, "") ?: ""
        set(value) = prefs.edit { putString(KEY_OWNER_NUMBER, value) }

    fun getAuthorizedNumbers(): List<String> {
        val json = prefs.getString(KEY_AUTHORIZED_NUMBERS, "[]") ?: "[]"
        val arr = JSONArray(json)
        return (0 until arr.length()).map { arr.getString(it) }
    }

    fun setAuthorizedNumbers(numbers: List<String>) {
        val arr = JSONArray(numbers)
        prefs.edit { putString(KEY_AUTHORIZED_NUMBERS, arr.toString()) }
    }

    fun isAuthorized(sender: String): Boolean {
        val nums = getAuthorizedNumbers()
        if (nums.isEmpty()) return true
        val normalizedSender = sender.replace(Regex("[^0-9]"), "")
        return nums.any { authorized ->
            val normalizedAuth = authorized.replace(Regex("[^0-9]"), "")
            normalizedAuth.isNotBlank() && normalizedSender.endsWith(normalizedAuth)
        }
    }

    var commandCount: Int
        get() = prefs.getInt(KEY_COMMAND_COUNT, 0)
        set(value) = prefs.edit { putInt(KEY_COMMAND_COUNT, value) }

    fun incrementCommandCount() {
        commandCount = commandCount + 1
    }

    var isBatteryEnabled: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_BATTERY_ENABLED, value) }

    var isPhotoEnabled: Boolean
        get() = prefs.getBoolean(KEY_PHOTO_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_PHOTO_ENABLED, value) }

    var isWipeEnabled: Boolean
        get() = prefs.getBoolean(KEY_WIPE_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_WIPE_ENABLED, value) }

    var isFlashEnabled: Boolean
        get() = prefs.getBoolean(KEY_FLASH_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_FLASH_ENABLED, value) }

    var isCallmeEnabled: Boolean
        get() = prefs.getBoolean(KEY_CALLME_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_CALLME_ENABLED, value) }

    var isWifiEnabled: Boolean
        get() = prefs.getBoolean(KEY_WIFI_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_WIFI_ENABLED, value) }

    var isDataEnabled: Boolean
        get() = prefs.getBoolean(KEY_DATA_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_DATA_ENABLED, value) }

    var isRecordEnabled: Boolean
        get() = prefs.getBoolean(KEY_RECORD_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_RECORD_ENABLED, value) }

    var isScreenshotEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCREENSHOT_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_SCREENSHOT_ENABLED, value) }
}
