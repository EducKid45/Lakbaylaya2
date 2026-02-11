package com.example.lakbaylaya.ui.screens.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lakbaylaya.ui.screens.map.models.SearchResult
import com.example.lakbaylaya.ui.screens.map.models.SearchUiState

/**
 * Search overlay component with full-screen coverage
 *
 * Displays different content based on SearchUiState:
 * - Idle: Shows recent searches or empty prompt
 * - Typing: Shows autocomplete or recent searches
 * - Searching: Shows loading indicator
 * - Results: Shows search results list (max 10 items)
 * - Empty: Shows "no results" message
 * - Error: Shows error message
 *
 * Features:
 * - Only visible when search is active
 * - Solid background that completely hides map and bottom bar
 * - Content positioned for optimal viewing
 * - Fade-in/out animation
 * - Scrollable list of results
 *
 * @param isVisible Whether the overlay is visible (only when search is active)
 * @param searchUiState Current search UI state
 * @param recentSearches List of recent searches
 * @param onResultClick Callback when a result is clicked
 * @param onNavigateClick Callback when navigate button is clicked
 * @param onDismiss Callback when overlay should be dismissed
 * @param modifier Modifier for customization
 */
@Composable
fun SearchOverlay(
    isVisible: Boolean,
    searchUiState: SearchUiState,
    recentSearches: List<SearchResult>,
    onResultClick: (SearchResult) -> Unit,
    onNavigateClick: (SearchResult) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate navigation bar inset for bottom padding only
    val density = LocalDensity.current
    val navigationBarHeight = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }

    // Full-screen solid background that completely hides the map and bottom bar
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .semantics { testTag = "search_overlay" }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                // Minimal top padding - results start close to search bar
                top = 8.dp,
                start = 0.dp,
                end = 0.dp,
                bottom = navigationBarHeight + 80.dp // Navigation bar + extra padding for bottom navigation bar
            )
        ) {
            when (searchUiState) {
                is SearchUiState.Idle -> {
                    // Show recent searches only when empty (no text)
                    if (recentSearches.isNotEmpty()) {
                        item { SectionHeader(text = "Recent Searches") }
                        items(
                            items = recentSearches.take(10),
                            key = { "recent_${it.id}" }
                        ) { result ->
                            SearchResultItem(
                                result = result,
                                onClick = { onResultClick(result) },
                                onNavigateClick = { onNavigateClick(result) }
                            )
                        }
                    } else {
                        item {
                            EmptyStateContent(
                                message = "Search for places to visit",
                                icon = Icons.Default.SearchOff
                            )
                        }
                    }
                }

                is SearchUiState.Typing -> {
                    // Show nothing while typing - wait for search results
                    // Don't show recent searches when user is typing
                }

                is SearchUiState.Searching -> {
                    // Show loading indicator
                    item { LoadingStateContent(query = searchUiState.query) }
                }

                is SearchUiState.Results -> {
                    // Show search results (max 10 items)
                    item {
                        SectionHeader(
                            text = "Search Results",
                            subtitle = "${searchUiState.results.size} place${if (searchUiState.results.size != 1) "s" else ""} found"
                        )
                    }
                    items(
                        items = searchUiState.results.take(10),
                        key = { it.id }
                    ) { result ->
                        SearchResultItem(
                            result = result,
                            onClick = { onResultClick(result) },
                            onNavigateClick = { onNavigateClick(result) }
                        )
                    }
                }

                is SearchUiState.Empty -> {
                    // Show empty results message
                    item {
                        EmptyStateContent(
                            message = "No results found for \"${searchUiState.query}\"",
                            subtitle = "Try a different search term",
                            icon = Icons.Default.SearchOff
                        )
                    }
                }

                is SearchUiState.Error -> {
                    // Show error message
                    item { ErrorStateContent(message = searchUiState.message) }
                }
            }
        }
    }
}

/**
 * Section header for search results
 */
@Composable
private fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Loading state content
 */
@Composable
private fun LoadingStateContent(
    query: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(40.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Searching for \"$query\"...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Empty state content
 */
@Composable
private fun EmptyStateContent(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Error state content
 */
@Composable
private fun ErrorStateContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        )
        Text(
            text = "Search Error",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

