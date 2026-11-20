package com.example.ui.state

import com.example.data.model.SavedCity
import com.example.data.model.WeatherReport
import com.example.data.model.WeatherSettings

sealed interface CityWeatherLoadState {
    data object Loading : CityWeatherLoadState
    data class Success(val report: WeatherReport, val isRefreshing: Boolean = false) : CityWeatherLoadState
    data class Error(val message: String, val cachedReport: WeatherReport? = null) : CityWeatherLoadState
}

data class WeatherHomeUiState(
    val savedCities: List<SavedCity> = emptyList(),
    val selectedCityId: String = "",
    val reportsByCityId: Map<String, CityWeatherLoadState> = emptyMap(),
    val settings: WeatherSettings = WeatherSettings(),
    val isGlobalRefreshing: Boolean = false,
    val isLocating: Boolean = false,
    val statusMessage: String? = null
)

data class CitySearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<SavedCity> = emptyList(),
    val errorMessage: String? = null
)
