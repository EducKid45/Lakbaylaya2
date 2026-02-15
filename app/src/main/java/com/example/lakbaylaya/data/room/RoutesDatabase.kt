package com.example.lakbaylaya.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SavedRouteEntity::class,
        SavedPlaceEntity::class,
        CustomMarkerEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class RoutesDatabase : RoomDatabase() {
    abstract fun routesDao(): RoutesDao
    abstract fun savedPlaceDao(): SavedPlaceDao
    abstract fun customMarkerDao(): CustomMarkerDao

    companion object {
        private const val DB_NAME = "routes_room.db"
        @Volatile
        private var INSTANCE: RoutesDatabase? = null

        fun getInstance(context: Context): RoutesDatabase {
            return INSTANCE ?: synchronized(this) {
                val inst = Room.databaseBuilder(
                    context.applicationContext,
                    RoutesDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration() // Allow destructive migration for development
                    .build()
                INSTANCE = inst
                inst
            }
        }
    }
}
