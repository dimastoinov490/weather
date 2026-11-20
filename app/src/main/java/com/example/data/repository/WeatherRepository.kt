package com.example.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
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
import java.util.Locale
import kotlin.coroutines.resume

class WeatherRepository(
    private val context: Context,
    val preferences: WeatherPreferencesRepository
) {
    private val forecastApi = NetworkModule.forecastApi
    private val airQualityApi = NetworkModule.airQualityApi
    private val geocodingApi = NetworkModule.geocodingApi

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
            val client = LocationServices.getFusedLocationProviderClient(context)
            val location: Location = suspendCancellableCoroutine<Location?> { continuation ->
                val cts = CancellationTokenSource()
                continuation.invokeOnCancellation { cts.cancel() }
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            continuation.resume(loc)
                        } else {
                            client.lastLocation
                                .addOnSuccessListener { last -> continuation.resume(last) }
                                .addOnFailureListener { continuation.resume(null) }
                        }
                    }
                    .addOnFailureListener {
                        client.lastLocation
                            .addOnSuccessListener { last -> continuation.resume(last) }
                            .addOnFailureListener { continuation.resume(null) }
                    }
            } ?: error("Не удалось определить координаты устройства")

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

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocodeCity(lat: Double, lon: Double): Triple<String, String, String> {
        return runCatching {
            val geocoder = Geocoder(context, Locale.forLanguageTag("ru-RU"))
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(lat, lon, 1) { list ->
                        cont.resume(list)
                    }
                }
            } else {
                geocoder.getFromLocation(lat, lon, 1).orEmpty()
            }
            val addr = addresses.firstOrNull()
            val name = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea ?: "Текущая геопозиция"
            val region = addr?.adminArea.orEmpty().takeIf { it != name }.orEmpty()
            val country = addr?.countryName.orEmpty()
            Triple(name, region, country)
        }.getOrDefault(Triple("Моё местоположение", "", ""))
    }
}
