package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.SavedCity
import com.example.data.model.TemperatureUnit
import com.example.data.model.WeatherFormatters
import com.example.data.model.WeatherMood
import com.example.data.model.WeatherReport
import com.example.data.model.WeatherSettings
import com.example.ui.components.FullBleedWeatherBackdrop
import com.example.ui.components.GlassCard
import com.example.ui.components.MinimalLineWeatherIcon
import com.example.ui.state.CityWeatherLoadState
import com.example.ui.state.WeatherHomeUiState
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedText
import com.example.ui.theme.ObsidianBg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherHomeScreen(
    uiState: WeatherHomeUiState,
    onOpenMenu: () -> Unit,
    onSelectCity: (String) -> Unit,
    onRefresh: () -> Unit,
    onRetryCity: (SavedCity) -> Unit,
    onOpenCities: () -> Unit,
    onOpenForecastDetail: () -> Unit
) {
    val selectedIndex = uiState.savedCities
        .indexOfFirst { it.id == uiState.selectedCityId }
        .coerceAtLeast(0)

    val pagerState = rememberPagerState(
        initialPage = selectedIndex,
        pageCount = { uiState.savedCities.size.coerceAtLeast(1) }
    )

    LaunchedEffect(uiState.selectedCityId, uiState.savedCities.size) {
        val target = uiState.savedCities.indexOfFirst { it.id == uiState.selectedCityId }
        if (target >= 0 && target != pagerState.currentPage) {
            pagerState.animateScrollToPage(target)
        }
    }

    LaunchedEffect(pagerState.currentPage, uiState.savedCities) {
        val city = uiState.savedCities.getOrNull(pagerState.currentPage)
        if (city != null && city.id != uiState.selectedCityId) {
            onSelectCity(city.id)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        if (uiState.savedCities.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                FullBleedWeatherBackdrop(mood = WeatherMood.CLEAR_NIGHT)
                MinimalTopMenuBar(
                    cityCount = 0,
                    currentPage = 0,
                    onOpenMenu = onOpenMenu
                )
                EmptyCitiesState(
                    onOpenCities = onOpenCities,
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                )
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val city = uiState.savedCities[pageIndex]
                val loadState = uiState.reportsByCityId[city.id] ?: CityWeatherLoadState.Loading

                PullToRefreshBox(
                    isRefreshing = uiState.isGlobalRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (loadState) {
                        is CityWeatherLoadState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                FullBleedWeatherBackdrop(mood = WeatherMood.CLEAR_NIGHT)
                                MinimalTopMenuBar(
                                    cityCount = uiState.savedCities.size,
                                    currentPage = pageIndex,
                                    onOpenMenu = onOpenMenu
                                )
                                LoadingWeatherState(cityName = city.name)
                            }
                        }
                        is CityWeatherLoadState.Error -> {
                            if (loadState.cachedReport != null) {
                                EditorialWeatherContent(
                                    report = loadState.cachedReport,
                                    settings = uiState.settings,
                                    cityCount = uiState.savedCities.size,
                                    currentPage = pageIndex,
                                    onOpenMenu = onOpenMenu,
                                    onOpenForecastDetail = onOpenForecastDetail
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    FullBleedWeatherBackdrop(mood = WeatherMood.OVERCAST)
                                    MinimalTopMenuBar(
                                        cityCount = uiState.savedCities.size,
                                        currentPage = pageIndex,
                                        onOpenMenu = onOpenMenu
                                    )
                                    ErrorWeatherState(
                                        city = city,
                                        onRetry = { onRetryCity(city) }
                                    )
                                }
                            }
                        }
                        is CityWeatherLoadState.Success -> {
                            EditorialWeatherContent(
                                report = loadState.report,
                                settings = uiState.settings,
                                cityCount = uiState.savedCities.size,
                                currentPage = pageIndex,
                                onOpenMenu = onOpenMenu,
                                onOpenForecastDetail = onOpenForecastDetail
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimalTopMenuBar(
    cityCount: Int,
    currentPage: Int,
    onOpenMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        IconButton(
            onClick = onOpenMenu,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .minimumInteractiveComponentSize()
                .testTag("menu_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Menu,
                contentDescription = "Меню",
                tint = IceWhite,
                modifier = Modifier.size(28.dp)
            )
        }

        if (cityCount > 1) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(cityCount) { idx ->
                    val isCurrent = idx == currentPage
                    Box(
                        modifier = Modifier
                            .size(if (isCurrent) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCurrent) IceWhite else IceWhite.copy(alpha = 0.42f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorialWeatherContent(
    report: WeatherReport,
    settings: WeatherSettings,
    cityCount: Int,
    currentPage: Int,
    onOpenMenu: () -> Unit,
    onOpenForecastDetail: () -> Unit
) {
    val paddedTemp = remember(report.temperatureC, settings.temperatureUnit) {
        formatEditorialTemperature(report.temperatureC, settings.temperatureUnit)
    }
    val dateLabel = remember(report.currentLocalIso) {
        formatEditorialDate(report.currentLocalIso)
    }
    val shortCondition = remember(report.weatherCode, report.isDay) {
        resolveShortConditionLabel(report.weatherCode, report.isDay)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Full-bleed atmospheric backdrop spanning the entire screen
        FullBleedWeatherBackdrop(mood = report.mood)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top bar with Hamburger Menu on the left and Pager Dots centered
            MinimalTopMenuBar(
                cityCount = cityCount,
                currentPage = currentPage,
                onOpenMenu = onOpenMenu
            )

            // Upper Editorial Hero Stage (~58% of screen height)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(0.32f))

                Text(
                    text = paddedTemp,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 92.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-2).sp,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.38f),
                            offset = androidx.compose.ui.geometry.Offset(0f, 3f),
                            blurRadius = 14f
                        )
                    ),
                    color = IceWhite,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.weight(0.68f))

                // City + Date (left) and Thin-line Icon + Condition (right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 22.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = report.city.name,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = IceWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = IceWhite.copy(alpha = 0.78f)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MinimalLineWeatherIcon(
                            weatherCode = report.weatherCode,
                            isDay = report.isDay,
                            contentDescriptionText = stringResource(R.string.cd_weather_illustration),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = shortCondition,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = IceWhite
                        )
                    }
                }
            }

            // Lower Translucent Dark Details Panel (~40% height) letting backdrop colors bleed through
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x7A04060A))
                    .clickable(onClick = onOpenForecastDetail)
                    .testTag("weather_details_panel")
            ) {
                HorizontalDivider(
                    thickness = 0.8.dp,
                    color = IceWhite.copy(alpha = 0.16f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 22.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        text = "Детали погоды",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = IceWhite
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    WeatherDetailRow(
                        label = "Влажность",
                        value = "${report.humidityPercent}%"
                    )
                    WeatherDetailRow(
                        label = "УФ-индекс",
                        value = "${report.uvIndexMax.roundToInt()}"
                    )
                    WeatherDetailRow(
                        label = "Облачность",
                        value = "${report.cloudCoverPercent}%"
                    )
                    WeatherDetailRow(
                        label = "Ветер",
                        value = WeatherFormatters.formatWind(
                            report.windSpeedKmh,
                            settings.windSpeedUnit
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun WeatherDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = IceWhite.copy(alpha = 0.56f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = IceWhite.copy(alpha = 0.94f)
        )
    }
}

private fun formatEditorialTemperature(tempC: Double, unit: TemperatureUnit): String {
    val value = WeatherFormatters.convertTemp(tempC, unit)
    val absStr = abs(value).toString().padStart(2, '0')
    return if (value < 0) "-$absStr°" else "$absStr°"
}

private fun formatEditorialDate(currentIso: String): String {
    return try {
        val dateStr = currentIso.substringBefore("T")
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsed = parser.parse(dateStr) ?: Date()
        val ru = Locale("ru", "RU")
        val dayOfWeek = SimpleDateFormat("EEEE", ru).format(parsed)
            .replaceFirstChar { it.uppercase(ru) }
        val dayMonth = SimpleDateFormat("d MMMM", ru).format(parsed)
        "$dayOfWeek, $dayMonth"
    } catch (_: Exception) {
        val ru = Locale("ru", "RU")
        val now = Date()
        val dayOfWeek = SimpleDateFormat("EEEE", ru).format(now)
            .replaceFirstChar { it.uppercase(ru) }
        val dayMonth = SimpleDateFormat("d MMMM", ru).format(now)
        "$dayOfWeek, $dayMonth"
    }
}

private fun resolveShortConditionLabel(weatherCode: Int, isDay: Boolean): String {
    return when (weatherCode) {
        0 -> if (isDay) "Ясно" else "Ясно"
        1 -> "Малооблачно"
        2, 3 -> "Облачно"
        45, 48 -> "Туман"
        51, 53, 55, 56, 57 -> "Морось"
        61, 63, 65, 66, 67, 80, 81, 82 -> "Дождь"
        71, 73, 75, 77, 85, 86 -> "Снег"
        95, 96, 99 -> "Гроза"
        else -> "Облачно"
    }
}

@Composable
private fun LoadingWeatherState(cityName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = IceWhite,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Загружаем погоду: $cityName…",
                style = MaterialTheme.typography.titleMedium,
                color = IceWhite.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun ErrorWeatherState(
    city: SavedCity,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(modifier = Modifier.widthIn(max = 420.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = null,
                    tint = AmberPrimary,
                    modifier = Modifier.size(46.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${stringResource(R.string.error_network_title)} (${city.name})",
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
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IceWhite,
                        contentColor = ObsidianBg
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("retry_button")
                ) {
                    Text(
                        text = stringResource(R.string.retry_button),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCitiesState(
    onOpenCities: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(modifier = Modifier.widthIn(max = 420.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddLocationAlt,
                    contentDescription = null,
                    tint = AmberPrimary,
                    modifier = Modifier.size(46.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.empty_cities_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = IceWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.empty_cities_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onOpenCities,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IceWhite,
                        contentColor = ObsidianBg
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("empty_add_city_button")
                ) {
                    Text(text = "Добавить город", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
