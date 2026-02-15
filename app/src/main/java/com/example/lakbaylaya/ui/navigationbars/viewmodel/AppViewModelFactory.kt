package com.example.lakbaylaya.ui.navigationbars.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * AppViewModelFactory - Factory for creating AppViewModel instances
 *
 * Handles dependency injection for:
 * - Application (for Bluetooth operations)
 *
 * Allows AppViewModel to be instantiated with required dependencies
 * while maintaining compatibility with Jetpack Compose's viewModel()
 */
class AppViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AppViewModel::class.java) -> {
                AppViewModel(application) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
