package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.WeatherFormatters
import com.example.ui.components.MinimalLineWeatherIcon
import com.example.ui.screens.CitiesSearchScreen
import com.example.ui.screens.ForecastDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WeatherHomeScreen
import com.example.ui.screens.WeatherMapScreen
import com.example.ui.state.CityWeatherLoadState
import com.example.ui.state.WeatherHomeUiState
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedText
import com.example.ui.theme.NeboWeatherTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianDivider
import com.example.ui.theme.ObsidianSurface
import com.example.ui.viewmodel.WeatherViewModel
import kotlinx.coroutines.launch

object WeatherRoutes {
    const val HOME = "weather_home"
    const val FORECAST = "weather_forecast"
    const val MAP = "weather_map"
    const val CITIES = "cities_search"
    const val SETTINGS = "weather_settings"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            NeboWeatherTheme {
                val context = LocalContext.current
                val navController = rememberNavController()
                val viewModel: WeatherViewModel = viewModel()
                val homeState by viewModel.homeState.collectAsStateWithLifecycle()
                val searchState by viewModel.searchState.collectAsStateWithLifecycle()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        viewModel.detectCurrentLocation()
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Разрешите доступ к геолокации, чтобы определять погоду рядом с вами"
                            )
                        }
                    }
                }

                val triggerLocationDetection = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        viewModel.detectCurrentLocation()
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                }

                LaunchedEffect(homeState.statusMessage) {
                    homeState.statusMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearStatusMessage()
                    }
                }

                val navigateFromDrawer: (String) -> Unit = { route ->
                    scope.launch {
                        drawerState.close()
                        if (route == WeatherRoutes.HOME) {
                            navController.popBackStack(WeatherRoutes.HOME, inclusive = false)
                        } else if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(WeatherRoutes.HOME)
                                launchSingleTop = true
                            }
                        }
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = currentRoute == WeatherRoutes.HOME,
                    scrimColor = Color.Black.copy(alpha = 0.65f),
                    drawerContent = {
                        WeatherSideDrawerContent(
                            homeState = homeState,
                            currentRoute = currentRoute,
                            onSelectRoute = navigateFromDrawer,
                            onDetectLocation = {
                                scope.launch {
                                    drawerState.close()
                                    navController.popBackStack(WeatherRoutes.HOME, inclusive = false)
                                    triggerLocationDetection()
                                }
                            },
                            onSelectCity = { cityId ->
                                viewModel.selectCity(cityId)
                                scope.launch {
                                    drawerState.close()
                                    navController.popBackStack(WeatherRoutes.HOME, inclusive = false)
                                }
                            }
                        )
                    }
                ) {
                    Scaffold(
                        containerColor = ObsidianBg,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = {
                            SnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier.navigationBarsPadding()
                            ) { data ->
                                Snackbar(
                                    snackbarData = data,
                                    containerColor = ObsidianSurface,
                                    contentColor = IceWhite
                                )
                            }
                        }
                    ) { _ ->
                        NavHost(
                            navController = navController,
                            startDestination = WeatherRoutes.HOME,
                            enterTransition = { fadeIn(animationSpec = tween(200)) },
                            exitTransition = { fadeOut(animationSpec = tween(200)) }
                        ) {
                            composable(WeatherRoutes.HOME) {
                                WeatherHomeScreen(
                                    uiState = homeState,
                                    onOpenMenu = { scope.launch { drawerState.open() } },
                                    onSelectCity = viewModel::selectCity,
                                    onRefresh = viewModel::refreshSelectedOrAll,
                                    onRetryCity = viewModel::retryCity,
                                    onOpenCities = { navController.navigate(WeatherRoutes.CITIES) },
                                    onOpenForecastDetail = { navController.navigate(WeatherRoutes.FORECAST) }
                                )
                            }

                            composable(WeatherRoutes.FORECAST) {
                                ForecastDetailScreen(
                                    uiState = homeState,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(WeatherRoutes.MAP) {
                                WeatherMapScreen(
                                    uiState = homeState,
                                    onOpenCities = { navController.navigate(WeatherRoutes.CITIES) },
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(WeatherRoutes.CITIES) {
                                CitiesSearchScreen(
                                    homeState = homeState,
                                    searchState = searchState,
                                    onQueryChange = viewModel::onSearchQueryChanged,
                                    onClearSearch = viewModel::clearSearch,
                                    onAddAndSelectCity = viewModel::addAndSelectCity,
                                    onSelectSavedCity = viewModel::selectCity,
                                    onRemoveCity = viewModel::removeCity,
                                    onDetectLocation = triggerLocationDetection,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(WeatherRoutes.SETTINGS) {
                                SettingsScreen(
                                    settings = homeState.settings,
                                    onSelectTemperatureUnit = viewModel::setTemperatureUnit,
                                    onSelectWindSpeedUnit = viewModel::setWindSpeedUnit,
                                    onSelectPressureUnit = viewModel::setPressureUnit,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherSideDrawerContent(
    homeState: WeatherHomeUiState,
    currentRoute: String?,
    onSelectRoute: (String) -> Unit,
    onDetectLocation: () -> Unit,
    onSelectCity: (String) -> Unit
) {
    val activeCity = homeState.savedCities.firstOrNull { it.id == homeState.selectedCityId }
        ?: homeState.savedCities.firstOrNull()
    val activeReport = activeCity?.let { homeState.reportsByCityId[it.id] }?.let { state ->
        when (state) {
            is CityWeatherLoadState.Success -> state.report
            is CityWeatherLoadState.Error -> state.cachedReport
            else -> null
        }
    }

    ModalDrawerSheet(
        drawerContainerColor = Color(0xF50B0F18),
        drawerContentColor = IceWhite,
        windowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.width(304.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 16.dp)
        ) {
            // Drawer Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Небо",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Light),
                    color = IceWhite
                )
                if (activeCity != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = activeCity.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = AmberPrimary
                        )
                        if (activeReport != null) {
                            Text(
                                text = " • ${WeatherFormatters.formatTemp(activeReport.temperatureC, homeState.settings.temperatureUnit)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = IceWhite
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = ObsidianDivider)
            Spacer(modifier = Modifier.height(10.dp))

            val itemColors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = AmberPrimary.copy(alpha = 0.18f),
                selectedIconColor = AmberPrimary,
                selectedTextColor = IceWhite,
                unselectedContainerColor = Color.Transparent,
                unselectedIconColor = MutedText,
                unselectedTextColor = IceWhite.copy(alpha = 0.88f)
            )

            NavigationDrawerItem(
                label = { Text("Главная", style = MaterialTheme.typography.titleMedium) },
                selected = currentRoute == WeatherRoutes.HOME,
                onClick = { onSelectRoute(WeatherRoutes.HOME) },
                icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                colors = itemColors,
                modifier = Modifier.testTag("drawer_home")
            )

            NavigationDrawerItem(
                label = { Text("Прогноз на 24ч и 7 дней", style = MaterialTheme.typography.titleMedium) },
                selected = currentRoute == WeatherRoutes.FORECAST,
                onClick = { onSelectRoute(WeatherRoutes.FORECAST) },
                icon = { Icon(Icons.Outlined.BarChart, contentDescription = null) },
                colors = itemColors,
                modifier = Modifier.testTag("drawer_forecast")
            )

            NavigationDrawerItem(
                label = { Text("Карта погоды", style = MaterialTheme.typography.titleMedium) },
                selected = currentRoute == WeatherRoutes.MAP,
                onClick = { onSelectRoute(WeatherRoutes.MAP) },
                icon = { Icon(Icons.Outlined.Map, contentDescription = null) },
                colors = itemColors,
                modifier = Modifier.testTag("drawer_map")
            )

            NavigationDrawerItem(
                label = { Text("Города и поиск", style = MaterialTheme.typography.titleMedium) },
                selected = currentRoute == WeatherRoutes.CITIES,
                onClick = { onSelectRoute(WeatherRoutes.CITIES) },
                icon = { Icon(Icons.Outlined.LocationCity, contentDescription = null) },
                colors = itemColors,
                modifier = Modifier.testTag("drawer_cities")
            )

            NavigationDrawerItem(
                label = { Text("Моё местоположение", style = MaterialTheme.typography.titleMedium) },
                selected = false,
                onClick = onDetectLocation,
                icon = { Icon(Icons.Outlined.MyLocation, contentDescription = null) },
                colors = itemColors,
                modifier = Modifier.testTag("drawer_location")
            )

            NavigationDrawerItem(
                label = { Text("Настройки", style = MaterialTheme.typography.titleMedium) },
                selected = currentRoute == WeatherRoutes.SETTINGS,
                onClick = { onSelectRoute(WeatherRoutes.SETTINGS) },
                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                colors = itemColors,
                modifier = Modifier.testTag("drawer_settings")
            )

            if (homeState.savedCities.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = ObsidianDivider)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "СОХРАНЁННЫЕ ГОРОДА",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                homeState.savedCities.forEach { city ->
                    val report = when (val st = homeState.reportsByCityId[city.id]) {
                        is CityWeatherLoadState.Success -> st.report
                        is CityWeatherLoadState.Error -> st.cachedReport
                        else -> null
                    }
                    val isSelected = city.id == homeState.selectedCityId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent
                            )
                            .clickable { onSelectCity(city.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = city.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (isSelected) AmberPrimary else IceWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (report != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MinimalLineWeatherIcon(
                                    weatherCode = report.weatherCode,
                                    isDay = report.isDay,
                                    contentDescriptionText = report.conditionTitle,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = WeatherFormatters.formatTemp(
                                        report.temperatureC,
                                        homeState.settings.temperatureUnit
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = IceWhite
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
