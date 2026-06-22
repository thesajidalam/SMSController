package com.smscontroller.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AudioRecorderHelper {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFilePath: String? = null
    private var isRecording = false

    fun isRecording(): Boolean = isRecording

    fun startRecording(context: Context): String {
        if (isRecording) return "Recording already in progress"

        try {
            val recordsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                "SMSController"
            )
            if (!recordsDir.exists()) recordsDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "RECORD_$timestamp.3gp"
            val file = File(recordsDir, fileName)
            currentFilePath = file.absolutePath

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setAudioChannels(1)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(currentFilePath)
                prepare()
                start()
            }

            isRecording = true
            return "Recording started: $fileName"
        } catch (e: Exception) {
            return "Recording Error: ${e.message}"
        }
    }

    fun stopRecording(): String {
        if (!isRecording) return "No recording in progress"

        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            mediaRecorder = null
            isRecording = false
            val path = currentFilePath ?: "unknown"
            currentFilePath = null
            return "Recording stopped. Saved: $path"
        } catch (e: Exception) {
            return "Recording Error: ${e.message}"
        }
    }

    fun cleanup() {
        if (isRecording) {
            stopRecording()
        }
    }
}
