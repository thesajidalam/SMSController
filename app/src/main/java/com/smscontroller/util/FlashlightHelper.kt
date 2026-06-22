package com.smscontroller.util

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build

object FlashlightHelper {
    private var isFlashOn = false

    fun toggleFlashlight(context: Context): String {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return "Flashlight Error: No flash available"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                isFlashOn = !isFlashOn
                cameraManager.setTorchMode(cameraId, isFlashOn)
                return if (isFlashOn) "Flashlight turned ON" else "Flashlight turned OFF"
            } else {
                return "Flashlight Error: Not supported on this device"
            }
        } catch (e: CameraAccessException) {
            return "Flashlight Error: Camera in use"
        } catch (e: Exception) {
            return "Flashlight Error: ${e.message}"
        }
    }

    fun turnOff(context: Context) {
        if (isFlashOn) {
            try {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                    val chars = cameraManager.getCameraCharacteristics(id)
                    chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                }
                if (cameraId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cameraManager.setTorchMode(cameraId, false)
                }
            } catch (_: Exception) {}
            isFlashOn = false
        }
    }
}
