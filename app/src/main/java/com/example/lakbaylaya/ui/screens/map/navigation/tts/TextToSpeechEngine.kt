package com.example.lakbaylaya.ui.screens.map.navigation.tts

/**
 * Interface for Text-to-Speech operations
 *
 * Provides abstraction over the TTS implementation, allowing for
 * testing and alternative implementations.
 */
interface TextToSpeechEngine {

    /**
     * Initialize the TTS engine
     *
     * @param onReady Callback invoked when TTS is ready to use
     * @param onError Callback invoked if TTS initialization fails
     */
    fun initialize(onReady: () -> Unit = {}, onError: (String) -> Unit = {})

    /**
     * Speak the given text
     *
     * @param text Text to speak
     * @param priority Priority of this utterance (true = interrupt current speech)
     */
    fun speak(text: String, priority: Boolean = false)

    /**
     * Stop current speech
     */
    fun stop()

    /**
     * Check if TTS is currently speaking
     */
    fun isSpeaking(): Boolean

    /**
     * Set speech rate
     *
     * @param rate Speech rate (1.0 = normal, 0.5 = half speed, 2.0 = double speed)
     */
    fun setSpeechRate(rate: Float)

    /**
     * Set speech pitch
     *
     * @param pitch Speech pitch (1.0 = normal)
     */
    fun setPitch(pitch: Float)

    /**
     * Release TTS resources
     * Must be called when done using TTS
     */
    fun shutdown()
}

