package com.example.lakbaylaya.utils.voice

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

/**
 * Lightweight wrapper around Android SpeechRecognizer to provide a start/stop API with timeout and
 * simple callbacks for partial and final transcripts.
 *
 * Usage:
 * val recognizer = VoiceRecognizer(context)
 * recognizer.startListening(onResult = { text, isFinal -> ... }, maxDurationMs = 5000)
 * recognizer.stopListening()
 */
class VoiceRecognizer(
    private val context: android.content.Context
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var handler: Handler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null

    private var onResultCallback: ((String, Boolean) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    private var isListening = false

    fun isListening(): Boolean = isListening

    fun startListening(
        maxDurationMs: Long = 5000L,
        languageModel: String = RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
        languageTag: String? = null,
        onResult: (String, Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        val appContext = context.applicationContext
        if (SpeechRecognizer.isRecognitionAvailable(appContext).not()) {
            onError("Speech recognition not available")
            return
        }

        stopListening() // ensure previous instance stopped

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d("VoiceRecognizer", "ready for speech")
                }

                override fun onBeginningOfSpeech() {
                    Log.d("VoiceRecognizer", "beginning of speech")
                }

                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    Log.d("VoiceRecognizer", "end of speech")
                    // we'll wait for onResults/onError
                }

                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                        else -> "Speech recognition error: $error"
                    }
                    onError(message)
                    stopListening()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.joinToString(separator = " ") ?: ""
                    if (text.isBlank()) {
                        Log.d("VoiceRecognizer", "onResults but no match")
                        onError("No recognized speech")
                    } else {
                        Log.d("VoiceRecognizer", "onResults: $text")
                        onResult(text, true)
                    }
                    stopListening()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches =
                        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.joinToString(separator = " ") ?: ""
                    if (text.isNotBlank()) {
                        Log.d("VoiceRecognizer", "onPartialResults: $text")
                        onResult(text, false)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        onResultCallback = onResult
        onErrorCallback = onError

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, languageModel)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            try {
                // Prefer explicit languageTag if provided for testing, otherwise use device default
                val lang = languageTag ?: Locale.getDefault().toString()
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
                // Try to improve silence handling (some engines respect these extras)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 700L)
                putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                    500L
                )
            } catch (t: Throwable) {
                Log.w("VoiceRecognizer", "Failed to set locale on intent: ${t.message}")
            }
        }

        try {
            // Use application context to avoid leaking activity; recognizer uses telephony services internally
            speechRecognizer?.startListening(intent)
            isListening = true

            // Schedule automatic stop after maxDurationMs
            stopRunnable = Runnable {
                try {
                    speechRecognizer?.cancel()
                } catch (ignored: Throwable) {
                }
                isListening = false
                onError("Timeout")
                stopRunnable = null
            }
            handler.postDelayed(stopRunnable!!, maxDurationMs)
        } catch (t: Throwable) {
            Log.e("VoiceRecognizer", "startListening failed: ${t.message}", t)
            onError("Failed to start listening: ${t.message}")
            stopListening()
        }
    }

    fun stopListening() {
        try {
            stopRunnable?.let { handler.removeCallbacks(it) }
            stopRunnable = null
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (t: Throwable) {
            // ignore
        } finally {
            speechRecognizer = null
            isListening = false
        }
    }
}
