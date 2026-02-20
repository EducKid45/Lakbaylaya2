package com.example.lakbaylaya.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

private const val TAG = "VoiceManager"

/**
 * Minimal TTS manager. Initialize with application Context.
 * This class wraps Android TextToSpeech and exposes simple controls.
 */
class VoiceManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ready: Boolean = false

    var language: Locale = Locale.getDefault()
        private set

    var rate: Float = 1.0f
        private set

    var pitch: Float = 1.0f
        private set

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(language)
            ready =
                result == TextToSpeech.LANG_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
            tts?.setSpeechRate(rate)
            tts?.setPitch(pitch)
            Log.d(TAG, "TTS initialized, ready=$ready")
        } else {
            Log.e(TAG, "TTS initialization failed: $status")
            ready = false
        }
    }

    fun setLanguage(locale: Locale) {
        language = locale
        tts?.setLanguage(locale)
    }

    fun setRate(r: Float) {
        rate = r
        tts?.setSpeechRate(rate)
    }

    fun setPitch(p: Float) {
        pitch = p
        tts?.setPitch(pitch)
    }

    fun speak(text: String) {
        if (!ready) {
            Log.w(TAG, "TTS not ready, attempting to speak anyway")
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "UTTERANCE_ID")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }

    fun isSpeaking(): Boolean {
        return tts?.isSpeaking ?: false
    }
}

