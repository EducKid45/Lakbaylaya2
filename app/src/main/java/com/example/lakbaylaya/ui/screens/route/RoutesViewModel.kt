package com.example.lakbaylaya.ui.screens.route

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lakbaylaya.data.repository.CustomMarkerRepository
import com.example.lakbaylaya.data.repository.RoutesRepository
import com.example.lakbaylaya.data.repository.SavedPlaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Unified location type for the Saved Locations list. */
enum class LocationItemType { ROUTE, PLACE }

/**
 * Unified holder for a saved route or saved place displayed in the
 * Saved Locations list. Using a single model prevents two separate empty
 * states and simplifies the UI.
 */
data class LocationItem(
    val id: String,
    val name: String,
    val type: LocationItemType,
    val subTitle: String = "",           // start → end for routes, address for places
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val dateSaved: Long = System.currentTimeMillis()
)

// ── Extra UI state for per-place interactions ─────────────────────────────────

data class PlaceInteractionState(
    val expandedPlaceId: String? = null,
    val renamingPlaceId: String? = null,
    val duplicateCandidate: SavedPlace? = null,
    val incomingPlace: SavedPlace? = null
)

/** Manages UI state and persistence operations for the Routes screen. */
class RoutesViewModel(
    private val routesRepository: RoutesRepository?,
    private val savedPlaceRepository: SavedPlaceRepository?,
    private val customMarkerRepository: CustomMarkerRepository?,
    private val appContext: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutesUiState())
    val uiState: StateFlow<RoutesUiState> = _uiState.asStateFlow()

    private val _placeInteraction = MutableStateFlow(PlaceInteractionState())
    val placeInteraction: StateFlow<PlaceInteractionState> = _placeInteraction.asStateFlow()

    init {
        observePlaces()
        loadRoutes()
        loadMarkers()
    }

    // ── Live DB observe for Places ────────────────────────────────────────────

    private fun observePlaces() {
        savedPlaceRepository ?: return
        viewModelScope.launch {
            savedPlaceRepository.observePlaces().collect { places ->
                _uiState.value = _uiState.value.copy(savedPlaces = places)
            }
        }
    }

    // ── Routes (one-shot load) ────────────────────────────────────────────────

    private fun loadRoutes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            if (routesRepository != null) {
                val res = routesRepository.listRoutes()
                if (res.isSuccess) {
                    _uiState.value = _uiState.value.copy(savedRoutes = res.getOrDefault(emptyList()))
                } else {
                    loadSampleRoutes()
                }
            } else {
                loadSampleRoutes()
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun loadMarkers() {
        viewModelScope.launch {
            if (customMarkerRepository != null) {
                val res = customMarkerRepository.listMarkers()
                if (res.isSuccess) {
                    _uiState.value = _uiState.value.copy(customMarkers = res.getOrDefault(emptyList()))
                }
            }
        }
    }

    private fun loadSampleRoutes() {
        _uiState.value = _uiState.value.copy(
            savedRoutes = listOf(
                SavedRoute(
                    id = "1", name = "Home to School",
                    startLocation = "Home", endLocation = "School",
                    distanceKm = 1.2, estimatedMinutes = 15,
                    hasVoiceNotes = true, hasDifficultSegments = true,
                    landmarks = listOf("Coffee Shop", "Park Entrance", "Pedestrian Crossing"),
                    voiceNoteCount = 3, difficultSegmentCount = 2
                )
            )
        )
    }

    // ── Saved Places: expand/collapse ─────────────────────────────────────────

    /** Toggle expand for a place card. Collapsing is done by tapping the same id again. */
    fun togglePlaceExpand(placeId: String) {
        val current = _placeInteraction.value.expandedPlaceId
        _placeInteraction.value = _placeInteraction.value.copy(
            expandedPlaceId = if (current == placeId) null else placeId
        )
    }

    // ── Saved Places: rename ──────────────────────────────────────────────────

    fun startRenamePlace(placeId: String) {
        _placeInteraction.value = _placeInteraction.value.copy(renamingPlaceId = placeId)
    }

    fun cancelRenamePlace() {
        _placeInteraction.value = _placeInteraction.value.copy(renamingPlaceId = null)
    }

    fun confirmRenamePlace(placeId: String, newName: String, newLabel: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val existing = _uiState.value.savedPlaces.find { it.id == placeId } ?: return@launch
            val updated = existing.copy(
                placeName = newName.trim(),
                label = newLabel.trim().ifBlank { existing.label }
            )
            savedPlaceRepository?.updatePlace(updated)
            // observePlaces() will refresh the list automatically
            _placeInteraction.value = _placeInteraction.value.copy(renamingPlaceId = null)
        }
    }

    // ── Saved Places: delete ──────────────────────────────────────────────────

    fun deletePlace(placeId: String) {
        viewModelScope.launch {
            savedPlaceRepository?.deletePlace(placeId)
            // observePlaces() refreshes automatically
            if (_placeInteraction.value.expandedPlaceId == placeId) {
                _placeInteraction.value = _placeInteraction.value.copy(expandedPlaceId = null)
            }
        }
    }

    // ── Saved Places: save with duplicate detection ───────────────────────────

    /**
     * Save a place — checks for duplicate coordinates first.
     * If a duplicate exists, the caller should show the overwrite dialog
     * by observing [placeInteraction.duplicateCandidate].
     */
    fun savePlaceWithDuplicateCheck(newPlace: SavedPlace) {
        viewModelScope.launch {
            val dupResult = savedPlaceRepository?.findByCoordinates(newPlace.latitude, newPlace.longitude)
            val existing = dupResult?.getOrNull()
            if (existing != null) {
                // Signal UI to show overwrite dialog
                _placeInteraction.value = _placeInteraction.value.copy(
                    duplicateCandidate = existing,
                    incomingPlace = newPlace
                )
            } else {
                savedPlaceRepository?.savePlace(newPlace)
            }
        }
    }

    fun confirmOverwritePlace() {
        val incoming = _placeInteraction.value.incomingPlace ?: return
        val dup = _placeInteraction.value.duplicateCandidate ?: return
        viewModelScope.launch {
            // Overwrite: delete old, save new with same id as existing so the row is replaced
            savedPlaceRepository?.deletePlace(dup.id)
            savedPlaceRepository?.savePlace(incoming.copy(id = dup.id))
            _placeInteraction.value = _placeInteraction.value.copy(
                duplicateCandidate = null, incomingPlace = null
            )
        }
    }

    fun cancelOverwritePlace() {
        _placeInteraction.value = _placeInteraction.value.copy(
            duplicateCandidate = null, incomingPlace = null
        )
    }

    // ── Routes ────────────────────────────────────────────────────────────────

    fun selectRoute(route: SavedRoute) {
        _uiState.value = _uiState.value.copy(selectedRoute = route, isDetailPanelVisible = true)
    }

    fun closeDetailPanel() {
        _uiState.value = _uiState.value.copy(selectedRoute = null, isDetailPanelVisible = false)
    }

    fun showRenameDialog() { _uiState.value = _uiState.value.copy(isRenameDialogVisible = true) }
    fun hideRenameDialog() { _uiState.value = _uiState.value.copy(isRenameDialogVisible = false) }

    fun renameRoute(newName: String) {
        val route = _uiState.value.selectedRoute ?: return
        val updated = route.copy(name = newName)
        _uiState.value = _uiState.value.copy(
            savedRoutes = _uiState.value.savedRoutes.map { if (it.id == route.id) updated else it },
            selectedRoute = updated,
            isRenameDialogVisible = false
        )
        viewModelScope.launch { routesRepository?.saveRoute(updated) }
    }

    fun showDeleteDialog() { _uiState.value = _uiState.value.copy(isDeleteDialogVisible = true) }
    fun hideDeleteDialog() { _uiState.value = _uiState.value.copy(isDeleteDialogVisible = false) }

    fun deleteRoute() {
        val route = _uiState.value.selectedRoute ?: return
        viewModelScope.launch {
            routesRepository?.deleteRoute(route.id)
            _uiState.value = _uiState.value.copy(
                savedRoutes = _uiState.value.savedRoutes.filter { it.id != route.id },
                selectedRoute = null, isDetailPanelVisible = false, isDeleteDialogVisible = false
            )
        }
    }

    fun saveCurrentRoute() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val routeToSave = _uiState.value.selectedRoute ?: SavedRoute(
                id = java.util.UUID.randomUUID().toString(),
                name = "Saved Route ${System.currentTimeMillis()}",
                startLocation = "Unknown", endLocation = "Unknown",
                distanceKm = 0.0, estimatedMinutes = 0
            )
            if (routesRepository == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "No repository configured")
                return@launch
            }
            val res = try { routesRepository.saveRoute(routeToSave) } catch (t: Throwable) { Result.failure(t) }
            if (res.isSuccess) {
                val listRes = routesRepository.listRoutes()
                _uiState.value = _uiState.value.copy(
                    savedRoutes = listRes.getOrDefault(emptyList()), isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Failed to save route"
                )
            }
        }
    }

    fun playVoiceNote(routeId: String) { /* TODO: TTS */ }
    fun playDifficultyWarning(routeId: String) { /* TODO: TTS */ }
    fun clearError() { _uiState.value = _uiState.value.copy(errorMessage = null) }

    // ── Markers ───────────────────────────────────────────────────────────────

    fun deleteMarker(markerId: String) {
        viewModelScope.launch {
            customMarkerRepository?.deleteMarker(markerId)
            val res = customMarkerRepository?.listMarkers()
            if (res?.isSuccess == true) {
                _uiState.value = _uiState.value.copy(customMarkers = res.getOrDefault(emptyList()))
            }
        }
    }

    // ── Voice commands ────────────────────────────────────────────────────────

    fun executeVoiceCommand(command: String) {
        val n = command.lowercase().trim()
        when {
            n.contains("show familiar routes") -> { /* already on screen */ }
            n.startsWith("open route") -> {
                val name = n.removePrefix("open route").trim()
                _uiState.value.savedRoutes.find { it.name.lowercase().contains(name) }
                    ?.let { selectRoute(it) }
            }
            n.contains("save this route") -> saveCurrentRoute()
            n.startsWith("delete route") -> {
                val name = n.removePrefix("delete route").trim()
                _uiState.value.savedRoutes.find { it.name.lowercase().contains(name) }
                    ?.let { selectRoute(it); showDeleteDialog() }
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
) {
    /**
     * Unified list of saved routes and places sorted by date saved (newest first).
     * Used by the refactored "Saved Locations" tab.
     */
    val savedLocations: List<LocationItem>
        get() {
            val routeItems = savedRoutes.map { r ->
                LocationItem(
                    id        = r.id,
                    name      = r.name,
                    type      = LocationItemType.ROUTE,
                    subTitle  = if (r.startLocation.isNotBlank() && r.endLocation.isNotBlank())
                                    "${r.startLocation} → ${r.endLocation}" else "",
                    latitude  = r.endLatitude,
                    longitude = r.endLongitude,
                    dateSaved = r.createdAt
                )
            }
            val placeItems = savedPlaces.map { p ->
                LocationItem(
                    id        = p.id,
                    name      = p.placeName,
                    type      = LocationItemType.PLACE,
                    subTitle  = p.address,
                    latitude  = p.latitude,
                    longitude = p.longitude,
                    dateSaved = p.createdAt
                )
            }
            return (routeItems + placeItems).sortedByDescending { it.dateSaved }
        }
}
