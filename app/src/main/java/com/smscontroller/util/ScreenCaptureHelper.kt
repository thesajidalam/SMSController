package com.smscontroller.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ScreenCaptureHelper {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var callback: ((String) -> Unit)? = null
    private var isInitialized = false

    fun init(projection: MediaProjection) {
        mediaProjection = projection
        isInitialized = true
    }

    fun isReady(): Boolean = isInitialized

    fun captureScreen(context: Context, senderNumber: String) {
        callback = { result -> SmsSender.send(context, senderNumber, result) }

        if (!isInitialized) {
            callback?.invoke("Screenshot Error: MediaProjection not initialized. Enable Screen Capture in Advanced Features and grant permission.")
            return
        }

        val projection = mediaProjection ?: run {
            callback?.invoke("Screenshot Error: MediaProjection not available")
            return
        }

        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            wm.defaultDisplay.getRealMetrics(metrics)

            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val densityDpi = metrics.densityDpi

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

            virtualDisplay = projection.createVirtualDisplay(
                "SMSControllerScreenCapture",
                width, height, densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader!!.surface,
                null,
                null
            )

            Handler(Looper.getMainLooper()).postDelayed({
                captureAndSaveImage(context)
            }, 500)
        } catch (e: Exception) {
            callback?.invoke("Screenshot Error: ${e.message}")
            cleanup()
        }
    }

    private fun captureAndSaveImage(context: Context) {
        val reader = imageReader ?: run {
            callback?.invoke("Screenshot Error: ImageReader not available")
            cleanup()
            return
        }

        val image = reader.acquireLatestImage()
        if (image == null) {
            callback?.invoke("Screenshot Error: No image data")
            cleanup()
            return
        }

        try {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "SMSController"
            )
            if (!dir.exists()) dir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "SCREENSHOT_$timestamp.png"
            val file = File(dir, fileName)

            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            val cropped = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            FileOutputStream(file).use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }
            cropped.recycle()
            bitmap.recycle()

            callback?.invoke("Screenshot saved: ${file.absolutePath}")
        } catch (e: Exception) {
            callback?.invoke("Screenshot Error: ${e.message}")
        } finally {
            image.close()
            cleanup()
        }
    }

    private fun cleanup() {
        try {
            virtualDisplay?.release()
            imageReader?.close()
        } catch (_: Exception) {}
        virtualDisplay = null
        imageReader = null
    }

    fun release() {
        cleanup()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mediaProjection?.stop()
            }
        } catch (_: Exception) {}
        mediaProjection = null
        isInitialized = false
    }
}
