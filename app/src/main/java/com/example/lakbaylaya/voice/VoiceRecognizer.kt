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

private const val TAG = "VoiceRecognizer"

/**
 * VoiceRecognizer — single-shot command listener.
 *
 * Key design decisions after Xiaomi/MIUI diagnosis:
 *
 *  1. Silence gate is ONLY started when actual text arrives (onPartialResults or onResults).
 *     Previously it started on onBeginningOfSpeech — but on Xiaomi MIUI, onPartialResults
 *     never fires, so the gate fired 2s after speech began with an empty lastPartial → error.
 *
 *  2. onResults always delivers immediately (gate not needed if no partials arrived).
 *
 *  3. onError with blank lastPartial → deliver error immediately, no waiting.
 *
 *  4. "No speech" safety timer: if onBeginningOfSpeech never fires within NO_SPEECH_MS,
 *     we deliver error so the dialog retries instead of hanging at the max timeout.
 */
class VoiceRecognizer(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())

    private var onFinalCallback: ((String) -> Unit)? = null
    private var onErrorCallback: (() -> Unit)?       = null

    private var lastPartial         = ""
    private var silenceTimerPending = false
    private var speechHasBegun      = false   // true once onBeginningOfSpeech fires

    private val silenceToken  = Any()
    private val timeoutToken  = Any()
    private val noSpeechToken = Any()

    // How long to wait for speech to begin before giving up
    private val NO_SPEECH_MS = 8_000L

    // Standard command window
    private val SILENCE_MS      = 1_500L
    private val MAX_DURATION_MS = 20_000L

    // Dialog window — all non-search dialog steps
    private val DIALOG_SILENCE_MS      = 1_500L
    private val DIALOG_MAX_DURATION_MS = 30_000L

    // Search window — place name input; longer max so user has time to speak
    private val SEARCH_SILENCE_MS      = 1_500L   // gate only starts AFTER text arrives
    private val SEARCH_MAX_DURATION_MS = 25_000L

    // ── Public API ────────────────────────────────────────────────────────────

    fun listen(onFinal: (String) -> Unit, onError: () -> Unit) =
        listenInternal(onFinal, onError, SILENCE_MS, MAX_DURATION_MS, forSearch = false)

    fun listenDialog(onFinal: (String) -> Unit, onError: () -> Unit) =
        listenInternal(onFinal, onError, DIALOG_SILENCE_MS, DIALOG_MAX_DURATION_MS, forSearch = false)

    /**
     * Used for place/destination input.
     * WEB_SEARCH model + online + gate only starts when text arrives.
     */
    fun listenSearch(onFinal: (String) -> Unit, onError: () -> Unit) =
        listenInternal(onFinal, onError, SEARCH_SILENCE_MS, SEARCH_MAX_DURATION_MS, forSearch = true)

    // ── Core ──────────────────────────────────────────────────────────────────

    private fun listenInternal(
        onFinal: (String) -> Unit,
        onError: () -> Unit,
        silenceMs: Long,
        maxDurationMs: Long,
        forSearch: Boolean
    ) {
        destroy()
        onFinalCallback     = onFinal
        onErrorCallback     = onError
        lastPartial         = ""
        silenceTimerPending = false
        speechHasBegun      = false

        Log.d(TAG, "listen() silenceMs=$silenceMs maxMs=$maxDurationMs forSearch=$forSearch")

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(p: Bundle?) {
                Log.d(TAG, "ready")
                // Safety: if no speech begins within NO_SPEECH_MS, deliver error
                handler.postDelayed({
                    if (!speechHasBegun) {
                        Log.w(TAG, "no speech began — timeout")
                        cancelTimers()
                        deliverError()
                    }
                }, noSpeechToken, NO_SPEECH_MS)
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "speech begun")
                speechHasBegun = true
                handler.removeCallbacksAndMessages(noSpeechToken)
                // ← Do NOT start silence gate here.
                //   Gate is only started when actual TEXT arrives (onPartialResults / onResults).
                //   On Xiaomi/MIUI, onPartialResults never fires — so onResults will handle it.
            }

            override fun onPartialResults(partial: Bundle?) {
                val text = partial
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.trim() ?: ""
                if (text.isBlank()) return
                Log.d(TAG, "partial: '$text'")
                if (text.length >= lastPartial.length) lastPartial = text
                // Start/restart gate only now that we have real text
                postSilenceGate(silenceMs)
            }

            override fun onResults(results: Bundle?) {
                // Collect ALL candidates — pick the longest non-blank one
                val candidates = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()
                val best = candidates.maxByOrNull { it.length } ?: ""
                Log.d(TAG, "onResults candidates=$candidates lastPartial='$lastPartial'")

                val final = bestOf(best, lastPartial)
                if (final.isNotBlank()) {
                    // We have text — cancel gate and deliver immediately
                    cancelTimers()
                    deliverFinal(final)
                } else if (silenceTimerPending) {
                    // Gate running but still nothing — cancel and error
                    cancelTimers()
                    deliverError()
                } else {
                    cancelTimers()
                    deliverError()
                }
            }

            override fun onError(error: Int) {
                Log.w(TAG, "onError=$error lastPartial='$lastPartial' gatePending=$silenceTimerPending")
                when {
                    lastPartial.isNotBlank() -> {
                        // Have partial text — cancel gate, deliver it now
                        cancelTimers()
                        deliverFinal(lastPartial)
                    }
                    else -> {
                        // Nothing at all — cancel everything and report error immediately
                        cancelTimers()
                        deliverError()
                    }
                }
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech")
                // If we already have partial text, let the gate handle delivery.
                // If no text yet, Android will fire onResults shortly — wait for it.
            }

            override fun onRmsChanged(db: Float)         {}
            override fun onBufferReceived(b: ByteArray?)  {}
            override fun onEvent(t: Int, p: Bundle?)      {}
        })

        val languageModel = if (forSearch)
            RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH
        else
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, languageModel)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            if (forSearch) {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-PH", "fil-PH"))
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            }
            // These hints are honoured on some devices; on MIUI they're often ignored —
            // our gate is the authoritative timer.
            putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", silenceMs)
            putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", silenceMs)
            putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 300L)
        }

        try {
            speechRecognizer?.startListening(intent)
            Log.d(TAG, "startListening() called")
            // Absolute ceiling — delivers whatever we have
            handler.postDelayed({
                Log.w(TAG, "hard timeout — lastPartial='$lastPartial'")
                cancelTimers()
                if (lastPartial.isNotBlank()) deliverFinal(lastPartial) else deliverError()
            }, timeoutToken, maxDurationMs)
        } catch (e: Exception) {
            Log.e(TAG, "startListening threw: ${e.message}")
            deliverError()
        }
    }

    // ── Silence gate ──────────────────────────────────────────────────────────

    private fun postSilenceGate(silenceMs: Long) {
        handler.removeCallbacksAndMessages(silenceToken)
        silenceTimerPending = true
        handler.postDelayed({
            silenceTimerPending = false
            Log.d(TAG, "silence gate fired — delivering '$lastPartial'")
            if (lastPartial.isNotBlank()) deliverFinal(lastPartial) else deliverError()
        }, silenceToken, silenceMs)
    }

    private fun bestOf(a: String, b: String): String = when {
        a.isNotBlank() && a.length >= b.length -> a
        b.isNotBlank()                          -> b
        a.isNotBlank()                          -> a
        else                                    -> ""
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun destroy() {
        cancelTimers()
        handler.removeCallbacksAndMessages(noSpeechToken)
        silenceTimerPending = false
        speechHasBegun      = false
        lastPartial         = ""
        onFinalCallback     = null
        onErrorCallback     = null
        try { speechRecognizer?.cancel()  } catch (_: Exception) {}
        try { speechRecognizer?.destroy() } catch (_: Exception) {}
        speechRecognizer = null
        Log.d(TAG, "destroyed")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun deliverFinal(text: String) {
        val cb = onFinalCallback
        destroy()
        Log.d(TAG, "deliverFinal: '$text'")
        cb?.invoke(text)
    }

    private fun deliverError() {
        val cb = onErrorCallback
        destroy()
        Log.d(TAG, "deliverError()")
        cb?.invoke()
    }

    private fun cancelTimers() {
        handler.removeCallbacksAndMessages(silenceToken)
        handler.removeCallbacksAndMessages(timeoutToken)
        silenceTimerPending = false
    }
}