package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.data.model.WeatherMapper
import com.example.data.model.WeatherMood
import com.example.ui.theme.AmberBright
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CoolCyan
import com.example.ui.theme.IceWhite
import com.example.ui.theme.SunGold
import com.example.ui.theme.SunWarmAmber
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Crisp thin-line white vector weather icon matching the minimalist editorial home screen style.
 */
@Composable
fun MinimalLineWeatherIcon(
    weatherCode: Int,
    isDay: Boolean,
    contentDescriptionText: String,
    modifier: Modifier = Modifier.size(38.dp)
) {
    val mood = WeatherMapper.resolveMood(weatherCode, isDay)
    Canvas(
        modifier = modifier.semantics { contentDescription = contentDescriptionText }
    ) {
        val w = size.width
        val h = size.height
        val strokeWidth = (w * 0.052f).coerceIn(2.2f, 4.5f)
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (mood) {
            WeatherMood.CLEAR_DAY -> {
                val c = Offset(w * 0.5f, h * 0.5f)
                val r = w * 0.22f
                drawCircle(color = IceWhite, radius = r, center = c, style = stroke)
                for (i in 0 until 8) {
                    val ang = i * PI / 4.0
                    drawLine(
                        color = IceWhite,
                        start = Offset(c.x + cos(ang).toFloat() * r * 1.42f, c.y + sin(ang).toFloat() * r * 1.42f),
                        end = Offset(c.x + cos(ang).toFloat() * r * 1.82f, c.y + sin(ang).toFloat() * r * 1.82f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
            WeatherMood.CLEAR_NIGHT -> {
                val moonPath = Path().apply {
                    moveTo(w * 0.58f, h * 0.20f)
                    cubicTo(w * 0.26f, h * 0.24f, w * 0.22f, h * 0.72f, w * 0.62f, h * 0.80f)
                    cubicTo(w * 0.42f, h * 0.66f, w * 0.42f, h * 0.36f, w * 0.58f, h * 0.20f)
                    close()
                }
                drawPath(moonPath, color = IceWhite, style = stroke)
            }
            WeatherMood.PARTLY_CLOUDY_DAY -> {
                // Small sun arc behind cloud
                val sunC = Offset(w * 0.66f, h * 0.34f)
                val sunR = w * 0.14f
                drawArc(
                    color = IceWhite,
                    startAngle = -140f,
                    sweepAngle = 210f,
                    useCenter = false,
                    topLeft = Offset(sunC.x - sunR, sunC.y - sunR),
                    size = Size(sunR * 2f, sunR * 2f),
                    style = stroke
                )
                drawLineCloud(w, h, yShift = 0.08f, stroke = stroke)
            }
            WeatherMood.PARTLY_CLOUDY_NIGHT -> {
                val moonPath = Path().apply {
                    moveTo(w * 0.72f, h * 0.16f)
                    cubicTo(w * 0.52f, h * 0.19f, w * 0.50f, h * 0.45f, w * 0.72f, h * 0.48f)
                }
                drawPath(moonPath, color = IceWhite, style = stroke)
                drawLineCloud(w, h, yShift = 0.08f, stroke = stroke)
            }
            WeatherMood.OVERCAST -> {
                drawLineCloud(w, h, yShift = 0.06f, stroke = stroke)
            }
            WeatherMood.FOG -> {
                drawLineCloud(w, h, yShift = -0.04f, stroke = stroke)
                listOf(0.68f, 0.80f).forEachIndexed { idx, yf ->
                    val pad = if (idx == 0) 0.18f else 0.26f
                    drawLine(
                        color = IceWhite,
                        start = Offset(w * pad, h * yf),
                        end = Offset(w * (1f - pad), h * yf),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
            WeatherMood.RAIN -> {
                drawLineCloud(w, h, yShift = -0.08f, stroke = stroke)
                val xs = listOf(0.32f, 0.44f, 0.56f, 0.68f)
                xs.forEach { xf ->
                    drawLine(
                        color = IceWhite,
                        start = Offset(w * xf, h * 0.65f),
                        end = Offset(w * (xf - 0.07f), h * 0.84f),
                        strokeWidth = strokeWidth * 0.85f,
                        cap = StrokeCap.Round
                    )
                }
            }
            WeatherMood.SNOW -> {
                drawLineCloud(w, h, yShift = -0.08f, stroke = stroke)
                val pts = listOf(
                    Offset(w * 0.34f, h * 0.72f),
                    Offset(w * 0.50f, h * 0.82f),
                    Offset(w * 0.66f, h * 0.72f)
                )
                pts.forEach { pt ->
                    drawCircle(color = IceWhite, radius = strokeWidth * 0.75f, center = pt)
                }
            }
            WeatherMood.THUNDERSTORM -> {
                drawLineCloud(w, h, yShift = -0.08f, stroke = stroke)
                val bolt = Path().apply {
                    moveTo(w * 0.54f, h * 0.60f)
                    lineTo(w * 0.43f, h * 0.74f)
                    lineTo(w * 0.53f, h * 0.74f)
                    lineTo(w * 0.46f, h * 0.90f)
                }
                drawPath(bolt, color = IceWhite, style = stroke)
            }
        }
    }
}

private fun DrawScope.drawLineCloud(
    w: Float,
    h: Float,
    yShift: Float,
    stroke: Stroke
) {
    val yOff = h * yShift
    val path = Path().apply {
        moveTo(w * 0.28f, h * 0.62f + yOff)
        lineTo(w * 0.72f, h * 0.62f + yOff)
        cubicTo(
            w * 0.87f, h * 0.62f + yOff,
            w * 0.87f, h * 0.40f + yOff,
            w * 0.70f, h * 0.40f + yOff
        )
        cubicTo(
            w * 0.68f, h * 0.22f + yOff,
            w * 0.42f, h * 0.20f + yOff,
            w * 0.36f, h * 0.36f + yOff
        )
        cubicTo(
            w * 0.16f, h * 0.35f + yOff,
            w * 0.13f, h * 0.62f + yOff,
            w * 0.28f, h * 0.62f + yOff
        )
        close()
    }
    drawPath(path = path, color = IceWhite, style = stroke)
}

@Composable
fun HeroWeatherIllustration(
    weatherCode: Int,
    isDay: Boolean,
    contentDescriptionText: String,
    modifier: Modifier = Modifier
) {
    val mood = WeatherMapper.resolveMood(weatherCode, isDay)
    val transition = rememberInfiniteTransition(label = "hero_weather_anim")
    val bob by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )
    val rayAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rays"
    )

    Canvas(
        modifier = modifier
            .size(116.dp)
            .semantics { contentDescription = contentDescriptionText }
    ) {
        drawWeatherGraphic(
            mood = mood,
            bobOffset = bob,
            rayRotationDeg = rayAngle,
            detailed = true
        )
    }
}

@Composable
fun CompactWeatherIcon(
    weatherCode: Int,
    isDay: Boolean,
    modifier: Modifier = Modifier.size(28.dp)
) {
    val mood = WeatherMapper.resolveMood(weatherCode, isDay)
    Canvas(modifier = modifier) {
        drawWeatherGraphic(
            mood = mood,
            bobOffset = 0f,
            rayRotationDeg = 0f,
            detailed = false
        )
    }
}

private fun DrawScope.drawWeatherGraphic(
    mood: WeatherMood,
    bobOffset: Float,
    rayRotationDeg: Float,
    detailed: Boolean
) {
    val w = size.width
    val h = size.height

    when (mood) {
        WeatherMood.CLEAR_DAY -> {
            drawSun(
                center = Offset(w * 0.5f, h * 0.5f + bobOffset * 0.4f),
                radius = w * 0.27f,
                rayRotationDeg = rayRotationDeg,
                detailed = detailed
            )
        }
        WeatherMood.CLEAR_NIGHT -> {
            drawCrescentMoon(
                center = Offset(w * 0.5f, h * 0.5f + bobOffset * 0.35f),
                radius = w * 0.27f,
                detailed = detailed
            )
        }
        WeatherMood.PARTLY_CLOUDY_DAY -> {
            drawSun(
                center = Offset(w * 0.64f, h * 0.34f + bobOffset * 0.3f),
                radius = w * 0.20f,
                rayRotationDeg = rayRotationDeg,
                detailed = detailed
            )
            drawSculptedCloud(
                topLeft = Offset(w * 0.12f, h * 0.36f + bobOffset * 0.5f),
                cloudWidth = w * 0.72f,
                cloudHeight = h * 0.40f,
                tintTop = Color(0xFFFFFFFF),
                tintBottom = Color(0xFFBACDF2)
            )
        }
        WeatherMood.PARTLY_CLOUDY_NIGHT -> {
            drawCrescentMoon(
                center = Offset(w * 0.37f, h * 0.36f + bobOffset * 0.25f),
                radius = w * 0.24f,
                detailed = detailed
            )
            drawSculptedCloud(
                topLeft = Offset(w * 0.15f, h * 0.38f + bobOffset * 0.5f),
                cloudWidth = w * 0.72f,
                cloudHeight = h * 0.42f,
                tintTop = Color(0xFFF6F9FF),
                tintBottom = Color(0xFFAEC4EE)
            )
        }
        WeatherMood.OVERCAST -> {
            drawSculptedCloud(
                topLeft = Offset(w * 0.24f, h * 0.24f - bobOffset * 0.3f),
                cloudWidth = w * 0.58f,
                cloudHeight = h * 0.32f,
                tintTop = Color(0xFFD5E1F0),
                tintBottom = Color(0xFF97ACC6)
            )
            drawSculptedCloud(
                topLeft = Offset(w * 0.12f, h * 0.36f + bobOffset * 0.45f),
                cloudWidth = w * 0.74f,
                cloudHeight = h * 0.40f,
                tintTop = Color(0xFFF3F8FF),
                tintBottom = Color(0xFFB8CCE8)
            )
        }
        WeatherMood.FOG -> {
            drawSculptedCloud(
                topLeft = Offset(w * 0.18f, h * 0.24f + bobOffset * 0.3f),
                cloudWidth = w * 0.64f,
                cloudHeight = h * 0.34f,
                tintTop = Color(0xFFEAF2FA),
                tintBottom = Color(0xFFB3C7E0)
            )
            val lineWidths = listOf(0.64f, 0.76f, 0.54f)
            lineWidths.forEachIndexed { idx, frac ->
                val y = h * (0.64f + idx * 0.11f)
                val startX = w * (0.5f - frac / 2f)
                drawLine(
                    color = Color.White.copy(alpha = 0.75f - idx * 0.12f),
                    start = Offset(startX, y),
                    end = Offset(startX + w * frac, y),
                    strokeWidth = (w * 0.065f).coerceAtLeast(2.2f),
                    cap = StrokeCap.Round
                )
            }
        }
        WeatherMood.RAIN -> {
            drawSculptedCloud(
                topLeft = Offset(w * 0.14f, h * 0.20f + bobOffset * 0.4f),
                cloudWidth = w * 0.72f,
                cloudHeight = h * 0.38f,
                tintTop = Color(0xFFEAF2FC),
                tintBottom = Color(0xFFA8C1E0)
            )
            val dropXs = listOf(0.32f, 0.50f, 0.68f)
            dropXs.forEachIndexed { idx, xf ->
                val startY = h * (0.65f + (idx % 2) * 0.05f)
                drawLine(
                    color = CoolCyan,
                    start = Offset(w * xf, startY),
                    end = Offset(w * (xf - 0.06f), startY + h * 0.18f),
                    strokeWidth = (w * 0.07f).coerceAtLeast(2.2f),
                    cap = StrokeCap.Round
                )
            }
        }
        WeatherMood.SNOW -> {
            drawSculptedCloud(
                topLeft = Offset(w * 0.14f, h * 0.20f + bobOffset * 0.4f),
                cloudWidth = w * 0.72f,
                cloudHeight = h * 0.38f,
                tintTop = Color.White,
                tintBottom = Color(0xFFCFE1F5)
            )
            val flakeCenters = listOf(
                Offset(w * 0.32f, h * 0.71f),
                Offset(w * 0.50f, h * 0.79f),
                Offset(w * 0.68f, h * 0.70f)
            )
            flakeCenters.forEach { c ->
                drawCircle(
                    color = Color.White,
                    radius = (w * 0.055f).coerceAtLeast(2f),
                    center = c
                )
            }
        }
        WeatherMood.THUNDERSTORM -> {
            drawSculptedCloud(
                topLeft = Offset(w * 0.14f, h * 0.18f + bobOffset * 0.4f),
                cloudWidth = w * 0.72f,
                cloudHeight = h * 0.38f,
                tintTop = Color(0xFFD6DFEE),
                tintBottom = Color(0xFF8C9EB8)
            )
            val bolt = Path().apply {
                moveTo(w * 0.53f, h * 0.54f)
                lineTo(w * 0.39f, h * 0.72f)
                lineTo(w * 0.51f, h * 0.72f)
                lineTo(w * 0.43f, h * 0.90f)
                lineTo(w * 0.63f, h * 0.66f)
                lineTo(w * 0.50f, h * 0.66f)
                close()
            }
            drawPath(bolt, color = AmberBright)
        }
    }
}

private fun DrawScope.drawSun(
    center: Offset,
    radius: Float,
    rayRotationDeg: Float,
    detailed: Boolean
) {
    if (detailed) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AmberPrimary.copy(alpha = 0.42f), Color.Transparent),
                center = center,
                radius = radius * 2.0f
            ),
            radius = radius * 2.0f,
            center = center
        )
    }

    rotate(degrees = rayRotationDeg, pivot = center) {
        val rayCount = 8
        for (i in 0 until rayCount) {
            val angleRad = (i * 2.0 * PI / rayCount)
            val inner = radius * 1.25f
            val outer = radius * 1.56f
            drawLine(
                color = SunGold.copy(alpha = 0.92f),
                start = Offset(
                    center.x + cos(angleRad).toFloat() * inner,
                    center.y + sin(angleRad).toFloat() * inner
                ),
                end = Offset(
                    center.x + cos(angleRad).toFloat() * outer,
                    center.y + sin(angleRad).toFloat() * outer
                ),
                strokeWidth = (radius * 0.22f).coerceAtLeast(2.2f),
                cap = StrokeCap.Round
            )
        }
    }

    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFFFF099), SunGold, SunWarmAmber),
            start = Offset(center.x - radius, center.y - radius),
            end = Offset(center.x + radius, center.y + radius)
        ),
        radius = radius,
        center = center
    )
}

