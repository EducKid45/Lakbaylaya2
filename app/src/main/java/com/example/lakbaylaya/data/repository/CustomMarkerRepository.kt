package com.example.lakbaylaya.data.repository

import com.example.lakbaylaya.ui.screens.route.CustomMarker

/**
 * Repository interface for custom markers
 */
interface CustomMarkerRepository {
    suspend fun saveMarker(marker: CustomMarker): Result<Unit>
    suspend fun listMarkers(): Result<List<CustomMarker>>
    suspend fun getMarkerById(id: String): Result<CustomMarker?>
    suspend fun deleteMarker(id: String): Result<Unit>
}

