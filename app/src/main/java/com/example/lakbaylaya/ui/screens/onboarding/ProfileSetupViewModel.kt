package com.example.lakbaylaya.ui.screens.onboarding

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lakbaylaya.data.repository.UserProfileRepository
import com.example.lakbaylaya.data.room.UserProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileSetupViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = UserProfileRepository.create(application.applicationContext)

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _emergencyName = MutableStateFlow("")
    val emergencyName: StateFlow<String> = _emergencyName.asStateFlow()

    private val _emergencyNumber = MutableStateFlow("")
    val emergencyNumber: StateFlow<String> = _emergencyNumber.asStateFlow()

    private val _home = MutableStateFlow("")
    val home: StateFlow<String> = _home.asStateFlow()

    companion object {
        private const val TAG = "ProfileSetupVM"
    }

    init {
        // load saved profile if any
        viewModelScope.launch {
            val p = repo.getProfile()
            p?.let {
                _name.value = it.name
                _emergencyName.value = it.emergencyContactName
                _emergencyNumber.value = it.emergencyContactNumber
                _home.value = it.homeAddress
            }
        }
    }

    fun onNameChange(v: String) {
        _name.value = v
    }

    fun onEmergencyNameChange(v: String) {
        _emergencyName.value = v
    }

    fun onEmergencyNumberChange(v: String) {
        _emergencyNumber.value = v
    }

    fun onHomeChange(v: String) {
        _home.value = v
    }

    fun saveProfile(onSaved: () -> Unit = {}) {
        // Name must not be blank when saving
        val nameVal = _name.value.trim()
        if (nameVal.isBlank()) {
            Log.w(TAG, "saveProfile called but name is blank - aborting save")
            return
        }

        viewModelScope.launch {
            try {
                Log.d(
                    TAG,
                    "Saving profile: name='$nameVal', emergencyName='${_emergencyName.value.trim()}', emergencyNumber='${_emergencyNumber.value.trim()}', home='${_home.value.trim()}'"
                )
                val entity = UserProfileEntity(
                    id = 0,
                    name = nameVal,
                    emergencyContactName = _emergencyName.value.trim(),
                    emergencyContactNumber = _emergencyNumber.value.trim(),
                    homeAddress = _home.value.trim()
                )
                repo.saveProfile(entity)
                Log.d(TAG, "Profile saved successfully")
                onSaved()
            } catch (t: Throwable) {
                Log.e(TAG, "Error saving profile", t)
            }
        }
    }
}
