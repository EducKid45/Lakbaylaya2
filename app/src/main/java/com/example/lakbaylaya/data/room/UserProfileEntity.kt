package com.example.lakbaylaya.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Simple single-row user profile entity. id is fixed to 0 so there's always at most one row.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 0,
    val name: String = "",
    val phoneNumber: String = "",
    val emergencyContactName: String = "",
    val emergencyContactNumber: String = "",
    val emergencyMessage: String = "I need help. Please call me or send assistance to my location.",
    val homeAddress: String = "",
    val homeLat: Double = 0.0,
    val homeLon: Double = 0.0,
    val workAddress: String = "",
    val workLat: Double = 0.0,
    val workLon: Double = 0.0,
    val autoSendArrivalNotification: Boolean = true
)

