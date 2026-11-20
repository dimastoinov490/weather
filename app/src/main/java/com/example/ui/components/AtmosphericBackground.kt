package com.example.ui.components

import androidx.compose.animation.Crossfade
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.WeatherMood
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard

/**
 * Full-bleed static photorealistic weather scene matching current weather conditions
 * (rain, sunny day, cloudy/overcast/fog/snow, moonlit night) with a subtle legibility scrim.
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

        // Static subtle vignette for typography legibility
        Canvas(modifier = Modifier.fillMaxSize()) {
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
