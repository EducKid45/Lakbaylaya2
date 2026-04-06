package com.example.lakbaylaya.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

private const val TAG = "WakeWordEngine"

/** Normalized wake word — all lowercase, no spaces. */
@Suppress("unused")
const val WAKE_WORD = "listen"
private const val WAKE_WORD_NORMALIZED = "listen"

/**
 * WakeWordEngine — short-burst SpeechRecognizer for wake-word detection.
 *
 * Design:
 *  - Destroys and recreates the recognizer for every burst (cleanest way to reset on real devices).
 *  - Checks partial AND final results for the wake word.
 *  - On any error or non-match result → schedules next burst after [RESTART_DELAY_MS].
 *  - On detection → stops itself, fires [onWake] exactly once.
 *  - Thread: all operations run on the main thread via [handler].
 */
class WakeWordEngine(
    private val context: Context,
    private val onWake: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null

    @Volatile var isRunning = false
        private set

    companion object {
        private const val RESTART_DELAY_MS = 500L
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun start() {
        if (isRunning) {
            Log.d(TAG, "start() already running — ignored")
            return
        }
        Log.d(TAG, "start()")
        isRunning = true
        handler.post { startBurst() }
    }

    fun stop() {
        Log.d(TAG, "stop()")
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        destroy()
    }

    // ── Burst logic ───────────────────────────────────────────────────────────

    private fun startBurst() {
        if (!isRunning) return
        destroy()

        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(p: Bundle?) {
                Log.d(TAG, "ready")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "speech begun")
            }

            override fun onPartialResults(bundle: Bundle?) {
                if (!isRunning) return
                val list = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
                for (text in list) {
                    Log.d(TAG, "partial='$text'")
                    if (matchesWakeWord(text)) {
                        handleDetected()
                        return
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                if (!isRunning) return
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?: emptyList<String>()
                Log.d(TAG, "results=$list")
                if (list.any { matchesWakeWord(it) }) {
                    handleDetected()
                } else {
                    restartLater()
                }
            }

            override fun onError(error: Int) {
                if (!isRunning) return
                Log.d(TAG, "error=$error — will restart")
                restartLater()
            }

            override fun onEndOfSpeech() {}
            override fun onRmsChanged(db: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEvent(t: Int, p: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }

        try {
            recognizer?.startListening(intent)
            Log.d(TAG, "startListening() called")
        } catch (e: Exception) {
            Log.e(TAG, "startListening threw: ${e.message}")
            restartLater()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun handleDetected() {
        Log.d(TAG, "WAKE WORD DETECTED")
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        destroy()
        // Post so any pending recognizer callbacks complete before we fire onWake
        handler.post { onWake() }
    }

    private fun restartLater() {
        destroy()
        handler.postDelayed({ if (isRunning) startBurst() }, RESTART_DELAY_MS)
    }

    private fun destroy() {
        try { recognizer?.cancel()  } catch (_: Exception) {}
        try { recognizer?.destroy() } catch (_: Exception) {}
        recognizer = null
    }

    private fun matchesWakeWord(text: String): Boolean {
        val normalized = text.lowercase().replace("\\s+".toRegex(), "")
        val matched = normalized.contains(WAKE_WORD_NORMALIZED)
        if (matched) Log.d(TAG, "MATCH '$text' → '$normalized'")
        return matched
    }
}
