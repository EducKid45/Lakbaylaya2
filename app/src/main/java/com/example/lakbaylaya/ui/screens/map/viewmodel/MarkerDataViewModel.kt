package com.example.lakbaylaya.ui.screens.map.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lakbaylaya.data.room.AppDatabase
import com.example.lakbaylaya.data.room.LandmarkEntity
import com.example.lakbaylaya.data.room.LandmarkVoiceNoteCrossRef
import com.example.lakbaylaya.data.room.VoiceNoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing marker landmarks and voice notes persistence.
 */
class MarkerDataViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val landmarkDao = database.landmarkDao()
    private val voiceNoteDao = database.voiceNoteDao()

    val allLandmarks: Flow<List<LandmarkEntity>> = landmarkDao.getAllLandmarks()
    val allVoiceNotes: Flow<List<VoiceNoteEntity>> = voiceNoteDao.getAllVoiceNotes()

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

    /**
     * Save a voice note to the database.
     */
    fun saveVoiceNote(
        latitude: Double,
        longitude: Double,
        locationName: String,
        audioFilePath: String,
        transcription: String,
        durationSeconds: Int
    ) {
        viewModelScope.launch {
            val voiceNote = VoiceNoteEntity(
                id = System.currentTimeMillis().toString(),
                latitude = latitude,
                longitude = longitude,
                locationName = locationName,
                audioFilePath = audioFilePath,
                transcription = transcription,
                durationSeconds = durationSeconds
            )
            voiceNoteDao.insertVoiceNote(voiceNote)
        }
    }

    /**
     * Attach a voice note to a landmark.
     */
    fun attachVoiceNoteToLandmark(landmarkId: String, voiceNoteId: String) {
        viewModelScope.launch {
            val crossRef = LandmarkVoiceNoteCrossRef(
                landmarkId = landmarkId,
                voiceNoteId = voiceNoteId
            )
            landmarkDao.insertLandmarkVoiceNoteCrossRef(crossRef)
        }
    }

    /**
     * Delete a voice note from the database.
     */
    fun deleteVoiceNote(voiceNoteId: String) {
        viewModelScope.launch {
            voiceNoteDao.deleteVoiceNoteById(voiceNoteId)
        }
    }

    /**
     * Get voice notes for a specific landmark.
     */
    fun getVoiceNotesForLandmark(landmarkId: String): Flow<List<VoiceNoteEntity>> {
        return landmarkDao.getVoiceNotesForLandmark(landmarkId)
    }
}

