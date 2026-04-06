package com.example.lakbaylaya.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global permission result handler
 * Allows MainActivity to communicate permission grant results to ViewModels
 */
object PermissionResultHandler {
    private val _permissionResults = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val permissionResults: StateFlow<Map<String, Boolean>> = _permissionResults.asStateFlow()

    fun setResults(results: Map<String, Boolean>) {
        _permissionResults.value = results
    }

    fun clearResults() {
        _permissionResults.value = emptyMap()
    }
}