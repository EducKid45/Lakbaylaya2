package com.example.lakbaylaya.ui.screens.map.utils

import com.example.lakbaylaya.ui.screens.map.models.PlaceIconType

/**
 * Utility object for mapping place categories to icon types
 *
 * This provides intelligent icon selection based on place category,
 * improving visual hierarchy and user recognition.
 */
object CategoryIconMapper {

    /**
     * Maps a place category string to the appropriate PlaceIconType
     *
     * @param category Place category string (case-insensitive)
     * @param isRecent Whether this is a recent search
     * @return Appropriate PlaceIconType
     */
    fun getCategoryIcon(category: String, isRecent: Boolean = false): PlaceIconType {
        if (isRecent) return PlaceIconType.RECENT

        return when {
            category.contains("restaurant", ignoreCase = true) ||
            category.contains("food", ignoreCase = true) ||
            category.contains("cafe", ignoreCase = true) ||
            category.contains("dining", ignoreCase = true) -> PlaceIconType.RESTAURANT

            category.contains("park", ignoreCase = true) ||
            category.contains("garden", ignoreCase = true) ||
            category.contains("nature", ignoreCase = true) -> PlaceIconType.PARK

            category.contains("museum", ignoreCase = true) ||
            category.contains("gallery", ignoreCase = true) -> PlaceIconType.MUSEUM

            category.contains("shopping", ignoreCase = true) ||
            category.contains("mall", ignoreCase = true) ||
            category.contains("store", ignoreCase = true) -> PlaceIconType.SHOPPING

            category.contains("hotel", ignoreCase = true) ||
            category.contains("accommodation", ignoreCase = true) ||
            category.contains("lodging", ignoreCase = true) -> PlaceIconType.HOTEL

            category.contains("historic", ignoreCase = true) ||
            category.contains("heritage", ignoreCase = true) ||
            category.contains("monument", ignoreCase = true) -> PlaceIconType.HISTORIC

            category.contains("church", ignoreCase = true) ||
            category.contains("temple", ignoreCase = true) ||
            category.contains("mosque", ignoreCase = true) ||
            category.contains("religious", ignoreCase = true) -> PlaceIconType.RELIGIOUS

            category.contains("attraction", ignoreCase = true) ||
            category.contains("tourist", ignoreCase = true) ||
            category.contains("sightseeing", ignoreCase = true) -> PlaceIconType.ATTRACTION

            category.contains("transport", ignoreCase = true) ||
            category.contains("station", ignoreCase = true) ||
            category.contains("terminal", ignoreCase = true) -> PlaceIconType.TRANSPORT

            else -> PlaceIconType.LOCATION
        }
    }
}
