package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.WeatherPreferencesRepository
import com.example.data.model.PressureUnit
import com.example.data.model.SavedCity
import com.example.data.model.TemperatureUnit
import com.example.data.model.WeatherSettings
import com.example.data.model.WindSpeedUnit
import com.example.data.repository.WeatherRepository
import com.example.ui.state.CitySearchUiState
import com.example.ui.state.CityWeatherLoadState
import com.example.ui.state.WeatherHomeUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = WeatherPreferencesRepository(application.applicationContext)
    private val weatherRepository = WeatherRepository(application.applicationContext, preferencesRepository)

    private val _homeState = MutableStateFlow(WeatherHomeUiState())
    val homeState: StateFlow<WeatherHomeUiState> = _homeState.asStateFlow()

    private val _searchState = MutableStateFlow(CitySearchUiState())
    val searchState: StateFlow<CitySearchUiState> = _searchState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            val cached = preferencesRepository.getCachedReports()
            if (cached.isNotEmpty()) {
                _homeState.update { current ->
                    val mapped = cached.mapValues { (_, report) ->
                        CityWeatherLoadState.Success(report = report, isRefreshing = true)
                    }
                    current.copy(reportsByCityId = mapped)
                }
            }

            combine(
                preferencesRepository.savedCitiesFlow,
                preferencesRepository.selectedCityIdFlow,
                preferencesRepository.settingsFlow
            ) { cities, selectedId, settings ->
                Triple(cities, selectedId, settings)
            }.collect { (cities, selectedId, settings) ->
                val resolvedSelectedId = if (cities.any { it.id == selectedId }) {
                    selectedId
                } else {
                    cities.firstOrNull()?.id.orEmpty()
                }
                _homeState.update { current ->
                    current.copy(
                        savedCities = cities,
                        selectedCityId = resolvedSelectedId,
                        settings = settings
                    )
                }
                cities.forEach { city ->
                    val existing = _homeState.value.reportsByCityId[city.id]
                    if (existing == null || (existing is CityWeatherLoadState.Success && existing.isRefreshing)) {
                        fetchCityWeather(city, silent = existing is CityWeatherLoadState.Success)
                    }
                }
            }
        }
    }

    fun selectCity(cityId: String) {
        viewModelScope.launch {
            preferencesRepository.setSelectedCityId(cityId)
        }
    }

    fun refreshSelectedOrAll() {
        val cities = _homeState.value.savedCities
        if (cities.isEmpty()) return
        viewModelScope.launch {
            _homeState.update { it.copy(isGlobalRefreshing = true) }
            val selectedCity = cities.firstOrNull { it.id == _homeState.value.selectedCityId } ?: cities.first()
            fetchCityWeather(selectedCity, silent = true)
            cities.filter { it.id != selectedCity.id }.forEach { city ->
                fetchCityWeather(city, silent = true)
            }
            _homeState.update { it.copy(isGlobalRefreshing = false) }
        }
    }

    fun retryCity(city: SavedCity) {
        viewModelScope.launch {
            fetchCityWeather(city, silent = false)
        }
    }

    private suspend fun fetchCityWeather(city: SavedCity, silent: Boolean) {
        val prevState = _homeState.value.reportsByCityId[city.id]
        val cachedReport = when (prevState) {
            is CityWeatherLoadState.Success -> prevState.report
            is CityWeatherLoadState.Error -> prevState.cachedReport
            else -> null
        }

        if (!silent && cachedReport == null) {
            _homeState.update { state ->
                state.copy(
                    reportsByCityId = state.reportsByCityId + (city.id to CityWeatherLoadState.Loading)
                )
            }
        }

        weatherRepository.fetchWeatherForCity(city)
            .onSuccess { report ->
                _homeState.update { state ->
                    state.copy(
                        reportsByCityId = state.reportsByCityId + (city.id to CityWeatherLoadState.Success(report))
                    )
                }
            }
            .onFailure { error ->
                _homeState.update { state ->
                    val fallbackMsg = error.localizedMessage ?: "Ошибка сети"
                    val newState = if (cachedReport != null && silent) {
                        CityWeatherLoadState.Success(cachedReport.copy(), isRefreshing = false)
                    } else {
                        CityWeatherLoadState.Error(fallbackMsg, cachedReport)
                    }
                    state.copy(
                        reportsByCityId = state.reportsByCityId + (city.id to newState)
                    )
                }
            }
    }

    fun onSearchQueryChanged(query: String) {
        _searchState.update { it.copy(query = query, errorMessage = null) }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _searchState.update { it.copy(isSearching = false, results = emptyList(), errorMessage = null) }
            return
        }
        searchJob = viewModelScope.launch {
            _searchState.update { it.copy(isSearching = true) }
            delay(320)
            weatherRepository.searchCities(query)
                .onSuccess { cities ->
                    _searchState.update {
                        it.copy(isSearching = false, results = cities, errorMessage = null)
                    }
                }
                .onFailure {
                    _searchState.update {
                        it.copy(
                            isSearching = false,
                            results = emptyList(),
                            errorMessage = "Не удалось выполнить поиск городов"
                        )
                    }
                }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchState.value = CitySearchUiState()
    }

    fun addAndSelectCity(city: SavedCity) {
        viewModelScope.launch {
            val current = preferencesRepository.savedCitiesFlow.first()
            val updated = if (current.any { it.id == city.id }) {
                current
            } else {
                listOf(city) + current
            }
            preferencesRepository.saveCities(updated)
            preferencesRepository.setSelectedCityId(city.id)
            fetchCityWeather(city, silent = false)
        }
    }

    fun removeCity(cityId: String) {
        viewModelScope.launch {
            val current = preferencesRepository.savedCitiesFlow.first()
            val updated = current.filterNot { it.id == cityId }
            preferencesRepository.saveCities(updated)
            if (_homeState.value.selectedCityId == cityId) {
                val nextId = updated.firstOrNull()?.id.orEmpty()
                preferencesRepository.setSelectedCityId(nextId)
            }
            _homeState.update { state ->
                state.copy(reportsByCityId = state.reportsByCityId - cityId)
            }
        }
    }

    fun detectCurrentLocation() {
        viewModelScope.launch {
            _homeState.update { it.copy(isLocating = true, statusMessage = null) }
            weatherRepository.detectCurrentLocationCity()
                .onSuccess { city ->
                    val current = preferencesRepository.savedCitiesFlow.first()
                    val withoutOldLoc = current.filterNot { it.isCurrentLocation || it.id == city.id }
                    val updated = listOf(city) + withoutOldLoc
                    preferencesRepository.saveCities(updated)
                    preferencesRepository.setSelectedCityId(city.id)
                    _homeState.update { it.copy(isLocating = false, statusMessage = "Геопозиция: ${city.name}") }
                    fetchCityWeather(city, silent = false)
                }
                .onFailure {
                    _homeState.update {
                        it.copy(
                            isLocating = false,
                            statusMessage = "Не удалось определить координаты. Проверьте GPS."
                        )
                    }
                }
        }
    }

    fun clearStatusMessage() {
        _homeState.update { it.copy(statusMessage = null) }
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        viewModelScope.launch { preferencesRepository.updateTemperatureUnit(unit) }
    }

    fun setWindSpeedUnit(unit: WindSpeedUnit) {
        viewModelScope.launch { preferencesRepository.updateWindSpeedUnit(unit) }
    }

    fun setPressureUnit(unit: PressureUnit) {
        viewModelScope.launch { preferencesRepository.updatePressureUnit(unit) }
    }
}
