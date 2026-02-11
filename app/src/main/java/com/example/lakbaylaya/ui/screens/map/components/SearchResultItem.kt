package com.example.lakbaylaya.ui.screens.map.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lakbaylaya.ui.screens.map.models.PlaceIconType
import com.example.lakbaylaya.ui.screens.map.models.SearchResult
import com.example.lakbaylaya.ui.screens.map.utils.DistanceUtils

/**
 * Search result item component with enhanced design
 *
 * Displays a single search result with:
 * - Category-specific icon on the left
 * - Place name, address, and distance
 * - Navigate action button aligned to the corner
 * - Clean separators and consistent spacing
 * - Material ripple effect
 * - Full accessibility support
 * - Minimum 48dp touch targets
 *
 * @param result The search result to display
 * @param onClick Callback when item is clicked
 * @param onNavigateClick Callback when navigate button is clicked
 * @param modifier Modifier for customization
 */
@Composable
fun SearchResultItem(
    result: SearchResult,
    onClick: () -> Unit,
    onNavigateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedDistance = DistanceUtils.formatDistance(result.distanceMeters)
    val accessibilityDescription = DistanceUtils.formatDistanceForAccessibility(result.distanceMeters)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${result.placeName}, ${result.address}, $accessibilityDescription"
            },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .heightIn(min = 64.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon
            Icon(
                imageVector = getIconForType(result.iconType),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            // Place info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Place name
                Text(
                    text = result.placeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Distance
                Text(
                    text = formattedDistance,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                // Address
                if (result.address.isNotEmpty()) {
                    Text(
                        text = result.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Navigate action button
            IconButton(
                onClick = onNavigateClick,
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = "Navigate to ${result.placeName}"
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.NearMe,
                    contentDescription = "Navigate",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // Divider
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 0.5.dp
    )
}

/**
 * Maps PlaceIconType to Material Icons
 */
@Composable
private fun getIconForType(iconType: PlaceIconType): ImageVector {
    return when (iconType) {
        PlaceIconType.RESTAURANT -> Icons.Default.Restaurant
        PlaceIconType.PARK -> Icons.Default.Park
        PlaceIconType.MUSEUM -> Icons.Default.Museum
        PlaceIconType.SHOPPING -> Icons.Default.ShoppingBag
        PlaceIconType.HOTEL -> Icons.Default.Hotel
        PlaceIconType.HISTORIC -> Icons.Default.AccountBalance
        PlaceIconType.RELIGIOUS -> Icons.Default.Church
        PlaceIconType.ATTRACTION -> Icons.Default.Attractions
        PlaceIconType.TRANSPORT -> Icons.Default.DirectionsBus
        PlaceIconType.RECENT -> Icons.Default.History
        PlaceIconType.LOCATION -> Icons.Default.LocationOn
    }
}
