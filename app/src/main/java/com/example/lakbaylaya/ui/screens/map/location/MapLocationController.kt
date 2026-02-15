package com.example.lakbaylaya.ui.screens.map.location

import com.example.lakbaylaya.data.repository.MapRepository
import com.example.lakbaylaya.ui.screens.map.models.*
import com.example.lakbaylaya.utils.DistanceUtils
import com.example.lakbaylaya.utils.LocationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Responsible for location updates and reverse-geocoding logic extracted from MapViewModel.
 * - startLocationUpdates() registers for GPS updates and keeps the MapState.currentLocation
 *   up-to-date.
 * - fetchReverseGeocodedLocation(lat, lon) performs reverse geocoding via repository and updates
 *   MapState.reverseGeocodedLocation while throttling updates based on movement distance.
 */
class MapLocationController(
    private val repository: MapRepository,
    private val state: MutableStateFlow<MapState>,
    private val coroutineScope: CoroutineScope,
    private val locationProvider: LocationProvider,
    private val distanceThresholdMeters: Double = 50.0
) {

    // Track last lat/lon used for reverse geocoding to avoid excessive API calls
    private var lastReverseGeocodedLatLng: Pair<Double, Double>? = null

    /** Start listening for location updates and keep state.currentLocation in sync. */
    fun startLocationUpdates() {
        locationProvider.startLocationUpdates { location ->
            // Update state with real GPS location
            state.update {
                it.copy(
                    currentLocation = Location(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                )
            }

            // Fire reverse geocoding in a coroutine to avoid blocking the provider callback
            coroutineScope.launch {
                fetchReverseGeocodedLocation(location.latitude, location.longitude)
            }
        }
    }

    /** Stop listening for location updates. */
    fun stopLocationUpdates() {
        try {
            locationProvider.stopLocationUpdates()
        } catch (e: Exception) {
            android.util.Log.w(
                "MapLocationController",
                "Failed to stop location updates: ${e.message}"
            )
        }
    }

    /**
     * Fetches reverse geocoded location to display readable location name
     * Only updates if user has moved more than distanceThresholdMeters since the last fetch.
     */
    suspend fun fetchReverseGeocodedLocation(latitude: Double, longitude: Double) {
        // Check if we need to update based on distance moved
        lastReverseGeocodedLatLng?.let { (lastLat, lastLon) ->
            val distanceMoved = DistanceUtils.calculateDistance(
                lat1 = lastLat,
                lon1 = lastLon,
                lat2 = latitude,
                lon2 = longitude
            )

            // If moved less than threshold, don't update
            if (distanceMoved < distanceThresholdMeters) {
                return
            }
        }

        // Set loading state
        state.update { it.copy(reverseGeocodedLocation = ReverseGeocodedLocation.loading()) }

        val result = repository.reverseGeocodeLocation(latitude, longitude, radius = 100)
        result.onSuccess { geocodedLocation ->
            state.update { it.copy(reverseGeocodedLocation = geocodedLocation) }
            // Update last geocoded position
            lastReverseGeocodedLatLng = Pair(latitude, longitude)
        }.onFailure {
            state.update { it.copy(reverseGeocodedLocation = ReverseGeocodedLocation.unknown()) }
        }
    }
}

