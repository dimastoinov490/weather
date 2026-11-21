package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Co2
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DailyForecastItem
import com.example.data.model.HourlyForecastPoint
import com.example.data.model.WeatherFormatters
import com.example.data.model.WeatherReport
import com.example.data.model.WeatherSettings
import com.example.ui.theme.AmberBright
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CoolCyan
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedText
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.SkyAzure
import com.example.ui.theme.SkyCyan
import com.example.ui.theme.StatusGood
import com.example.ui.theme.StatusHazardous
import com.example.ui.theme.StatusModerate
import com.example.ui.theme.StatusSensitive
import com.example.ui.theme.StatusUnhealthy
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun DetailsSectionCard(
    report: WeatherReport,
    settings: WeatherSettings,
    modifier: Modifier = Modifier
) {
    val windText = "${WeatherFormatters.formatWind(report.windSpeedKmh, settings.windSpeedUnit)} ${WeatherFormatters.windDirectionCompass(report.windDirectionDeg)}"
    val pressureNum = WeatherFormatters.formatPressureValue(report.pressureHpa, settings.pressureUnit)
    val pressureUnitLabel = settings.pressureUnit.label
    val visibilityText = WeatherFormatters.formatVisibility(report.visibilityKm)
    val sunriseText = WeatherFormatters.extractTimeHm(report.sunriseIso)
    val sunsetText = WeatherFormatters.extractTimeHm(report.sunsetIso)

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.BarChart,
                    contentDescription = null,
                    tint = AmberPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Подробности",
                    style = MaterialTheme.typography.titleLarge,
                    color = IceWhite
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailMiniTile(
                        icon = Icons.Filled.WaterDrop,
                        iconTint = SkyAzure,
                        label = "Влажность",
                        value = "${report.humidityPercent}%",
                        modifier = Modifier.weight(1f)
                    )
                    DetailMiniTile(
                        icon = Icons.Outlined.Air,
                        iconTint = SkyCyan,
                        label = "Ветер",
                        value = windText,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailMiniTile(
                        icon = Icons.Outlined.Speed,
                        iconTint = AmberPrimary,
                        label = "Давление",
                        value = pressureNum,
                        unitSuffix = pressureUnitLabel,
                        modifier = Modifier.weight(1f)
                    )
                    DetailMiniTile(
                        icon = Icons.Outlined.Visibility,
                        iconTint = SkyAzure,
                        label = "Видимость",
                        value = visibilityText,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailMiniTile(
                        icon = Icons.Outlined.WbTwilight,
                        iconTint = AmberPrimary,
                        label = "Восход",
                        value = sunriseText,
                        modifier = Modifier.weight(1f)
                    )
                    DetailMiniTile(
                        icon = Icons.Outlined.WbSunny,
                        iconTint = AmberPrimary,
                        label = "Закат",
                        value = sunsetText,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailMiniTile(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    unitSuffix: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161D2C))
            .border(1.dp, ObsidianBorder.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = IceWhite
                )
                if (unitSuffix != null) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = unitSuffix,
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WeatherBentoGrid(
    report: WeatherReport,
    settings: WeatherSettings,
    columns: Int,
    selectedDay: DailyForecastItem? = null,
    dayHours: List<HourlyForecastPoint> = emptyList(),
    isToday: Boolean = true,
    modifier: Modifier = Modifier
) {
    val effectiveDay = selectedDay
    val feelsLikeValueC: Double? = if (isToday || effectiveDay == null) {
        report.apparentTemperatureC
    } else {
        effectiveDay.apparentTempMaxC ?: dayHours.mapNotNull { it.apparentTempC }.maxOrNull()
    }
    val feelsLikeMinC: Double? = if (isToday || effectiveDay == null) {
        null
    } else {
        effectiveDay.apparentTempMinC ?: dayHours.mapNotNull { it.apparentTempC }.minOrNull()
    }

    val windSpeedKmh: Double = if (isToday || effectiveDay == null) {
        report.windSpeedKmh
    } else {
        effectiveDay.windSpeedMaxKmh
    }
    val windDirectionDeg: Int? = if (isToday || effectiveDay == null) {
        report.windDirectionDeg
    } else {
        effectiveDay.windDirectionDeg ?: dayHours.mapNotNull { it.windDirectionDeg }.firstOrNull()
    }

    val sunriseIso = effectiveDay?.sunriseIso?.takeIf { it.isNotBlank() }
        ?: if (isToday) report.sunriseIso else ""
    val sunsetIso = effectiveDay?.sunsetIso?.takeIf { it.isNotBlank() }
        ?: if (isToday) report.sunsetIso else ""

    val uvIndex: Double? = if (isToday || effectiveDay == null) {
        report.uvIndexMax
    } else {
        effectiveDay.uvIndexMaxAvailable ?: dayHours.mapNotNull { it.uvIndex }.maxOrNull()
    }

    val humidity: Int? = if (isToday || effectiveDay == null) {
        report.humidityPercent
    } else {
        val list = dayHours.mapNotNull { it.humidityPercent }
        if (list.isNotEmpty()) list.average().roundToInt() else null
    }
    val cloudCover: Int? = if (isToday || effectiveDay == null) {
        report.cloudCoverPercent
    } else {
        val list = dayHours.mapNotNull { it.cloudCoverPercent }
        if (list.isNotEmpty()) list.average().roundToInt() else null
    }

    val pressureHpa: Double? = if (isToday || effectiveDay == null) {
        report.pressureHpa
    } else {
        val list = dayHours.mapNotNull { it.pressureHpa }
        if (list.isNotEmpty()) list.average() else null
    }

    val cards: List<@Composable () -> Unit> = listOf(
        {
            FeelsLikeCard(
                apparentTempC = feelsLikeValueC,
                actualTempC = if (isToday) report.temperatureC else null,
                apparentMinC = feelsLikeMinC,
                isToday = isToday,
                settings = settings
            )
        },
        {
            WindCompassCard(
                windSpeedKmh = windSpeedKmh,
                windDirectionDeg = windDirectionDeg,
                settings = settings
            )
        },
        {
            SunriseSunsetCard(
                currentLocalIso = report.currentLocalIso,
                sunriseIso = sunriseIso,
                sunsetIso = sunsetIso,
                showSunDot = isToday && report.isDay
            )
        },
        { UvIndexCard(uvIndex = uvIndex) },
        { HumidityCard(humidity = humidity, cloudCover = cloudCover) },
        { PressureGaugeCard(pressureHpa = pressureHpa, settings = settings) }
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weather_bento_grid"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isToday && report.aqiValue != null) {
            AirQualityCard(
                aqi = report.aqiValue,
                pm25 = report.pm25,
                pm10 = report.pm10
            )
        }

        val chunked = cards.chunked(columns.coerceAtLeast(1))
        chunked.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { card ->
                    Box(modifier = Modifier.weight(1f)) {
                        card()
                    }
                }
                if (rowItems.size < columns) {
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BentoHeader(
    icon: ImageVector,
    title: String,
    iconTint: Color = AmberPrimary
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MutedText
        )
    }
}

@Composable
private fun FeelsLikeCard(
    apparentTempC: Double?,
    actualTempC: Double?,
    apparentMinC: Double?,
    isToday: Boolean,
    settings: WeatherSettings
) {
    val noDataText = stringResource(R.string.forecast_no_data)
    val valueText = apparentTempC?.let {
        WeatherFormatters.formatTemp(it, settings.temperatureUnit)
    } ?: noDataText

    val note = when {
        apparentTempC == null -> "Нет данных на день"
        isToday && actualTempC != null -> {
            val diff = apparentTempC - actualTempC
            when {
                diff <= -2.5 -> "Холоднее из-за ветра"
                diff >= 2.5 -> "Теплее из-за влажности"
                else -> "Совпадает с фактической"
            }
        }
        apparentMinC != null -> {
            val minFormatted = WeatherFormatters.formatTemp(apparentMinC, settings.temperatureUnit)
            val maxFormatted = WeatherFormatters.formatTemp(apparentTempC, settings.temperatureUnit)
            "От $minFormatted до $maxFormatted"
        }
        else -> "Дневной максимум"
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(162.dp)
            .testTag("bento_feels_like")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            BentoHeader(
                icon = Icons.Outlined.DeviceThermostat,
                title = stringResource(R.string.feels_like_title)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = valueText,
                style = if (apparentTempC != null) {
                    MaterialTheme.typography.displayMedium
                } else {
                    MaterialTheme.typography.titleLarge
                },
                color = IceWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WindCompassCard(
    windSpeedKmh: Double,
    windDirectionDeg: Int?,
    settings: WeatherSettings
) {
    val speedFormatted = WeatherFormatters.formatWind(windSpeedKmh, settings.windSpeedUnit)
    val dirShort = windDirectionDeg?.let { WeatherFormatters.windDirectionCompass(it) } ?: "—"
    val dirFull = windDirectionDeg?.let { WeatherFormatters.windDirectionFull(it) } ?: "Направление неизвестно"

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(162.dp)
            .testTag("bento_wind")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            BentoHeader(
                icon = Icons.Outlined.Air,
                title = stringResource(R.string.wind_title),
                iconTint = SkyCyan
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = speedFormatted,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = IceWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dirFull,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(64.dp)) {
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val r = size.minDimension / 2f - 4f
                        drawCircle(
                            color = ObsidianBorder,
                            radius = r,
                            center = c,
                            style = Stroke(width = 2.5f)
                        )
                        for (i in 0 until 4) {
                            val ang = i * PI / 2.0
                            val p1 = Offset(c.x + cos(ang).toFloat() * (r - 6f), c.y + sin(ang).toFloat() * (r - 6f))
                            val p2 = Offset(c.x + cos(ang).toFloat() * r, c.y + sin(ang).toFloat() * r)
                            drawLine(Color.White.copy(alpha = 0.45f), p1, p2, strokeWidth = 2f)
                        }
                        if (windDirectionDeg != null) {
                            val rad = ((windDirectionDeg - 90) * PI / 180.0)
                            val tip = Offset(c.x + cos(rad).toFloat() * (r - 5f), c.y + sin(rad).toFloat() * (r - 5f))
                            val tail = Offset(c.x - cos(rad).toFloat() * (r - 12f), c.y - sin(rad).toFloat() * (r - 12f))
                            drawLine(
                                color = SkyCyan,
                                start = tail,
                                end = tip,
                                strokeWidth = 3.5f,
                                cap = StrokeCap.Round
                            )
                            drawCircle(color = AmberPrimary, radius = 5f, center = tip)
                        }
                    }
                    Text(
                        text = dirShort,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = IceWhite
                    )
                }
            }
        }
    }
}

@Composable
private fun SunriseSunsetCard(
    currentLocalIso: String,
    sunriseIso: String,
    sunsetIso: String,
    showSunDot: Boolean
) {
    val sunrise = WeatherFormatters.extractTimeHm(sunriseIso)
    val sunset = WeatherFormatters.extractTimeHm(sunsetIso)
    val progress = if (showSunDot) {
        WeatherFormatters.computeDayProgress(currentLocalIso, sunriseIso, sunsetIso)
    } else {
        0.5f
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(162.dp)
            .testTag("bento_sunrise_sunset")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            BentoHeader(
                icon = Icons.Outlined.WbTwilight,
                title = stringResource(R.string.sunrise_sunset_title)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                val horizonY = size.height * 0.78f
                val startX = size.width * 0.08f
                val endX = size.width * 0.92f
                val midX = (startX + endX) / 2f
                val peakY = size.height * 0.10f

                drawLine(
                    color = ObsidianBorder,
                    start = Offset(0f, horizonY),
                    end = Offset(size.width, horizonY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                )

                val ctrlY = peakY - (horizonY - peakY) * 0.35f
                val arcPath = Path().apply {
                    moveTo(startX, horizonY)
                    quadraticTo(midX, ctrlY, endX, horizonY)
                }
                drawPath(
                    path = arcPath,
                    color = AmberPrimary.copy(alpha = 0.35f),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )

                if (showSunDot) {
                    val t = progress
                    val oneMinusT = 1f - t
                    val sunX = oneMinusT * oneMinusT * startX + 2f * oneMinusT * t * midX + t * t * endX
                    val sunY = oneMinusT * oneMinusT * horizonY + 2f * oneMinusT * t * ctrlY + t * t * horizonY

                    drawCircle(
                        color = AmberPrimary.copy(alpha = 0.36f),
                        radius = 15f,
                        center = Offset(sunX, sunY)
                    )
                    drawCircle(
                        color = AmberBright,
                        radius = 7.5f,
                        center = Offset(sunX, sunY)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Восход", style = MaterialTheme.typography.labelSmall, color = MutedText)
                    Text(text = sunrise, style = MaterialTheme.typography.titleMedium, color = IceWhite)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Закат", style = MaterialTheme.typography.labelSmall, color = MutedText)
                    Text(text = sunset, style = MaterialTheme.typography.titleMedium, color = IceWhite)
                }
            }
        }
    }
}

@Composable
private fun UvIndexCard(uvIndex: Double?) {
    val noDataText = stringResource(R.string.forecast_no_data)
    val uvInt = uvIndex?.roundToInt()
    val level = uvIndex?.let { WeatherFormatters.uvIndexDescription(it) } ?: "УФ-индекс недоступен"
    val uvFrac = uvIndex?.let { (it / 11.0).toFloat().coerceIn(0f, 1f) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(162.dp)
            .testTag("bento_uv")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            BentoHeader(
                icon = Icons.Outlined.WbSunny,
                title = stringResource(R.string.uv_index_title)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = uvInt?.toString() ?: noDataText,
                style = if (uvInt != null) {
                    MaterialTheme.typography.displayMedium
                } else {
                    MaterialTheme.typography.titleLarge
                },
                color = IceWhite
            )
            Text(
                text = level,
                style = MaterialTheme.typography.titleMedium,
                color = MutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.weight(1f))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            ) {
                val r = size.height / 2f
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(StatusGood, StatusModerate, StatusSensitive, StatusUnhealthy, StatusHazardous)
                    ),
                    size = Size(size.width, size.height * 0.65f),
                    topLeft = Offset(0f, size.height * 0.175f),
                    cornerRadius = CornerRadius(r, r)
                )
                if (uvFrac != null) {
                    val dotX = (uvFrac * size.width).coerceIn(r, size.width - r)
                    drawCircle(color = IceWhite, radius = size.height * 0.55f, center = Offset(dotX, size.height / 2f))
                }
            }
        }
    }
}

