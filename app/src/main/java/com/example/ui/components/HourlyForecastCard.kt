package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.HourlyForecastPoint
import com.example.data.model.TemperatureUnit
import com.example.data.model.WeatherFormatters
import com.example.ui.theme.AmberBright
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CoolCyan
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedText

@Composable
fun HourlyTilesSection(
    hourlyPoints: List<HourlyForecastPoint>,
    temperatureUnit: TemperatureUnit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (hourlyPoints.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.hourly_forecast_title),
                style = MaterialTheme.typography.titleLarge,
                color = IceWhite
            )

            Row(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onSeeAllClick)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("hourly_see_all_button"),
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

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(hourlyPoints.take(16)) { index, point ->
                GlassCard(
                    cornerRadius = 18.dp,
                    surfaceAlpha = if (index == 0) 0.92f else 0.78f,
                    amberGlow = index == 0,
                    modifier = Modifier.width(68.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (index == 0) "Сейчас" else point.hourLabel,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (index == 0) AmberBright else MutedText
                        )

                        CompactWeatherIcon(
                            weatherCode = point.weatherCode,
                            isDay = point.isDay,
                            modifier = Modifier.size(30.dp)
                        )

                        Text(
                            text = WeatherFormatters.formatTemp(point.tempC, temperatureUnit),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = IceWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HourlyForecastCard(
    hourlyPoints: List<HourlyForecastPoint>,
    temperatureUnit: TemperatureUnit,
    modifier: Modifier = Modifier
) {
    if (hourlyPoints.isEmpty()) return

    val itemWidthDp = 64.dp
    val totalWidthDp = itemWidthDp * hourlyPoints.size
    val minTemp = hourlyPoints.minOf { it.tempC }
    val maxTemp = hourlyPoints.maxOf { it.tempC }
    val tempSpan = (maxTemp - minTemp).coerceAtLeast(1.0)

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        amberGlow = true
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = AmberPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Почасовой график",
                    style = MaterialTheme.typography.titleMedium,
                    color = IceWhite
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 10.dp)
            ) {
                Column(modifier = Modifier.width(totalWidthDp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        hourlyPoints.forEachIndexed { idx, point ->
                            Box(
                                modifier = Modifier.width(itemWidthDp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = WeatherFormatters.formatTemp(point.tempC, temperatureUnit),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.SemiBold
                                    ),
                                    color = if (idx == 0) AmberBright else IceWhite
                                )
                            }
                        }
                    }

                    Canvas(
                        modifier = Modifier
                            .width(totalWidthDp)
                            .height(56.dp)
                    ) {
                        val stepPx = size.width / hourlyPoints.size
                        val topPad = 14f
                        val bottomPad = 14f
                        val usableHeight = (size.height - topPad - bottomPad).coerceAtLeast(10f)

                        val coords = hourlyPoints.mapIndexed { idx, p ->
                            val norm = ((p.tempC - minTemp) / tempSpan).toFloat()
                            val x = stepPx * idx + stepPx / 2f
                            val y = topPad + (1f - norm) * usableHeight
                            Offset(x, y)
                        }

                        if (coords.size >= 2) {
                            val linePath = Path().apply {
                                moveTo(coords.first().x, coords.first().y)
                                for (i in 0 until coords.lastIndex) {
                                    val p0 = coords[i]
                                    val p1 = coords[i + 1]
                                    val midX = (p0.x + p1.x) / 2f
                                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                                }
                            }

                            val fillPath = Path().apply {
                                addPath(linePath)
                                lineTo(coords.last().x, size.height)
                                lineTo(coords.first().x, size.height)
                                close()
                            }

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        AmberPrimary.copy(alpha = 0.28f),
                                        Color.Transparent
                                    )
                                )
                            )

                            drawPath(
                                path = linePath,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(AmberBright, AmberPrimary, CoolCyan)
                                ),
                                style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                            )
                        }

                        coords.forEachIndexed { idx, pt ->
                            if (idx == 0) {
                                drawCircle(
                                    color = AmberPrimary.copy(alpha = 0.36f),
                                    radius = 12f,
                                    center = pt
                                )
                                drawCircle(
                                    color = AmberBright,
                                    radius = 6f,
                                    center = pt
                                )
                            } else if (idx % 3 == 0) {
                                drawCircle(
                                    color = IceWhite.copy(alpha = 0.9f),
                                    radius = 4f,
                                    center = pt
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        hourlyPoints.forEachIndexed { idx, point ->
                            Column(
                                modifier = Modifier
                                    .width(itemWidthDp)
                                    .padding(horizontal = 3.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (idx == 0) AmberPrimary.copy(alpha = 0.16f) else Color.Transparent
                                    )
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CompactWeatherIcon(
                                    weatherCode = point.weatherCode,
                                    isDay = point.isDay,
                                    modifier = Modifier.size(26.dp)
                                )
                                if (point.precipitationProb >= 15) {
                                    Text(
                                        text = "${point.precipitationProb}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CoolCyan
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(14.dp))
                                }
                                Text(
                                    text = if (idx == 0) "Сейчас" else point.hourLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (idx == 0) AmberBright else MutedText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
