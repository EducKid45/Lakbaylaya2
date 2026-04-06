@file:Suppress("unused", "MemberVisibilityCanBePrivate")
package com.example.lakbaylaya.bluetooth

import android.app.Activity
import android.app.Application
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.lakbaylaya.utils.PermissionResultHandler

/**
 * BluetoothViewModel - Manages Bluetooth state, device list, and bottom sheet visibility.
 *
 * Architecture:
 * - MVVM: UI observes StateFlow; ViewModel delegates to BluetoothManagerHelper.
 * - Exposes discovered device list for the Bluetooth bottom sheet.
 * - Controls bottom sheet show/hide.
 * - Handles permission flow with auto-resume.
 */
class BluetoothViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val appContext = getApplication<Application>().applicationContext
    internal val bluetoothHelper = BluetoothManagerHelper(appContext)
    private val notificationManager = BluetoothNotificationManager(appContext)

    // ── Permission rationale ────────────────────────────────────────────────
    @Suppress("unused")
    private val _showPermissionRationale = MutableStateFlow(false)
    @Suppress("unused")
    val showPermissionRationale: StateFlow<Boolean> = _showPermissionRationale.asStateFlow()

    private var permissionsDeniedBefore = false

    // ── Connection state ────────────────────────────────────────────────────
    private val _isBluetoothConnected = MutableStateFlow(false)
    val isBluetoothConnected: StateFlow<Boolean> = _isBluetoothConnected.asStateFlow()

    private val _bluetoothState = MutableStateFlow(BluetoothState.DISCONNECTED)
    val bluetoothState: StateFlow<BluetoothState> = _bluetoothState.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    @Suppress("unused")
    private val _isConnecting = MutableStateFlow(false)
    @Suppress("unused")
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    // ── Discovered devices (for bottom sheet) ──────────────────────────────
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = bluetoothHelper.discoveredDevices

    // ── Bottom sheet visibility ─────────────────────────────────────────────
    private val _showBluetoothSheet = MutableStateFlow(false)
    val showBluetoothSheet: StateFlow<Boolean> = _showBluetoothSheet.asStateFlow()

    // Flag to resume Bluetooth flow after permission grant
    private var shouldResumeBluetoothFlowAfterPermission = false

    init {
        // Collect state from helper and update UI state
        viewModelScope.launch {
            bluetoothHelper.bluetoothState.collect { state ->
                _bluetoothState.value = state
                _isBluetoothConnected.value = state == BluetoothState.CONNECTED
                _isConnecting.value = state in listOf(
                    BluetoothState.ENABLING,
                    BluetoothState.SCANNING,
                    BluetoothState.CONNECTING
                )

                when (state) {
                    BluetoothState.ENABLING -> showToast("Enabling Bluetooth…")
                    BluetoothState.SCANNING -> { /* scanning — UI shows spinner in sheet */ }
                    BluetoothState.CONNECTING -> showToast("Connecting…")
                    BluetoothState.CONNECTED -> {
                        _connectedDeviceName.value?.let { name ->
                            showToast("Connected to $name")
                            notificationManager.showConnectedNotification(name)
                        }
                    }
                    BluetoothState.DISCONNECTED -> {
                        notificationManager.dismissNotification()
                    }
                    BluetoothState.ERROR -> {
                        notificationManager.dismissNotification()
                        bluetoothHelper.errorMessage.value?.let { showToast(it) }
                    }
                }
            }
        }

        // Collect device name
        viewModelScope.launch {
            bluetoothHelper.connectedDeviceName.collect { name ->
                _connectedDeviceName.value = name
            }
        }

        // Collect error messages
        viewModelScope.launch {
            bluetoothHelper.errorMessage.collect { error ->
                if (error != null && _bluetoothState.value == BluetoothState.ERROR) {
                    showToast(error)
                }
            }
        }

        // Listen for permission results from MainActivity and auto-resume
        viewModelScope.launch {
            PermissionResultHandler.permissionResults.collect { results ->
                if (results.isNotEmpty()) {
                    handlePermissionResults(results)
                    PermissionResultHandler.clearResults()
                }
            }
        }
    }

    // ── Permission handling ─────────────────────────────────────────────────

    private fun handlePermissionResults(results: Map<String, Boolean>) {
        val allGranted = results.values.all { it }

        if (allGranted) {
            showToast("Permissions granted")
            permissionsDeniedBefore = false
            _showPermissionRationale.value = false

            if (shouldResumeBluetoothFlowAfterPermission) {
                shouldResumeBluetoothFlowAfterPermission = false
                bluetoothHelper.enableBluetooth()
            }
        } else {
            if (!permissionsDeniedBefore) {
                permissionsDeniedBefore = true
                _showPermissionRationale.value = true
                showToast("Bluetooth permissions required")
            } else {
                showToast("Permissions denied. Cannot connect via Bluetooth.")
            }
        }
    }

    fun setPermissionLauncher(launcher: ActivityResultLauncher<Array<String>>) {
        bluetoothHelper.setPermissionLauncher(launcher)
    }

    fun setActivity(activity: Activity?) {
        bluetoothHelper.setActivity(activity)
    }

    fun dismissPermissionRationale() {
        _showPermissionRationale.value = false
    }

    fun requestPermissionsAgainAfterRationale() {
        _showPermissionRationale.value = false
        shouldResumeBluetoothFlowAfterPermission = true
        bluetoothHelper.requestBluetoothPermissions()
    }

    // ── Bottom sheet ────────────────────────────────────────────────────────

    /**
     * Called when the Bluetooth icon in TopBar is tapped.
     * Opens the bottom sheet and triggers a scan.
     */
    fun openBluetoothSheet() {
        _showBluetoothSheet.value = true
        startScanFlow()
    }

    /** Dismiss the bottom sheet. Does NOT disconnect the connected device. */
    fun dismissBluetoothSheet() {
        _showBluetoothSheet.value = false
    }

    // ── Scan ────────────────────────────────────────────────────────────────

    /**
     * Start scanning (enable BT first if needed, then discover).
     */
    fun startScanFlow() {
        if (!bluetoothHelper.isBluetoothAvailable()) {
            showToast("Bluetooth not supported on this device")
            return
        }

        shouldResumeBluetoothFlowAfterPermission = true

        if (!bluetoothHelper.hasPermissions()) {
            bluetoothHelper.requestBluetoothPermissions()
            return
        }

        bluetoothHelper.enableBluetooth()
    }

    /** Restart the scan (e.g., pull-to-refresh in sheet). */
    fun rescan() {
        if (bluetoothHelper.hasPermissions()) {
            bluetoothHelper.startDeviceDiscovery()
        } else {
            startScanFlow()
        }
    }

    // ── Connect / Disconnect ────────────────────────────────────────────────

    /**
     * Toggle: if device is connected → disconnect; if disconnected → connect.
     */
    fun toggleDeviceConnection(address: String) {
        val device = discoveredDevices.value.firstOrNull { it.address == address } ?: return
        if (device.isConnected) {
            bluetoothHelper.disconnectDevice(address)
            notificationManager.dismissNotification()
            showToast("Disconnected from ${device.name}")
        } else if (!device.isConnecting) {
            bluetoothHelper.connectToDevice(address)
        }
    }

    // ── Legacy helpers (still used by AppViewModel toggleBluetooth) ─────────

    fun startBluetoothFlow() {
        if (_isBluetoothConnected.value) {
            disconnect()
            return
        }
        openBluetoothSheet()
    }

    fun disconnect() {
        bluetoothHelper.disconnect()
        notificationManager.dismissNotification()
        _isBluetoothConnected.value = false
        _connectedDeviceName.value = null
        showToast("Bluetooth disconnected")
    }

    fun enableHardwareBluetooth() {
        if (bluetoothHelper.hasPermissions()) {
            bluetoothHelper.enableBluetooth()
        } else {
            shouldResumeBluetoothFlowAfterPermission = true
            bluetoothHelper.requestBluetoothPermissions()
        }
    }

    fun disableHardwareBluetooth() {
        bluetoothHelper.disableBluetooth()
    }

    // ── Utils ───────────────────────────────────────────────────────────────

    private fun showToast(message: String) {
        viewModelScope.launch {
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun resetError() {
        _bluetoothState.value = BluetoothState.DISCONNECTED
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothHelper.cleanup()
        notificationManager.dismissNotification()
    }
}
