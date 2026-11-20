package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.SavedCity
import com.example.data.model.WeatherFormatters
import com.example.ui.components.CompactWeatherIcon
import com.example.ui.components.GlassCard
import com.example.ui.state.CitySearchUiState
import com.example.ui.state.CityWeatherLoadState
import com.example.ui.state.WeatherHomeUiState
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedText
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder

@Composable
fun CitiesSearchScreen(
    homeState: WeatherHomeUiState,
    searchState: CitySearchUiState,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onAddAndSelectCity: (SavedCity) -> Unit,
    onSelectSavedCity: (String) -> Unit,
    onRemoveCity: (String) -> Unit,
    onDetectLocation: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onDetectLocation()
            onNavigateBack()
        }
    }

    val handleLocationClick = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            onDetectLocation()
            onNavigateBack()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("cities_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = IceWhite
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.cities_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = IceWhite
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = searchState.query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_city_hint),
                        color = MutedText
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = AmberPrimary
                    )
                },
                trailingIcon = {
                    if (searchState.query.isNotEmpty()) {
                        IconButton(
                            onClick = onClearSearch,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.cd_clear_search),
                                tint = IceWhite
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = IceWhite,
                    unfocusedTextColor = IceWhite,
                    cursorColor = AmberPrimary,
                    focusedBorderColor = AmberPrimary,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedContainerColor = Color(0xFF111623),
                    unfocusedContainerColor = Color(0xFF111623)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("city_search_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (searchState.query.trim().length >= 2) {
                    if (searchState.isSearching) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = AmberPrimary,
                                    strokeWidth = 2.5.dp
                                )
                            }
                        }
                    } else if (searchState.errorMessage != null) {
                        item {
                            Text(
                                text = searchState.errorMessage,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MutedText,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else if (searchState.results.isEmpty()) {
                        item {
                            Text(
                                text = "Ничего не найдено по запросу «${searchState.query}»",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MutedText,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        items(searchState.results, key = { it.id }) { city ->
                            SearchResultRow(
                                city = city,
                                onClick = {
                                    onAddAndSelectCity(city)
                                    onClearSearch()
                                    onNavigateBack()
                                }
                            )
                        }
                    }
                } else {
                    item {
                        GlassCard(
                            amberGlow = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .clickable(onClick = handleLocationClick)
                                .testTag("my_location_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(AmberPrimary.copy(alpha = 0.20f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.MyLocation,
                                        contentDescription = null,
                                        tint = AmberPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.my_location_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = IceWhite
                                    )
                                    Text(
                                        text = stringResource(R.string.my_location_subtitle),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MutedText
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "СОХРАНЁННЫЕ ГОРОДА",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedText,
                            modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                        )
                    }

                    if (homeState.savedCities.isEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stringResource(R.string.empty_cities_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = IceWhite
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.empty_cities_subtitle),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MutedText
                                    )
                                }
                            }
                        }
                    } else {
                        items(homeState.savedCities, key = { it.id }) { city ->
                            val loadState = homeState.reportsByCityId[city.id]
                            val report = when (loadState) {
                                is CityWeatherLoadState.Success -> loadState.report
                                is CityWeatherLoadState.Error -> loadState.cachedReport
                                else -> null
                            }
                            SavedCityRow(
                                city = city,
                                isSelected = city.id == homeState.selectedCityId,
                                temperatureText = report?.let {
                                    WeatherFormatters.formatTemp(
                                        it.temperatureC,
                                        homeState.settings.temperatureUnit
                                    )
                                } ?: "--°",
                                conditionText = report?.conditionTitle ?: "Загрузка…",
                                weatherCode = report?.weatherCode,
                                isDay = report?.isDay ?: true,
                                onClick = {
                                    onSelectSavedCity(city.id)
                                    onNavigateBack()
                                },
                                onDelete = { onRemoveCity(city.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    city: SavedCity,
    onClick: () -> Unit
) {
    GlassCard(
        cornerRadius = 18.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("search_result_item")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = AmberPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = city.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = IceWhite
                )
                val subtitle = listOf(city.region, city.country)
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = AmberPrimary
            )
        }
    }
}

@Composable
private fun SavedCityRow(
    city: SavedCity,
    isSelected: Boolean,
    temperatureText: String,
    conditionText: String,
    weatherCode: Int?,
    isDay: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        cornerRadius = 20.dp,
        amberGlow = isSelected,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("saved_city_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (city.isCurrentLocation) {
                        Icon(
                            imageVector = Icons.Outlined.MyLocation,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = city.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = IceWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = conditionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (weatherCode != null) {
                CompactWeatherIcon(
                    weatherCode = weatherCode,
                    isDay = isDay,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Text(
                text = temperatureText,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Normal),
                color = IceWhite
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("delete_city_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(R.string.cd_delete_city),
                    tint = MutedText
                )
            }
        }
    }
}
