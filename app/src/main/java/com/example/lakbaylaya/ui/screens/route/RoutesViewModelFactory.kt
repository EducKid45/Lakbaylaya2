package com.example.lakbaylaya.ui.screens.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lakbaylaya.data.repository.RoutesRepository

/** Factory that creates `RoutesViewModel` instances with a provided `RoutesRepository`. */
class RoutesViewModelFactory(private val repo: RoutesRepository?) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RoutesViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
