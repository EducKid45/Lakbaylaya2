package com.example.lakbaylaya.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for landmark operations with voice note relationships.
 */
@Dao
interface LandmarkDao {
    @Query("SELECT * FROM landmarks ORDER BY timestamp DESC")
    fun getAllLandmarks(): Flow<List<LandmarkEntity>>

    @Query("SELECT * FROM landmarks WHERE id = :id")
    suspend fun getLandmarkById(id: String): LandmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLandmark(landmark: LandmarkEntity)

    @Update
    suspend fun updateLandmark(landmark: LandmarkEntity)

    @Delete
    suspend fun deleteLandmark(landmark: LandmarkEntity)

    @Transaction
    @Query("SELECT * FROM landmarks WHERE id = :landmarkId")
    suspend fun getLandmarkWithVoiceNotes(landmarkId: String): LandmarkWithVoiceNotes?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLandmarkVoiceNoteCrossRef(crossRef: LandmarkVoiceNoteCrossRef)

    @Query("DELETE FROM landmark_voice_note WHERE landmarkId = :landmarkId AND voiceNoteId = :voiceNoteId")
    suspend fun deleteLandmarkVoiceNoteCrossRef(landmarkId: String, voiceNoteId: String)

    @Query("SELECT * FROM voice_notes INNER JOIN landmark_voice_note ON voice_notes.id = landmark_voice_note.voiceNoteId WHERE landmark_voice_note.landmarkId = :landmarkId")
    fun getVoiceNotesForLandmark(landmarkId: String): Flow<List<VoiceNoteEntity>>
}

/**
 * Data class combining landmark with its associated voice notes.
 */
data class LandmarkWithVoiceNotes(
    @Embedded val landmark: LandmarkEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            LandmarkVoiceNoteCrossRef::class,
            parentColumn = "landmarkId",
            entityColumn = "voiceNoteId"
        )
    )
    val voiceNotes: List<VoiceNoteEntity>
)

