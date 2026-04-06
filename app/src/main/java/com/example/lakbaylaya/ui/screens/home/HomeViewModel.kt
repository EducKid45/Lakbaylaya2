package com.example.lakbaylaya.ui.screens.home

import android.app.Application
import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lakbaylaya.bluetooth.BluetoothManagerHelper
import com.example.lakbaylaya.bluetooth.BluetoothState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeviceStatus(
    val internetStatus: ConnectionStatus = ConnectionStatus.UNKNOWN,
    val gpsStatus: GpsStatus = GpsStatus.SEARCHING,
    val wearableStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED
)

enum class ConnectionStatus {
    ONLINE, OFFLINE, UNKNOWN, CONNECTED, DISCONNECTED
}

enum class GpsStatus {
    READY, SEARCHING, UNAVAILABLE
}

data class HomeUiState(
    val isListening: Boolean = false,
    val deviceStatus: DeviceStatus = DeviceStatus(),
    val lastCommand: String? = null,
    val isCommandListExpanded: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    companion object { private const val TAG = "HomeViewModel" }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // ConnectivityManager callback — kept as field so we can unregister it
    private val connectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateInternetStatus(ConnectionStatus.ONLINE)
        }
        override fun onLost(network: Network) {
            updateInternetStatus(ConnectionStatus.OFFLINE)
        }
        override fun onUnavailable() {
            updateInternetStatus(ConnectionStatus.OFFLINE)
        }
    }

    // Shared BluetoothManagerHelper singleton exposed via App
    private val bluetoothHelper: BluetoothManagerHelper by lazy {
        BluetoothManagerHelper(application)
    }

    init {
        registerNetworkCallback()
        checkInternetStatus()
        checkGpsStatus()
        observeBluetoothState()
    }

    // ── Internet ─────────────────────────────────────────────────────────────────

    private fun registerNetworkCallback() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Could not register network callback: ${e.message}")
        }
    }

    private fun updateInternetStatus(status: ConnectionStatus) {
        _uiState.value = _uiState.value.copy(
            deviceStatus = _uiState.value.deviceStatus.copy(internetStatus = status)
        )
    }

    fun checkInternetStatus() {
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        updateInternetStatus(if (isOnline) ConnectionStatus.ONLINE else ConnectionStatus.OFFLINE)
    }

    // ── GPS ──────────────────────────────────────────────────────────────────────

    fun checkGpsStatus() {
        val locationManager =
            getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val status = if (gpsEnabled) GpsStatus.READY else GpsStatus.UNAVAILABLE
        _uiState.value = _uiState.value.copy(
            deviceStatus = _uiState.value.deviceStatus.copy(gpsStatus = status)
        )
    }

    // ── Bluetooth / ESP32 ────────────────────────────────────────────────────────

    private fun observeBluetoothState() {
        viewModelScope.launch {
            bluetoothHelper.bluetoothState.collect { state ->
                val wearable = when (state) {
                    BluetoothState.CONNECTED -> ConnectionStatus.CONNECTED
                    else -> ConnectionStatus.DISCONNECTED
                }
                _uiState.value = _uiState.value.copy(
                    deviceStatus = _uiState.value.deviceStatus.copy(wearableStatus = wearable)
                )
            }
        }
    }

    fun checkWearableStatus() {
        val wearable = if (bluetoothHelper.isConnected())
            ConnectionStatus.CONNECTED else ConnectionStatus.DISCONNECTED
        _uiState.value = _uiState.value.copy(
            deviceStatus = _uiState.value.deviceStatus.copy(wearableStatus = wearable)
        )
    }

    // ── Voice commands ───────────────────────────────────────────────────────────

    fun startVoiceRecognition() {
        _uiState.value = _uiState.value.copy(isListening = true)
    }

    fun stopVoiceRecognition() {
        _uiState.value = _uiState.value.copy(isListening = false)
    }

    fun toggleCommandList() {
        _uiState.value = _uiState.value.copy(
            isCommandListExpanded = !_uiState.value.isCommandListExpanded
        )
    }

    fun executeVoiceCommand(command: String) {
        _uiState.value = _uiState.value.copy(lastCommand = command)
    }

    fun updateDeviceStatus(status: DeviceStatus) {
        _uiState.value = _uiState.value.copy(deviceStatus = status)
    }

    override fun onCleared() {
        super.onCleared()
        try { connectivityManager.unregisterNetworkCallback(networkCallback) }
        catch (e: Exception) { Log.w(TAG, "Failed to unregister network callback: ${e.message}") }
    }
}
