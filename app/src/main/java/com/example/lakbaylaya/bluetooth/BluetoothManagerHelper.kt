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
 * BluetoothManagerHelper - Handles all Bluetooth operations
 *
 * Responsibilities:
 * - Check and request runtime permissions (BLUETOOTH_SCAN, BLUETOOTH_CONNECT)
 * - Enable Bluetooth via system dialog
 * - Scan for nearby Bluetooth devices
 * - Auto-connect to ESP32 device
 * - Monitor Bluetooth state changes
 * - Disconnect from device
 *
 * Architecture:
 * - Wrapper around Android Bluetooth APIs
 * - Uses BroadcastReceiver to monitor device discovery and state changes
 * - StateFlow for reactive state management
 * - Integrates with Activity for permission requests
 *
 * Device Discovery:
 * - Filters for devices with "ESP32" in name
 * - Auto-connects on first match
 */
class BluetoothManagerHelper(
    private val context: Context,
    private var activity: Activity? = null
) {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    // State management
    private val _bluetoothState = MutableStateFlow(BluetoothState.DISCONNECTED)
    val bluetoothState: StateFlow<BluetoothState> = _bluetoothState.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Discovered devices
    private val _discoveredDevices = mutableListOf<BluetoothDevice>()
    private var currentConnectingDevice: BluetoothDevice? = null

    // Permission request launcher (set by activity)
    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null

    // Broadcast receiver for device discovery and Bluetooth state changes
    private var discoveryReceiver: BroadcastReceiver? = null

    /**
     * Set the current Activity if the helper needs to launch system dialogs (enable Bluetooth)
     */
    fun setActivity(activity: Activity?) {
        this.activity = activity
    }

    /**
     * Set up permission request launcher
     * Call from MainActivity's onCreate() with contract launcher
     */
    fun setPermissionLauncher(launcher: ActivityResultLauncher<Array<String>>) {
        permissionLauncher = launcher
    }

    /**
     * Check if both required permissions are granted
     * For Android 12+, requires BLUETOOTH_SCAN and BLUETOOTH_CONNECT
     * For Android 11 and below, only BLUETOOTH and BLUETOOTH_ADMIN needed
     */
    private fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires explicit runtime permissions
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                    hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // Android 11 and below use legacy permissions
            hasPermission(Manifest.permission.BLUETOOTH) &&
                    hasPermission(Manifest.permission.BLUETOOTH_ADMIN)
        }
    }

    /**
     * Helper to check single permission
     * Min SDK >= 29 so API 23 check is unnecessary; directly use checkSelfPermission
     */
    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request required Bluetooth permissions
     */
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

            permissionLauncher?.launch(permissions)
                ?: run {
                    _errorMessage.value = "Permission launcher not set"
                    _bluetoothState.value = BluetoothState.ERROR
                }
        }
    }

    /**
     * Enable Bluetooth via adapter.enable() when permitted; otherwise request via system dialog.
     */
    fun enableBluetooth() {
        if (bluetoothAdapter == null) {
            _errorMessage.value = "Bluetooth not supported on this device"
            _bluetoothState.value = BluetoothState.ERROR
            return
        }

        try {
            // If already enabled, start discovery
            if (bluetoothAdapter.isEnabled) {
                _bluetoothState.value = BluetoothState.SCANNING
                startDeviceDiscovery()
                return
            }

            // If we have connect permission (or older SDK that doesn't require it), try to enable programmatically
            val canProgrammaticallyEnable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            if (canProgrammaticallyEnable) {
                val invoked = bluetoothAdapter.enable()
                if (invoked) {
                    // api call succeeded in initiating enable; state will be updated via ACTION_STATE_CHANGED receiver
                    _bluetoothState.value = BluetoothState.ENABLING
                    return
                }
                // If enable() returned false, fall through to request dialog as fallback
            }

            // Fallback: request user to enable Bluetooth via system dialog
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
     * Start scanning for devices
     * Filters for devices with "ESP32" in name
     */
    private fun startDeviceDiscovery() {
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

        _discoveredDevices.clear()
        _bluetoothState.value = BluetoothState.SCANNING

        // Register broadcast receiver for discovery
        registerDiscoveryReceiver()

        // Check already bonded devices first
        try {
            // For API >= S check BLUETOOTH_CONNECT, otherwise fall back to legacy BLUETOOTH
            val hasConnect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH
                ) == PackageManager.PERMISSION_GRANTED
            }

            if (!hasConnect) {
                _errorMessage.value = "Missing BLUETOOTH_CONNECT permission"
                _bluetoothState.value = BluetoothState.ERROR
                return
            }

            val bondedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
            bondedDevices?.forEach { device ->
                if (device.name?.contains("ESP32") == true) {
                    _discoveredDevices.add(device)
                    attemptConnection(device)
                    return@forEach
                }
            }
        } catch (e: SecurityException) {
            _errorMessage.value = "Permission denied to check bonded devices"
            _bluetoothState.value = BluetoothState.ERROR
            return
        }

        // Start discovery for new devices
        try {
            val hasScan = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH
                ) == PackageManager.PERMISSION_GRANTED
            }

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
     * Register broadcast receiver for device discovery
     */
    private fun registerDiscoveryReceiver() {
        unregisterDiscoveryReceiver() // Clean up any existing receiver

        discoveryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        try {
                            // For API >= S check BLUETOOTH_CONNECT, otherwise fall back to legacy BLUETOOTH
                            val hasConnect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                ContextCompat.checkSelfPermission(
                                    this@BluetoothManagerHelper.context,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                ) == PackageManager.PERMISSION_GRANTED
                            } else {
                                ContextCompat.checkSelfPermission(
                                    this@BluetoothManagerHelper.context,
                                    Manifest.permission.BLUETOOTH
                                ) == PackageManager.PERMISSION_GRANTED
                            }

                            if (!hasConnect) {
                                _errorMessage.value = "Missing BLUETOOTH_CONNECT permission"
                                _bluetoothState.value = BluetoothState.ERROR
                                return@onReceive
                            }

                            val device: BluetoothDevice? =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    intent.getParcelableExtra(
                                        BluetoothDevice.EXTRA_DEVICE,
                                        BluetoothDevice::class.java
                                    )
                                } else {
                                    // older API: use legacy getParcelableExtra only on older platforms
                                    @Suppress("DEPRECATION")
                                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                                }

                            if (device != null && device.name?.contains("ESP32") == true) {
                                if (!_discoveredDevices.any { it.address == device.address }) {
                                    _discoveredDevices.add(device)
                                    attemptConnection(device)
                                }
                            }
                        } catch (e: SecurityException) {
                            _errorMessage.value =
                                "Permission denied during device discovery: ${e.message}"
                            _bluetoothState.value = BluetoothState.ERROR
                        } catch (e: Exception) {
                            _errorMessage.value = "Error discovering device: ${e.message}"
                        }
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        // Discovery finished, check if we found any devices
                        if (_bluetoothState.value == BluetoothState.SCANNING && _connectedDeviceName.value == null) {
                            _errorMessage.value = "No ESP32 devices found"
                            _bluetoothState.value = BluetoothState.ERROR
                        }
                    }

                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        try {
                            val state = intent.getIntExtra(
                                BluetoothAdapter.EXTRA_STATE,
                                BluetoothAdapter.ERROR
                            )
                            if (state == BluetoothAdapter.STATE_OFF) {
                                // User manually turned off Bluetooth
                                _bluetoothState.value = BluetoothState.DISCONNECTED
                                _connectedDeviceName.value = null
                            } else if (state == BluetoothAdapter.STATE_ON && _bluetoothState.value == BluetoothState.ENABLING) {
                                // Bluetooth just enabled, start scanning
                                _bluetoothState.value = BluetoothState.SCANNING
                                startDeviceDiscovery()
                            }
                        } catch (e: SecurityException) {
                            _errorMessage.value =
                                "Permission denied checking Bluetooth state: ${e.message}"
                            _bluetoothState.value = BluetoothState.ERROR
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
                context.registerReceiver(
                    discoveryReceiver,
                    filter,
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(discoveryReceiver, filter)
            }
        } catch (e: Exception) {
            _errorMessage.value = "Failed to register Bluetooth receiver: ${e.message}"
            _bluetoothState.value = BluetoothState.ERROR
        }
    }

    /**
     * Unregister discovery receiver
     */
    private fun unregisterDiscoveryReceiver() {
        if (discoveryReceiver != null) {
            try {
                context.unregisterReceiver(discoveryReceiver)
            } catch (e: IllegalArgumentException) {
                // Receiver not registered
            }
            discoveryReceiver = null
        }
    }

    /**
     * Attempt to connect to a device using BluetoothSocket
     * Establishes a real RFCOMM connection to the ESP32 device
     */
    private fun attemptConnection(device: BluetoothDevice) {
        if (_bluetoothState.value == BluetoothState.CONNECTED) {
            return // Already connected
        }

        currentConnectingDevice = device
        _bluetoothState.value = BluetoothState.CONNECTING
        _connectedDeviceName.value = device.name

        // Stop discovery with permission check
        val hasScan3 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
        if (hasScan3) {
            try {
                bluetoothAdapter?.cancelDiscovery()
            } catch (e: SecurityException) {
                // Permission denied, log and continue
            }
        }

        // Attempt real BluetoothSocket connection on background thread
        Thread {
            try {
                // Check permission before creating socket
                val hasConnect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH
                    ) == PackageManager.PERMISSION_GRANTED
                }

                if (!hasConnect) {
                    _errorMessage.value =
                        "Missing BLUETOOTH_CONNECT permission for socket connection"
                    _bluetoothState.value = BluetoothState.ERROR
                    return@Thread
                }

                // Create BluetoothSocket (RFCOMM) to ESP32
                // Using standard UUID for SPP (Serial Port Profile)
                val uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
                val socket = device.createRfcommSocketToServiceRecord(uuid)

                // Connect to socket (blocking call)
                socket.connect()

                // Connection successful
                _bluetoothState.value = BluetoothState.CONNECTED
                _connectedDeviceName.value = device.name

                // Keep socket open for potential future communication
                // In production, you would read/write data via socket.inputStream / socket.outputStream

            } catch (e: SecurityException) {
                _errorMessage.value = "Permission denied during socket connection: ${e.message}"
                _bluetoothState.value = BluetoothState.ERROR
                _connectedDeviceName.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Connection failed: ${e.message}"
                _bluetoothState.value = BluetoothState.ERROR
                _connectedDeviceName.value = null
            } finally {
                unregisterDiscoveryReceiver()
            }
        }.start()
    }

    /**
     * Disconnect from Bluetooth device
     */
    fun disconnect() {
        // Cancel discovery with permission check
        val hasScan4 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
        if (hasScan4) {
            try {
                bluetoothAdapter?.cancelDiscovery()
            } catch (e: SecurityException) {
                // Permission denied, log and continue
            }
        }

        unregisterDiscoveryReceiver()
        _bluetoothState.value = BluetoothState.DISCONNECTED
        _connectedDeviceName.value = null
        currentConnectingDevice = null
    }

    /**
     * Public check for whether required Bluetooth permissions are present
     */
    fun hasPermissions(): Boolean = hasRequiredPermissions()

    /**
     * Disable Bluetooth adapter (if available). Updates state accordingly.
     * Requires appropriate permission on some Android versions.
     */
    fun disableBluetooth() {
        if (bluetoothAdapter == null) {
            _errorMessage.value = "Bluetooth not supported"
            _bluetoothState.value = BluetoothState.ERROR
            return
        }

        try {
            // Some devices require BLUETOOTH_CONNECT to manage adapter state
            val hasConnect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                // Older APIs allowed adapter.disable() without runtime permission
                true
            }

            if (!hasConnect) {
                _errorMessage.value = "Missing permission to disable Bluetooth"
                _bluetoothState.value = BluetoothState.ERROR
                return
            }

            val disabled = bluetoothAdapter.disable()
            if (disabled) {
                _bluetoothState.value = BluetoothState.DISCONNECTED
                _connectedDeviceName.value = null
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

    /**
     * Check if Bluetooth is currently connected
     */
    fun isConnected(): Boolean = _bluetoothState.value == BluetoothState.CONNECTED

    /**
     * Check if Bluetooth adapter exists and is available
     */
    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null

    /**
     * Cleanup resources
     * Call from Activity onDestroy()
     */
    fun cleanup() {
        unregisterDiscoveryReceiver()
        disconnect()
    }
}
