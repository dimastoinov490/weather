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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.WeatherFormatters
import com.example.data.model.WeatherMapper
import com.example.ui.components.DailyForecastCard
import com.example.ui.components.GlassCard
import com.example.ui.components.HourlyForecastCard
import com.example.ui.components.WeatherBentoGrid
import com.example.ui.state.CityWeatherLoadState
import com.example.ui.state.WeatherHomeUiState
import com.example.ui.theme.AmberBright
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
    val loadState = activeCity?.let { uiState.reportsByCityId[it.id] }
    val report = when (loadState) {
        is CityWeatherLoadState.Success -> loadState.report
        is CityWeatherLoadState.Error -> loadState.cachedReport
        else -> null
    }

    var selectedDateIso by remember(activeCity?.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(activeCity?.id, report?.dailyForecasts) {
        val days = report?.dailyForecasts.orEmpty()
        if (days.isNotEmpty() && (selectedDateIso == null || days.none { it.dateIso == selectedDateIso })) {
            selectedDateIso = days.first().dateIso
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val dailyForecasts = report?.dailyForecasts.orEmpty()
            val activeDateIso = selectedDateIso
                ?.takeIf { iso -> dailyForecasts.any { it.dateIso == iso } }
                ?: dailyForecasts.firstOrNull()?.dateIso
            val selectedDayIndex = dailyForecasts
                .indexOfFirst { it.dateIso == activeDateIso }
                .coerceAtLeast(0)
            val selectedDay = dailyForecasts.getOrNull(selectedDayIndex)

            val currentDateIso = report?.currentLocalIso?.substringBefore("T").orEmpty()
            val isToday = selectedDay == null ||
                selectedDay.dateIso == currentDateIso ||
                (selectedDayIndex == 0 && currentDateIso.isBlank())

            val headerCondition = when {
                report == null -> ""
                isToday -> report.conditionTitle
                else -> WeatherMapper.resolveConditionTitle(selectedDay.weatherCode, true)
            }
            val headerTempText = when {
                report == null -> null
                isToday -> WeatherFormatters.formatTemp(report.temperatureC, uiState.settings.temperatureUnit)
                else -> WeatherFormatters.formatTemp(selectedDay.tempMaxC, uiState.settings.temperatureUnit)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.forecast_detail_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = IceWhite
                    )
                    if (report != null) {
                        Text(
                            text = "${report.city.name} • $headerCondition",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (headerTempText != null) {
                    Text(
                        text = headerTempText,
                        style = MaterialTheme.typography.headlineMedium,
                        color = AmberBright,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            when {
                report == null && loadState is CityWeatherLoadState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassCard(modifier = Modifier.widthIn(max = 380.dp)) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudOff,
                                    contentDescription = null,
                                    tint = AmberPrimary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.error_network_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = IceWhite,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.error_network_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedText,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                report == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AmberPrimary)
                    }
                }

                else -> {
                    val allHoursSource = remember(report) {
                        if (report.allHourlyPoints.isNotEmpty()) {
                            report.allHourlyPoints
                        } else {
                            report.hourlyPoints
                        }
                    }
                    val targetDateIso = selectedDay?.dateIso
                    val dayHours = remember(allHoursSource, targetDateIso) {
                        if (targetDateIso != null) {
                            allHoursSource.filter { it.timeIso.startsWith(targetDateIso) }
                        } else {
                            report.hourlyPoints
                        }
                    }
                    val dayTitle = remember(selectedDay, selectedDayIndex) {
                        selectedDay?.let { WeatherFormatters.resolveDayFullTitle(it, selectedDayIndex) }
                    }
                    val currentHourPrefix = report.currentLocalIso.take(13)

                    val scrollState = rememberScrollState()
                    LaunchedEffect(activeCity?.id) {
                        scrollState.scrollTo(0)
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val columns = if (maxWidth >= 720.dp) 3 else 2

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 860.dp)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .navigationBarsPadding(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                HourlyForecastCard(
                                    hourlyPoints = dayHours,
                                    temperatureUnit = uiState.settings.temperatureUnit,
                                    activeHourPrefix = if (isToday) currentHourPrefix else null,
                                    dayTitleSuffix = dayTitle,
                                    isEmptyForDate = true
                                )

                                DailyForecastCard(
                                    dailyForecasts = report.dailyForecasts,
                                    weekMinTempC = report.weekMinTempC,
                                    weekMaxTempC = report.weekMaxTempC,
                                    currentTempC = report.temperatureC,
                                    temperatureUnit = uiState.settings.temperatureUnit,
                                    showRangeBars = true,
                                    selectedDateIso = selectedDay?.dateIso,
                                    onSelectDay = { clickedDay ->
                                        selectedDateIso = clickedDay.dateIso
                                    }
                                )

                                WeatherBentoGrid(
                                    report = report,
                                    settings = uiState.settings,
                                    columns = columns,
                                    selectedDay = selectedDay,
                                    dayHours = dayHours,
                                    isToday = isToday
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
