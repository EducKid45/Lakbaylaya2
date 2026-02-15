package com.example.lakbaylaya.data.repository

import com.example.lakbaylaya.ui.screens.route.SavedPlace

/**
 * Repository interface for saved places
 */
interface SavedPlaceRepository {
    suspend fun savePlace(place: SavedPlace): Result<Unit>
    suspend fun listPlaces(): Result<List<SavedPlace>>
    suspend fun getPlaceById(id: String): Result<SavedPlace?>
    suspend fun deletePlace(id: String): Result<Unit>
    suspend fun getPlacesByLabel(label: String): Result<List<SavedPlace>>
}

