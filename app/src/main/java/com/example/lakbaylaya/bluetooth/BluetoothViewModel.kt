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
import com.example.lakbaylaya.PermissionResultHandler

/**
 * BluetoothViewModel - Manages Bluetooth connection state and operations
 */
class BluetoothViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val appContext = getApplication<Application>().applicationContext
    private val bluetoothHelper = BluetoothManagerHelper(appContext)
    private val notificationManager = BluetoothNotificationManager(appContext)

    // Track if we should show permission rationale dialog
    private val _showPermissionRationale = MutableStateFlow(false)
    val showPermissionRationale: StateFlow<Boolean> = _showPermissionRationale.asStateFlow()

    // Track if user has denied permissions before
    private var permissionsDeniedBefore = false

    // Expose connection state to UI
    private val _isBluetoothConnected = MutableStateFlow(false)
    val isBluetoothConnected: StateFlow<Boolean> = _isBluetoothConnected.asStateFlow()

    private val _bluetoothState = MutableStateFlow(BluetoothState.DISCONNECTED)
    val bluetoothState: StateFlow<BluetoothState> = _bluetoothState.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

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
                    BluetoothState.ENABLING -> showToast("Enabling Bluetooth...")
                    BluetoothState.SCANNING -> showToast("Searching for ESP32…")
                    BluetoothState.CONNECTING -> showToast("Connecting to ESP32...")
                    BluetoothState.CONNECTED -> {
                        showToast("Connected to ESP32")
                        _connectedDeviceName.value?.let { deviceName ->
                            notificationManager.showConnectedNotification(deviceName)
                        }
                    }

                    BluetoothState.DISCONNECTED -> {
                        notificationManager.dismissNotification()
                        _connectedDeviceName.value = null
                    }

                    BluetoothState.ERROR -> {
                        notificationManager.dismissNotification()
                        bluetoothHelper.errorMessage.value?.let { error ->
                            showToast(error)
                        }
                    }
                }
            }
        }

        // Collect device name from helper
        viewModelScope.launch {
            bluetoothHelper.connectedDeviceName.collect { deviceName ->
                _connectedDeviceName.value = deviceName
            }
        }

        // Collect error messages from helper
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

    /**
     * Handle permission results from MainActivity
     * Auto-resume Bluetooth flow if permissions granted
     */
    private fun handlePermissionResults(results: Map<String, Boolean>) {
        val allGranted = results.values.all { it }

        if (allGranted) {
            // All permissions granted, auto-resume Bluetooth flow
            showToast("Permissions granted, resuming Bluetooth flow...")
            permissionsDeniedBefore = false
            _showPermissionRationale.value = false

            // Auto-resume the flow
            if (shouldResumeBluetoothFlowAfterPermission) {
                shouldResumeBluetoothFlowAfterPermission = false
                bluetoothHelper.enableBluetooth()
            }
        } else {
            // Some permissions denied
            if (!permissionsDeniedBefore) {
                // First time denied - show rationale
                permissionsDeniedBefore = true
                _showPermissionRationale.value = true
                showToast("Bluetooth permissions required to connect to ESP32")
            } else {
                // Already shown rationale - user still denied
                showToast("Permissions denied. Cannot proceed with Bluetooth connection.")
            }
        }
    }

    /**
     * Set permission launcher for the Bluetooth helper
     */
    fun setPermissionLauncher(launcher: ActivityResultLauncher<Array<String>>) {
        bluetoothHelper.setPermissionLauncher(launcher)
    }

    /**
     * Set the current Activity
     */
    fun setActivity(activity: android.app.Activity?) {
        bluetoothHelper.setActivity(activity)
    }

    /**
     * Dismiss permission rationale dialog
     */
    fun dismissPermissionRationale() {
        _showPermissionRationale.value = false
    }

    /**
     * Request permissions again after rationale was shown
     */
    fun requestPermissionsAgainAfterRationale() {
        _showPermissionRationale.value = false
        shouldResumeBluetoothFlowAfterPermission = true
        bluetoothHelper.requestBluetoothPermissions()
    }

    /**
     * Main entry point: Start Bluetooth connection flow
     */
    fun startBluetoothFlow() {
        if (_isBluetoothConnected.value) {
            disconnect()
            return
        }

        if (!bluetoothHelper.isBluetoothAvailable()) {
            showToast("Bluetooth not supported on this device")
            return
        }

        // Mark that we should resume after permissions
        shouldResumeBluetoothFlowAfterPermission = true

        // Request permissions (callback will auto-resume if granted)
        bluetoothHelper.requestBluetoothPermissions()
    }

    /**
     * Disconnect from Bluetooth device
     */
    fun disconnect() {
        bluetoothHelper.disconnect()
        notificationManager.dismissNotification()
        _isBluetoothConnected.value = false
        _connectedDeviceName.value = null
        showToast("Disconnected from ESP32")
    }

    /**
     * Request permissions and enable Bluetooth hardware. If permissions already granted,
     * call helper.enableBluetooth() immediately; otherwise request and auto-resume.
     */
    fun enableHardwareBluetooth() {
        if (bluetoothHelper.hasPermissions()) {
            bluetoothHelper.enableBluetooth()
        } else {
            shouldResumeBluetoothFlowAfterPermission = true
            bluetoothHelper.requestBluetoothPermissions()
        }
    }

    /**
     * Disable the Bluetooth adapter (turn off hardware).
     */
    fun disableHardwareBluetooth() {
        bluetoothHelper.disableBluetooth()
    }

    /**
     * Show toast message to user
     */
    private fun showToast(message: String) {
        viewModelScope.launch {
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Reset error state
     */
    fun resetError() {
        _bluetoothState.value = BluetoothState.DISCONNECTED
    }

    /**
     * Cleanup resources
     */
    override fun onCleared() {
        super.onCleared()
        bluetoothHelper.cleanup()
        notificationManager.dismissNotification()
    }
}
