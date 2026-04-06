@file:Suppress("DEPRECATION", "unused")
package com.example.lakbaylaya.bluetooth

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.content.edit as prefsEdit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bluetooth connection state enum
 *
 * State transitions:
 * DISCONNECTED → ENABLING (if BT off) → SCANNING → CONNECTING → CONNECTED
 *              → ERROR (at any step if operation fails)
 */
enum class BluetoothState {
    DISCONNECTED,
    ENABLING,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * Data class representing a discovered Bluetooth device with its connection status.
 */
data class DiscoveredDevice(
    val device: BluetoothDevice,
    val name: String,
    val address: String,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false
)

/**
 * BluetoothManagerHelper - Handles all Bluetooth operations
 *
 * Responsibilities:
 * - Check and request runtime permissions (BLUETOOTH_SCAN, BLUETOOTH_CONNECT)
 * - Enable Bluetooth via system dialog
 * - Scan for ALL nearby Bluetooth devices (no device-name filtering)
 * - Connect/disconnect per device tap
 * - Auto-reconnect to previously connected device when it appears in scan
 * - Monitor Bluetooth state changes
 *
 * Architecture:
 * - Wrapper around Android Bluetooth APIs
 * - Uses BroadcastReceiver to monitor device discovery and state changes
 * - StateFlow for reactive state management
 * - Integrates with Activity for permission requests
 */
class BluetoothManagerHelper(
    private val context: Context,
    private var activity: Activity? = null
) {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val prefs = context.getSharedPreferences("bt_prefs", Context.MODE_PRIVATE)
    private val KEY_LAST_ADDRESS = "last_connected_address"

    // State management
    private val _bluetoothState = MutableStateFlow(BluetoothState.DISCONNECTED)
    val bluetoothState: StateFlow<BluetoothState> = _bluetoothState.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // All discovered devices (reactive list for UI)
    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    // Currently connected device address
    private var connectedDeviceAddress: String? = null

    // Currently open socket (for disconnect)
    private var activeSocket: android.bluetooth.BluetoothSocket? = null

    // Permission request launcher (set by activity)
    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null

    // Broadcast receiver for device discovery and Bluetooth state changes
    private var discoveryReceiver: BroadcastReceiver? = null

    /** Set the current Activity if the helper needs to launch system dialogs (enable Bluetooth) */
    fun setActivity(activity: Activity?) {
        this.activity = activity
    }

    /** Set up permission request launcher. Call from MainActivity's onCreate(). */
    fun setPermissionLauncher(launcher: ActivityResultLauncher<Array<String>>) {
        permissionLauncher = launcher
    }

    private fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                    hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission(Manifest.permission.BLUETOOTH) &&
                    hasPermission(Manifest.permission.BLUETOOTH_ADMIN)
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    /** Request required Bluetooth permissions */
    fun requestBluetoothPermissions() {
        if (!hasRequiredPermissions()) {
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            }
            permissionLauncher?.launch(permissions) ?: run {
                _errorMessage.value = "Permission launcher not set"
                _bluetoothState.value = BluetoothState.ERROR
            }
        }
    }

