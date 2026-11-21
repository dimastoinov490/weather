package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.WeatherFormatters
import com.example.ui.components.MapBgPrimary
import com.example.ui.components.MapBgSecondary
import com.example.ui.components.MapBottomWeatherPanel
import com.example.ui.components.MapControlSquareButton
import com.example.ui.components.MapModeAndActionRow
import com.example.ui.components.MapStatusWarningPill
import com.example.ui.components.MapTextSecondary
import com.example.ui.components.MapTopHeaderRow
import com.example.ui.components.NativeOsmdroidWeatherMap
import com.example.ui.components.PrecipitationLegendCard
import com.example.ui.components.RadarFrameInfo
import com.example.ui.components.RadarLayerToggleChip
import com.example.ui.components.RadarTimelineBar
import com.example.ui.components.WeatherMapController
import com.example.ui.components.WeatherMapMarkerInfo
import com.example.ui.components.WeatherMapMode
import com.example.ui.components.rememberNetworkAvailability
import com.example.ui.components.weatherConditionEmoji
import com.example.ui.state.CityWeatherLoadState
import com.example.ui.state.WeatherHomeUiState
import kotlinx.coroutines.delay

@Composable
fun WeatherMapScreen(
    uiState: WeatherHomeUiState,
    onOpenCities: () -> Unit,
    onNavigateBack: () -> Unit,
    onSelectCity: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    onDetectLocation: () -> Unit = {},
    onOpenForecastDetail: () -> Unit = {}
) {
    val activeCity = uiState.savedCities.firstOrNull { it.id == uiState.selectedCityId }
        ?: uiState.savedCities.firstOrNull()
    val report = activeCity?.let { uiState.reportsByCityId[it.id] }?.let { state ->
        when (state) {
            is CityWeatherLoadState.Success -> state.report
            is CityWeatherLoadState.Error -> state.cachedReport
            else -> null
        }
    }

    var zoom by remember { mutableIntStateOf(7) }
    var mapMode by remember { mutableStateOf(WeatherMapMode.MAP) }
    var isRadarEnabled by remember { mutableStateOf(true) }
    var warningMessage by remember { mutableStateOf<String?>(null) }
    var mapController by remember { mutableStateOf(WeatherMapController()) }
    var radarFrames by remember { mutableStateOf<List<RadarFrameInfo>>(emptyList()) }
    var selectedRadarIndex by remember { mutableIntStateOf(0) }
    var isRadarPlaying by remember { mutableStateOf(false) }
    var selectedHourIndex by remember { mutableStateOf<Int?>(null) }
    var showCityMenu by remember { mutableStateOf(false) }
    val isOnline = rememberNetworkAvailability()

    LaunchedEffect(activeCity?.id) {
        selectedHourIndex = null
    }

    LaunchedEffect(mapMode) {
        if (mapMode != WeatherMapMode.FORECAST) {
            isRadarPlaying = false
        }
    }

    LaunchedEffect(isRadarPlaying, isRadarEnabled, radarFrames) {
        if (!isRadarPlaying || !isRadarEnabled || radarFrames.size <= 1) return@LaunchedEffect
        while (true) {
            delay(900L)
            selectedRadarIndex = (selectedRadarIndex + 1) % radarFrames.size
        }
    }

    val markerInfo = remember(activeCity, report, uiState.settings) {
        val lat = activeCity?.latitude ?: 55.7558
        val lon = activeCity?.longitude ?: 37.6173
        val cityName = activeCity?.name ?: "Москва"
        val tempText = if (report != null) {
            WeatherFormatters.formatTemp(report.temperatureC, uiState.settings.temperatureUnit)
        } else {
            "--°"
        }
        val condText = report?.conditionTitle ?: "Загрузка погоды"
        val emoji = if (report != null) {
            weatherConditionEmoji(report.weatherCode, report.isDay)
        } else {
            "⛅"
        }
        val humidityText = if (report != null) "${report.humidityPercent}%" else "--"
        val windText = if (report != null) {
            WeatherFormatters.formatWind(report.windSpeedKmh, uiState.settings.windSpeedUnit)
        } else {
            "--"
        }
        WeatherMapMarkerInfo(
            latitude = lat,
            longitude = lon,
            cityName = cityName,
            temperatureText = tempText,
            conditionText = condText,
            iconEmoji = emoji,
            humidityText = humidityText,
            windText = windText,
            isCurrentLocation = activeCity?.isCurrentLocation == true
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MapBgPrimary)
    ) {
        NativeOsmdroidWeatherMap(
            markerInfo = markerInfo,
            initialZoom = zoom,
            mapMode = mapMode,
            isRadarEnabled = isRadarEnabled,
            selectedRadarIndex = selectedRadarIndex,
            locationRecenterToken = uiState.locationRecenterToken,
            isOnline = isOnline,
            onZoomChanged = { newZoom -> zoom = newZoom },
            onRadarFramesLoaded = { frames, defaultIndex ->
                radarFrames = frames
                if (frames.isNotEmpty()) {
                    selectedRadarIndex = defaultIndex.coerceIn(0, frames.lastIndex)
                }
            },
            onRadarStatusMessage = { msg -> warningMessage = msg },
            onControllerReady = { controller -> mapController = controller },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MapTopHeaderRow(
                activeCity = activeCity,
                savedCities = uiState.savedCities,
                reportsByCityId = uiState.reportsByCityId,
                settings = uiState.settings,
                showCityMenu = showCityMenu,
                onToggleCityMenu = { showCityMenu = it },
                onSelectCity = { cityId ->
                    showCityMenu = false
                    onSelectCity(cityId)
                },
                onOpenCities = {
                    showCityMenu = false
                    onOpenCities()
                },
                onNavigateBack = onNavigateBack
            )

            MapModeAndActionRow(
                selectedMode = mapMode,
                onSelectMode = { mode ->
                    mapMode = mode
                    if (mode == WeatherMapMode.PRECIPITATION || mode == WeatherMapMode.FORECAST) {
                        isRadarEnabled = true
                    }
                    if (mode == WeatherMapMode.MAP && radarFrames.isNotEmpty()) {
                        val lastLive = radarFrames.indexOfLast { !it.isNowcast }
                        selectedRadarIndex = if (lastLive >= 0) lastLive else radarFrames.lastIndex
                    }
                },
                onRecenterAndRefresh = {
                    mapController.recenter()
                    mapController.reloadRadar()
                    onRefresh()
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrecipitationLegendCard(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .widthIn(max = 220.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                val currentFrame = radarFrames.getOrNull(selectedRadarIndex)
                RadarLayerToggleChip(
                    isEnabled = isRadarEnabled,
                    frame = currentFrame,
                    showTime = mapMode == WeatherMapMode.PRECIPITATION,
                    onToggle = {
                        isRadarEnabled = !isRadarEnabled
                        if (!isRadarEnabled) {
                            isRadarPlaying = false
                        }
                    }
                )
            }

            if (!isOnline || warningMessage != null) {
                val statusText = if (!isOnline) {
                    stringResource(R.string.map_offline_message)
                } else {
                    warningMessage ?: stringResource(R.string.map_tile_error_message)
                }
                MapStatusWarningPill(
                    message = statusText,
                    onRetry = {
                        warningMessage = null
                        mapController.reloadRadar()
                        onRefresh()
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MapControlSquareButton(
                        onClick = onDetectLocation,
                        tag = "map_gps_button",
                        contentDescription = stringResource(R.string.cd_my_location),
                        icon = Icons.Outlined.MyLocation,
                        highlighted = activeCity?.isCurrentLocation == true,
                        isLoading = uiState.isLocating
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MapBgSecondary.copy(alpha = 0.78f)
                    ) {
                        Text(
                            text = stringResource(R.string.map_attribution),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = MapTextSecondary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MapControlSquareButton(
                        onClick = { mapController.zoomIn() },
                        tag = "map_zoom_in",
                        contentDescription = stringResource(R.string.map_zoom_in),
                        icon = Icons.Outlined.Add
                    )
                    MapControlSquareButton(
                        onClick = { mapController.zoomOut() },
                        tag = "map_zoom_out",
                        contentDescription = stringResource(R.string.map_zoom_out),
                        icon = Icons.Outlined.Remove
                    )
                }
            }

            AnimatedVisibility(
                visible = mapMode == WeatherMapMode.FORECAST && isRadarEnabled,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                RadarTimelineBar(
                    frames = radarFrames,
                    selectedIndex = selectedRadarIndex,
                    isPlaying = isRadarPlaying,
                    onTogglePlay = {
                        if (radarFrames.size > 1) {
                            isRadarPlaying = !isRadarPlaying
                        }
                    },
                    onSelectIndex = { idx ->
                        isRadarPlaying = false
                        selectedRadarIndex = idx.coerceIn(0, (radarFrames.size - 1).coerceAtLeast(0))
                    },
                    onRetry = { mapController.reloadRadar() }
                )
            }

            MapBottomWeatherPanel(
                report = report,
                settings = uiState.settings,
                selectedHourIndex = selectedHourIndex,
                onSelectHour = { index ->
                    selectedHourIndex = if (selectedHourIndex == index) null else index
                },
                onOpenForecastDetail = onOpenForecastDetail
            )
        }
    }
}
