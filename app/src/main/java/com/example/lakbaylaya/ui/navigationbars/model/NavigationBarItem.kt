package com.example.lakbaylaya.ui.navigationbars.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Lightweight abstraction representing a navigation item used in top/bottom bars.
 * Use data-driven items in your composables to separate configuration from UI.
 */
sealed interface NavigationBarItem {
    val id: String
    val icon: ImageVector
    val selectedIcon: ImageVector?
    @get:StringRes
    val labelRes: Int
    @get:StringRes
    val contentDescriptionResSelected: Int?
    @get:StringRes
    val contentDescriptionResUnselected: Int?
}

/**
 * Simple concrete implementation for common navigation items.
 */
data class SimpleNavigationBarItem(
    override val id: String,
    override val icon: ImageVector,
    override val selectedIcon: ImageVector? = null,
    @param:StringRes override val labelRes: Int,
    @param:StringRes override val contentDescriptionResSelected: Int? = null,
    @param:StringRes override val contentDescriptionResUnselected: Int? = null,
) : NavigationBarItem

