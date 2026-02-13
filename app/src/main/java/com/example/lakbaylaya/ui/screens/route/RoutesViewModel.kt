package com.example.lakbaylaya.ui.screens.route

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents a saved/familiar route
 */
data class SavedRoute(
    val id: String,
    val name: String,
    val startLocation: String,
    val endLocation: String,
    val distanceKm: Double,
    val estimatedMinutes: Int,
    val hasVoiceNotes: Boolean = false,
    val hasDifficultSegments: Boolean = false,
    val landmarks: List<String> = emptyList(),
    val voiceNoteCount: Int = 0,
    val difficultSegmentCount: Int = 0
)

/**
 * UI state for the Routes screen
 */
data class RoutesUiState(
    val savedRoutes: List<SavedRoute> = emptyList(),
    val selectedRoute: SavedRoute? = null,
    val isDetailPanelVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRenameDialogVisible: Boolean = false,
    val isDeleteDialogVisible: Boolean = false
)

/**
 * ViewModel for managing saved/familiar routes
 */
class RoutesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RoutesUiState())
    val uiState: StateFlow<RoutesUiState> = _uiState.asStateFlow()

    init {
        // Load sample routes for demonstration
        loadSampleRoutes()
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
            ),
            SavedRoute(
                id = "3",
                name = "Home to Grocery Store",
                startLocation = "Home",
                endLocation = "Grocery Store",
                distanceKm = 0.8,
                estimatedMinutes = 10,
                hasVoiceNotes = false,
                hasDifficultSegments = true,
                landmarks = listOf("Corner Store", "Traffic Light"),
                voiceNoteCount = 0,
                difficultSegmentCount = 1
            ),
            SavedRoute(
                id = "4",
                name = "Home to Park",
                startLocation = "Home",
                endLocation = "City Park",
                distanceKm = 1.0,
                estimatedMinutes = 12,
                hasVoiceNotes = false,
                hasDifficultSegments = false,
                landmarks = listOf("Fountain", "Playground"),
                voiceNoteCount = 0,
                difficultSegmentCount = 0
            )
        )
        _uiState.value = _uiState.value.copy(savedRoutes = sampleRoutes)
    }

    fun selectRoute(route: SavedRoute) {
        _uiState.value = _uiState.value.copy(
            selectedRoute = route,
            isDetailPanelVisible = true
        )
    }

    fun closeDetailPanel() {
        _uiState.value = _uiState.value.copy(
            selectedRoute = null,
            isDetailPanelVisible = false
        )
    }

    fun showRenameDialog() {
        _uiState.value = _uiState.value.copy(isRenameDialogVisible = true)
    }

    fun hideRenameDialog() {
        _uiState.value = _uiState.value.copy(isRenameDialogVisible = false)
    }

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
    }

    fun showDeleteDialog() {
        _uiState.value = _uiState.value.copy(isDeleteDialogVisible = true)
    }

    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(isDeleteDialogVisible = false)
    }

    fun deleteRoute() {
        val selectedRoute = _uiState.value.selectedRoute ?: return
        val updatedRoutes = _uiState.value.savedRoutes.filter { it.id != selectedRoute.id }
        _uiState.value = _uiState.value.copy(
            savedRoutes = updatedRoutes,
            selectedRoute = null,
            isDetailPanelVisible = false,
            isDeleteDialogVisible = false
        )
    }

    fun saveCurrentRoute() {
        // TODO: Integrate with navigation system to save current active route
        _uiState.value = _uiState.value.copy(
            errorMessage = "Route saved successfully"
        )
    }

    fun playVoiceNote(routeId: String) {
        // TODO: Integrate with audio playback system
    }

    fun playDifficultyWarning(routeId: String) {
        // TODO: Integrate with TTS to announce difficulty warnings
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
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
