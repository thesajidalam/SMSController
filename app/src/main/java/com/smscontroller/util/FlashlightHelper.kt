package com.smscontroller.util

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build

object FlashlightHelper {
    private var isFlashOn = false
    private var cameraId: String? = null

    fun toggleFlashlight(context: Context): String {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val flashId = cameraId ?: findFlashCamera(cameraManager) ?: return "Flashlight Error: No flash available"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                synchronized(this) {
                    isFlashOn = !isFlashOn
                    cameraManager.setTorchMode(flashId, isFlashOn)
                }
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

    private fun findFlashCamera(manager: CameraManager): String? {
        try {
            for (id in manager.cameraIdList) {
                val chars = manager.getCameraCharacteristics(id)
                if (chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                    cameraId = id
                    return id
                }
            }
        } catch (_: Exception) {}
        return null
    }

    fun turnOff(context: Context) {
        synchronized(this) {
            if (isFlashOn) {
                try {
                    val flashId = cameraId
                    if (flashId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                        cameraManager.setTorchMode(flashId, false)
                    }
                } catch (_: Exception) {}
                isFlashOn = false
            }
        }
    }
}