private fun DrawScope.drawCrescentMoon(
    center: Offset,
    radius: Float,
    detailed: Boolean
) {
    if (detailed) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x45FFF6D6), Color.Transparent),
                center = center,
                radius = radius * 1.9f
            ),
            radius = radius * 1.9f,
            center = center
        )
    }
    val moonPath = Path().apply {
        moveTo(center.x + radius * 0.25f, center.y - radius * 0.95f)
        cubicTo(
            center.x - radius * 0.72f, center.y - radius * 0.85f,
            center.x - radius * 0.90f, center.y + radius * 0.68f,
            center.x + radius * 0.35f, center.y + radius * 0.94f
        )
        cubicTo(
            center.x - radius * 0.25f, center.y + radius * 0.55f,
            center.x - radius * 0.20f, center.y - radius * 0.45f,
            center.x + radius * 0.25f, center.y - radius * 0.95f
        )
        close()
    }
    drawPath(
        path = moonPath,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFFE2E8F0)),
            start = Offset(center.x - radius, center.y - radius),
            end = Offset(center.x + radius, center.y + radius)
        )
    )
}

private fun DrawScope.drawSculptedCloud(
    topLeft: Offset,
    cloudWidth: Float,
    cloudHeight: Float,
    tintTop: Color,
    tintBottom: Color
) {
    val brush = Brush.verticalGradient(
        colors = listOf(tintTop, tintBottom),
        startY = topLeft.y,
        endY = topLeft.y + cloudHeight
    )

    val baseHeight = cloudHeight * 0.54f
    val baseTop = topLeft.y + cloudHeight - baseHeight
    drawRoundRect(
        brush = brush,
        topLeft = Offset(topLeft.x, baseTop),
        size = Size(cloudWidth, baseHeight),
        cornerRadius = CornerRadius(baseHeight / 2f, baseHeight / 2f)
    )

    drawCircle(
        brush = brush,
        radius = cloudHeight * 0.34f,
        center = Offset(topLeft.x + cloudWidth * 0.30f, topLeft.y + cloudHeight * 0.52f)
    )
    drawCircle(
        brush = brush,
        radius = cloudHeight * 0.44f,
        center = Offset(topLeft.x + cloudWidth * 0.53f, topLeft.y + cloudHeight * 0.44f)
    )
    drawCircle(
        brush = brush,
        radius = cloudHeight * 0.30f,
        center = Offset(topLeft.x + cloudWidth * 0.76f, topLeft.y + cloudHeight * 0.58f)
    )
}
