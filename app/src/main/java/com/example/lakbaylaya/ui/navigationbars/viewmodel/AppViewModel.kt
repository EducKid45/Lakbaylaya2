package com.example.lakbaylaya.ui.navigationbars.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Main ViewModel for the app
 *
 * Manages state for:
 * - Bluetooth connectivity
 * - Notifications
 * - Emergency alerts
 *
 * Architecture:
 * - MVVM pattern
 * - Single source of truth for UI state
 * - Exposes state via StateFlow for reactive UI
 * - Business logic isolated from UI
 *
 * SOLID Principles:
 * - Single Responsibility: Manages only app-level state
 * - Open/Closed: Can extend with new state without modifying existing
 * - Dependency Inversion: UI depends on ViewModel interface, not implementation
 *
 * Testing:
 * - Unit testable (no Android dependencies)
 * - State changes can be verified
 */
class AppViewModel : ViewModel() {

    // Bluetooth state
    private val _isBluetoothEnabled = MutableStateFlow(false)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    // Notification count
    private val _notificationCount = MutableStateFlow(0)
    val notificationCount: StateFlow<Int> = _notificationCount.asStateFlow()

    // Emergency state (prevents multiple triggers)
    private val _isEmergencyTriggered = MutableStateFlow(false)
    val isEmergencyTriggered: StateFlow<Boolean> = _isEmergencyTriggered.asStateFlow()

    /**
     * Toggle Bluetooth state
     * In production, this would interact with Android Bluetooth APIs
     */
    fun toggleBluetooth() {
        _isBluetoothEnabled.value = !_isBluetoothEnabled.value
        // TODO: Integrate with BluetoothAdapter in data layer
    }

    /**
     * Handle settings action
     */
    fun onSettingsClick() {
        // TODO: Navigate to settings screen or show settings dialog
    }

    /**
     * Handle emergency action
     * Prevents multiple simultaneous triggers
     */
    fun onEmergencyClick() {
        if (!_isEmergencyTriggered.value) {
            _isEmergencyTriggered.value = true
            // TODO: Trigger emergency protocol in domain layer
            // - Send alert to emergency contacts
            // - Share location
            // - Start audio recording

            // Reset after delay (in production, handle differently)
            // This is a simplified example
        }
    }

    /**
     * Reset emergency state
     */
    fun resetEmergency() {
        _isEmergencyTriggered.value = false
    }

    /**
     * Handle notifications action
     */
    fun onNotificationsClick() {
        // TODO: Navigate to notifications screen
        // Clear badge
        _notificationCount.value = 0
    }

    /**
     * Add a notification (for testing/demo)
     */
    fun addNotification() {
        _notificationCount.value += 1
    }

    /**
     * Simulate receiving notifications (for demo purposes)
     */
    fun simulateNotifications(count: Int) {
        _notificationCount.value = count
    }
}
