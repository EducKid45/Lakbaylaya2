package com.example.lakbaylaya.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Simple single-row user profile entity. id is fixed to 0 so there's always at most one row.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 0,
    val name: String,
    val emergencyContactName: String,
    val emergencyContactNumber: String,
    val homeAddress: String
)

