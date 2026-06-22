package com.smscontroller.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

object GpsProvider {
    private var fusedClient: FusedLocationProviderClient? = null
    private var activeCallback: LocationCallback? = null
    private var isRequestActive = false

    fun getLocationAndSend(context: Context, senderNumber: String) {
        if (isRequestActive) {
            SmsSender.send(context, senderNumber, "GPS request already in progress. Wait a moment.")
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            SmsSender.send(context, senderNumber, "GPS Error: Location permission not granted.")
            return
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            SmsSender.send(context, senderNumber, "GPS is OFF. Trying to enable via settings...")
            try {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
            SmsSender.send(context, senderNumber, "Please enable location and send 'gps' again.")
            return
        }

        isRequestActive = true
        fusedClient = LocationServices.getFusedLocationProviderClient(context)

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000
        ).setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(1000)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
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
                } else {
                    SmsSender.send(context, senderNumber, "GPS Error: Unable to get location. Try again.")
                }
                removeUpdates()
            }
        }
        activeCallback = callback

        try {
            fusedClient?.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            SmsSender.send(context, senderNumber, "GPS Error: Location permission denied.")
            removeUpdates()
        }
    }

    private fun removeUpdates() {
        try {
            activeCallback?.let { fusedClient?.removeLocationUpdates(it) }
        } catch (_: Exception) {}
        activeCallback = null
        fusedClient = null
        isRequestActive = false
    }
}
