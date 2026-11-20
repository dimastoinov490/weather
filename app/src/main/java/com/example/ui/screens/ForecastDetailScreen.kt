package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.WeatherFormatters
import com.example.ui.components.DailyForecastCard
import com.example.ui.components.HourlyForecastCard
import com.example.ui.components.WeatherBentoGrid
import com.example.ui.state.CityWeatherLoadState
import com.example.ui.state.WeatherHomeUiState
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedText
import com.example.ui.theme.ObsidianBg

@Composable
fun ForecastDetailScreen(
    uiState: WeatherHomeUiState,
    onNavigateBack: () -> Unit
) {
    val activeCity = uiState.savedCities.firstOrNull { it.id == uiState.selectedCityId }
        ?: uiState.savedCities.firstOrNull()
    val report = activeCity?.let { uiState.reportsByCityId[it.id] }?.let { state ->
        when (state) {
            is CityWeatherLoadState.Success -> state.report
            is CityWeatherLoadState.Error -> state.cachedReport
            else -> null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("forecast_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = IceWhite
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Подробный прогноз",
                            style = MaterialTheme.typography.headlineMedium,
                            color = IceWhite
                        )
                        if (report != null) {
                            Text(
                                text = "${report.city.name} • ${report.conditionTitle}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedText
                            )
                        }
                    }
                }

                if (report != null) {
                    Text(
                        text = WeatherFormatters.formatTemp(
                            report.temperatureC,
                            uiState.settings.temperatureUnit
                        ),
                        style = MaterialTheme.typography.headlineLarge,
                        color = AmberPrimary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            }

            if (report == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AmberPrimary)
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val columns = if (maxWidth >= 720.dp) 3 else 2

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 840.dp)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .navigationBarsPadding(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            HourlyForecastCard(
                                hourlyPoints = report.hourlyPoints,
                                temperatureUnit = uiState.settings.temperatureUnit
                            )

                            DailyForecastCard(
                                dailyForecasts = report.dailyForecasts,
                                weekMinTempC = report.weekMinTempC,
                                weekMaxTempC = report.weekMaxTempC,
                                currentTempC = report.temperatureC,
                                temperatureUnit = uiState.settings.temperatureUnit,
                                showRangeBars = true
                            )

                            WeatherBentoGrid(
                                report = report,
                                settings = uiState.settings,
                                columns = columns
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}
