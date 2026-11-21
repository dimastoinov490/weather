package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.HourlyForecastPoint
import com.example.data.model.WeatherFormatters
import com.example.data.model.WeatherMapper
import com.example.data.model.WeatherReport
import com.example.data.model.WeatherSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// Unified Dark-Blue Map Color System
internal val MapBgPrimary = Color(0xFF07111F)
internal val MapBgSecondary = Color(0xFF0B1930)
internal val MapGlassPanel = Color(0xE00B1930)
internal val MapSurfaceButton = Color(0xE6102442)
internal val MapActiveTabBg = Color(0xF0173D70)
internal val MapActiveGlow = Color(0xFF2478D4)
internal val MapAccentPrimary = Color(0xFF369BFF)
internal val MapGlowSecondary = Color(0xFF65B8FF)
internal val MapTextPrimary = Color(0xFFFFFFFF)
internal val MapTextSecondary = Color(0xFFA9BCD6)
internal val MapElementBorder = Color(0xFF203858)

// Precipitation intensity colors (unchanged: Cyan -> Blue -> Purple)
internal val MapRadarCyan = Color(0xFF38BDF8)
internal val MapRadarBlue = Color(0xFF2563EB)
internal val MapRadarPurple = Color(0xFF7C3AED)

@Composable
fun RadarTimelineBar(
    frames: List<RadarFrameInfo>,
    selectedIndex: Int,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onSelectIndex: (Int) -> Unit,
    onRetry: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MapGlassPanel,
        border = BorderStroke(1.dp, MapElementBorder),
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("map_radar_timeline")
    ) {
        if (frames.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.map_radar_no_frames),
                    style = MaterialTheme.typography.bodySmall,
                    color = MapTextSecondary
                )
                IconButton(
                    onClick = onRetry,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.retry_button),
                        tint = MapAccentPrimary
                    )
                }
            }
        } else {
            val safeIndex = selectedIndex.coerceIn(0, frames.lastIndex)
            val currentFrame = frames[safeIndex]
            val timeLabel = remember(currentFrame.timeEpochSec) {
                formatEpochHm(currentFrame.timeEpochSec)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("map_radar_play_pause")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = if (isPlaying) {
                            stringResource(R.string.map_radar_pause)
                        } else {
                            stringResource(R.string.map_radar_play)
                        },
                        tint = MapGlowSecondary
                    )
                }

                IconButton(
                    onClick = {
                        val prev = if (safeIndex > 0) safeIndex - 1 else frames.lastIndex
                        onSelectIndex(prev)
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SkipPrevious,
                        contentDescription = stringResource(R.string.map_prev_frame),
                        tint = MapTextPrimary
                    )
                }

                Slider(
                    value = safeIndex.toFloat(),
                    onValueChange = { value -> onSelectIndex(value.roundToInt()) },
                    valueRange = 0f..(frames.lastIndex.coerceAtLeast(1)).toFloat(),
                    steps = (frames.size - 2).coerceAtLeast(0),
                    colors = SliderDefaults.colors(
                        thumbColor = MapGlowSecondary,
                        activeTrackColor = MapAccentPrimary,
                        inactiveTrackColor = MapElementBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("map_radar_slider")
                )

                IconButton(
                    onClick = {
                        val next = (safeIndex + 1) % frames.size
                        onSelectIndex(next)
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SkipNext,
                        contentDescription = stringResource(R.string.map_next_frame),
                        tint = MapTextPrimary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MapTextPrimary
                    )
                    Text(
                        text = if (currentFrame.isNowcast) "Прогноз" else "Радар",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (currentFrame.isNowcast) MapGlowSecondary else MapAccentPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun MapBottomWeatherPanel(
    report: WeatherReport?,
    settings: WeatherSettings,
    selectedHourIndex: Int?,
    onSelectHour: (Int) -> Unit,
    onOpenForecastDetail: () -> Unit
) {
    val selectedHour: HourlyForecastPoint? = remember(report, selectedHourIndex) {
        if (report == null || selectedHourIndex == null) null
        else report.hourlyPoints.getOrNull(selectedHourIndex)
    }

    val displayCode = selectedHour?.weatherCode ?: report?.weatherCode ?: 2
    val displayIsDay = selectedHour?.isDay ?: report?.isDay ?: true
    val displayTemp = selectedHour?.tempC ?: report?.temperatureC
    val displayCondition = remember(displayCode, displayIsDay, report, selectedHour) {
        when {
            selectedHour != null -> WeatherMapper.resolveConditionTitle(displayCode, displayIsDay)
            report != null -> report.conditionTitle
            else -> "Загрузка…"
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MapGlassPanel,
        border = BorderStroke(1.dp, MapElementBorder),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("map_bottom_weather_panel")
    ) {
        Box(
            modifier = Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(Color(0x33173D70), Color(0x1407111F))
                )
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1.18f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MapGlowingWeatherIcon(
                            weatherCode = displayCode,
                            isDay = displayIsDay,
                            contentDescriptionText = displayCondition,
                            modifier = Modifier.size(46.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (displayTemp != null) {
                                WeatherFormatters.formatTemp(displayTemp, settings.temperatureUnit)
                            } else {
                                "--°"
                            },
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Light
                            ),
                            color = MapTextPrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayCondition,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MapTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val secondaryText = when {
                                selectedHour != null -> "В ${selectedHour.hourLabel} • осадки ${selectedHour.precipitationProb}%"
                                report != null -> "Ощущается как ${
                                    WeatherFormatters.formatTemp(report.apparentTemperatureC, settings.temperatureUnit)
                                }"
                                else -> "Обновление данных"
                            }
                            Text(
                                text = secondaryText,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MapTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .width(1.dp)
                            .height(48.dp)
                            .background(MapElementBorder)
                    )

                    Column(
                        modifier = Modifier
                            .weight(0.88f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenForecastDetail)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .testTag("map_open_forecast_detail"),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
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
                                Icon(
                                    imageVector = Icons.Outlined.Air,
                                    contentDescription = stringResource(R.string.wind_title),
                                    tint = MapTextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val windLabel = if (report != null) {
                                    "${WeatherFormatters.formatWind(report.windSpeedKmh, settings.windSpeedUnit)}, ${
                                        WeatherFormatters.windDirectionCompass(report.windDirectionDeg)
                                    }"
                                } else {
                                    "--"
                                }
                                Text(
                                    text = windLabel,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                    color = MapTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.map_open_full_forecast),
                                tint = MapGlowSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.WaterDrop,
                                contentDescription = stringResource(R.string.humidity_title),
                                tint = MapAccentPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (report != null) "${report.humidityPercent}%" else "--%",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                color = MapTextPrimary,
                                maxLines = 1
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Speed,
                                contentDescription = stringResource(R.string.pressure_title),
                                tint = MapTextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (report != null) {
                                    WeatherFormatters.formatPressure(report.pressureHpa, settings.pressureUnit)
                                } else {
                                    "--"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                color = MapTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                val hourlyPoints = report?.hourlyPoints.orEmpty()
                if (hourlyPoints.isNotEmpty()) {
                    HorizontalDivider(color = MapElementBorder.copy(alpha = 0.7f))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(hourlyPoints) { index, point ->
                            if (index > 0 && point.hourLabel.startsWith("00")) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .width(1.dp)
                                        .height(42.dp)
                                        .background(MapElementBorder)
                                )
                            }
                            val isSelected = selectedHourIndex == index
                            Column(
                                modifier = Modifier
                                    .widthIn(min = 56.dp)
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) MapActiveTabBg else Color.Transparent
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) MapActiveGlow else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onSelectHour(index) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("map_hour_item_$index"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = point.hourLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                                    color = if (isSelected) MapGlowSecondary else MapTextSecondary
                                )
                                MapGlowingWeatherIcon(
                                    weatherCode = point.weatherCode,
                                    isDay = point.isDay,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = WeatherFormatters.formatTemp(
                                        point.tempC,
                                        settings.temperatureUnit
                                    ),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MapTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun formatEpochHm(epochSeconds: Long): String {
    return try {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochSeconds * 1000L))
    } catch (_: Exception) {
        "--:--"
    }
}
