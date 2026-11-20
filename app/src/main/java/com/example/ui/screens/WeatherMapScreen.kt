package com.example.ui.screens

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.WeatherFormatters
import com.example.ui.components.GlassCard
import com.example.ui.components.InteractiveWeatherWebViewMap
import com.example.ui.components.WeatherMapController
import com.example.ui.components.WeatherMapLayer
import com.example.ui.components.WeatherMapMarkerInfo
import com.example.ui.components.isNetworkAvailable
import com.example.ui.components.weatherConditionEmoji
import com.example.ui.state.CityWeatherLoadState
import com.example.ui.state.WeatherHomeUiState
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CoolCyan
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedText
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WeatherMapScreen(
    uiState: WeatherHomeUiState,
    onOpenCities: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activeCity = uiState.savedCities.firstOrNull { it.id == uiState.selectedCityId }
        ?: uiState.savedCities.firstOrNull()
    val report = activeCity?.let { uiState.reportsByCityId[it.id] }?.let { state ->
        when (state) {
            is CityWeatherLoadState.Success -> state.report
            is CityWeatherLoadState.Error -> state.cachedReport
            else -> null
        }
    }

    var zoom by remember { mutableIntStateOf(6) }
    var activeLayer by remember { mutableStateOf(WeatherMapLayer.PRECIPITATION) }
    var layerTimestampSec by remember { mutableLongStateOf(0L) }
    var cloudsIsFallback by remember { mutableStateOf(false) }
    var mapController by remember { mutableStateOf(WeatherMapController()) }
    var isOnline by remember { mutableStateOf(isNetworkAvailable(context)) }

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

    val layerBadgeText = remember(activeLayer, layerTimestampSec, cloudsIsFallback) {
        val timeLabel = if (layerTimestampSec > 0L) {
            val fmt = SimpleDateFormat("HH:mm", Locale.forLanguageTag("ru-RU"))
            fmt.format(Date(layerTimestampSec * 1000L))
        } else {
            null
        }
        when (activeLayer) {
            WeatherMapLayer.PRECIPITATION -> {
                if (timeLabel != null) "Радар RainViewer • $timeLabel" else "Радар осадков RainViewer"
            }
            WeatherMapLayer.CLOUDS -> {
                when {
                    timeLabel != null -> "Облачность RainViewer • $timeLabel"
                    cloudsIsFallback -> "Спутник облачности • Live"
                    else -> "Слой облачности • Live"
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        InteractiveWeatherWebViewMap(
            markerInfo = markerInfo,
            activeLayer = activeLayer,
            zoom = zoom,
            onZoomChanged = { newZoom -> zoom = newZoom },
            onLayerStatusChanged = { _, ts, isFallback ->
                layerTimestampSec = ts
                cloudsIsFallback = isFallback
            },
            onControllerReady = { controller -> mapController = controller },
            modifier = Modifier.fillMaxSize()
        )

        // Top floating header card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassCard(
                cornerRadius = 20.dp,
                surfaceAlpha = 0.90f,
                amberGlow = true,
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
                            color = AmberPrimary,
                            modifier = Modifier.padding(start = 8.dp, end = 10.dp)
                        )
                    }
                }
            }

            if (!isOnline) {
                GlassCard(
                    cornerRadius = 16.dp,
                    surfaceAlpha = 0.92f,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
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
                                tint = AmberPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.map_offline_message),
                                style = MaterialTheme.typography.bodySmall,
                                color = IceWhite
                            )
                        }
                        IconButton(
                            onClick = {
                                isOnline = isNetworkAvailable(context)
                                mapController.reload()
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("map_retry_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = stringResource(R.string.retry_button),
                                tint = AmberPrimary
                            )
                        }
                    }
                }
            }
        }

        // Bottom-left layer switcher (Осадки / Облачность) + live status pill
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xD90D121E))
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = layerBadgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = IceWhite.copy(alpha = 0.88f)
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xE6111623))
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MapLayerToggleChip(
                    label = stringResource(R.string.map_layer_precipitation),
                    icon = Icons.Outlined.WaterDrop,
                    selected = activeLayer == WeatherMapLayer.PRECIPITATION,
                    accentColor = CoolCyan,
                    tag = "map_layer_precipitation",
                    onClick = { activeLayer = WeatherMapLayer.PRECIPITATION }
                )
                MapLayerToggleChip(
                    label = stringResource(R.string.map_layer_clouds),
                    icon = Icons.Outlined.Cloud,
                    selected = activeLayer == WeatherMapLayer.CLOUDS,
                    accentColor = AmberPrimary,
                    tag = "map_layer_clouds",
                    onClick = { activeLayer = WeatherMapLayer.CLOUDS }
                )
            }
        }

        // Bottom-right recenter & zoom controls (+ / -)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MapControlButton(
                onClick = { mapController.recenter() },
                tag = "map_recenter"
            ) {
                Icon(
                    imageVector = Icons.Outlined.MyLocation,
                    contentDescription = stringResource(R.string.map_recenter),
                    tint = AmberPrimary
                )
            }
            MapControlButton(
                onClick = { mapController.zoomIn() },
                tag = "map_zoom_in"
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.map_zoom_in),
                    tint = IceWhite
                )
            }
            MapControlButton(
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
private fun MapLayerToggleChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(44.dp)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) accentColor.copy(alpha = 0.20f) else Color.Transparent
            )
            .then(
                if (selected) {
                    Modifier.border(1.dp, accentColor.copy(alpha = 0.70f), RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) accentColor else MutedText,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = if (selected) IceWhite else MutedText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MapControlButton(
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
            .background(Color(0xE6111623))
            .border(
                1.dp,
                if (highlighted) AmberPrimary else ObsidianBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
