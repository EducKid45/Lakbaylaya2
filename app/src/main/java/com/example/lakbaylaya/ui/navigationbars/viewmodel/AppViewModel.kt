@file:Suppress("unused", "MemberVisibilityCanBePrivate")
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
import com.example.lakbaylaya.bluetooth.DiscoveredDevice
import kotlinx.coroutines.launch

/**
 * Main ViewModel for the app
 *
 * Manages state for:
 * - Bluetooth connectivity (delegates to BluetoothViewModel)
 * - Bluetooth bottom sheet visibility
 * - Notifications
 * - Emergency alerts
 */
class AppViewModel(
    application: Application
) : AndroidViewModel(application) {

    // BluetoothViewModel is a single shared instance owned by AppViewModel
    internal val bluetoothViewModel = BluetoothViewModel(application)

    // ── Bluetooth state (forwarded from BluetoothViewModel) ─────────────────
    private val _isBluetoothEnabled = MutableStateFlow(false)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _bluetoothState = MutableStateFlow(BluetoothState.DISCONNECTED)
    val bluetoothState: StateFlow<BluetoothState> = _bluetoothState.asStateFlow()

    // ── Bottom sheet visibility (forwarded from BluetoothViewModel) ──────────
    val showBluetoothSheet: StateFlow<Boolean> = bluetoothViewModel.showBluetoothSheet

    // ── Discovered devices (forwarded for UI) ────────────────────────────────
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = bluetoothViewModel.discoveredDevices

    // ── Notifications ────────────────────────────────────────────────────────
    private val _notificationCount = MutableStateFlow(0)
    val notificationCount: StateFlow<Int> = _notificationCount.asStateFlow()

    // ── Emergency state ──────────────────────────────────────────────────────
    @Suppress("unused")
    private val _isEmergencyTriggered = MutableStateFlow(false)
    @Suppress("unused")
    val isEmergencyTriggered: StateFlow<Boolean> = _isEmergencyTriggered.asStateFlow()

    init {
        viewModelScope.launch {
            bluetoothViewModel.isBluetoothConnected.collect { isConnected ->
                _isBluetoothEnabled.value = isConnected
            }
        }
        viewModelScope.launch {
            bluetoothViewModel.bluetoothState.collect { state ->
                _bluetoothState.value = state
            }
        }
    }

    // ── Bluetooth permission/activity plumbing ───────────────────────────────

    fun setBluetoothPermissionLauncher(launcher: ActivityResultLauncher<Array<String>>) {
        bluetoothViewModel.setPermissionLauncher(launcher)
    }

    fun setActivity(activity: android.app.Activity?) {
        bluetoothViewModel.setActivity(activity)
    }

    // ── Bluetooth icon tap ───────────────────────────────────────────────────

    /**
     * Called when the Bluetooth icon in the TopBar is tapped.
     * Opens the ModalBottomSheet and starts scanning.
     */
    fun onBluetoothIconClick() {
        bluetoothViewModel.openBluetoothSheet()
    }

    /** Dismiss the Bluetooth bottom sheet (does NOT disconnect). */
    fun dismissBluetoothSheet() {
        bluetoothViewModel.dismissBluetoothSheet()
    }

    /** Toggle per-device connection (connect if disconnected, disconnect if connected). */
    fun toggleDeviceConnection(address: String) {
        bluetoothViewModel.toggleDeviceConnection(address)
    }

    /** Rescan for devices. */
    fun rescanBluetooth() {
        bluetoothViewModel.rescan()
    }

    // ── Legacy toggle (kept for backward compatibility) ───────────────────────

    fun toggleBluetooth() {
        bluetoothViewModel.openBluetoothSheet()
    }

    // ── Settings ─────────────────────────────────────────────────────────────

    fun onSettingsClick() { /* navigation handled by caller */ }

    // ── Emergency ────────────────────────────────────────────────────────────

    fun onEmergencyClick() {
        if (!_isEmergencyTriggered.value) {
            _isEmergencyTriggered.value = true
        }
    }

    fun resetEmergency() {
        _isEmergencyTriggered.value = false
    }

    // ── Notifications ────────────────────────────────────────────────────────

    fun onNotificationsClick() {
        _notificationCount.value = 0
    }

    fun addNotification() {
        _notificationCount.value += 1
    }

    fun simulateNotifications(count: Int) {
        _notificationCount.value = count
    }

    // ── Hardware BT helpers (legacy, used by old callers) ────────────────────

    fun enableHardwareBluetooth() {
        bluetoothViewModel.enableHardwareBluetooth()
    }

    fun disableHardwareBluetooth() {
        bluetoothViewModel.disableHardwareBluetooth()
    }
}
