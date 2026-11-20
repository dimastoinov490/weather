package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.WeatherFormatters
import com.example.ui.components.NativeOsmdroidWeatherMap
import com.example.ui.components.WeatherMapController
import com.example.ui.components.WeatherMapMarkerInfo
import com.example.ui.components.rememberNetworkAvailability
import com.example.ui.components.weatherConditionEmoji
import com.example.ui.state.CityWeatherLoadState
import com.example.ui.state.WeatherHomeUiState
import com.example.ui.theme.CoolCyan
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedText
import com.example.ui.theme.ObsidianBg
import java.util.Locale

private val VioletGlassBg = Color(0xDE130E26)
private val VioletGlassBorder = Color(0x668B5CF6)
private val VioletNeonAccent = Color(0xFFA78BFA)

@Composable
fun WeatherMapScreen(
    uiState: WeatherHomeUiState,
    onOpenCities: () -> Unit,
    onNavigateBack: () -> Unit
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
    var warningMessage by remember { mutableStateOf<String?>(null) }
    var mapController by remember { mutableStateOf(WeatherMapController()) }
    val isOnline = rememberNetworkAvailability()

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
            windText = windText
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        NativeOsmdroidWeatherMap(
            markerInfo = markerInfo,
            initialZoom = zoom,
            isOnline = isOnline,
            onZoomChanged = { newZoom -> zoom = newZoom },
            onRadarStatusMessage = { msg -> warningMessage = msg },
            onControllerReady = { controller -> mapController = controller },
            modifier = Modifier.fillMaxSize()
        )

        // Top floating header card + optional network/radar warning pill
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VioletGlassSurface(
                cornerRadius = 20.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenCities)
                    .testTag("map_header_city_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("map_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = IceWhite
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeCity?.name ?: stringResource(R.string.map_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = IceWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = buildString {
                                    if (report != null) {
                                        append(report.conditionTitle)
                                        append(" • ")
                                    }
                                    append(
                                        String.format(
                                            Locale.US,
                                            "%.2f°, %.2f° • z%d",
                                            markerInfo.latitude,
                                            markerInfo.longitude,
                                            zoom
                                        )
                                    )
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (report != null) {
                        Text(
                            text = markerInfo.temperatureText,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = CoolCyan,
                            modifier = Modifier.padding(start = 8.dp, end = 10.dp)
                        )
                    }
                }
            }

            if (!isOnline || warningMessage != null) {
                val statusText = if (!isOnline) {
                    stringResource(R.string.map_offline_message)
                } else {
                    warningMessage ?: stringResource(R.string.map_tile_error_message)
                }
                VioletGlassSurface(
                    cornerRadius = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WifiOff,
                                contentDescription = null,
                                tint = VioletNeonAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = IceWhite
                            )
                        }
                        IconButton(
                            onClick = {
                                warningMessage = null
                                mapController.reloadRadar()
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("map_retry_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = stringResource(R.string.retry_button),
                                tint = VioletNeonAccent
                            )
                        }
                    }
                }
            }
        }

        // Simple Zoom (+ / -) and Recenter buttons on bottom-right (no bottom timeline bar)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VioletMapControlButton(
                onClick = { mapController.recenter() },
                highlighted = true,
                tag = "map_recenter"
            ) {
                Icon(
                    imageVector = Icons.Outlined.MyLocation,
                    contentDescription = stringResource(R.string.map_recenter),
                    tint = VioletNeonAccent
                )
            }

            VioletMapControlButton(
                onClick = { mapController.zoomIn() },
                tag = "map_zoom_in"
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.map_zoom_in),
                    tint = IceWhite
                )
            }

            VioletMapControlButton(
                onClick = { mapController.zoomOut() },
                tag = "map_zoom_out"
            ) {
                Icon(
                    imageVector = Icons.Outlined.Remove,
                    contentDescription = stringResource(R.string.map_zoom_out),
                    tint = IceWhite
                )
            }
        }
    }
}

@Composable
private fun VioletGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = VioletGlassBg,
        border = BorderStroke(1.dp, VioletGlassBorder),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x336D28D9),
                        Color(0x1A1E1B4B)
                    )
                )
            )
        ) {
            content()
        }
    }
}

@Composable
private fun VioletMapControlButton(
    onClick: () -> Unit,
    highlighted: Boolean = false,
    tag: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .background(VioletGlassBg)
            .border(
                1.dp,
                if (highlighted) VioletNeonAccent.copy(alpha = 0.85f) else VioletGlassBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
