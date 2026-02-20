package com.example.lakbaylaya.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Main Room database for the app including user profiles, voice notes, and landmarks.
 */
@Database(
    entities = [
        UserProfileEntity::class,
        VoiceNoteEntity::class,
        LandmarkEntity::class,
        LandmarkVoiceNoteCrossRef::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun voiceNoteDao(): VoiceNoteDao
    abstract fun landmarkDao(): LandmarkDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lakbaylaya_db"
                )
                    .fallbackToDestructiveMigration(true) // Allow destructive migration for development
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

