package com.smscontroller.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SmsLogger {
    private const val PREFS_NAME = "sms_logger"
    private const val KEY_LOG = "event_log"
    private const val MAX_EVENTS = 100

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun log(event: String) {
        val p = prefs ?: return
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val entry = "[$timestamp] $event"
        val log = getLog().toMutableList()
        log.add(entry)
        if (log.size > MAX_EVENTS) {
            log.removeAt(0)
        }
        p.edit { putString(KEY_LOG, log.joinToString("\n")) }
    }

    fun getLog(): List<String> {
        val p = prefs ?: return emptyList()
        return p.getString(KEY_LOG, "")?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
    }

    fun clear() {
        val p = prefs ?: return
        p.edit { remove(KEY_LOG) }
    }
}
