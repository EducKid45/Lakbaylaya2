package com.example.lakbaylaya.ui.screens.map.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lakbaylaya.data.room.AppDatabase
import com.example.lakbaylaya.data.room.LandmarkEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing marker landmarks persistence.
 * Voice notes have been removed.
 */
class MarkerDataViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val landmarkDao = database.landmarkDao()

    val allLandmarks: Flow<List<LandmarkEntity>> = landmarkDao.getAllLandmarks()

    /**
     * Save a landmark marker to the database.
     */
    fun saveLandmark(
        latitude: Double,
        longitude: Double,
        locationName: String,
        routeDifficulty: String,
        description: String
    ) {
        viewModelScope.launch {
            val landmark = LandmarkEntity(
                id = System.currentTimeMillis().toString(),
                latitude = latitude,
                longitude = longitude,
                locationName = locationName,
                routeDifficulty = routeDifficulty,
                description = description
            )
            landmarkDao.insertLandmark(landmark)
        }
    }

    /** No-op kept for compatibility */
    @Suppress("UNUSED_PARAMETER")
    fun saveVoiceNote(
        latitude: Double, longitude: Double, locationName: String,
        audioFilePath: String, transcription: String, durationSeconds: Int
    ) { /* Voice notes removed */ }

    /** No-op kept for compatibility */
    @Suppress("UNUSED_PARAMETER")
    fun attachVoiceNoteToLandmark(landmarkId: String, voiceNoteId: String) { /* Voice notes removed */ }

    /** No-op kept for compatibility */
    @Suppress("UNUSED_PARAMETER")
    fun deleteVoiceNote(voiceNoteId: String) { /* Voice notes removed */ }
}
