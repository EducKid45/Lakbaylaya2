package com.example.lakbaylaya.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.lakbaylaya.ui.screens.route.SavedRoute
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/** File-level: lightweight SQLite-backed implementation of RoutesRepository used for legacy data.
 * Implements simple CRUD on a single `routes` table and stores landmarks as JSON.
 */

private const val DB_NAME = "routes.db"
private const val DB_VERSION = 1
private const val TABLE_ROUTES = "routes"

private object Columns {
    const val ID = "id"
    const val NAME = "name"
    const val START = "start"
    const val END = "end"
    const val DISTANCE = "distance"
    const val ETA = "eta"
    const val VOICE_NOTES = "voice_notes"
    const val DIFFICULT = "difficult"
    const val LANDMARKS = "landmarks"
    const val VOICE_NOTE_COUNT = "voice_note_count"
    const val DIFFICULT_COUNT = "difficult_count"
    const val POLYLINE = "polyline"
}

/** Simple SQLiteOpenHelper that creates the `routes` table. */
private class RoutesDbHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        val sql = buildString {
            append("CREATE TABLE $TABLE_ROUTES (")
            append("${Columns.ID} TEXT PRIMARY KEY, ")
            append("${Columns.NAME} TEXT, ")
            append("${Columns.START} TEXT, ")
            append("${Columns.END} TEXT, ")
            append("${Columns.DISTANCE} REAL, ")
            append("${Columns.ETA} INTEGER, ")
            append("${Columns.VOICE_NOTES} INTEGER, ")
            append("${Columns.DIFFICULT} INTEGER, ")
            append("${Columns.LANDMARKS} TEXT, ")
            append("${Columns.VOICE_NOTE_COUNT} INTEGER, ")
            append("${Columns.DIFFICULT_COUNT} INTEGER, ")
            append("${Columns.POLYLINE} TEXT")
            append(")")
        }
        db.execSQL(sql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No-op for v1
    }
}

/**
 * Lightweight, file-backed RoutesRepository implementation.
 * Methods are suspending and run on Dispatchers.IO.
 */
class RoutesRepositorySqlite(private val context: Context) : RoutesRepository {
    private val gson = Gson()
    private val dbHelper by lazy { RoutesDbHelper(context.applicationContext) }

    /** Save or update a SavedRoute into the legacy sqlite table. */
    override suspend fun saveRoute(route: SavedRoute): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(Columns.ID, route.id.ifBlank { UUID.randomUUID().toString() })
                put(Columns.NAME, route.name)
                put(Columns.START, route.startLocation)
                put(Columns.END, route.endLocation)
                put(Columns.DISTANCE, route.distanceKm)
                put(Columns.ETA, route.estimatedMinutes)
                put(Columns.VOICE_NOTES, if (route.hasVoiceNotes) 1 else 0)
                put(Columns.DIFFICULT, if (route.hasDifficultSegments) 1 else 0)
                put(Columns.LANDMARKS, gson.toJson(route.landmarks))
                put(Columns.VOICE_NOTE_COUNT, route.voiceNoteCount)
                put(Columns.DIFFICULT_COUNT, route.difficultSegmentCount)
                put(Columns.POLYLINE, route.polyline)
            }
            db.insertWithOnConflict(TABLE_ROUTES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /** Read all saved routes from the legacy sqlite table. */
    override suspend fun listRoutes(): Result<List<SavedRoute>> = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.readableDatabase
            val cursor = db.query(TABLE_ROUTES, null, null, null, null, null, null)
            val list = mutableListOf<SavedRoute>()
            cursor.use { c ->
                if (c.moveToFirst()) {
                    do {
                        val id = c.getString(c.getColumnIndexOrThrow(Columns.ID))
                        val name = c.getString(c.getColumnIndexOrThrow(Columns.NAME))
                        val start = c.getString(c.getColumnIndexOrThrow(Columns.START))
                        val end = c.getString(c.getColumnIndexOrThrow(Columns.END))
                        val distance = c.getDouble(c.getColumnIndexOrThrow(Columns.DISTANCE))
                        val eta = c.getInt(c.getColumnIndexOrThrow(Columns.ETA))
                        val hasVoice = c.getInt(c.getColumnIndexOrThrow(Columns.VOICE_NOTES)) == 1
                        val hasDifficult = c.getInt(c.getColumnIndexOrThrow(Columns.DIFFICULT)) == 1
                        val landmarksJson = c.getString(c.getColumnIndexOrThrow(Columns.LANDMARKS))
                        val voiceCount = c.getInt(c.getColumnIndexOrThrow(Columns.VOICE_NOTE_COUNT))
                        val difficultCount =
                            c.getInt(c.getColumnIndexOrThrow(Columns.DIFFICULT_COUNT))
                        val polyline = try {
                            c.getString(c.getColumnIndexOrThrow(Columns.POLYLINE))
                        } catch (_: Exception) {
                            null
                        }

                        val landmarksType = object : TypeToken<List<String>>() {}.type
                        val landmarks: List<String> = try {
                            gson.fromJson(landmarksJson, landmarksType) ?: emptyList()
                        } catch (t: Throwable) {
                            emptyList()
                        }

                        list.add(
                            SavedRoute(
                                id = id,
                                name = name,
                                startLocation = start,
                                endLocation = end,
                                distanceKm = distance,
                                estimatedMinutes = eta,
                                hasVoiceNotes = hasVoice,
                                hasDifficultSegments = hasDifficult,
                                landmarks = landmarks,
                                voiceNoteCount = voiceCount,
                                difficultSegmentCount = difficultCount,
                                polyline = polyline
                            )
                        )
                    } while (c.moveToNext())
                }
            }
            Result.success(list)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /** Delete a saved route by id. */
    override suspend fun deleteRoute(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            db.delete(TABLE_ROUTES, "${Columns.ID} = ?", arrayOf(id))
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
