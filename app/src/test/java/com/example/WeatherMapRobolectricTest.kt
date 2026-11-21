package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.model.DailyForecastItem
import com.example.data.model.HourlyForecastPoint
import com.example.data.model.NominatimReverseResponse
import com.example.data.model.RainViewerMapsResponse
import com.example.data.model.SavedCity
import com.example.data.model.WeatherReport
import com.example.data.remote.NetworkModule
import com.example.ui.screens.WeatherMapScreen
import com.example.ui.state.CityWeatherLoadState
import com.example.ui.state.WeatherHomeUiState
import com.example.ui.theme.NeboWeatherTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class WeatherMapRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mapnikTileSourceUsesOpenStreetMapWithoutCartoOrApiKey() {
        val tileIndex = MapTileIndex.getTileIndex(7, 73, 44)
        val tileUrl = TileSourceFactory.MAPNIK.getTileURLString(tileIndex)
        assertTrue("Expected OpenStreetMap URL, got $tileUrl", tileUrl.startsWith("https://tile.openstreetmap.org/"))
        assertTrue(tileUrl.endsWith("/7/73/44.png"))
        assertFalse(tileUrl.contains("carto", ignoreCase = true))
    }

    @Test
    fun rainViewerJsonParsesPastAndNowcastFrames() {
        val sampleJson = """
            {
              "host": "https://tilecache.rainviewer.com",
              "radar": {
                "past": [
                  { "time": 1700000000, "path": "/v2/radar/1700000000" }
                ],
                "nowcast": [
                  { "time": 1700000600, "path": "/v2/radar/nowcast_1700000600" }
                ]
              }
            }
        """.trimIndent()

        val adapter = NetworkModule.sharedMoshi().adapter(RainViewerMapsResponse::class.java)
        val parsed = adapter.fromJson(sampleJson)
        assertNotNull(parsed)
        assertEquals("https://tilecache.rainviewer.com", parsed?.host)
        assertEquals(1, parsed?.radar?.past?.size)
        assertEquals(1, parsed?.radar?.nowcast?.size)
        assertEquals("/v2/radar/1700000000", parsed?.radar?.past?.first()?.path)
    }

    @Test
    fun nominatimReverseGeocodeJsonParsesCityStateAndCountry() {
        val sampleJson = """
            {
              "name": "Тирасполь",
              "address": {
                "city": "Тирасполь",
                "state": "Приднестровье",
                "country": "Молдова"
              }
            }
        """.trimIndent()

        val adapter = NetworkModule.sharedMoshi().adapter(NominatimReverseResponse::class.java)
        val parsed = adapter.fromJson(sampleJson)
        assertNotNull(parsed)
        assertEquals("Тирасполь", parsed?.address?.city)
        assertEquals("Приднестровье", parsed?.address?.state)
        assertEquals("Молдова", parsed?.address?.country)
    }

    @Test
    fun weatherMapScreenDisplaysControlsLegendPanelAndTogglesRadar() {
        val city = SavedCity(
            id = "tiraspol",
            name = "Тирасполь",
            country = "Молдова",
            latitude = 46.8403,
            longitude = 29.6433,
            isCurrentLocation = true
        )
        val report = sampleReport(city)
        val uiState = WeatherHomeUiState(
            savedCities = listOf(city),
            selectedCityId = city.id,
            reportsByCityId = mapOf(city.id to CityWeatherLoadState.Success(report)),
            locationRecenterToken = 1
        )

        var gpsClicked = false
        composeRule.setContent {
            NeboWeatherTheme {
                WeatherMapScreen(
                    uiState = uiState,
                    onOpenCities = {},
                    onNavigateBack = {},
                    onDetectLocation = { gpsClicked = true }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("weather_map_native_container").assertIsDisplayed()
        composeRule.onNodeWithTag("map_tab_map").assertIsDisplayed()
        composeRule.onNodeWithTag("map_tab_forecast").assertIsDisplayed()
        composeRule.onNodeWithTag("map_tab_precipitation").assertIsDisplayed()
        composeRule.onNodeWithText("Слабо").assertIsDisplayed()
        composeRule.onNodeWithText("Умеренно").assertIsDisplayed()
        composeRule.onNodeWithText("Сильно").assertIsDisplayed()
        composeRule.onNodeWithTag("map_zoom_in").assertIsDisplayed()
        composeRule.onNodeWithTag("map_zoom_out").assertIsDisplayed()
        composeRule.onNodeWithTag("map_gps_button").assertIsDisplayed()
        composeRule.onNodeWithTag("map_city_selector").assertIsDisplayed()
        composeRule.onNodeWithTag("map_bottom_weather_panel").assertIsDisplayed()
        composeRule.onNodeWithText("© OpenStreetMap • RainViewer").assertIsDisplayed()

        // Verify GPS button invokes location detection callback
        composeRule.onNodeWithTag("map_gps_button").performClick()
        composeRule.waitForIdle()
        assertTrue(gpsClicked)

        // Verify radar toggle switches cleanly between enabled and disabled
        composeRule.onNodeWithText("Осадки: вкл").assertIsDisplayed()
        composeRule.onNodeWithTag("map_radar_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Осадки: выкл").assertIsDisplayed()

        // Switching to Forecast tab re-enables radar and shows timeline
        composeRule.onNodeWithTag("map_tab_forecast").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("map_radar_timeline").assertIsDisplayed()
        composeRule.onNodeWithText("Осадки: вкл").assertIsDisplayed()

        // Switching to Precipitation tab preserves enabled radar
        composeRule.onNodeWithTag("map_tab_precipitation").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("map_tab_precipitation").assertIsDisplayed()
    }

    @Test
    fun weatherMapScreenDisablesGpsButtonClicksWhileLocating() {
        val city = SavedCity(
            id = "moscow",
            name = "Москва",
            country = "Россия",
            latitude = 55.7558,
            longitude = 37.6173
        )
        val uiState = WeatherHomeUiState(
            savedCities = listOf(city),
            selectedCityId = city.id,
            reportsByCityId = mapOf(city.id to CityWeatherLoadState.Success(sampleReport(city))),
            isLocating = true
        )

        var gpsClicks = 0
        composeRule.setContent {
            NeboWeatherTheme {
                WeatherMapScreen(
                    uiState = uiState,
                    onOpenCities = {},
                    onNavigateBack = {},
                    onDetectLocation = { gpsClicks++ }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("map_gps_button").assertIsDisplayed()
        composeRule.onNodeWithTag("map_gps_button").performClick()
        composeRule.waitForIdle()
        assertEquals(0, gpsClicks)
    }

    private fun sampleReport(city: SavedCity): WeatherReport {
        return WeatherReport(
            city = city,
            updatedAtMillis = 1700000000000L,
            currentLocalIso = "2026-11-21T21:00",
            temperatureC = 12.0,
            apparentTemperatureC = 10.0,
            humidityPercent = 94,
            isDay = false,
            weatherCode = 61,
            weatherMoodName = "RAIN",
            conditionTitle = "Дождь",
            adviceTip = "Возьмите зонт",
            cloudCoverPercent = 90,
            pressureHpa = 1008.0,
            windSpeedKmh = 7.2,
            windDirectionDeg = 315,
            uvIndexMax = 1.0,
            aqiValue = 20,
            pm25 = 5.0,
            pm10 = 10.0,
            sunriseIso = "2026-11-21T07:10",
            sunsetIso = "2026-11-21T16:25",
            hourlyPoints = listOf(
                HourlyForecastPoint("2026-11-21T21:00", "21:00", 12.0, 80, 61, false),
                HourlyForecastPoint("2026-11-21T22:00", "22:00", 11.0, 75, 61, false)
            ),
            dailyForecasts = listOf(
                DailyForecastItem("2026-11-21", "Сегодня", "21 ноя", 61, 8.0, 13.0, 85, 1.0, 12.0)
            ),
            weekMinTempC = 8.0,
            weekMaxTempC = 13.0
        )
    }
}
