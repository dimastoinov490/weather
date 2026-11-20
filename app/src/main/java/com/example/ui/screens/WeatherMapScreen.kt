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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.WeatherFormatters
import com.example.ui.components.GlassCard
import com.example.ui.components.LiveWeatherRadarMap
import com.example.ui.state.CityWeatherLoadState
import com.example.ui.state.WeatherHomeUiState
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CoolCyan
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedText
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import java.util.Locale

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

    var zoom by remember { mutableIntStateOf(6) }
    var showRadar by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        if (activeCity != null) {
            LiveWeatherRadarMap(
                latitude = activeCity.latitude,
                longitude = activeCity.longitude,
                zoom = zoom,
                showRadarOverlay = showRadar,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            GlassCard(
                cornerRadius = 20.dp,
                surfaceAlpha = 0.90f,
                amberGlow = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenCities)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                        Column {
                            Text(
                                text = activeCity?.name ?: "Карта погоды",
                                style = MaterialTheme.typography.titleLarge,
                                color = IceWhite
                            )
                            if (activeCity != null) {
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "%.2f°, %.2f° • z%d",
                                        activeCity.latitude,
                                        activeCity.longitude,
                                        zoom
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedText
                                )
                            }
                        }
                    }

                    if (report != null) {
                        Text(
                            text = WeatherFormatters.formatTemp(
                                report.temperatureC,
                                uiState.settings.temperatureUnit
                            ),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = AmberPrimary,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MapControlButton(
                onClick = { showRadar = !showRadar },
                highlighted = showRadar,
                tag = "map_toggle_radar"
            ) {
                Icon(
                    imageVector = Icons.Outlined.Layers,
                    contentDescription = "Слой осадков",
                    tint = if (showRadar) CoolCyan else IceWhite
                )
            }
            MapControlButton(
                onClick = { zoom = (zoom + 1).coerceAtMost(10) },
                tag = "map_zoom_in"
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Приблизить",
                    tint = IceWhite
                )
            }
            MapControlButton(
                onClick = { zoom = (zoom - 1).coerceAtLeast(3) },
                tag = "map_zoom_out"
            ) {
                Icon(
                    imageVector = Icons.Outlined.Remove,
                    contentDescription = "Отдалить",
                    tint = IceWhite
                )
            }
        }
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
