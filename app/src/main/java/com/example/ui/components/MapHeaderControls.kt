package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.SavedCity
import com.example.data.model.WeatherFormatters
import com.example.data.model.WeatherSettings
import com.example.ui.state.CityWeatherLoadState
import com.example.ui.theme.CoolCyan
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedText

@Composable
fun MapTopHeaderRow(
    activeCity: SavedCity?,
    savedCities: List<SavedCity>,
    reportsByCityId: Map<String, CityWeatherLoadState>,
    settings: WeatherSettings,
    showCityMenu: Boolean,
    onToggleCityMenu: (Boolean) -> Unit,
    onSelectCity: (String) -> Unit,
    onOpenCities: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(MapGlassSoftBg)
                    .border(1.dp, MapGlassBorder, CircleShape)
                    .clickable(onClick = onNavigateBack)
                    .testTag("map_back_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = IceWhite,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            MapAppBrandIcon(modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.map_brand_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    ),
                    color = IceWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.map_brand_subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = MutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MapGlassBg,
                border = BorderStroke(1.dp, MapGlassBorder),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onToggleCityMenu(true) }
                    .testTag("map_city_selector")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NearMe,
                        contentDescription = null,
                        tint = IceWhite,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = activeCity?.name ?: "Москва",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = IceWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 118.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.map_city_selector_cd),
                        tint = MutedText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = showCityMenu,
                onDismissRequest = { onToggleCityMenu(false) },
                containerColor = Color(0xF50B1628)
            ) {
                savedCities.forEach { city ->
                    val cityReport = when (val st = reportsByCityId[city.id]) {
                        is CityWeatherLoadState.Success -> st.report
                        is CityWeatherLoadState.Error -> st.cachedReport
                        else -> null
                    }
                    val tempSuffix = cityReport?.let {
                        "  •  ${WeatherFormatters.formatTemp(it.temperatureC, settings.temperatureUnit)}"
                    }.orEmpty()
                    val isSelected = city.id == activeCity?.id
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${city.name}$tempSuffix",
                                color = if (isSelected) MapAmberText else IceWhite,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        onClick = { onSelectCity(city.id) }
                    )
                }
                if (savedCities.isNotEmpty()) {
                    HorizontalDivider(color = MapGlassBorder)
                }
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = CoolCyan
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.map_choose_other_city),
                            color = CoolCyan
                        )
                    },
                    onClick = onOpenCities
                )
            }
        }
    }
}

@Composable
fun MapModeAndActionRow(
    selectedMode: WeatherMapMode,
    onSelectMode: (WeatherMapMode) -> Unit,
    onRecenterAndRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MapModeTabPill(
                label = stringResource(R.string.map_tab_map),
                icon = Icons.Outlined.Map,
                selected = selectedMode == WeatherMapMode.MAP,
                tag = "map_tab_map",
                onClick = { onSelectMode(WeatherMapMode.MAP) }
            )
            MapModeTabPill(
                label = stringResource(R.string.map_tab_forecast),
                icon = Icons.Outlined.CalendarMonth,
                selected = selectedMode == WeatherMapMode.FORECAST,
                tag = "map_tab_forecast",
                onClick = { onSelectMode(WeatherMapMode.FORECAST) }
            )
            MapModeTabPill(
                label = stringResource(R.string.map_tab_precipitation),
                icon = Icons.Outlined.WaterDrop,
                selected = selectedMode == WeatherMapMode.PRECIPITATION,
                tag = "map_tab_precipitation",
                onClick = { onSelectMode(WeatherMapMode.PRECIPITATION) }
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        MapControlSquareButton(
            onClick = onRecenterAndRefresh,
            tag = "map_recenter",
            contentDescription = stringResource(R.string.map_recenter),
            icon = Icons.Rounded.NearMe,
            tint = IceWhite
        )
    }
}

@Composable
private fun MapModeTabPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val bgColor = if (selected) MapAmberBg else MapGlassSoftBg
    val borderColor = if (selected) MapAmberBorder else MapGlassBorder
    val contentColor = if (selected) MapAmberText else IceWhite

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun PrecipitationLegendCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MapGlassBg,
        border = BorderStroke(1.dp, MapGlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(MapRadarCyan, MapRadarBlue, MapRadarPurple)
                        )
                    )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.map_legend_light),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = IceWhite.copy(alpha = 0.88f)
                )
                Text(
                    text = stringResource(R.string.map_legend_moderate),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = IceWhite.copy(alpha = 0.88f)
                )
                Text(
                    text = stringResource(R.string.map_legend_heavy),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = IceWhite.copy(alpha = 0.88f)
                )
            }
        }
    }
}

@Composable
fun RadarLayerToggleChip(
    isEnabled: Boolean,
    frame: RadarFrameInfo?,
    showTime: Boolean,
    onToggle: () -> Unit
) {
    val timeText = remember(frame?.timeEpochSec) {
        frame?.let { formatEpochHm(it.timeEpochSec) }
    }
    val borderColor = if (isEnabled) CoolCyan.copy(alpha = 0.65f) else MapGlassBorder
    val label = when {
        !isEnabled -> stringResource(R.string.map_radar_off)
        showTime && frame != null && timeText != null -> {
            if (frame.isNowcast) {
                "${stringResource(R.string.map_radar_forecast_badge)} • $timeText"
            } else {
                "${stringResource(R.string.map_radar_on)} • $timeText"
            }
        }
        else -> stringResource(R.string.map_radar_on)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isEnabled) MapGlassBg else MapGlassSoftBg,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .testTag("map_radar_toggle")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            !isEnabled -> MutedText.copy(alpha = 0.55f)
                            frame?.isNowcast == true -> MapAmberText
                            else -> CoolCyan
                        }
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                color = if (isEnabled) IceWhite else MutedText,
                maxLines = 1
            )
        }
    }
}

@Composable
fun MapStatusWarningPill(
    message: String,
    onRetry: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MapGlassBg,
        border = BorderStroke(1.dp, MapAmberBorder.copy(alpha = 0.65f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
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
                    tint = MapAmberText,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = IceWhite
                )
            }
            IconButton(
                onClick = onRetry,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("map_retry_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.retry_button),
                    tint = CoolCyan
                )
            }
        }
    }
}

@Composable
fun MapControlSquareButton(
    onClick: () -> Unit,
    tag: String,
    contentDescription: String,
    icon: ImageVector,
    tint: Color
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(46.dp)
            .minimumInteractiveComponentSize()
            .clip(shape)
            .background(MapGlassBg)
            .border(1.dp, MapGlassBorder, shape)
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
    }
}
