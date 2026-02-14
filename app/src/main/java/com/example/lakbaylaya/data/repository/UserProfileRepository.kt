package com.example.lakbaylaya.data.repository

import android.content.Context
import android.util.Log
import com.example.lakbaylaya.data.room.AppDatabase
import com.example.lakbaylaya.data.room.UserProfileEntity
import kotlinx.coroutines.flow.Flow

class UserProfileRepository(private val db: AppDatabase) {
    private val dao = db.userProfileDao()

    suspend fun getProfile(): UserProfileEntity? = dao.getProfile()

    fun observeProfile(): Flow<UserProfileEntity?> = dao.observeProfile()

    suspend fun saveProfile(profile: UserProfileEntity) {
        Log.d(
            TAG,
            "saveProfile called: name='${profile.name}', emergencyName='${profile.emergencyContactName}', emergencyNumber='${profile.emergencyContactNumber}', home='${profile.homeAddress}'"
        )
        dao.upsert(profile)
        Log.d(TAG, "saveProfile completed")
    }

    suspend fun clear() = dao.clearProfile()

    companion object {
        private const val TAG = "UserProfileRepo"

        fun create(context: Context): UserProfileRepository {
            val db = AppDatabase.getInstance(context)
            return UserProfileRepository(db)
        }
    }
}
