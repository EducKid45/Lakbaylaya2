@file:Suppress("unused", "MemberVisibilityCanBePrivate")
package com.example.lakbaylaya.ui.screens.map.navigation.voicenote

import android.content.Context
import com.example.lakbaylaya.ui.screens.map.navigation.tts.TextToSpeechEngine

/**
 * Stub — voice notes have been removed from the app.
 * This class is kept as a no-op to avoid breaking any remaining call-sites.
 */
class VoiceNotePlayerManager(
    @Suppress("UNUSED_PARAMETER") context: Context,
    @Suppress("UNUSED_PARAMETER") private val ttsEngine: TextToSpeechEngine? = null
) {
    companion object {
        private const val TAG = "VoiceNotePlayer"
        const val MAX_VOICE_NOTES_PER_MARKER = 3
        const val PROXIMITY_RADIUS_METERS = 30.0
    }

    @Volatile var isPlaying = false
        private set
    @Volatile var currentNoteId: String? = null
        private set
    @Volatile var navigationIsSpeaking: Boolean = false

    fun checkProximityAndPlay(userLat: Double, userLon: Double) { /* no-op */ }
    fun stop() { isPlaying = false; currentNoteId = null }
    fun release() { stop() }
    suspend fun canAddVoiceNote(@Suppress("UNUSED_PARAMETER") landmarkId: String): Boolean = false
}
