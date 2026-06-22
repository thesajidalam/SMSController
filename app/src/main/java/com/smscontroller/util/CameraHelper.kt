package com.smscontroller.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CameraHelper {
    private var cameraDevice: CameraDevice? = null
    private var imageReader: ImageReader? = null
    private var session: CameraCaptureSession? = null
    private var callback: ((String) -> Unit)? = null

    fun takePhoto(context: Context, senderNumber: String, useFrontCamera: Boolean = false) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            SmsSender.send(context, senderNumber, "Photo Error: Camera permission not granted.")
            return
        }

        callback = { result -> SmsSender.send(context, senderNumber, result) }

        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = findCamera(cameraManager, useFrontCamera)

            if (cameraId == null) {
                callback?.invoke("Photo Error: No camera found")
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraDevice = camera
                        startCaptureSession(context)
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close()
                        cameraDevice = null
                        callback?.invoke("Photo Error: Camera disconnected")
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        camera.close()
                        cameraDevice = null
                        callback?.invoke("Photo Error: Camera error $error")
                    }
                }, Handler(Looper.getMainLooper()))
            } else {
                callback?.invoke("Photo Error: Device not supported")
            }
        } catch (e: CameraAccessException) {
            callback?.invoke("Photo Error: Camera access denied")
        } catch (e: SecurityException) {
            callback?.invoke("Photo Error: Camera permission denied")
        }
    }

    private fun findCamera(manager: CameraManager, useFront: Boolean): String? {
        try {
            for (id in manager.cameraIdList) {
                val chars = manager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (useFront && facing == CameraCharacteristics.LENS_FACING_FRONT) return id
                if (!useFront && facing == CameraCharacteristics.LENS_FACING_BACK) return id
            }
            return manager.cameraIdList.firstOrNull()
        } catch (_: Exception) {
            return null
        }
    }

    private fun startCaptureSession(context: Context) {
        val device = cameraDevice ?: return

        val sizes = device.id.let {
            try {
                val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val chars = manager.getCameraCharacteristics(it)
                chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?.getOutputSizes(ImageFormat.JPEG)
            } catch (_: Exception) { null }
        }

        val width = sizes?.firstOrNull()?.width ?: 1920
        val height = sizes?.firstOrNull()?.height ?: 1080

        imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                saveImage(context, image)
                image.close()
            }
            closeCamera()
        }, Handler(Looper.getMainLooper()))

        try {
            device.createCaptureSession(
                listOf(imageReader!!.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(s: CameraCaptureSession) {
                        session = s
                        val request = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                            addTarget(imageReader!!.surface)
                            set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                            set(CaptureRequest.JPEG_QUALITY, 100.toByte())
                        }
                        s.capture(request.build(), null, null)
                    }

                    override fun onConfigureFailed(s: CameraCaptureSession) {
                        callback?.invoke("Photo Error: Session config failed")
                        closeCamera()
                    }
                },
                null
            )
        } catch (e: CameraAccessException) {
            callback?.invoke("Photo Error: Camera access failed")
            closeCamera()
        }
    }

    private fun saveImage(context: Context, image: Image) {
        try {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "SMSController"
            )
            if (!dir.exists()) dir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "PHOTO_$timestamp.jpg"
            val file = File(dir, fileName)

            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            FileOutputStream(file).use { it.write(bytes) }

            callback?.invoke("Photo captured: ${file.absolutePath}")
        } catch (e: Exception) {
            callback?.invoke("Photo Error: ${e.message}")
        }
    }

    private fun closeCamera() {
        try {
            session?.close()
            imageReader?.close()
            cameraDevice?.close()
        } catch (_: Exception) {}
        session = null
        imageReader = null
        cameraDevice = null
    }
}
