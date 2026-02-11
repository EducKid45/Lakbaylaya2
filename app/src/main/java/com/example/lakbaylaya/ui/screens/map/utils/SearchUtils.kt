package com.example.lakbaylaya.ui.screens.map.utils

import com.example.lakbaylaya.ui.screens.map.models.SearchResult

/**
 * Utility object for filtering and sorting search results
 */
object SearchUtils {

    /**
     * Filters search results based on query
     *
     * @param results List of all available results
     * @param query Search query string
     * @return Filtered list matching the query
     */
    fun filterResults(results: List<SearchResult>, query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        val normalizedQuery = query.trim().lowercase()
        return results.filter { result ->
            result.placeName.lowercase().contains(normalizedQuery) ||
            result.address.lowercase().contains(normalizedQuery) ||
            result.category.lowercase().contains(normalizedQuery)
        }
    }

    /**
     * Sorts search results by distance
     *
     * @param results List of search results to sort
     * @return Sorted list (nearest first)
     */
    fun sortByDistance(results: List<SearchResult>): List<SearchResult> {
        return results.sortedBy { it.distanceMeters }
    }

    /**
     * Limits search results to maximum count
     *
     * @param results List of search results
     * @param maxResults Maximum number of results (default 10)
     * @return Limited list
     */
    fun limitResults(results: List<SearchResult>, maxResults: Int = 10): List<SearchResult> {
        return results.take(maxResults)
    }
}
