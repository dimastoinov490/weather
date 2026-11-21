package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DailyForecastItem
import com.example.data.model.TemperatureUnit
import com.example.data.model.WeatherFormatters
import com.example.ui.theme.AmberBright
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CoolCyan
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedText
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDivider

@Composable
fun DailyForecastCard(
    dailyForecasts: List<DailyForecastItem>,
    weekMinTempC: Double,
    weekMaxTempC: Double,
    currentTempC: Double,
    temperatureUnit: TemperatureUnit,
    onSeeAllClick: (() -> Unit)? = null,
    showRangeBars: Boolean = false,
    selectedDateIso: String? = null,
    onSelectDay: ((DailyForecastItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (dailyForecasts.isEmpty()) return

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_forecast_card"),
        amberGlow = true
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = AmberPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.daily_forecast_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = IceWhite
                    )
                }

                if (onSeeAllClick != null) {
                    Row(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onSeeAllClick)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("daily_see_all_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Все",
                            style = MaterialTheme.typography.labelLarge,
                            color = AmberPrimary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = "Открыть подробный прогноз",
                            tint = AmberPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            dailyForecasts.forEachIndexed { index, item ->
                val isToday = index == 0
                val isSelected = if (selectedDateIso != null) {
                    item.dateIso == selectedDateIso
                } else {
                    isToday
                }
                val rowModifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .then(
                        if (onSelectDay != null) {
                            Modifier
                                .minimumInteractiveComponentSize()
                                .clickable { onSelectDay(item) }
                        } else {
                            Modifier
                        }
                    )
                    .testTag("daily_forecast_day_$index")

                if (isSelected) {
                    Row(
                        modifier = rowModifier
                            .background(Color(0xFF192030))
                            .border(1.dp, ObsidianBorder.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.dayName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = IceWhite
                            )
                            Text(
                                text = item.dateLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedText
                            )
                        }

                        CompactWeatherIcon(
                            weatherCode = item.weatherCode,
                            isDay = true,
                            modifier = Modifier.size(30.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        if (showRangeBars) {
                            TemperatureRangeBar(
                                dayMin = item.tempMinC,
                                dayMax = item.tempMaxC,
                                weekMin = weekMinTempC,
                                weekMax = weekMaxTempC,
                                currentTemp = if (isToday) currentTempC else null,
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(7.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Text(
                            text = WeatherFormatters.formatTemp(item.tempMaxC, temperatureUnit),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = IceWhite,
                            modifier = Modifier.width(44.dp),
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = WeatherFormatters.formatTemp(item.tempMinC, temperatureUnit),
                            style = MaterialTheme.typography.titleMedium,
                            color = MutedText,
                            modifier = Modifier.width(44.dp),
                            textAlign = TextAlign.End
                        )
                    }
                } else {
                    Row(
                        modifier = rowModifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.dayName}, ",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                                color = IceWhite
                            )
                            Text(
                                text = item.dateLabel,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MutedText
                            )
                        }

                        CompactWeatherIcon(
                            weatherCode = item.weatherCode,
                            isDay = true,
                            modifier = Modifier.size(26.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        if (showRangeBars) {
                            TemperatureRangeBar(
                                dayMin = item.tempMinC,
                                dayMax = item.tempMaxC,
                                weekMin = weekMinTempC,
                                weekMax = weekMaxTempC,
                                currentTemp = if (isToday) currentTempC else null,
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(7.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Text(
                            text = WeatherFormatters.formatTemp(item.tempMaxC, temperatureUnit),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = IceWhite,
                            modifier = Modifier.width(44.dp),
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = WeatherFormatters.formatTemp(item.tempMinC, temperatureUnit),
                            style = MaterialTheme.typography.titleMedium,
                            color = MutedText,
                            modifier = Modifier.width(44.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }

                if (index < dailyForecasts.lastIndex && !isSelected) {
                    val nextSelected = if (selectedDateIso != null) {
                        dailyForecasts.getOrNull(index + 1)?.dateIso == selectedDateIso
                    } else {
                        false
                    }
                    if (!nextSelected) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = ObsidianDivider
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TemperatureRangeBar(
    dayMin: Double,
    dayMax: Double,
    weekMin: Double,
    weekMax: Double,
    currentTemp: Double?,
    modifier: Modifier = Modifier
) {
    val span = (weekMax - weekMin).coerceAtLeast(1.0)
    val startFrac = ((dayMin - weekMin) / span).toFloat().coerceIn(0f, 1f)
    val endFrac = ((dayMax - weekMin) / span).toFloat().coerceIn(startFrac + 0.08f, 1f)

    Canvas(modifier = modifier) {
        val barRadius = size.height / 2f
        drawRoundRect(
            color = Color.White.copy(alpha = 0.14f),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = CornerRadius(barRadius, barRadius)
        )

        val left = startFrac * size.width
        val width = ((endFrac - startFrac) * size.width).coerceAtLeast(size.height)
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(CoolCyan, AmberBright, AmberPrimary),
                startX = 0f,
                endX = size.width
            ),
            topLeft = Offset(left, 0f),
            size = Size(width, size.height),
            cornerRadius = CornerRadius(barRadius, barRadius)
        )

        if (currentTemp != null) {
            val dotFrac = ((currentTemp - weekMin) / span).toFloat().coerceIn(0f, 1f)
            val dotX = (dotFrac * size.width).coerceIn(barRadius, size.width - barRadius)
            drawCircle(
                color = Color(0xFF0A101C),
                radius = size.height * 0.72f,
                center = Offset(dotX, size.height / 2f)
            )
            drawCircle(
                color = IceWhite,
                radius = size.height * 0.48f,
                center = Offset(dotX, size.height / 2f)
            )
        }
    }
}
