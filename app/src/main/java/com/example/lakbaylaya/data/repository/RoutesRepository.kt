package com.example.lakbaylaya.data.repository

import com.example.lakbaylaya.ui.screens.route.SavedRoute

/**
 * Persistence contract for saved/familiar routes.
 */
interface RoutesRepository {
    suspend fun saveRoute(route: SavedRoute): Result<Unit>
    suspend fun listRoutes(): Result<List<SavedRoute>>
    suspend fun deleteRoute(id: String): Result<Unit>
}

