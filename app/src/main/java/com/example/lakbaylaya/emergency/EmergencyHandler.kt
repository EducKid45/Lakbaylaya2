package com.example.lakbaylaya.emergency

/**
 * A small interface common to emergency flow handlers.
 */
interface EmergencyHandler {
    fun sendEmergencySms()
    fun onPermissionResults(results: Map<String, Boolean>)
}

