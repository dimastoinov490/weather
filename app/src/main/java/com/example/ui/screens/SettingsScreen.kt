package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PressureUnit
import com.example.data.model.TemperatureUnit
import com.example.data.model.WeatherSettings
import com.example.data.model.WindSpeedUnit
import com.example.ui.components.GlassCard
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedText
import com.example.ui.theme.ObsidianBg

@Composable
fun SettingsScreen(
    settings: WeatherSettings,
    onSelectTemperatureUnit: (TemperatureUnit) -> Unit,
    onSelectWindSpeedUnit: (WindSpeedUnit) -> Unit,
    onSelectPressureUnit: (PressureUnit) -> Unit,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = IceWhite
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = IceWhite
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    UnitSelectorCard(
                        title = "Температура",
                        options = listOf(
                            TemperatureUnit.CELSIUS to "Цельсий (°C)",
                            TemperatureUnit.FAHRENHEIT to "Фаренгейт (°F)"
                        ),
                        selected = settings.temperatureUnit,
                        onSelect = onSelectTemperatureUnit,
                        tagPrefix = "temp_unit"
                    )

                    UnitSelectorCard(
                        title = "Скорость ветра",
                        options = listOf(
                            WindSpeedUnit.MS to "м/с",
                            WindSpeedUnit.KMH to "км/ч"
                        ),
                        selected = settings.windSpeedUnit,
                        onSelect = onSelectWindSpeedUnit,
                        tagPrefix = "wind_unit"
                    )

                    UnitSelectorCard(
                        title = "Атмосферное давление",
                        options = listOf(
                            PressureUnit.MMHG to "мм рт. ст.",
                            PressureUnit.HPA to "гПа"
                        ),
                        selected = settings.pressureUnit,
                        onSelect = onSelectPressureUnit,
                        tagPrefix = "pressure_unit"
                    )

                    GlassCard(modifier = Modifier.fillMaxWidth(), amberGlow = true) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "О ПРИЛОЖЕНИИ",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Небо",
                                style = MaterialTheme.typography.titleLarge,
                                color = IceWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Метеоданные, почасовая кривая температуры, прогноз на 7 дней и индекс качества воздуха (AQI) предоставляются Open-Meteo. Радар осадков — RainViewer и CartoDB.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedText
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> UnitSelectorCard(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    tagPrefix: String
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MutedText
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0A0E17))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEachIndexed { idx, (value, label) ->
                    val isSelected = value == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (isSelected) AmberPrimary else Color.Transparent)
                            .clickable { onSelect(value) }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                            .testTag("${tagPrefix}_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) ObsidianBg else IceWhite.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}
