package com.example.lakbaylaya.ui.screens.map.navigation.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Android implementation of TextToSpeechEngine using Android TTS API
 *
 * This class handles all TTS operations including initialization,
 * speech synthesis, and lifecycle management.
 */
class AndroidTextToSpeechEngine(
    private val context: Context
) : TextToSpeechEngine {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    companion object {
        private const val TAG = "AndroidTTS"
        private const val UTTERANCE_ID = "navigation_instruction"
    }

    /**
     * Initialize the TTS engine with the system's default language
     */
    override fun initialize(onReady: () -> Unit, onError: (String) -> Unit) {
        try {
            tts = TextToSpeech(context) { status ->
                when (status) {
                    TextToSpeech.SUCCESS -> {
                        tts?.let { engine ->
                            // Try to set English as the language
                            val result = engine.setLanguage(Locale.US)

                            if (result == TextToSpeech.LANG_MISSING_DATA ||
                                result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                Log.w(TAG, "English not fully supported, using default")
                            }

                            // Set up utterance listener for tracking speech events
                            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                                override fun onStart(utteranceId: String?) {
                                    Log.d(TAG, "TTS started speaking")
                                }

                                override fun onDone(utteranceId: String?) {
                                    Log.d(TAG, "TTS finished speaking")
                                }

                                @Deprecated("Deprecated in Java")
                                override fun onError(utteranceId: String?) {
                                    Log.e(TAG, "TTS error occurred")
                                }
                            })

                            isInitialized = true
                            onReady()
                        }
                    }
                    else -> {
                        val errorMsg = "TTS initialization failed with status: $status"
                        Log.e(TAG, errorMsg)
                        onError(errorMsg)
                    }
                }
            }
        } catch (e: Exception) {
            val errorMsg = "Exception during TTS initialization: ${e.message}"
            Log.e(TAG, errorMsg, e)
            onError(errorMsg)
        }
    }

    /**
     * Speak the given text with optional priority
     *
     * High priority interrupts current speech, low priority queues it
     */
    override fun speak(text: String, priority: Boolean) {
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS not initialized, cannot speak")
            return
        }

        try {
            val queueMode = if (priority) {
                TextToSpeech.QUEUE_FLUSH  // Interrupt current speech
            } else {
                TextToSpeech.QUEUE_ADD     // Queue after current speech
            }

            tts?.speak(text, queueMode, null, UTTERANCE_ID)
            Log.d(TAG, "Speaking: $text")
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text: ${e.message}", e)
        }
    }

    /**
     * Stop any ongoing speech immediately
     */
    override fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS: ${e.message}", e)
        }
    }

    /**
     * Check if TTS is currently speaking
     */
    override fun isSpeaking(): Boolean {
        return try {
            tts?.isSpeaking ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking speaking status: ${e.message}", e)
            false
        }
    }

    /**
     * Set speech rate (1.0 = normal speed)
     */
    override fun setSpeechRate(rate: Float) {
        try {
            tts?.setSpeechRate(rate)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting speech rate: ${e.message}", e)
        }
    }

    /**
     * Set speech pitch (1.0 = normal pitch)
     */
    override fun setPitch(pitch: Float) {
        try {
            tts?.setPitch(pitch)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting pitch: ${e.message}", e)
        }
    }

    /**
     * Release TTS resources
     * Must be called when done using TTS (e.g., in onDestroy)
     */
    override fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
            Log.d(TAG, "TTS shut down successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during TTS shutdown: ${e.message}", e)
        }
    }
}


