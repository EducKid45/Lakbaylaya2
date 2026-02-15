package com.example.lakbaylaya.data.repository

import android.content.Context
import com.example.lakbaylaya.data.room.RoutesDatabase
import com.example.lakbaylaya.data.room.SavedPlaceEntity
import com.example.lakbaylaya.ui.screens.route.SavedPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Room-backed implementation of SavedPlaceRepository
 */
class SavedPlaceRepositoryImpl(context: Context) : SavedPlaceRepository {
    private val db = RoutesDatabase.getInstance(context)
    private val dao = db.savedPlaceDao()

    override suspend fun savePlace(place: SavedPlace): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = SavedPlaceEntity(
                id = place.id.ifBlank { UUID.randomUUID().toString() },
                placeName = place.placeName,
                address = place.address,
                latitude = place.latitude,
                longitude = place.longitude,
                label = place.label,
                category = place.category,
                createdAt = place.createdAt
            )
            dao.insert(entity)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun listPlaces(): Result<List<SavedPlace>> = withContext(Dispatchers.IO) {
        try {
            val entities = dao.getAll()
            val places = entities.map { e ->
                SavedPlace(
                    id = e.id,
                    placeName = e.placeName,
                    address = e.address,
                    latitude = e.latitude,
                    longitude = e.longitude,
                    label = e.label,
                    category = e.category,
                    createdAt = e.createdAt
                )
            }
            Result.success(places)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun getPlaceById(id: String): Result<SavedPlace?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = dao.getById(id)
                val place = entity?.let { e ->
                    SavedPlace(
                        id = e.id,
                        placeName = e.placeName,
                        address = e.address,
                        latitude = e.latitude,
                        longitude = e.longitude,
                        label = e.label,
                        category = e.category,
                        createdAt = e.createdAt
                    )
                }
                Result.success(place)
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    override suspend fun deletePlace(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.delete(id)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun getPlacesByLabel(label: String): Result<List<SavedPlace>> =
        withContext(Dispatchers.IO) {
            try {
                val entities = dao.getByLabel(label)
                val places = entities.map { e ->
                    SavedPlace(
                        id = e.id,
                        placeName = e.placeName,
                        address = e.address,
                        latitude = e.latitude,
                        longitude = e.longitude,
                        label = e.label,
                        category = e.category,
                        createdAt = e.createdAt
                    )
                }
                Result.success(places)
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }
}

