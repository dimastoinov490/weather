package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.data.model.WeatherMapper
import com.example.data.model.WeatherMood
import com.example.ui.theme.CoolCyan
import com.example.ui.theme.SkyBluePrimary
import com.example.ui.theme.SunGold
import com.example.ui.theme.SunWarmAmber
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Top-left brand badge matching the glowing blue raincloud tile in the radar map header.
 */
@Composable
fun MapAppBrandIcon(
    modifier: Modifier = Modifier.size(40.dp)
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF13294B), Color(0xFF0A162C))
                )
            )
            .border(1.dp, Color(0x5538BDF8), shape)
            .padding(6.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawGlowingRainIcon(glow = true)
        }
    }
}

/**
 * Luminous weather icon designed for the dark-navy translucent map weather panel and hourly strip.
 */
@Composable
fun MapGlowingWeatherIcon(
    weatherCode: Int,
    isDay: Boolean,
    contentDescriptionText: String? = null,
    modifier: Modifier = Modifier.size(44.dp)
) {
    val mood = WeatherMapper.resolveMood(weatherCode, isDay)
    Canvas(
        modifier = if (contentDescriptionText != null) {
            modifier.semantics { contentDescription = contentDescriptionText }
        } else {
            modifier
        }
    ) {
        when (mood) {
            WeatherMood.RAIN -> drawGlowingRainIcon(glow = true)
            WeatherMood.THUNDERSTORM -> drawGlowingStormIcon()
            WeatherMood.SNOW -> drawGlowingSnowIcon()
            WeatherMood.OVERCAST -> drawBlueCloudOnly(yOffset = 0.06f, glow = true)
            WeatherMood.FOG -> drawGlowingFogIcon()
            WeatherMood.CLEAR_DAY -> drawGlowingSunIcon(center = Offset(size.width * 0.5f, size.height * 0.5f), radius = size.width * 0.27f)
            WeatherMood.CLEAR_NIGHT -> drawGlowingMoonIcon(center = Offset(size.width * 0.5f, size.height * 0.5f), radius = size.width * 0.27f)
            WeatherMood.PARTLY_CLOUDY_DAY -> {
                drawGlowingSunIcon(center = Offset(size.width * 0.66f, size.height * 0.32f), radius = size.width * 0.18f)
                drawBlueCloudOnly(yOffset = 0.12f, glow = false)
            }
            WeatherMood.PARTLY_CLOUDY_NIGHT -> {
                drawGlowingMoonIcon(center = Offset(size.width * 0.66f, size.height * 0.30f), radius = size.width * 0.18f)
                drawBlueCloudOnly(yOffset = 0.12f, glow = false)
            }
        }
    }
}

private fun DrawScope.drawGlowingRainIcon(glow: Boolean) {
    val w = size.width
    val h = size.height
    if (glow) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x5538BDF8), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.42f),
                radius = w * 0.56f
            ),
            radius = w * 0.56f,
            center = Offset(w * 0.5f, h * 0.42f)
        )
    }

    drawCyanCloud(topLeft = Offset(w * 0.10f, h * 0.14f), cloudW = w * 0.80f, cloudH = h * 0.48f)

    // Three slanted rain drops matching the screenshot's glowing cyan drops
    val dropXs = listOf(0.32f, 0.50f, 0.68f)
    dropXs.forEachIndexed { index, xf ->
        val startY = h * (0.68f + if (index == 1) 0.02f else 0f)
        val endY = startY + h * 0.16f
        drawLine(
            color = CoolCyan,
            start = Offset(w * xf, startY),
            end = Offset(w * (xf - 0.08f), endY),
            strokeWidth = (w * 0.085f).coerceAtLeast(2.2f),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawGlowingStormIcon() {
    val w = size.width
    val h = size.height
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x4560A5FA), Color.Transparent),
            center = Offset(w * 0.5f, h * 0.42f),
            radius = w * 0.56f
        ),
        radius = w * 0.56f,
        center = Offset(w * 0.5f, h * 0.42f)
    )
    drawCyanCloud(topLeft = Offset(w * 0.10f, h * 0.12f), cloudW = w * 0.80f, cloudH = h * 0.46f)
    val bolt = Path().apply {
        moveTo(w * 0.55f, h * 0.58f)
        lineTo(w * 0.40f, h * 0.74f)
        lineTo(w * 0.52f, h * 0.74f)
        lineTo(w * 0.44f, h * 0.92f)
        lineTo(w * 0.65f, h * 0.68f)
        lineTo(w * 0.52f, h * 0.68f)
        close()
    }
    drawPath(bolt, color = SunWarmAmber)
}

