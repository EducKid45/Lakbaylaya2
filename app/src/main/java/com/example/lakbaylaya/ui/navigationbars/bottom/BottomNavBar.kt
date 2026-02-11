package com.example.lakbaylaya.ui.navigationbars.bottom

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lakbaylaya.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import com.example.lakbaylaya.ui.navigationbars.nav.NavRoutes
import com.example.lakbaylaya.ui.navigationbars.model.NavigationBarItem
import com.example.lakbaylaya.ui.navigationbars.model.SimpleNavigationBarItem
import com.example.lakbaylaya.ui.navigationbars.system.SystemBarDefaults
import com.example.lakbaylaya.ui.navigationbars.system.systemBottomBarPadding
import com.example.lakbaylaya.ui.navigationbars.top.contentDescriptionForItem

/**
 * Bottom Navigation Bar for main screen navigation
 *
 * Screens: Home | Map | Route | Profile
 *
 * Design:
 * - Fixed at bottom
 * - Icon + text label for each item
 * - Rounded tinted background highlights selected item
 * - Consistent width for highlight
 *
 * Animations:
 * - Background tint fade-in (spring)
 *
 * Accessibility:
 * - TalkBack announces "selected" or "not selected"
 * - Focus order: Home  Map  Route  Profile
 * - Touch targets  48dp
 * - High contrast colors
 *
 * Responsive:
 * - Adapts to screen width
 * - Truncates text on narrow screens
 *
 * @param currentRoute The currently selected route
 * @param onNavigate Callback when navigation item is clicked
 */

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        SimpleNavigationBarItem(
            id = NavRoutes.Home.route,
            icon = Icons.Default.Home,
            labelRes = R.string.nav_home,
            contentDescriptionResSelected = R.string.nav_home_selected,
            contentDescriptionResUnselected = R.string.nav_home_not_selected
        ),
        SimpleNavigationBarItem(
            id = NavRoutes.Map.route,
            icon = Icons.Default.Map,
            labelRes = R.string.nav_map,
            contentDescriptionResSelected = R.string.nav_map_selected,
            contentDescriptionResUnselected = R.string.nav_map_not_selected
        ),
        SimpleNavigationBarItem(
            id = NavRoutes.Route.route,
            icon = Icons.Default.Route,
            labelRes = R.string.nav_route,
            contentDescriptionResSelected = R.string.nav_route_selected,
            contentDescriptionResUnselected = R.string.nav_route_not_selected
        ),
        SimpleNavigationBarItem(
            id = NavRoutes.Profile.route,
            icon = Icons.Default.Person,
            labelRes = R.string.nav_profile,
            contentDescriptionResSelected = R.string.nav_profile_selected,
            contentDescriptionResUnselected = R.string.nav_profile_not_selected
        )
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .systemBottomBarPadding()
            .height(SystemBarDefaults.BottomBarMinHeight + 24.dp) // keep original visual height (~80dp)
    ) {
        // NavigationBar provides a RowScope receiver here, so apply weight in this scope
        items.forEach { item ->
            BottomNavItemView(
                itemLabel = stringResource(item.labelRes),
                item = item,
                isSelected = currentRoute == item.id,
                onClick = { onNavigate(item.id) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomNavItemView(
    itemLabel: String,
    item: NavigationBarItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier // accept modifier so weight can be applied by caller in RowScope
) {
    val alpha by animateFloatAsState(
        targetValue = if (isSelected) 0.22f else 0f,
        animationSpec = spring()
    )

    val backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha)

    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    // compute content description string in composable context
    val computedContentDescription = contentDescriptionForItem(item, isSelected) ?: itemLabel

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .animateContentSize()
            .selectable(selected = isSelected, onClick = onClick)
            .padding(vertical = 4.dp) // reduce vertical padding to tighten icon+label spacing
            .semantics {
                contentDescription = computedContentDescription
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center, // center items vertically to remove large gaps
            modifier = Modifier // remove fillMaxHeight so Column wraps its content
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(SystemBarDefaults.IconSize)
            )

            Spacer(modifier = Modifier.height(2.dp)) // small spacer to separate icon and text

            Text(
                text = itemLabel,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
