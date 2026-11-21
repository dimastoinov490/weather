package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.CurrentWeatherDto
import com.example.data.model.DailyForecastItem
import com.example.data.model.DailyWeatherDto
import com.example.data.model.HourlyForecastPoint
import com.example.data.model.HourlyWeatherDto
import com.example.data.model.OpenMeteoForecastResponse
import com.example.data.model.SavedCity
import com.example.data.model.WeatherMapper
import com.example.data.model.WeatherReport
import com.example.ui.screens.ForecastDetailScreen
import com.example.ui.state.CityWeatherLoadState
import com.example.ui.state.WeatherHomeUiState
import com.example.ui.theme.NeboWeatherTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class ForecastDetailRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mapperPreservesAllDaysHourlyPointsAndPerDayMetrics() {
        val city = SavedCity(
            id = "tiraspol",
            name = "Тирасполь",
            country = "Молдова",
            latitude = 46.84,
            longitude = 29.64
        )
        val hours = mutableListOf<String>()
        val temps = mutableListOf<Double>()
        val apparent = mutableListOf<Double>()
        val humidity = mutableListOf<Int>()
        val rainProb = mutableListOf<Int>()
        val rainMm = mutableListOf<Double>()
        val codes = mutableListOf<Int>()
        val wind = mutableListOf<Double>()
        val windDir = mutableListOf<Int>()
        val pressure = mutableListOf<Double>()
        val uv = mutableListOf<Double>()
        val isDay = mutableListOf<Int>()

        val dates = listOf("2026-11-21", "2026-11-22", "2026-11-23")
        dates.forEachIndexed { dIdx, d ->
            for (h in 0 until 24) {
                hours.add("${d}T${h.toString().padStart(2, '0')}:00")
                temps.add(10.0 + dIdx * 3.0 + (if (h in 11..16) 4.0 else 0.0))
                apparent.add(8.0 + dIdx * 3.0)
                humidity.add(70 + dIdx * 5)
                rainProb.add(if (dIdx == 1) 80 else 15)
                rainMm.add(if (dIdx == 1) 1.4 else 0.0)
                codes.add(if (dIdx == 1) 61 else 2)
                wind.add(10.0 + dIdx * 6.0)
                windDir.add(90 + dIdx * 90)
                pressure.add(1012.0 - dIdx * 4.0)
                uv.add(if (h in 11..15) 3.0 + dIdx else 0.0)
                isDay.add(if (h in 7..16) 1 else 0)
            }
        }

        val response = OpenMeteoForecastResponse(
            latitude = city.latitude,
            longitude = city.longitude,
            utcOffsetSeconds = 7200,
            timezone = "Europe/Chisinau",
            current = CurrentWeatherDto(
                time = "2026-11-21T14:00",
                temperature2m = 14.0,
                relativeHumidity2m = 70,
                apparentTemperature = 12.0,
                isDay = 1,
                weatherCode = 2,
                pressureMsl = 1012.0,
                windSpeed10m = 10.0,
                windDirection10m = 90
            ),
            hourly = HourlyWeatherDto(
                time = hours,
                temperature2m = temps,
                apparentTemperature = apparent,
                relativeHumidity2m = humidity,
                precipitationProbability = rainProb,
                precipitation = rainMm,
                weatherCode = codes,
                pressureMsl = pressure,
                windSpeed10m = wind,
                windDirection10m = windDir,
                uvIndex = uv,
                isDay = isDay
            ),
            daily = DailyWeatherDto(
                time = dates,
                weatherCode = listOf(2, 61, 0),
                temperature2mMax = listOf(14.0, 17.0, 20.0),
                temperature2mMin = listOf(6.0, 9.0, 11.0),
                apparentTemperatureMax = listOf(12.0, 15.0, 19.0),
                apparentTemperatureMin = listOf(4.0, 7.0, 9.0),
                sunrise = listOf("2026-11-21T07:10", "2026-11-22T07:11", "2026-11-23T07:13"),
                sunset = listOf("2026-11-21T16:25", "2026-11-22T16:24", "2026-11-23T16:23"),
                uvIndexMax = listOf(3.0, 4.0, 5.0),
                precipitationSum = listOf(0.0, 12.5, 0.2),
                precipitationProbabilityMax = listOf(15, 80, 10),
                windSpeed10mMax = listOf(14.0, 22.0, 18.0),
                windDirection10mDominant = listOf(90, 180, 270)
            )
        )

        val report = WeatherMapper.mapToReport(city, response, null)
        assertEquals(72, report.allHourlyPoints.size)
        assertEquals(3, report.dailyForecasts.size)
        assertEquals("Сегодня", report.dailyForecasts[0].fullDayName)
        assertEquals("Завтра", report.dailyForecasts[1].fullDayName)
        assertEquals("Понедельник", report.dailyForecasts[2].fullDayName)

        val day2Hours = report.allHourlyPoints.filter { it.timeIso.startsWith("2026-11-22") }
        assertEquals(24, day2Hours.size)
        assertEquals("00:00", day2Hours.first().hourLabel)
        assertEquals("23:00", day2Hours.last().hourLabel)
        assertEquals(1.4, day2Hours.first().precipitationMm ?: -1.0, 0.01)
        assertNotNull(day2Hours.first().windSpeedKmh)
        assertEquals(12.5, report.dailyForecasts[1].precipitationSumMm ?: -1.0, 0.01)
    }

    @Test
    fun forecastDetailScreenSwitchesDaysShowsDaySpecificHoursAndResetsOnCityChange() {
        val city1 = SavedCity(id = "city_1", name = "Тирасполь", latitude = 46.84, longitude = 29.64)
        val city2 = SavedCity(id = "city_2", name = "Сочи", latitude = 43.58, longitude = 39.72)

        val report1 = buildReport(city1, todayTemp = 11.0, tomorrowTemp = 19.0)
        val report2 = buildReport(city2, todayTemp = 22.0, tomorrowTemp = 25.0)

        var currentState by mutableStateOf(
            WeatherHomeUiState(
                savedCities = listOf(city1, city2),
                selectedCityId = city1.id,
                reportsByCityId = mapOf(
                    city1.id to CityWeatherLoadState.Success(report1),
                    city2.id to CityWeatherLoadState.Success(report2)
                )
            )
        )

        composeRule.setContent {
            NeboWeatherTheme {
                ForecastDetailScreen(
                    uiState = currentState,
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Default is Today with original HourlyForecastCard, DailyForecastCard, and WeatherBentoGrid
        composeRule.onNodeWithTag("hourly_forecast_card").assertIsDisplayed()
        composeRule.onNodeWithText("Почасовой график • Сегодня").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_forecast_card").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_forecast_day_0").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_forecast_day_1").assertIsDisplayed()
        composeRule.onNodeWithText("AQI 25").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("68%").performScrollTo().assertIsDisplayed()

        // Select Tomorrow (day 1) inside DailyForecastCard
        composeRule.onNodeWithTag("daily_forecast_day_1").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hourly_forecast_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Почасовой график • Завтра").assertIsDisplayed()
        composeRule.onNodeWithText("00:00").assertIsDisplayed()
        composeRule.onAllNodesWithText("+19°").onFirst().assertIsDisplayed()

        // WeatherBentoGrid updates to Tomorrow's metrics (88% humidity, +17° feels-like max) and hides Today-only AQI
        composeRule.onNodeWithTag("bento_humidity").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("88%").assertIsDisplayed()
        composeRule.onNodeWithTag("bento_feels_like").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("+17°").onFirst().assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("AQI 25").fetchSemanticsNodes().size)

        // Re-select Today
        composeRule.onNodeWithTag("daily_forecast_day_0").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hourly_forecast_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Почасовой график • Сегодня").assertIsDisplayed()
        composeRule.onNodeWithText("68%").performScrollTo().assertIsDisplayed()

        // Select day 2 with missing hourly points -> clean empty message without showing today's hours
        composeRule.onNodeWithTag("daily_forecast_day_2").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("forecast_hourly_empty").performScrollTo().assertIsDisplayed()

        // Switch city -> selection automatically resets to Today in new city
        currentState = currentState.copy(selectedCityId = city2.id)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Сочи • Переменная облачность").assertIsDisplayed()
        composeRule.onNodeWithTag("hourly_forecast_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Почасовой график • Сегодня").performScrollTo().assertIsDisplayed()
    }

    private fun buildReport(city: SavedCity, todayTemp: Double, tomorrowTemp: Double): WeatherReport {
        val todayHours = (0 until 24).map { h ->
            HourlyForecastPoint(
                timeIso = "2026-11-21T${h.toString().padStart(2, '0')}:00",
                hourLabel = "${h.toString().padStart(2, '0')}:00",
                tempC = todayTemp,
                precipitationProb = 15,
                weatherCode = 2,
                isDay = h in 7..16,
                precipitationMm = 0.0,
                windSpeedKmh = 10.8,
                windDirectionDeg = 90,
                apparentTempC = todayTemp - 1.0,
                humidityPercent = 68,
                pressureHpa = 1015.0,
                uvIndex = 2.0,
                cloudCoverPercent = 40
            )
        }
        val tomorrowHours = (0 until 24).map { h ->
            HourlyForecastPoint(
                timeIso = "2026-11-22T${h.toString().padStart(2, '0')}:00",
                hourLabel = "${h.toString().padStart(2, '0')}:00",
                tempC = tomorrowTemp,
                precipitationProb = 80,
                weatherCode = 61,
                isDay = h in 7..16,
                precipitationMm = 1.6,
                windSpeedKmh = 21.6,
                windDirectionDeg = 225,
                apparentTempC = tomorrowTemp - 2.0,
                humidityPercent = 88,
                pressureHpa = 1004.0,
                uvIndex = 1.0,
                cloudCoverPercent = 95
            )
        }
        return WeatherReport(
            city = city,
            updatedAtMillis = 1700000000000L,
            currentLocalIso = "2026-11-21T14:00",
            temperatureC = todayTemp,
            apparentTemperatureC = todayTemp - 1.0,
            humidityPercent = 68,
            isDay = true,
            weatherCode = 2,
            weatherMoodName = "PARTLY_CLOUDY_DAY",
            conditionTitle = "Переменная облачность",
            adviceTip = "Хорошая погода",
            cloudCoverPercent = 40,
            pressureHpa = 1015.0,
            windSpeedKmh = 10.8,
            windDirectionDeg = 90,
            uvIndexMax = 2.0,
            aqiValue = 25,
            pm25 = 6.0,
            pm10 = 12.0,
            sunriseIso = "2026-11-21T07:10",
            sunsetIso = "2026-11-21T16:25",
            hourlyPoints = todayHours.drop(14),
            allHourlyPoints = todayHours + tomorrowHours,
            dailyForecasts = listOf(
                DailyForecastItem(
                    dateIso = "2026-11-21",
                    dayName = "Сегодня",
                    dateLabel = "21 ноя",
                    weatherCode = 2,
                    tempMinC = todayTemp - 3.0,
                    tempMaxC = todayTemp + 2.0,
                    precipitationProbMax = 15,
                    uvIndexMax = 2.0,
                    windSpeedMaxKmh = 14.0,
                    fullDayName = "Сегодня",
                    sunriseIso = "2026-11-21T07:10",
                    sunsetIso = "2026-11-21T16:25",
                    precipitationSumMm = 0.0,
                    windDirectionDeg = 90,
                    apparentTempMinC = todayTemp - 4.0,
                    apparentTempMaxC = todayTemp + 1.0,
                    uvIndexMaxAvailable = 2.0
                ),
                DailyForecastItem(
                    dateIso = "2026-11-22",
                    dayName = "Пн",
                    dateLabel = "22 ноя",
                    weatherCode = 61,
                    tempMinC = tomorrowTemp - 4.0,
                    tempMaxC = tomorrowTemp,
                    precipitationProbMax = 80,
                    uvIndexMax = 1.0,
                    windSpeedMaxKmh = 21.6,
                    fullDayName = "Завтра",
                    sunriseIso = "2026-11-22T07:11",
                    sunsetIso = "2026-11-22T16:24",
                    precipitationSumMm = 8.4,
                    windDirectionDeg = 225,
                    apparentTempMinC = tomorrowTemp - 6.0,
                    apparentTempMaxC = tomorrowTemp - 2.0,
                    uvIndexMaxAvailable = 1.0
                ),
                DailyForecastItem(
                    dateIso = "2026-11-23",
                    dayName = "Вт",
                    dateLabel = "23 ноя",
                    weatherCode = 0,
                    tempMinC = 7.0,
                    tempMaxC = 16.0,
                    precipitationProbMax = 5,
                    uvIndexMax = 0.0,
                    windSpeedMaxKmh = 11.0,
                    fullDayName = "Понедельник",
                    sunriseIso = "2026-11-23T07:13",
                    sunsetIso = "2026-11-23T16:23",
                    precipitationSumMm = null,
                    windDirectionDeg = null,
                    apparentTempMinC = null,
                    apparentTempMaxC = null,
                    uvIndexMaxAvailable = null
                )
            ),
            weekMinTempC = 7.0,
            weekMaxTempC = tomorrowTemp
        )
    }
}