private fun DrawScope.drawGlowingSnowIcon() {
    val w = size.width
    val h = size.height
    drawCyanCloud(topLeft = Offset(w * 0.10f, h * 0.14f), cloudW = w * 0.80f, cloudH = h * 0.46f)
    val flakes = listOf(
        Offset(w * 0.31f, h * 0.74f),
        Offset(w * 0.50f, h * 0.82f),
        Offset(w * 0.69f, h * 0.74f)
    )
    flakes.forEach { pt ->
        drawCircle(
            color = Color(0xFFE0F2FE),
            radius = (w * 0.06f).coerceAtLeast(2f),
            center = pt
        )
    }
}

private fun DrawScope.drawGlowingFogIcon() {
    val w = size.width
    val h = size.height
    drawCyanCloud(topLeft = Offset(w * 0.12f, h * 0.16f), cloudW = w * 0.76f, cloudH = h * 0.42f)
    listOf(0.68f to 0.64f, 0.80f to 0.48f).forEach { (yf, span) ->
        val startX = w * (0.5f - span / 2f)
        drawLine(
            color = Color(0xFFBFE4FF),
            start = Offset(startX, h * yf),
            end = Offset(startX + w * span, h * yf),
            strokeWidth = (w * 0.075f).coerceAtLeast(2.2f),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawBlueCloudOnly(yOffset: Float, glow: Boolean) {
    val w = size.width
    val h = size.height
    if (glow) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x4560A5FA), Color.Transparent),
                center = Offset(w * 0.5f, h * (0.48f + yOffset)),
                radius = w * 0.54f
            ),
            radius = w * 0.54f,
            center = Offset(w * 0.5f, h * (0.48f + yOffset))
        )
    }
    drawCyanCloud(
        topLeft = Offset(w * 0.10f, h * (0.20f + yOffset)),
        cloudW = w * 0.80f,
        cloudH = h * 0.50f
    )
}

private fun DrawScope.drawGlowingSunIcon(center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x55FBBF24), Color.Transparent),
            center = center,
            radius = radius * 2.1f
        ),
        radius = radius * 2.1f,
        center = center
    )
    for (i in 0 until 8) {
        val ang = i * 2.0 * PI / 8.0
        drawLine(
            color = SunGold,
            start = Offset(
                center.x + cos(ang).toFloat() * radius * 1.32f,
                center.y + sin(ang).toFloat() * radius * 1.32f
            ),
            end = Offset(
                center.x + cos(ang).toFloat() * radius * 1.65f,
                center.y + sin(ang).toFloat() * radius * 1.65f
            ),
            strokeWidth = (radius * 0.22f).coerceAtLeast(2f),
            cap = StrokeCap.Round
        )
    }
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFFEF08A), SunGold, SunWarmAmber),
            start = Offset(center.x - radius, center.y - radius),
            end = Offset(center.x + radius, center.y + radius)
        ),
        radius = radius,
        center = center
    )
}

private fun DrawScope.drawGlowingMoonIcon(center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x45BAE6FD), Color.Transparent),
            center = center,
            radius = radius * 2f
        ),
        radius = radius * 2f,
        center = center
    )
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
            colors = listOf(Color(0xFFF8FAFC), Color(0xFFBAE6FD), SkyBluePrimary),
            start = Offset(center.x - radius, center.y - radius),
            end = Offset(center.x + radius, center.y + radius)
        )
    )
}

private fun DrawScope.drawCyanCloud(
    topLeft: Offset,
    cloudW: Float,
    cloudH: Float
) {
    val brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFA5E3FF),
            Color(0xFF4FB3FF),
            Color(0xFF2563EB)
        ),
        startY = topLeft.y,
        endY = topLeft.y + cloudH
    )
    val baseH = cloudH * 0.54f
    val baseTop = topLeft.y + cloudH - baseH
    drawRoundRect(
        brush = brush,
        topLeft = Offset(topLeft.x, baseTop),
        size = Size(cloudW, baseH),
        cornerRadius = CornerRadius(baseH / 2f, baseH / 2f)
    )
    drawCircle(
        brush = brush,
        radius = cloudH * 0.34f,
        center = Offset(topLeft.x + cloudW * 0.30f, topLeft.y + cloudH * 0.52f)
    )
    drawCircle(
        brush = brush,
        radius = cloudH * 0.44f,
        center = Offset(topLeft.x + cloudW * 0.53f, topLeft.y + cloudH * 0.44f)
    )
    drawCircle(
        brush = brush,
        radius = cloudH * 0.30f,
        center = Offset(topLeft.x + cloudW * 0.76f, topLeft.y + cloudH * 0.58f)
    )
}
