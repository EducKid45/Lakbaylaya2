package com.example.lakbaylaya.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

private const val TAG = "SpeechRecManager"

/**
 * Lightweight wrapper around Android SpeechRecognizer. Use callbacks to receive results.
 * Plays a short TTS cue when the recognizer is ready.
 */
class SpeechRecognitionManager(private val context: Context, private val callback: Callback) {
    interface Callback {
        fun onReadyForSpeech()
        fun onPartialResult(text: String)
        fun onFinalResult(text: String)
        fun onError(errorMessage: String)
    }

    private var recognizer: SpeechRecognizer? = null

    // Use local VoiceManager to announce "I'm listening" when ready
    private val voiceManager by lazy { VoiceManager(context.applicationContext) }

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    // Announce that recognizer is ready
                    try {
                        voiceManager.speak("I'm listening")
                    } catch (e: Exception) {
                        Log.w(TAG, "TTS announce failed: ${e.message}")
                    }
                    callback.onReadyForSpeech()
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    // Stop any ongoing TTS when speech ends
                    try {
                        voiceManager.stop()
                    } catch (e: Exception) {
                        Log.w(TAG, "TTS stop failed: ${e.message}")
                    }
                }

                override fun onError(error: Int) {
                    try {
                        voiceManager.stop()
                    } catch (e: Exception) {
                        Log.w(TAG, "TTS stop failed: ${e.message}")
                    }
                    callback.onError("Recognizer error: $error")
                }

                override fun onResults(results: Bundle?) {
                    // Ensure TTS is stopped when results arrive
                    try {
                        voiceManager.stop()
                    } catch (e: Exception) {
                        Log.w(TAG, "TTS stop failed: ${e.message}")
                    }
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    callback.onFinalResult(text)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches =
                        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    callback.onPartialResult(text)
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        } else {
            callback.onError("Speech recognition not available on device")
        }
    }

    fun startListening(language: String? = null) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            language?.let { putExtra(RecognizerIntent.EXTRA_LANGUAGE, it) }
        }
        recognizer?.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
        try {
            voiceManager.stop()
        } catch (e: Exception) {
            Log.w(TAG, "TTS stop failed: ${e.message}")
        }
    }

    fun cancel() {
        recognizer?.cancel()
        try {
            voiceManager.stop()
        } catch (e: Exception) {
            Log.w(TAG, "TTS stop failed: ${e.message}")
        }
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
        try {
            voiceManager.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "TTS shutdown failed: ${e.message}")
        }
    }
}
