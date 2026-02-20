package com.example.lakbaylaya.data.repository

import android.content.Context
import com.example.lakbaylaya.data.room.RoutesDatabase
import com.example.lakbaylaya.data.room.SavedRouteEntity
import com.example.lakbaylaya.ui.screens.route.SavedRoute
import com.example.lakbaylaya.ui.screens.map.models.DirectionStep
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * RoutesRepositoryRoom: Room-backed repository for saved routes.
 * Delegates to the Room DAO to persist and read SavedRoute entities.
 */
class RoutesRepositoryRoom(context: Context) : RoutesRepository {
    private val db = RoutesDatabase.getInstance(context)
    private val dao = db.routesDao()
    private val gson = Gson()

    /** Save or update a SavedRoute into Room. */
    override suspend fun saveRoute(route: SavedRoute): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = SavedRouteEntity(
                id = route.id.ifBlank { UUID.randomUUID().toString() },
                name = route.name,
                startLocation = route.startLocation,
                endLocation = route.endLocation,
                startLatitude = route.startLatitude,
                startLongitude = route.startLongitude,
                endLatitude = route.endLatitude,
                endLongitude = route.endLongitude,
                distanceKm = route.distanceKm,
                estimatedMinutes = route.estimatedMinutes,
                hasVoiceNotes = route.hasVoiceNotes,
                hasDifficultSegments = route.hasDifficultSegments,
                landmarksJson = gson.toJson(route.landmarks),
                voiceNoteCount = route.voiceNoteCount,
                difficultSegmentCount = route.difficultSegmentCount,
                polyline = route.polyline,
                routeStepsJson = route.routeSteps?.let { gson.toJson(it) },
                createdAt = route.createdAt
            )
            dao.insert(entity)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /** Read all saved routes from Room. */
    override suspend fun listRoutes(): Result<List<SavedRoute>> = withContext(Dispatchers.IO) {
        try {
            val entities = dao.getAll()
            val list = entities.map { e ->
                val landmarksType = object : TypeToken<List<String>>() {}.type
                val landmarks: List<String> = try {
                    gson.fromJson(e.landmarksJson, landmarksType) ?: emptyList()
                } catch (t: Throwable) {
                    emptyList()
                }

                val routeStepsType = object : TypeToken<List<DirectionStep>>() {}.type
                val routeSteps: List<DirectionStep>? = try {
                    e.routeStepsJson?.let { gson.fromJson(it, routeStepsType) }
                } catch (t: Throwable) {
                    null
                }

                SavedRoute(
                    id = e.id,
                    name = e.name,
                    startLocation = e.startLocation,
                    endLocation = e.endLocation,
                    startLatitude = e.startLatitude,
                    startLongitude = e.startLongitude,
                    endLatitude = e.endLatitude,
                    endLongitude = e.endLongitude,
                    distanceKm = e.distanceKm,
                    estimatedMinutes = e.estimatedMinutes,
                    hasVoiceNotes = e.hasVoiceNotes,
                    hasDifficultSegments = e.hasDifficultSegments,
                    landmarks = landmarks,
                    voiceNoteCount = e.voiceNoteCount,
                    difficultSegmentCount = e.difficultSegmentCount,
                    polyline = e.polyline,
                    routeSteps = routeSteps,
                    createdAt = e.createdAt
                )
            }
            Result.success(list)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /** Delete a saved route by id in Room. */
    override suspend fun deleteRoute(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.delete(id)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
