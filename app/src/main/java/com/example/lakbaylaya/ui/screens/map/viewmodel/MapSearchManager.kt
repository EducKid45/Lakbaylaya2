package com.example.lakbaylaya.ui.screens.map.viewmodel

import com.example.lakbaylaya.data.repository.MapRepository
import com.example.lakbaylaya.ui.screens.map.models.*
import com.example.lakbaylaya.utils.CategoryIconMapper
import com.example.lakbaylaya.utils.Debouncer
import com.example.lakbaylaya.utils.DistanceUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Responsible for search-related logic extracted from MapViewModel:
 * - Debounced search handling
 * - Calling repository.searchPlaces
 * - Transforming results (distance, icons, sorting)
 * - Managing recent searches list helper
 */
class MapSearchManager(
    private val repository: MapRepository,
    private val state: MutableStateFlow<MapState>,
    private val coroutineScope: CoroutineScope,
    debounceDelayMs: Long = 300L
) {

    companion object {
        private const val MIN_SEARCH_QUERY_LENGTH = 3
        private const val MAX_RECENT_SEARCHES = 10
        private const val MAX_SEARCH_RESULTS = 10
    }

    private val debouncer =
        Debouncer(delayMillis = debounceDelayMs, coroutineScope = coroutineScope)

    /** Called by UI to update search text and trigger debounced searches. */
    fun onSearchQueryChange(query: String) {
        state.update { it.copy(searchQuery = query) }

        // Cancel any pending search
        debouncer.cancel()

        when {
            query.isEmpty() -> {
                state.update { it.copy(searchUiState = SearchUiState.Idle) }
            }

            query.length < MIN_SEARCH_QUERY_LENGTH -> {
                state.update { it.copy(searchUiState = SearchUiState.Typing(query)) }
            }

            else -> {
                state.update { it.copy(searchUiState = SearchUiState.Typing(query)) }
                debouncer.debounce {
                    performSearch(query)
                }
            }
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank() || query.length < MIN_SEARCH_QUERY_LENGTH) return

        state.update { it.copy(searchUiState = SearchUiState.Searching(query)) }

        coroutineScope.launch {
            val currentLocation = state.value.currentLocation
            val apiResult = repository.searchPlaces(
                query = query,
                latitude = currentLocation?.latitude,
                longitude = currentLocation?.longitude,
                limit = MAX_SEARCH_RESULTS
            )

            apiResult.onSuccess { rawResults ->
                val transformedResults = transformSearchResults(rawResults, currentLocation)

                val newState = if (transformedResults.isEmpty()) {
                    SearchUiState.Empty(query)
                } else {
                    SearchUiState.Results(query, transformedResults)
                }
                state.update { it.copy(searchUiState = newState) }
            }.onFailure { error ->
                state.update {
                    it.copy(
                        searchUiState = SearchUiState.Error(
                            query = query,
                            message = error.message ?: "Failed to search places"
                        )
                    )
                }
            }
        }
    }

    private fun transformSearchResults(
        results: List<SearchResult>,
        userLocation: Location?
    ): List<SearchResult> {
        if (userLocation == null) return results.take(MAX_SEARCH_RESULTS)

        return results
            .map { result ->
                val accurateDistance = DistanceUtils.calculateDistance(
                    lat1 = userLocation.latitude,
                    lon1 = userLocation.longitude,
                    lat2 = result.latitude,
                    lon2 = result.longitude
                )

                val iconType = CategoryIconMapper.getCategoryIcon(
                    category = result.category,
                    isRecent = result.isRecent
                )

                result.copy(
                    distanceMeters = accurateDistance,
                    iconType = iconType
                )
            }
            .sortedBy { it.distanceMeters }
            .take(MAX_SEARCH_RESULTS)
    }

    /** Adds a result to recent searches (max MAX_RECENT_SEARCHES). Returns updated list. */
    fun addToRecentSearches(result: SearchResult): List<SearchResult> {
        val currentRecent = state.value.recentSearches
        val filtered = currentRecent.filterNot { it.id == result.id }
        val recentResult = result.copy(isRecent = true, iconType = PlaceIconType.RECENT)
        return listOf(recentResult) + filtered.take(MAX_RECENT_SEARCHES - 1)
    }

    fun cancelPending() {
        debouncer.cancel()
    }
}
