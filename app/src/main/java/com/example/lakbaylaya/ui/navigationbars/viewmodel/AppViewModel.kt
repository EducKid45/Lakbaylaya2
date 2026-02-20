package com.example.lakbaylaya.ui.navigationbars.viewmodel

import android.app.Application
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.lakbaylaya.bluetooth.BluetoothViewModel
import com.example.lakbaylaya.bluetooth.BluetoothState
import kotlinx.coroutines.launch

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
 * - Delegates Bluetooth operations to BluetoothViewModel
 *
 * SOLID Principles:
 * - Single Responsibility: Manages app-level state, delegates Bluetooth to BluetoothViewModel
 * - Open/Closed: Can extend with new state without modifying existing
 * - Dependency Inversion: UI depends on ViewModel interface, not implementation
 *
 * Testing:
 * - Unit testable (no Android dependencies)
 * - State changes can be verified
 */
class AppViewModel(
    application: Application
) : AndroidViewModel(application) {

    // Bluetooth ViewModel for managing Bluetooth state (uses application context)
    private val bluetoothViewModel = BluetoothViewModel(application)

    // Bluetooth state - expose from BluetoothViewModel
    private val _isBluetoothEnabled = MutableStateFlow(false)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    // Also expose full BluetoothState for richer UI (scanning/connecting/etc.)
    private val _bluetoothState = MutableStateFlow(BluetoothState.DISCONNECTED)
    val bluetoothState: StateFlow<BluetoothState> = _bluetoothState.asStateFlow()

    // Notification count
    private val _notificationCount = MutableStateFlow(0)
    val notificationCount: StateFlow<Int> = _notificationCount.asStateFlow()

    // Emergency state (prevents multiple triggers)
    private val _isEmergencyTriggered = MutableStateFlow(false)
    val isEmergencyTriggered: StateFlow<Boolean> = _isEmergencyTriggered.asStateFlow()

    init {
        // Collect Bluetooth connected state from BluetoothViewModel
        bluetoothViewModel.let { vm ->
            viewModelScope.launch {
                vm.isBluetoothConnected.collect { isConnected ->
                    _isBluetoothEnabled.value = isConnected
                }
            }
            // Collect full bluetoothState as well
            viewModelScope.launch {
                vm.bluetoothState.collect { state ->
                    _bluetoothState.value = state
                }
            }
        }
    }

    /**
     * Set permission launcher for Bluetooth operations
     * Call from MainActivity
     */
    fun setBluetoothPermissionLauncher(launcher: ActivityResultLauncher<Array<String>>) {
        bluetoothViewModel.setPermissionLauncher(launcher)
    }

    /**
     * Toggle Bluetooth state
     */
    fun toggleBluetooth() {
        // Optimistically update UI immediately so icon toggles.
        val currentlyEnabled = _isBluetoothEnabled.value

        bluetoothViewModel.let { vm ->
            if (currentlyEnabled) {
                // If currently enabled, disable hardware Bluetooth
                _isBluetoothEnabled.value = false
                vm.disableHardwareBluetooth()
            } else {
                // If currently disabled, enable hardware Bluetooth
                _isBluetoothEnabled.value = true
                vm.enableHardwareBluetooth()
            }
        }
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

    /**
     * Set the current Activity on the BluetoothViewModel so it can launch system dialogs
     */
    fun setActivity(activity: android.app.Activity?) {
        bluetoothViewModel.setActivity(activity)
    }
}
