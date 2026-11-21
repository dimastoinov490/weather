package com.example.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.data.local.WeatherPreferencesRepository
import com.example.data.model.SavedCity
import com.example.data.model.WeatherMapper
import com.example.data.model.WeatherReport
import com.example.data.remote.NetworkModule
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

class LocationPermissionMissingException : Exception("Разрешите доступ к геолокации в настройках приложения")
class LocationServicesDisabledException : Exception("Геолокация отключена в настройках устройства. Включите GPS и попробуйте снова.")
class LocationUnavailableException : Exception("Не удалось определить текущие координаты. Попробуйте ещё раз.")

class WeatherRepository(
    private val context: Context,
    val preferences: WeatherPreferencesRepository
) {
    private val forecastApi = NetworkModule.forecastApi
    private val airQualityApi = NetworkModule.airQualityApi
    private val geocodingApi = NetworkModule.geocodingApi
    private val nominatimApi = NetworkModule.nominatimApi

    suspend fun fetchWeatherForCity(city: SavedCity): Result<WeatherReport> = withContext(Dispatchers.IO) {
        runCatching {
            coroutineScope {
                val forecastDeferred = async {
                    forecastApi.getForecast(
                        latitude = city.latitude,
                        longitude = city.longitude
                    )
                }
                val airQualityDeferred = async {
                    runCatching {
                        airQualityApi.getAirQuality(
                            latitude = city.latitude,
                            longitude = city.longitude
                        )
                    }.getOrNull()
                }
                val forecast = forecastDeferred.await()
                val airQuality = airQualityDeferred.await()
                val report = WeatherMapper.mapToReport(city, forecast, airQuality)
                preferences.cacheReport(report)
                report
            }
        }
    }

    suspend fun searchCities(query: String): Result<List<SavedCity>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext Result.success(emptyList())
        runCatching {
            val response = geocodingApi.searchCities(query = trimmed, count = 12, language = "ru")
            response.results.orEmpty().map { dto ->
                SavedCity(
                    id = "city_${dto.id}",
                    name = dto.name,
                    region = dto.admin1.orEmpty(),
                    country = dto.country.orEmpty(),
                    latitude = dto.latitude,
                    longitude = dto.longitude
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun detectCurrentLocationCity(): Result<SavedCity> = withContext(Dispatchers.IO) {
        runCatching {
            val hasFine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasFine && !hasCoarse) {
                throw LocationPermissionMissingException()
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager != null && !LocationManagerCompat.isLocationEnabled(locationManager)) {
                throw LocationServicesDisabledException()
            }

            val priority = if (hasFine) {
                Priority.PRIORITY_HIGH_ACCURACY
            } else {
                Priority.PRIORITY_BALANCED_POWER_ACCURACY
            }

            val location = getFusedLocation(priority)
                ?: getSystemLocationManagerFallback(locationManager, hasFine)
                ?: throw LocationUnavailableException()

            val (cityName, regionName, countryName) = reverseGeocodeCity(
                location.latitude,
                location.longitude
            )

            SavedCity(
                id = "current_location",
                name = cityName,
                region = regionName,
                country = countryName,
                latitude = location.latitude,
                longitude = location.longitude,
                isCurrentLocation = true
            )
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getFusedLocation(priority: Int): Location? {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val current = withTimeoutOrNull(8_000L) {
            suspendCancellableCoroutine<Location?> { continuation ->
                val cts = CancellationTokenSource()
                continuation.invokeOnCancellation { cts.cancel() }
                client.getCurrentLocation(priority, cts.token)
                    .addOnSuccessListener { loc ->
                        if (continuation.isActive) continuation.resume(loc)
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            }
        }
        if (current != null) return current

        return withTimeoutOrNull(2_000L) {
            suspendCancellableCoroutine<Location?> { continuation ->
                client.lastLocation
                    .addOnSuccessListener { last ->
                        if (continuation.isActive) continuation.resume(last)
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getSystemLocationManagerFallback(
        locationManager: LocationManager?,
        hasFine: Boolean
    ): Location? {
        if (locationManager == null) return null
        val providers = buildList {
            if (hasFine && runCatching { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (runCatching { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
            add(LocationManager.PASSIVE_PROVIDER)
        }

        for (provider in providers) {
            val fresh = withTimeoutOrNull(4_000L) {
                suspendCancellableCoroutine<Location?> { continuation ->
                    val cancelSignal = android.os.CancellationSignal()
                    continuation.invokeOnCancellation { cancelSignal.cancel() }
                    runCatching {
                        LocationManagerCompat.getCurrentLocation(
                            locationManager,
                            provider,
                            cancelSignal,
                            ContextCompat.getMainExecutor(context)
                        ) { loc ->
                            if (continuation.isActive) continuation.resume(loc)
                        }
                    }.onFailure {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            }
            if (fresh != null) return fresh
        }

        val lastCandidates = providers.mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }
        return lastCandidates.maxByOrNull { it.time }
    }

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocodeCity(lat: Double, lon: Double): Triple<String, String, String> {
        val fromAndroidGeocoder = runCatching {
            if (!Geocoder.isPresent()) return@runCatching null
            val geocoder = Geocoder(context, Locale.forLanguageTag("ru-RU"))
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                withTimeoutOrNull(4_000L) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(lat, lon, 1) { list ->
                            if (cont.isActive) cont.resume(list)
                        }
                    }
                }.orEmpty()
            } else {
                geocoder.getFromLocation(lat, lon, 1).orEmpty()
            }
            val addr = addresses.firstOrNull() ?: return@runCatching null
            val name = addr.locality
                ?: addr.subLocality
                ?: addr.subAdminArea
                ?: addr.adminArea
                ?: addr.featureName
            if (name.isNullOrBlank()) return@runCatching null
            val region = addr.adminArea.orEmpty().takeIf { it != name }.orEmpty()
            val country = addr.countryName.orEmpty()
            Triple(name, region, country)
        }.getOrNull()

        if (fromAndroidGeocoder != null) {
            return fromAndroidGeocoder
        }

        val fromNominatim = runCatching {
            val response = nominatimApi.reverseGeocode(latitude = lat, longitude = lon)
            val addr = response.address
            val name = addr?.city
                ?: addr?.town
                ?: addr?.village
                ?: addr?.hamlet
                ?: addr?.municipality
                ?: addr?.county
                ?: addr?.state
                ?: response.name
            if (name.isNullOrBlank()) return@runCatching null
            val region = (addr?.state ?: addr?.region).orEmpty().takeIf { it != name }.orEmpty()
            val country = addr?.country.orEmpty()
            Triple(name, region, country)
        }.getOrNull()

        if (fromNominatim != null) {
            return fromNominatim
        }

        val fallbackCoords = String.format(Locale.US, "%.2f°, %.2f°", lat, lon)
        return Triple(fallbackCoords, "", "")
    }
}
