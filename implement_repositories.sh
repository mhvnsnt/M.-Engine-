#!/bin/bash
set -e

cat << 'KOTLIN' > app/src/main/java/com/example/data/LocationRepository.kt
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
}
KOTLIN

cat << 'KOTLIN' > app/src/main/java/com/example/data/AstroNumerologyRepository.kt
package com.example.data

import kotlinx.coroutines.flow.Flow

class AstroNumerologyRepository(
    private val astroDao: AstroDao
) {
    val astroProfileFlow: Flow<AstroProfile?> = astroDao.getAstroProfileFlow()

    suspend fun updateAstroProfile(profile: AstroProfile) {
        astroDao.insertAstroProfile(profile)
    }

    // Simplified numerology calculation
    fun calculateLifePath(birthDateStr: String): Int {
        // Assume format YYYY-MM-DD
        val digits = birthDateStr.filter { it.isDigit() }
        var sum = digits.map { it.toString().toInt() }.sum()
        while (sum > 9 && sum != 11 && sum != 22 && sum != 33) {
            sum = sum.toString().map { it.toString().toInt() }.sum()
        }
        return sum
    }
    
    // In a real app, this would use Swiss Ephemeris for transits.
    // We provide a stub for the LLM context.
    fun getCurrentTransitsContext(): String {
        return "Current Astrological Weather: Leo Season, Active Transits. Numerology: Universal Day 5."
    }
}
KOTLIN

