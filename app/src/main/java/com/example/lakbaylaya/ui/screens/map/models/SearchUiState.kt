package com.example.lakbaylaya.ui.screens.map.models

/**
 * Sealed class representing distinct search UI states
 *
 * This follows the State Pattern and provides type-safe state management
 * for the search feature, ensuring clear visual and logical separation
 * of different UI states.
 *
 * States:
 * - Idle: No search activity, can show recent searches or empty prompt
 * - Typing: User is actively typing, preparing to search
 * - Searching: Debounced query in progress (loading indicator)
 * - Results: Search completed with results
 * - Empty: Search completed but no results found
 * - Error: Search failed with error message
 */
sealed class SearchUiState {
    /**
     * Idle state - no search activity
     */
    data object Idle : SearchUiState()

    /**
     * Typing state - user is entering text but search hasn't triggered yet
     * @property query Current query text
     */
    data class Typing(val query: String) : SearchUiState()

    /**
     * Searching state - search request in progress
     * @property query Query being searched
     */
    data class Searching(val query: String) : SearchUiState()

    /**
     * Results state - search completed with results
     * @property query Search query
     * @property results List of search results
     */
    data class Results(
        val query: String,
        val results: List<SearchResult>
    ) : SearchUiState()

    /**
     * Empty state - search completed but no results
     * @property query Search query that returned no results
     */
    data class Empty(val query: String) : SearchUiState()

    /**
     * Error state - search failed
     * @property query Search query that failed
     * @property message Error message
     */
    data class Error(
        val query: String,
        val message: String
    ) : SearchUiState()
}
