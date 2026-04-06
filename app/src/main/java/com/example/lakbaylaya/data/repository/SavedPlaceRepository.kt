package com.example.lakbaylaya.data.repository

import com.example.lakbaylaya.ui.screens.route.SavedPlace
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for saved places
 */
interface SavedPlaceRepository {
    suspend fun savePlace(place: SavedPlace): Result<Unit>
    suspend fun listPlaces(): Result<List<SavedPlace>>
    fun observePlaces(): Flow<List<SavedPlace>>
    suspend fun getPlaceById(id: String): Result<SavedPlace?>
    suspend fun updatePlace(place: SavedPlace): Result<Unit>
    suspend fun deletePlace(id: String): Result<Unit>
    suspend fun getPlacesByLabel(label: String): Result<List<SavedPlace>>
    /** Returns the existing place if one already exists at these coordinates, else null. */
    suspend fun findByCoordinates(lat: Double, lon: Double): Result<SavedPlace?>
}
