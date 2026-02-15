package com.example.lakbaylaya.ui.screens.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lakbaylaya.data.repository.RoutesRepository
import com.example.lakbaylaya.data.repository.SavedPlaceRepository
import com.example.lakbaylaya.data.repository.CustomMarkerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Manages UI state and persistence operations for the Routes screen. */
class RoutesViewModel(
    private val routesRepository: RoutesRepository?,
    private val savedPlaceRepository: SavedPlaceRepository?,
    private val customMarkerRepository: CustomMarkerRepository?
) : ViewModel() {

    private val _uiState: MutableStateFlow<RoutesUiState> = MutableStateFlow(RoutesUiState())
    val uiState: StateFlow<RoutesUiState> = _uiState.asStateFlow()

    init {
        // Load all persisted data
        loadAllData()
    }

    private fun loadAllData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Load routes
            if (routesRepository != null) {
                val routesRes = routesRepository.listRoutes()
                if (routesRes.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        savedRoutes = routesRes.getOrDefault(emptyList())
                    )
                } else {
                    loadSampleRoutes()
                }
            } else {
                loadSampleRoutes()
            }

            // Load places
            if (savedPlaceRepository != null) {
                val placesRes = savedPlaceRepository.listPlaces()
                if (placesRes.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        savedPlaces = placesRes.getOrDefault(emptyList())
                    )
                }
            }

            // Load markers
            if (customMarkerRepository != null) {
                val markersRes = customMarkerRepository.listMarkers()
                if (markersRes.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        customMarkers = markersRes.getOrDefault(emptyList())
                    )
                }
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun loadSampleRoutes() {
        val sampleRoutes = listOf(
            SavedRoute(
                id = "1",
                name = "Home to School",
                startLocation = "Home",
                endLocation = "School",
                distanceKm = 1.2,
                estimatedMinutes = 15,
                hasVoiceNotes = true,
                hasDifficultSegments = true,
                landmarks = listOf("Coffee Shop", "Park Entrance", "Pedestrian Crossing"),
                voiceNoteCount = 3,
                difficultSegmentCount = 2
            ),
            SavedRoute(
                id = "2",
                name = "Home to Office",
                startLocation = "Home",
                endLocation = "Office",
                distanceKm = 2.5,
                estimatedMinutes = 30,
                hasVoiceNotes = true,
                hasDifficultSegments = false,
                landmarks = listOf("Bus Stop", "Main Street", "Office Building"),
                voiceNoteCount = 2,
                difficultSegmentCount = 0
            )
        )
        _uiState.value = _uiState.value.copy(savedRoutes = sampleRoutes)
    }

    /** Selects a route and shows the detail panel. */
    fun selectRoute(route: SavedRoute) {
        _uiState.value = _uiState.value.copy(
            selectedRoute = route,
            isDetailPanelVisible = true
        )
    }

    /** Closes the detail panel and clears the selection. */
    fun closeDetailPanel() {
        _uiState.value = _uiState.value.copy(
            selectedRoute = null,
            isDetailPanelVisible = false
        )
    }

    /** Shows the rename dialog. */
    fun showRenameDialog() {
        _uiState.value = _uiState.value.copy(isRenameDialogVisible = true)
    }

    /** Hides the rename dialog. */
    fun hideRenameDialog() {
        _uiState.value = _uiState.value.copy(isRenameDialogVisible = false)
    }

    /** Rename selected route and persist change.
     * @param newName New display name for the route.
     */
    fun renameRoute(newName: String) {
        val selectedRoute = _uiState.value.selectedRoute ?: return
        val updatedRoute = selectedRoute.copy(name = newName)
        val updatedRoutes = _uiState.value.savedRoutes.map {
            if (it.id == selectedRoute.id) updatedRoute else it
        }
        _uiState.value = _uiState.value.copy(
            savedRoutes = updatedRoutes,
            selectedRoute = updatedRoute,
            isRenameDialogVisible = false
        )

        // Persist rename if repository available
        viewModelScope.launch {
            if (routesRepository != null) {
                routesRepository.saveRoute(updatedRoute)
                // refresh
                val r = routesRepository.listRoutes()
                if (r.isSuccess) _uiState.value =
                    _uiState.value.copy(savedRoutes = r.getOrDefault(emptyList()))
            }
        }
    }

    /** Shows delete confirmation dialog. */
    fun showDeleteDialog() {
        _uiState.value = _uiState.value.copy(isDeleteDialogVisible = true)
    }

    /** Hides delete confirmation dialog. */
    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(isDeleteDialogVisible = false)
    }

    /** Delete the selected route and update persistence. */
    fun deleteRoute() {
        val selectedRoute = _uiState.value.selectedRoute ?: return
        viewModelScope.launch {
            if (routesRepository != null) {
                routesRepository.deleteRoute(selectedRoute.id)
                val listRes = routesRepository.listRoutes()
                if (listRes.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        savedRoutes = listRes.getOrDefault(emptyList()),
                        selectedRoute = null,
                        isDeleteDialogVisible = false,
                        isDetailPanelVisible = false
                    )
                    return@launch
                }
            }
            val updatedRoutes = _uiState.value.savedRoutes.filter { it.id != selectedRoute.id }
            _uiState.value = _uiState.value.copy(
                savedRoutes = updatedRoutes,
                selectedRoute = null,
                isDetailPanelVisible = false,
                isDeleteDialogVisible = false
            )
        }
    }

    /** Save the current route (or placeholder) into storage. */
    fun saveCurrentRoute() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val snapshotSource = _uiState.value.selectedRoute
            val routeToSave = snapshotSource ?: SavedRoute(
                id = java.util.UUID.randomUUID().toString(),
                name = "Saved Route ${System.currentTimeMillis()}",
                startLocation = "Unknown",
                endLocation = "Unknown",
                distanceKm = 0.0,
                estimatedMinutes = 0
            )

            if (routesRepository == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "No repository configured"
                )
                return@launch
            }

            val res = try {
                routesRepository.saveRoute(routeToSave)
            } catch (t: Throwable) {
                Result.failure<Unit>(t)
            }

            if (res.isSuccess) {
                val listRes = routesRepository.listRoutes()
                if (listRes.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        savedRoutes = listRes.getOrDefault(emptyList()),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = listRes.exceptionOrNull()?.localizedMessage
                            ?: "Saved but failed to load"
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Failed to save route"
                )
            }
        }
    }

    /** Play a sample voice note for a route (TODO: implement playback). */
    fun playVoiceNote(routeId: String) {
        // TODO: Integrate with audio playback system
    }

    /** Play difficulty warning for a route (TODO: implement TTS). */
    fun playDifficultyWarning(routeId: String) {
        // TODO: Integrate with TTS to announce difficulty warnings
    }

    /** Clears any UI error message. */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /** Delete a place by id. */
    fun deletePlace(placeId: String) {
        viewModelScope.launch {
            savedPlaceRepository?.deletePlace(placeId)
            // Reload places
            val placesRes = savedPlaceRepository?.listPlaces()
            if (placesRes?.isSuccess == true) {
                _uiState.value = _uiState.value.copy(
                    savedPlaces = placesRes.getOrDefault(emptyList())
                )
            }
        }
    }

    /** Delete a marker by id. */
    fun deleteMarker(markerId: String) {
        viewModelScope.launch {
            customMarkerRepository?.deleteMarker(markerId)
            // Reload markers
            val markersRes = customMarkerRepository?.listMarkers()
            if (markersRes?.isSuccess == true) {
                _uiState.value = _uiState.value.copy(
                    customMarkers = markersRes.getOrDefault(emptyList())
                )
            }
        }
    }

    // Voice command handlers
    fun executeVoiceCommand(command: String) {
        val normalizedCommand = command.lowercase().trim()

        when {
            normalizedCommand.contains("show familiar routes") -> {
                // Already on routes screen, no action needed
            }

            normalizedCommand.startsWith("open route") -> {
                val routeName = normalizedCommand.removePrefix("open route").trim()
                val route = _uiState.value.savedRoutes.find {
                    it.name.lowercase().contains(routeName)
                }
                route?.let { selectRoute(it) }
            }

            normalizedCommand.contains("save this route") -> {
                saveCurrentRoute()
            }

            normalizedCommand.startsWith("delete route") -> {
                val routeName = normalizedCommand.removePrefix("delete route").trim()
                val route = _uiState.value.savedRoutes.find {
                    it.name.lowercase().contains(routeName)
                }
                route?.let {
                    selectRoute(it)
                    showDeleteDialog()
                }
            }
        }
    }
}

/** Domain model for a saved route. */
data class SavedRoute(
    val id: String,
    val name: String,
    val startLocation: String,
    val endLocation: String,
    val startLatitude: Double = 0.0,
    val startLongitude: Double = 0.0,
    val endLatitude: Double = 0.0,
    val endLongitude: Double = 0.0,
    val distanceKm: Double,
    val estimatedMinutes: Int,
    val hasVoiceNotes: Boolean = false,
    val hasDifficultSegments: Boolean = false,
    val landmarks: List<String> = emptyList(),
    val voiceNoteCount: Int = 0,
    val difficultSegmentCount: Int = 0,
    val polyline: String? = null,
    val routeSteps: List<com.example.lakbaylaya.ui.screens.map.models.DirectionStep>? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/** UI state container for the Routes screen. */
data class RoutesUiState(
    val savedRoutes: List<SavedRoute> = emptyList(),
    val savedPlaces: List<SavedPlace> = emptyList(),
    val customMarkers: List<CustomMarker> = emptyList(),
    val selectedRoute: SavedRoute? = null,
    val isDetailPanelVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRenameDialogVisible: Boolean = false,
    val isDeleteDialogVisible: Boolean = false
)
