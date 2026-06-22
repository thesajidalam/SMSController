package com.smscontroller.util

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings

object NetworkHelper {

    fun enableWifi(context: Context): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val panelIntent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(panelIntent)
                return "WiFi settings opened. Please enable manually."
            } else {
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = true
                return "WiFi turned ON"
            }
        } catch (e: Exception) {
            return "WiFi Error: ${e.message}"
        }
    }

    fun enableMobileData(context: Context): String {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val panelIntent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(panelIntent)
                return "Data settings opened. Please enable mobile data manually."
            } else {
                @Suppress("DEPRECATION")
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val dataMethod = cm.javaClass.getMethod("setMobileDataEnabled", Boolean::class.java)
                dataMethod.invoke(cm, true)
                return "Mobile Data turned ON"
            }
        } catch (e: Exception) {
            return "Data Error: ${e.message}"
        }
    }
}
