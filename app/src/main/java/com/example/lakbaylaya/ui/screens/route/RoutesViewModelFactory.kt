package com.example.lakbaylaya.ui.screens.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lakbaylaya.data.repository.RoutesRepository
import com.example.lakbaylaya.data.repository.SavedPlaceRepository
import com.example.lakbaylaya.data.repository.CustomMarkerRepository

/** Factory that creates `RoutesViewModel` instances with provided repositories. */
class RoutesViewModelFactory(
    private val routesRepo: RoutesRepository?,
    private val placesRepo: SavedPlaceRepository?,
    private val markersRepo: CustomMarkerRepository?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RoutesViewModel(routesRepo, placesRepo, markersRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