@Composable
private fun HumidityCard(humidity: Int?, cloudCover: Int?) {
    val noDataText = stringResource(R.string.forecast_no_data)
    val comfort = humidity?.let { WeatherFormatters.humidityComfortLabel(it) } ?: "Почасовых данных нет"

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(162.dp)
            .testTag("bento_humidity")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            BentoHeader(
                icon = Icons.Outlined.WaterDrop,
                title = stringResource(R.string.humidity_title),
                iconTint = SkyAzure
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = humidity?.let { "$it%" } ?: noDataText,
                style = if (humidity != null) {
                    MaterialTheme.typography.displayMedium
                } else {
                    MaterialTheme.typography.titleLarge
                },
                color = IceWhite
            )
            Text(
                text = comfort,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.weight(1f))
            if (cloudCover != null) {
                Text(
                    text = "Облачность: $cloudCover%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkyAzure
                )
            }
        }
    }
}

@Composable
private fun PressureGaugeCard(
    pressureHpa: Double?,
    settings: WeatherSettings
) {
    val noDataText = stringResource(R.string.forecast_no_data)
    val numberPart = pressureHpa?.let { WeatherFormatters.formatPressureValue(it, settings.pressureUnit) } ?: noDataText
    val unitPart = if (pressureHpa != null) settings.pressureUnit.label else ""
    val norm = pressureHpa?.let { ((it - 970.0) / 80.0).toFloat().coerceIn(0f, 1f) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(162.dp)
            .testTag("bento_pressure")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            BentoHeader(
                icon = Icons.Outlined.Compress,
                title = stringResource(R.string.pressure_title)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = numberPart,
                    style = if (pressureHpa != null) {
                        MaterialTheme.typography.headlineLarge
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                    color = IceWhite
                )
                if (unitPart.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = unitPart,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                val arcSize = Size(size.width * 0.74f, size.width * 0.74f)
                val topLeft = Offset((size.width - arcSize.width) / 2f, 6f)
                drawArc(
                    color = ObsidianBorder,
                    startAngle = 190f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = 7f, cap = StrokeCap.Round)
                )
                if (norm != null) {
                    drawArc(
                        brush = Brush.horizontalGradient(listOf(SkyAzure, AmberPrimary)),
                        startAngle = 190f,
                        sweepAngle = 160f * norm,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = 7f, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@Composable
fun AirQualityCard(
    aqi: Int,
    pm25: Double?,
    pm10: Double?,
    modifier: Modifier = Modifier
) {
    val label = WeatherFormatters.aqiDescription(aqi)
    val badgeColor = when {
        aqi <= 20 -> StatusGood
        aqi <= 40 -> Color(0xFFA3E635)
        aqi <= 60 -> StatusModerate
        aqi <= 80 -> StatusSensitive
        aqi <= 100 -> StatusUnhealthy
        else -> StatusHazardous
    }
    val progress = (aqi / 120f).coerceIn(0.05f, 1f)

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BentoHeader(
                    icon = Icons.Outlined.Co2,
                    title = stringResource(R.string.air_quality_title)
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(badgeColor.copy(alpha = 0.22f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = label, style = MaterialTheme.typography.labelLarge, color = IceWhite)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "AQI $aqi",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = IceWhite
                )
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    pm25?.let {
                        Text(
                            text = "PM2.5: ${it.roundToInt()} мкг/м³",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedText
                        )
                    }
                    pm10?.let {
                        Text(
                            text = "PM10: ${it.roundToInt()} мкг/м³",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            ) {
                val r = size.height / 2f
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(StatusGood, StatusModerate, StatusSensitive, StatusUnhealthy, StatusHazardous)
                    ),
                    size = Size(size.width, size.height * 0.7f),
                    topLeft = Offset(0f, size.height * 0.15f),
                    cornerRadius = CornerRadius(r, r)
                )
                val dotX = (progress * size.width).coerceIn(r, size.width - r)
                drawCircle(color = IceWhite, radius = size.height * 0.58f, center = Offset(dotX, size.height / 2f))
            }
        }
    }
}
