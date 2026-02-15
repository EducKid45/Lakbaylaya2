package com.example.lakbaylaya.ui.screens.route

/**
 * Domain model for a custom marker
 */
data class CustomMarker(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val label: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

