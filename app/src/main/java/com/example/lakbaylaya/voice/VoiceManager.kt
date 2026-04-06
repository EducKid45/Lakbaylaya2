package com.example.lakbaylaya.voice

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "VoiceManager"

/**
 * TTS wrapper with a speakThenDo() method that fires a callback only after
 * the utterance fully finishes playing. This prevents opening the mic while
 * TTS audio is still outputting (which causes ERROR_RECOGNIZER_BUSY or
 * the recognizer picking up TTS as speech input).
 */
class VoiceManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ready = false
    private val utteranceCounter = AtomicInteger(0)

    // All speakThenDo callbacks are dispatched here — guarantees main thread
    // because UtteranceProgressListener fires on an arbitrary TTS engine thread,
    // and SpeechRecognizer requires the main thread.
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile private var pendingCallback: (() -> Unit)? = null

    var language: Locale = Locale.getDefault()
        private set
    var rate: Float = 0.85f   // slightly slower than default for clarity
        private set
    var pitch: Float = 1.0f
        private set

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(language)
            ready = result == TextToSpeech.LANG_AVAILABLE ||
                    result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                    result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
            tts?.setSpeechRate(rate)
            tts?.setPitch(pitch)

            // Register utterance listener once so we can fire callbacks on completion
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                @Deprecated("Deprecated in API 21", ReplaceWith("onError(utteranceId, errorCode)"))
                override fun onError(utteranceId: String?) {
                    Log.w(TAG, "TTS utterance error id=$utteranceId")
                    firePendingCallback()
                }
                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "TTS done id=$utteranceId")
                    firePendingCallback()
                }
                // API 21+
                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.w(TAG, "TTS utterance error id=$utteranceId code=$errorCode")
                    firePendingCallback()
                }
            })
            Log.d(TAG, "TTS initialized ready=$ready")
        } else {
            Log.e(TAG, "TTS init failed: $status")
            ready = false
        }
    }

    @Suppress("unused") fun setLanguage(locale: Locale) { language = locale; tts?.setLanguage(locale) }
    @Suppress("unused") fun setRate(r: Float)           { rate = r;          tts?.setSpeechRate(r) }
    @Suppress("unused") fun setPitch(p: Float)          { pitch = p;         tts?.setPitch(p) }

    /**
     * Speak [text] and invoke [onDone] when the utterance finishes.
     * If TTS is not ready, [onDone] is called immediately so the caller
     * is never blocked.
     */
    fun speakThenDo(text: String, onDone: () -> Unit) {
        if (!ready || tts == null) {
            Log.w(TAG, "TTS not ready — calling onDone on main thread immediately")
            mainHandler.post(onDone)
            return
        }
        val id = "utt_${utteranceCounter.incrementAndGet()}"
        pendingCallback = onDone
        val params = Bundle().apply { putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id) }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, id)
        Log.d(TAG, "speakThenDo id=$id text='$text'")
    }

    /**
     * Speak [text], wait [pauseMs] milliseconds AFTER the utterance finishes, then
     * invoke [onDone]. Useful for chaining back-to-back announcements so the user
     * can clearly distinguish each sentence.
     */
    fun speakThenPauseThenDo(text: String, pauseMs: Long = 500L, onDone: () -> Unit) {
        speakThenDo(text) {
            mainHandler.postDelayed(onDone, pauseMs)
        }
    }

    /** Fire and clear speak without a completion callback. */
    fun speak(text: String) {
        if (!ready) Log.w(TAG, "TTS not ready, speaking anyway")
        val id = "utt_${utteranceCounter.incrementAndGet()}"
        val params = Bundle().apply { putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id) }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, id)
    }

    fun stop() {
        mainHandler.removeCallbacksAndMessages(null)
        pendingCallback = null
        tts?.stop()
    }

    fun shutdown() {
        mainHandler.removeCallbacksAndMessages(null)
        pendingCallback = null
        tts?.shutdown()
        tts = null
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking ?: false

    private fun firePendingCallback() {
        val cb = pendingCallback
        pendingCallback = null
        if (cb != null) {
            // Post to main thread — UtteranceProgressListener callbacks run on a
            // TTS background thread, but SpeechRecognizer must be called from main.
            mainHandler.post(cb)
        }
    }
}

