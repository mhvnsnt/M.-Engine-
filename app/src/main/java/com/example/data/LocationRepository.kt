package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationRepository(
    private val context: Context,
    private val locationDao: LocationDao
) {
    val userConstraintsFlow = locationDao.getUserConstraintsFlow()
    val allRegionsFlow = locationDao.getAllRegionsFlow()

    suspend fun updateUserConstraints(constraints: UserConstraints) {
        locationDao.insertUserConstraints(constraints)
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocationAndRegion(): RegionProfile? = withContext(Dispatchers.IO) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val provider = LocationManager.NETWORK_PROVIDER
            val location: Location? = locationManager.getLastKnownLocation(provider)
            
            if (location != null) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                
                var regionId = "UNKNOWN"
                var displayName = "Unknown Region"
                var geocodedStr = ""
                
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val city = address.locality ?: address.subAdminArea ?: ""
                    val state = address.adminArea ?: ""
                    regionId = "${city}_${state}".lowercase().replace(" ", "_")
                    displayName = if (city.isNotEmpty() && state.isNotEmpty()) "$city, $state" else city.ifEmpty { state }
                    geocodedStr = address.getAddressLine(0) ?: displayName
                }
                
                // Save Snapshot
                locationDao.insertLocationSnapshot(
                    LocationSnapshot(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        geocodedAddress = geocodedStr
                    )
                )
                
                // Handle Region
                var region = locationDao.getRegionProfile(regionId)
                val now = System.currentTimeMillis()
                if (region == null) {
                    region = RegionProfile(
                        regionId = regionId,
                        displayName = displayName,
                        firstSeenTimestamp = now,
                        lastActiveTimestamp = now
                    )
                } else {
                    region = region.copy(lastActiveTimestamp = now)
                }
                locationDao.insertRegionProfile(region)
                return@withContext region
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun deleteAllRegions() {
        locationDao.deleteAllRegions()
    }

    suspend fun deleteSnapshots() {
        locationDao.deleteSnapshots()
    }
}
