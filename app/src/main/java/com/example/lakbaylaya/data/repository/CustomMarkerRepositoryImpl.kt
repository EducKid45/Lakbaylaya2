package com.example.lakbaylaya.data.repository

import android.content.Context
import com.example.lakbaylaya.data.room.CustomMarkerEntity
import com.example.lakbaylaya.data.room.RoutesDatabase
import com.example.lakbaylaya.ui.screens.route.CustomMarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Room-backed implementation of CustomMarkerRepository
 */
class CustomMarkerRepositoryImpl(context: Context) : CustomMarkerRepository {
    private val db = RoutesDatabase.getInstance(context)
    private val dao = db.customMarkerDao()

    override suspend fun saveMarker(marker: CustomMarker): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val entity = CustomMarkerEntity(
                    id = marker.id.ifBlank { UUID.randomUUID().toString() },
                    latitude = marker.latitude,
                    longitude = marker.longitude,
                    label = marker.label,
                    description = marker.description,
                    createdAt = marker.createdAt
                )
                dao.insert(entity)
                Result.success(Unit)
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    override suspend fun listMarkers(): Result<List<CustomMarker>> = withContext(Dispatchers.IO) {
        try {
            val entities = dao.getAll()
            val markers = entities.map { e ->
                CustomMarker(
                    id = e.id,
                    latitude = e.latitude,
                    longitude = e.longitude,
                    label = e.label,
                    description = e.description,
                    createdAt = e.createdAt
                )
            }
            Result.success(markers)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun getMarkerById(id: String): Result<CustomMarker?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = dao.getById(id)
                val marker = entity?.let { e ->
                    CustomMarker(
                        id = e.id,
                        latitude = e.latitude,
                        longitude = e.longitude,
                        label = e.label,
                        description = e.description,
                        createdAt = e.createdAt
                    )
                }
                Result.success(marker)
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    override suspend fun deleteMarker(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.delete(id)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}

