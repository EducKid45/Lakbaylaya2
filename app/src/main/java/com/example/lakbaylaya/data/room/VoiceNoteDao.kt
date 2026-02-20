package com.example.lakbaylaya.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for voice note operations.
 */
@Dao
interface VoiceNoteDao {
    @Query("SELECT * FROM voice_notes ORDER BY timestamp DESC")
    fun getAllVoiceNotes(): Flow<List<VoiceNoteEntity>>

    @Query("SELECT * FROM voice_notes WHERE id = :id")
    suspend fun getVoiceNoteById(id: String): VoiceNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceNote(voiceNote: VoiceNoteEntity)

    @Update
    suspend fun updateVoiceNote(voiceNote: VoiceNoteEntity)

    @Delete
    suspend fun deleteVoiceNote(voiceNote: VoiceNoteEntity)

    @Query("DELETE FROM voice_notes WHERE id = :id")
    suspend fun deleteVoiceNoteById(id: String)
}

