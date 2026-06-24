package com.smscontroller.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object RateLimiter {
    private const val PREFS_NAME = "sms_rate_limiter"
    private const val KEY_PREFIX_HOUR = "hour_"
    private const val KEY_PREFIX_DAY = "day_"
    private const val KEY_PREFIX_LAST = "last_reset_"
    private const val MAX_PER_HOUR = 20
    private const val MAX_PER_DAY = 100
    private const val HOUR_MS = 3600000L
    private const val DAY_MS = 86400000L

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun canSend(senderNumber: String): Boolean {
        val p = prefs ?: return true
        val now = System.currentTimeMillis()
        val hourKey = KEY_PREFIX_HOUR + senderNumber
        val dayKey = KEY_PREFIX_DAY + senderNumber
        val lastResetKey = KEY_PREFIX_LAST + senderNumber

        val lastReset = p.getLong(lastResetKey, now)
        val hourCount = p.getInt(hourKey, 0)
        val dayCount = p.getInt(dayKey, 0)

        val hourElapsed = now - lastReset
        if (hourElapsed > HOUR_MS) {
            p.edit { putInt(hourKey, 0) }
        }
        if (hourElapsed > DAY_MS) {
            p.edit { putInt(dayKey, 0); putLong(lastResetKey, now) }
        }

        if (p.getInt(hourKey, 0) >= MAX_PER_HOUR) return false
        if (p.getInt(dayKey, 0) >= MAX_PER_DAY) return false

        p.edit {
            putInt(hourKey, p.getInt(hourKey, 0) + 1)
            putInt(dayKey, p.getInt(dayKey, 0) + 1)
            putLong(lastResetKey, now)
        }
        return true
    }

    fun reset(senderNumber: String) {
        val p = prefs ?: return
        p.edit {
            remove(KEY_PREFIX_HOUR + senderNumber)
            remove(KEY_PREFIX_DAY + senderNumber)
            remove(KEY_PREFIX_LAST + senderNumber)
        }
    }
}