    /**
     * Enable Bluetooth. If already enabled, start device discovery immediately.
     */
    fun enableBluetooth() {
        if (bluetoothAdapter == null) {
            _errorMessage.value = "Bluetooth not supported on this device"
            _bluetoothState.value = BluetoothState.ERROR
            return
        }

        try {
            if (bluetoothAdapter.isEnabled) {
                _bluetoothState.value = BluetoothState.SCANNING
                startDeviceDiscovery()
                return
            }

            val canProgrammaticallyEnable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            } else true

            if (canProgrammaticallyEnable) {
                val invoked = bluetoothAdapter.enable()
                if (invoked) {
                    _bluetoothState.value = BluetoothState.ENABLING
                    return
                }
            }

            _bluetoothState.value = BluetoothState.ENABLING
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity?.startActivity(enableBtIntent)
        } catch (e: SecurityException) {
            _errorMessage.value = "Permission denied to enable Bluetooth: ${e.message}"
            _bluetoothState.value = BluetoothState.ERROR
        } catch (e: Exception) {
            _errorMessage.value = "Error enabling Bluetooth: ${e.message}"
            _bluetoothState.value = BluetoothState.ERROR
        }
    }

    /**
     * Start scanning for ALL nearby Bluetooth devices (no name filter).
     * Includes bonded devices in the list immediately, then starts discovery for new ones.
     */
    fun startDeviceDiscovery() {
        if (!hasRequiredPermissions()) {
            _errorMessage.value = "Missing Bluetooth permissions"
            _bluetoothState.value = BluetoothState.ERROR
            return
        }

        if (bluetoothAdapter == null) {
            _errorMessage.value = "Bluetooth adapter not available"
            _bluetoothState.value = BluetoothState.ERROR
            return
        }

        try {
            if (!bluetoothAdapter.isEnabled) {
                _errorMessage.value = "Bluetooth is not enabled"
                _bluetoothState.value = BluetoothState.ERROR
                return
            }
        } catch (e: SecurityException) {
            _errorMessage.value = "Permission denied to check Bluetooth state"
            _bluetoothState.value = BluetoothState.ERROR
            return
        }

        // Clear discovered list (but keep currently connected device)
        val existingConnected = _discoveredDevices.value.filter { it.isConnected }
        _discoveredDevices.value = existingConnected
        _bluetoothState.value = BluetoothState.SCANNING

        registerDiscoveryReceiver()

        // Add all already-bonded devices to the list first
        try {
            val hasConnect = hasConnectPermission()
            if (!hasConnect) {
                _errorMessage.value = "Missing BLUETOOTH_CONNECT permission"
                _bluetoothState.value = BluetoothState.ERROR
                return
            }

            val lastAddress = prefs.getString(KEY_LAST_ADDRESS, null)
            val bondedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
            bondedDevices?.forEach { device ->
                addOrUpdateDevice(device, autoConnect = device.address == lastAddress)
            }
        } catch (e: SecurityException) {
            _errorMessage.value = "Permission denied to check bonded devices"
            _bluetoothState.value = BluetoothState.ERROR
            return
        }

        // Start active discovery
        try {
            val hasScan = hasScanPermission()
            if (!hasScan) {
                _errorMessage.value = "Missing BLUETOOTH_SCAN permission"
                _bluetoothState.value = BluetoothState.ERROR
                return
            }
            bluetoothAdapter.startDiscovery()
        } catch (e: SecurityException) {
            _errorMessage.value = "Permission denied to start discovery"
            _bluetoothState.value = BluetoothState.ERROR
        }
    }

    /**
     * Connect to a specific device by address (user tap or auto-reconnect).
     */
    fun connectToDevice(address: String) {
        val target = _discoveredDevices.value.firstOrNull { it.address == address } ?: return
        if (target.isConnected || target.isConnecting) return

        // Disconnect from any existing connection first
        disconnectCurrentDevice()

        updateDeviceState(address, isConnecting = true)
        _bluetoothState.value = BluetoothState.CONNECTING
        _connectedDeviceName.value = target.name

        // Stop discovery to improve connection speed
        cancelDiscoverySafe()

        Thread {
            try {
                val hasConnect = hasConnectPermission()
                if (!hasConnect) {
                    _errorMessage.value = "Missing BLUETOOTH_CONNECT permission"
                    _bluetoothState.value = BluetoothState.ERROR
                    updateDeviceState(address, isConnecting = false)
                    return@Thread
                }

                val uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
                val socket = target.device.createRfcommSocketToServiceRecord(uuid)
                socket.connect()

                activeSocket = socket
                connectedDeviceAddress = address
                Esp32SocketWriter.getInstance().attach(socket.outputStream)

                // Persist for future auto-reconnect
                prefs.prefsEdit { putString(KEY_LAST_ADDRESS, address) }

                _bluetoothState.value = BluetoothState.CONNECTED
                _connectedDeviceName.value = target.name
                updateDeviceState(address, isConnected = true, isConnecting = false)

            } catch (e: SecurityException) {
                _errorMessage.value = "Permission denied during socket connection: ${e.message}"
                _bluetoothState.value = BluetoothState.ERROR
                _connectedDeviceName.value = null
                updateDeviceState(address, isConnecting = false)
            } catch (e: Exception) {
                _errorMessage.value = "Connection failed: ${e.message}"
                _bluetoothState.value = BluetoothState.ERROR
                _connectedDeviceName.value = null
                updateDeviceState(address, isConnecting = false)
            }
        }.start()
    }

    /**
     * Disconnect from the specified device address.
     */
    fun disconnectDevice(address: String) {
        if (connectedDeviceAddress == address) {
            disconnectCurrentDevice()
        }
    }

    private fun disconnectCurrentDevice() {
        val addr = connectedDeviceAddress ?: return
        cancelDiscoverySafe()
        try {
            activeSocket?.close()
        } catch (_: Exception) {}
        activeSocket = null
        connectedDeviceAddress = null
        Esp32SocketWriter.getInstance().detach()
        _bluetoothState.value = BluetoothState.DISCONNECTED
        _connectedDeviceName.value = null
        updateDeviceState(addr, isConnected = false, isConnecting = false)
    }

    /** Full disconnect (public, for ViewModel). */
    fun disconnect() {
        disconnectCurrentDevice()
    }

    private fun updateDeviceState(
        address: String,
        isConnected: Boolean? = null,
        isConnecting: Boolean? = null
    ) {
        _discoveredDevices.value = _discoveredDevices.value.map { d ->
            if (d.address == address) {
                d.copy(
                    isConnected = isConnected ?: d.isConnected,
                    isConnecting = isConnecting ?: d.isConnecting
                )
            } else d
        }
    }

    private fun addOrUpdateDevice(device: BluetoothDevice, autoConnect: Boolean = false) {
        try {
            val name = device.name ?: device.address
            val existing = _discoveredDevices.value.firstOrNull { it.address == device.address }
            if (existing == null) {
                val discovered = DiscoveredDevice(
                    device = device,
                    name = name,
                    address = device.address,
                    isConnected = device.address == connectedDeviceAddress
                )
                _discoveredDevices.value = _discoveredDevices.value + discovered
                if (autoConnect && connectedDeviceAddress == null) {
                    connectToDevice(device.address)
                }
            }
        } catch (_: SecurityException) {}
    }

    private fun registerDiscoveryReceiver() {
        unregisterDiscoveryReceiver()

        discoveryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        try {
                            if (!hasConnectPermission()) return

                            val device: BluetoothDevice? =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    intent.getParcelableExtra(
                                        BluetoothDevice.EXTRA_DEVICE,
                                        BluetoothDevice::class.java
                                    )
                                } else {
                                    @Suppress("DEPRECATION")
                                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                                }

                            if (device != null) {
                                val lastAddress = prefs.getString(KEY_LAST_ADDRESS, null)
                                addOrUpdateDevice(
                                    device,
                                    autoConnect = device.address == lastAddress
                                )
                            }
                        } catch (e: SecurityException) {
                            _errorMessage.value = "Permission denied during device discovery: ${e.message}"
                        } catch (e: Exception) {
                            _errorMessage.value = "Error discovering device: ${e.message}"
                        }
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        if (_bluetoothState.value == BluetoothState.SCANNING) {
                            // Keep SCANNING state if nothing connected; update to DISCONNECTED if list is empty
                            if (_discoveredDevices.value.isEmpty()) {
                                _bluetoothState.value = BluetoothState.DISCONNECTED
                            } else {
                                // Scan finished but devices found - just leave state as-is or go DISCONNECTED
                                if (connectedDeviceAddress == null) {
                                    _bluetoothState.value = BluetoothState.DISCONNECTED
                                }
                            }
                        }
                    }

                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        try {
                            val state = intent.getIntExtra(
                                BluetoothAdapter.EXTRA_STATE,
                                BluetoothAdapter.ERROR
                            )
                            when (state) {
                                BluetoothAdapter.STATE_OFF -> {
                                    _bluetoothState.value = BluetoothState.DISCONNECTED
                                    _connectedDeviceName.value = null
                                    connectedDeviceAddress = null
                                    activeSocket = null
                                    _discoveredDevices.value = emptyList()
                                }
                                BluetoothAdapter.STATE_ON -> {
                                    if (_bluetoothState.value == BluetoothState.ENABLING) {
                                        _bluetoothState.value = BluetoothState.SCANNING
                                        startDeviceDiscovery()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            _errorMessage.value = "Error checking Bluetooth state: ${e.message}"
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(discoveryReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(discoveryReceiver, filter)
            }
        } catch (e: Exception) {
            _errorMessage.value = "Failed to register Bluetooth receiver: ${e.message}"
            _bluetoothState.value = BluetoothState.ERROR
        }
    }

    private fun unregisterDiscoveryReceiver() {
        if (discoveryReceiver != null) {
            try {
                context.unregisterReceiver(discoveryReceiver)
            } catch (_: IllegalArgumentException) {}
            discoveryReceiver = null
        }
    }

    private fun cancelDiscoverySafe() {
        if (!hasScanPermission()) return
        try {
            bluetoothAdapter?.cancelDiscovery()
        } catch (_: SecurityException) {}
    }

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /** Public check for whether required Bluetooth permissions are present */
    fun hasPermissions(): Boolean = hasRequiredPermissions()

    /**
     * Disable Bluetooth adapter. Updates state accordingly.
     */
    fun disableBluetooth() {
        if (bluetoothAdapter == null) {
            _errorMessage.value = "Bluetooth not supported"
            _bluetoothState.value = BluetoothState.ERROR
            return
        }

        try {
            val hasConnect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            } else true

            if (!hasConnect) {
                _errorMessage.value = "Missing permission to disable Bluetooth"
                _bluetoothState.value = BluetoothState.ERROR
                return
            }

            val disabled = bluetoothAdapter.disable()
            if (disabled) {
                _bluetoothState.value = BluetoothState.DISCONNECTED
                _connectedDeviceName.value = null
                connectedDeviceAddress = null
                _discoveredDevices.value = emptyList()
            } else {
                _errorMessage.value = "Failed to disable Bluetooth"
                _bluetoothState.value = BluetoothState.ERROR
            }
        } catch (e: SecurityException) {
            _errorMessage.value = "Permission denied disabling Bluetooth: ${e.message}"
            _bluetoothState.value = BluetoothState.ERROR
        } catch (e: Exception) {
            _errorMessage.value = "Error disabling Bluetooth: ${e.message}"
            _bluetoothState.value = BluetoothState.ERROR
        }
    }

    fun isConnected(): Boolean = _bluetoothState.value == BluetoothState.CONNECTED
    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null

    /** Cleanup resources. Call from Activity onDestroy(). */
    fun cleanup() {
        unregisterDiscoveryReceiver()
        cancelDiscoverySafe()
        disconnectCurrentDevice()
    }
}
