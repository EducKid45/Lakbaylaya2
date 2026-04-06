package com.example.lakbaylaya.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Main Room database for the app including user profiles and landmarks.
 * Voice notes have been removed.
 */
@Database(
    entities = [
        UserProfileEntity::class,
        LandmarkEntity::class,
        NavigationStatsEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun landmarkDao(): LandmarkDao
    abstract fun navigationStatsDao(): NavigationStatsDao

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
