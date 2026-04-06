package com.example.lakbaylaya.data.repository

import android.content.Context
import com.example.lakbaylaya.data.room.RoutesDatabase
import com.example.lakbaylaya.data.room.SavedPlaceEntity
import com.example.lakbaylaya.ui.screens.route.SavedPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Room-backed implementation of SavedPlaceRepository
 */
class SavedPlaceRepositoryImpl(context: Context) : SavedPlaceRepository {
    private val db  = RoutesDatabase.getInstance(context)
    private val dao = db.savedPlaceDao()

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun SavedPlaceEntity.toDomain() = SavedPlace(
        id = id, placeName = placeName, address = address,
        latitude = latitude, longitude = longitude,
        label = label, category = category, createdAt = createdAt
    )

    private fun SavedPlace.toEntity() = SavedPlaceEntity(
        id = id.ifBlank { UUID.randomUUID().toString() },
        placeName = placeName, address = address,
        latitude = latitude, longitude = longitude,
        label = label, category = category, createdAt = createdAt
    )

    // ── interface ─────────────────────────────────────────────────────────────

    override suspend fun savePlace(place: SavedPlace): Result<Unit> = withContext(Dispatchers.IO) {
        try { dao.insert(place.toEntity()); Result.success(Unit) }
        catch (t: Throwable) { Result.failure(t) }
    }

    override suspend fun listPlaces(): Result<List<SavedPlace>> = withContext(Dispatchers.IO) {
        try { Result.success(dao.getAll().map { it.toDomain() }) }
        catch (t: Throwable) { Result.failure(t) }
    }

    override fun observePlaces(): Flow<List<SavedPlace>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getPlaceById(id: String): Result<SavedPlace?> = withContext(Dispatchers.IO) {
        try { Result.success(dao.getById(id)?.toDomain()) }
        catch (t: Throwable) { Result.failure(t) }
    }

    override suspend fun updatePlace(place: SavedPlace): Result<Unit> = withContext(Dispatchers.IO) {
        try { dao.update(place.toEntity()); Result.success(Unit) }
        catch (t: Throwable) { Result.failure(t) }
    }

    override suspend fun deletePlace(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try { dao.delete(id); Result.success(Unit) }
        catch (t: Throwable) { Result.failure(t) }
    }

    override suspend fun getPlacesByLabel(label: String): Result<List<SavedPlace>> = withContext(Dispatchers.IO) {
        try { Result.success(dao.getByLabel(label).map { it.toDomain() }) }
        catch (t: Throwable) { Result.failure(t) }
    }

    override suspend fun findByCoordinates(lat: Double, lon: Double): Result<SavedPlace?> = withContext(Dispatchers.IO) {
        try { Result.success(dao.findByCoordinates(lat, lon)?.toDomain()) }
        catch (t: Throwable) { Result.failure(t) }
    }
}
