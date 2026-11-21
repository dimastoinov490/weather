package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.PressureUnit
import com.example.data.model.SavedCity
import com.example.data.model.TemperatureUnit
import com.example.data.model.WeatherReport
import com.example.data.model.WeatherSettings
import com.example.data.model.WindSpeedUnit
import com.example.data.remote.NetworkModule
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.weatherDataStore: DataStore<Preferences> by preferencesDataStore(name = "nebo_weather_prefs")

class WeatherPreferencesRepository(private val context: Context) {

    private val moshi = NetworkModule.sharedMoshi()
    private val cityListType = Types.newParameterizedType(List::class.java, SavedCity::class.java)
    private val cityListAdapter = moshi.adapter<List<SavedCity>>(cityListType)
    private val reportMapType = Types.newParameterizedType(
        Map::class.java,
        String::class.java,
        WeatherReport::class.java
    )
    private val reportMapAdapter = moshi.adapter<Map<String, WeatherReport>>(reportMapType)

    companion object {
        private val KEY_SAVED_CITIES = stringPreferencesKey("saved_cities_json")
        private val KEY_SELECTED_CITY_ID = stringPreferencesKey("selected_city_id")
        private val KEY_TEMP_UNIT = stringPreferencesKey("temp_unit")
        private val KEY_WIND_UNIT = stringPreferencesKey("wind_unit")
        private val KEY_PRESSURE_UNIT = stringPreferencesKey("pressure_unit")
        private val KEY_CACHED_REPORTS = stringPreferencesKey("cached_reports_json")
        private val KEY_LOCATION_PROMPTED = booleanPreferencesKey("location_permission_prompted")

        val DEFAULT_CITIES = listOf(
            SavedCity(
                id = "moscow_55.7558_37.6173",
                name = "Москва",
                region = "Москва",
                country = "Россия",
                latitude = 55.7558,
                longitude = 37.6173
            ),
            SavedCity(
                id = "spb_59.9343_30.3351",
                name = "Санкт-Петербург",
                region = "Санкт-Петербург",
                country = "Россия",
                latitude = 59.9343,
                longitude = 30.3351
            ),
            SavedCity(
                id = "sochi_43.5855_39.7231",
                name = "Сочи",
                region = "Краснодарский край",
                country = "Россия",
                latitude = 43.5855,
                longitude = 39.7231
            )
        )
    }

    val savedCitiesFlow: Flow<List<SavedCity>> = context.weatherDataStore.data.map { prefs ->
        val json = prefs[KEY_SAVED_CITIES]
        if (json.isNullOrBlank()) {
            DEFAULT_CITIES
        } else {
            runCatching { cityListAdapter.fromJson(json) }.getOrNull() ?: DEFAULT_CITIES
        }
    }

    val selectedCityIdFlow: Flow<String> = context.weatherDataStore.data.map { prefs ->
        prefs[KEY_SELECTED_CITY_ID] ?: DEFAULT_CITIES.first().id
    }

    val settingsFlow: Flow<WeatherSettings> = context.weatherDataStore.data.map { prefs ->
        val tempUnit = runCatching {
            TemperatureUnit.valueOf(prefs[KEY_TEMP_UNIT] ?: TemperatureUnit.CELSIUS.name)
        }.getOrDefault(TemperatureUnit.CELSIUS)

        val windUnit = runCatching {
            WindSpeedUnit.valueOf(prefs[KEY_WIND_UNIT] ?: WindSpeedUnit.MS.name)
        }.getOrDefault(WindSpeedUnit.MS)

        val pressureUnit = runCatching {
            PressureUnit.valueOf(prefs[KEY_PRESSURE_UNIT] ?: PressureUnit.MMHG.name)
        }.getOrDefault(PressureUnit.MMHG)

        WeatherSettings(
            temperatureUnit = tempUnit,
            windSpeedUnit = windUnit,
            pressureUnit = pressureUnit
        )
    }

    suspend fun saveCities(cities: List<SavedCity>) {
        context.weatherDataStore.edit { prefs ->
            prefs[KEY_SAVED_CITIES] = cityListAdapter.toJson(cities)
        }
    }

    suspend fun setSelectedCityId(cityId: String) {
        context.weatherDataStore.edit { prefs ->
            prefs[KEY_SELECTED_CITY_ID] = cityId
        }
    }

    suspend fun updateTemperatureUnit(unit: TemperatureUnit) {
        context.weatherDataStore.edit { prefs ->
            prefs[KEY_TEMP_UNIT] = unit.name
        }
    }

    suspend fun updateWindSpeedUnit(unit: WindSpeedUnit) {
        context.weatherDataStore.edit { prefs ->
            prefs[KEY_WIND_UNIT] = unit.name
        }
    }

    suspend fun updatePressureUnit(unit: PressureUnit) {
        context.weatherDataStore.edit { prefs ->
            prefs[KEY_PRESSURE_UNIT] = unit.name
        }
    }

    suspend fun getCachedReports(): Map<String, WeatherReport> {
        val prefs = context.weatherDataStore.data.first()
        val json = prefs[KEY_CACHED_REPORTS] ?: return emptyMap()
        return runCatching { reportMapAdapter.fromJson(json) }.getOrNull() ?: emptyMap()
    }

    suspend fun cacheReport(report: WeatherReport) {
        context.weatherDataStore.edit { prefs ->
            val currentMap = prefs[KEY_CACHED_REPORTS]?.let {
                runCatching { reportMapAdapter.fromJson(it) }.getOrNull()
            }?.toMutableMap() ?: mutableMapOf()
            currentMap[report.city.id] = report
            prefs[KEY_CACHED_REPORTS] = reportMapAdapter.toJson(currentMap)
        }
    }

    val hasPromptedLocationFlow: Flow<Boolean> = context.weatherDataStore.data.map { prefs ->
        prefs[KEY_LOCATION_PROMPTED] ?: false
    }

    suspend fun markLocationPermissionPrompted() {
        context.weatherDataStore.edit { prefs ->
            prefs[KEY_LOCATION_PROMPTED] = true
        }
    }
}
