package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.WeatherMood
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import kotlin.math.PI
import kotlin.math.sin

private data class WeatherParticle(
    val xFrac: Float,
    val yFrac: Float,
    val speed: Float,
    val size: Float,
    val alpha: Float
)

/**
 * Full-bleed photorealistic weather scene with subtle animated weather overlay
 * matching real-world weather conditions (raindrops on glass, bright sun rays over city,
 * moody overcast clouds, starry moonlit night, drifting snow, and storm flash).
 */
@Composable
fun FullBleedWeatherBackdrop(
    mood: WeatherMood,
    modifier: Modifier = Modifier
) {
    val drawableRes = when (mood) {
        WeatherMood.RAIN, WeatherMood.THUNDERSTORM -> R.drawable.img_weather_rain_1795187855051
        WeatherMood.CLEAR_DAY -> R.drawable.img_weather_sunny_1795187855787
        WeatherMood.PARTLY_CLOUDY_DAY, WeatherMood.OVERCAST, WeatherMood.FOG, WeatherMood.SNOW ->
            R.drawable.img_weather_cloudy_1795187856287
        WeatherMood.CLEAR_NIGHT, WeatherMood.PARTLY_CLOUDY_NIGHT ->
            R.drawable.img_weather_night_1795187856222
    }

    val transition = rememberInfiniteTransition(label = "weather_fx")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fx_phase"
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fx_pulse"
    )

    val particles = remember {
        List(44) { idx ->
            WeatherParticle(
                xFrac = ((idx * 37 + 13) % 100) / 100f,
                yFrac = ((idx * 67 + 29) % 100) / 100f,
                speed = 0.7f + ((idx * 19) % 10) / 10f,
                size = 1.8f + ((idx * 23) % 5) * 0.8f,
                alpha = 0.28f + ((idx * 31) % 5) * 0.10f
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = drawableRes,
            animationSpec = tween(durationMillis = 650),
            label = "weather_photo_crossfade"
        ) { resId ->
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Legibility scrim + subtle dynamic weather effects
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Subtle top & horizon vignette for white temperature & city text legibility
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0x52000000),
                        0.28f to Color(0x1F000000),
                        0.58f to Color(0x45000000),
                        1.0f to Color(0x66000000)
                    )
                )
            )

            when (mood) {
                WeatherMood.RAIN -> {
                    // Light animated rain streaks gliding past the glass
                    particles.take(26).forEach { p ->
                        val yProg = ((p.yFrac + phase * p.speed * 3.2f) % 1.0f)
                        val x = p.xFrac * size.width
                        val y = yProg * size.height * 0.75f
                        val len = 18f + p.size * 5f
                        drawLine(
                            color = Color.White.copy(alpha = p.alpha * 0.35f),
                            start = Offset(x, y),
                            end = Offset(x - len * 0.14f, y + len),
                            strokeWidth = 1.3f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                WeatherMood.THUNDERSTORM -> {
                    // Moody indigo storm tint + animated rain + subtle lightning pulse
                    drawRect(Color(0x330F0B26))
                    if (pulse > 0.88f) {
                        drawRect(Color.White.copy(alpha = (pulse - 0.88f) * 1.4f))
                    }
                    particles.forEach { p ->
                        val yProg = ((p.yFrac + phase * p.speed * 4.2f) % 1.0f)
                        val x = p.xFrac * size.width
                        val y = yProg * size.height * 0.80f
                        val len = 24f + p.size * 5f
                        drawLine(
                            color = Color(0xFFD6E8FF).copy(alpha = p.alpha * 0.45f),
                            start = Offset(x, y),
                            end = Offset(x - len * 0.22f, y + len),
                            strokeWidth = 1.6f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                WeatherMood.SNOW -> {
                    // Cool arctic blue tint + gently drifting snowflakes
                    drawRect(Color(0x2E183254))
                    particles.forEachIndexed { idx, p ->
                        val yProg = ((p.yFrac + phase * p.speed * 1.3f) % 1.0f)
                        val sway = sin(phase * 2 * PI + idx).toFloat() * 14f
                        drawCircle(
                            color = Color.White.copy(alpha = p.alpha * 0.85f),
                            radius = p.size * 1.15f,
                            center = Offset(p.xFrac * size.width + sway, yProg * size.height * 0.82f)
                        )
                    }
                }
                WeatherMood.FOG -> {
                    drawRect(Color(0x4564748B))
                }
                WeatherMood.CLEAR_DAY -> {
                    // Subtle warm sunbeam glow shimmer
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFF3B0).copy(alpha = 0.14f + pulse * 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.46f, size.height * 0.46f),
                            radius = size.minDimension * 0.55f
                        ),
                        radius = size.minDimension * 0.55f,
                        center = Offset(size.width * 0.46f, size.height * 0.46f)
                    )
                }
                else -> Unit
            }
        }
    }
}

@Composable
fun AtmosphericSkyBackdrop(
    mood: WeatherMood,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    surfaceAlpha: Float = 0.86f,
    amberGlow: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val borderColor = if (amberGlow) {
        Color(0x5593C5FD)
    } else {
        ObsidianBorder.copy(alpha = 0.85f)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = ObsidianCard.copy(alpha = surfaceAlpha),
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp
    ) {
        Box(content = content)
    }
}
