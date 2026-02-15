package com.example.lakbaylaya.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * Utility class for recording audio voice notes.
 * Supports recording between 2-10 seconds with playback preview.
 */
class VoiceRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isRecording = false
    private var startTime: Long = 0

    companion object {
        const val MIN_RECORDING_DURATION_MS = 2000L // 2 seconds
        const val MAX_RECORDING_DURATION_MS = 10000L // 10 seconds
        private const val TAG = "VoiceRecorder"
    }

    /**
     * Start recording audio. Returns the output file path.
     */
    fun startRecording(): String? {
        try {
            // Create output file
            val outputDir = context.getExternalFilesDir(null)
            outputFile = File.createTempFile("voice_note_", ".m4a", outputDir)

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile?.absolutePath)

                prepare()
                start()
            }

            isRecording = true
            startTime = System.currentTimeMillis()
            Log.d(TAG, "Recording started: ${outputFile?.absolutePath}")
            return outputFile?.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start recording", e)
            cleanup()
            return null
        }
    }

    /**
     * Stop recording and return the duration in seconds.
     * Returns null if recording was too short.
     */
    fun stopRecording(): Int? {
        if (!isRecording) return null

        try {
            val duration = System.currentTimeMillis() - startTime

            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false

            // Check minimum duration
            if (duration < MIN_RECORDING_DURATION_MS) {
                Log.w(TAG, "Recording too short: ${duration}ms")
                cleanup()
                return null
            }

            val durationSeconds = (duration / 1000).toInt()
            Log.d(TAG, "Recording stopped: ${durationSeconds}s")
            return durationSeconds
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            cleanup()
            return null
        }
    }

    /**
     * Get the current recording duration in milliseconds.
     */
    fun getRecordingDuration(): Long {
        return if (isRecording) {
            System.currentTimeMillis() - startTime
        } else 0L
    }

    /**
     * Cancel recording and delete the file.
     */
    fun cancelRecording() {
        try {
            if (isRecording) {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false
            }
            cleanup()
            Log.d(TAG, "Recording cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel recording", e)
        }
    }

    /**
     * Delete the output file.
     */
    private fun cleanup() {
        outputFile?.let { file ->
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Deleted recording file: ${file.absolutePath}")
            }
        }
        outputFile = null
    }

    /**
     * Check if currently recording.
     */
    fun isRecording() = isRecording

    /**
     * Get the output file path.
     */
    fun getOutputFilePath(): String? = outputFile?.absolutePath
}

