package com.example.lakbaylaya.ui.screens.route

/**
 * Domain model for a saved place
 */
data class SavedPlace(
    val id: String,
    val placeName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val label: String, // e.g., "Home", "Work", "Favorite"
    val category: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

