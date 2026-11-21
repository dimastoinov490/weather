package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// --- Open-Meteo Forecast API DTOs ---

@JsonClass(generateAdapter = true)
data class OpenMeteoForecastResponse(
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "utc_offset_seconds") val utcOffsetSeconds: Int = 0,
    @Json(name = "timezone") val timezone: String = "",
    @Json(name = "current") val current: CurrentWeatherDto,
    @Json(name = "hourly") val hourly: HourlyWeatherDto,
    @Json(name = "daily") val daily: DailyWeatherDto
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
    @Json(name = "time") val time: String,
    @Json(name = "temperature_2m") val temperature2m: Double,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int,
    @Json(name = "apparent_temperature") val apparentTemperature: Double,
    @Json(name = "is_day") val isDay: Int,
    @Json(name = "precipitation") val precipitation: Double = 0.0,
    @Json(name = "weather_code") val weatherCode: Int,
    @Json(name = "cloud_cover") val cloudCover: Int = 0,
    @Json(name = "pressure_msl") val pressureMsl: Double = 1013.0,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double = 0.0,
    @Json(name = "wind_direction_10m") val windDirection10m: Int = 0,
    @Json(name = "visibility") val visibility: Double? = null
)

@JsonClass(generateAdapter = true)
data class HourlyWeatherDto(
    @Json(name = "time") val time: List<String>,
    @Json(name = "temperature_2m") val temperature2m: List<Double>,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int?> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int>,
    @Json(name = "is_day") val isDay: List<Int> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DailyWeatherDto(
    @Json(name = "time") val time: List<String>,
    @Json(name = "weather_code") val weatherCode: List<Int>,
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double>,
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double>,
    @Json(name = "sunrise") val sunrise: List<String> = emptyList(),
    @Json(name = "sunset") val sunset: List<String> = emptyList(),
    @Json(name = "uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int?> = emptyList(),
    @Json(name = "wind_speed_10m_max") val windSpeed10mMax: List<Double?> = emptyList()
)

// --- Open-Meteo Air Quality API DTOs ---

@JsonClass(generateAdapter = true)
data class AirQualityResponse(
    @Json(name = "current") val current: CurrentAirQualityDto? = null
)

@JsonClass(generateAdapter = true)
data class CurrentAirQualityDto(
    @Json(name = "european_aqi") val europeanAqi: Int? = null,
    @Json(name = "pm10") val pm10: Double? = null,
    @Json(name = "pm2_5") val pm25: Double? = null
)

// --- Open-Meteo Geocoding API DTOs ---

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    @Json(name = "results") val results: List<GeocodingResultDto>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingResultDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "country") val country: String? = null,
    @Json(name = "admin1") val admin1: String? = null,
    @Json(name = "country_code") val countryCode: String? = null
)

@JsonClass(generateAdapter = true)
data class NominatimReverseResponse(
    @Json(name = "name") val name: String? = null,
    @Json(name = "address") val address: NominatimAddressDto? = null
)

@JsonClass(generateAdapter = true)
data class NominatimAddressDto(
    @Json(name = "city") val city: String? = null,
    @Json(name = "town") val town: String? = null,
    @Json(name = "village") val village: String? = null,
    @Json(name = "hamlet") val hamlet: String? = null,
    @Json(name = "municipality") val municipality: String? = null,
    @Json(name = "county") val county: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "region") val region: String? = null,
    @Json(name = "country") val country: String? = null
)

// --- RainViewer Live Radar DTOs ---

@JsonClass(generateAdapter = true)
data class RainViewerMapsResponse(
    @Json(name = "host") val host: String = "https://tilecache.rainviewer.com",
    @Json(name = "radar") val radar: RainViewerRadarDto? = null
)

@JsonClass(generateAdapter = true)
data class RainViewerRadarDto(
    @Json(name = "past") val past: List<RainViewerFrameDto> = emptyList(),
    @Json(name = "nowcast") val nowcast: List<RainViewerFrameDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RainViewerFrameDto(
    @Json(name = "time") val time: Long,
    @Json(name = "path") val path: String
)

// --- Domain & Persistence Models ---

@JsonClass(generateAdapter = true)
data class SavedCity(
    val id: String,
    val name: String,
    val region: String = "",
    val country: String = "",
    val latitude: Double,
    val longitude: Double,
    val isCurrentLocation: Boolean = false
)

enum class WeatherMood {
    CLEAR_DAY,
    CLEAR_NIGHT,
    PARTLY_CLOUDY_DAY,
    PARTLY_CLOUDY_NIGHT,
    OVERCAST,
    FOG,
    RAIN,
    SNOW,
    THUNDERSTORM
}

enum class TemperatureUnit(val symbol: String) {
    CELSIUS("°C"),
    FAHRENHEIT("°F")
}

enum class WindSpeedUnit(val label: String) {
    MS("м/с"),
    KMH("км/ч")
}

enum class PressureUnit(val label: String) {
    MMHG("мм рт. ст."),
    HPA("гПа")
}

data class WeatherSettings(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val windSpeedUnit: WindSpeedUnit = WindSpeedUnit.MS,
    val pressureUnit: PressureUnit = PressureUnit.MMHG
)

@JsonClass(generateAdapter = true)
data class HourlyForecastPoint(
    val timeIso: String,
    val hourLabel: String,
    val tempC: Double,
    val precipitationProb: Int,
    val weatherCode: Int,
    val isDay: Boolean
)

@JsonClass(generateAdapter = true)
data class DailyForecastItem(
    val dateIso: String,
    val dayName: String,
    val dateLabel: String,
    val weatherCode: Int,
    val tempMinC: Double,
    val tempMaxC: Double,
    val precipitationProbMax: Int,
    val uvIndexMax: Double,
    val windSpeedMaxKmh: Double
)

@JsonClass(generateAdapter = true)
data class WeatherReport(
    val city: SavedCity,
    val updatedAtMillis: Long,
    val currentLocalIso: String,
    val temperatureC: Double,
    val apparentTemperatureC: Double,
    val humidityPercent: Int,
    val isDay: Boolean,
    val weatherCode: Int,
    val weatherMoodName: String,
    val conditionTitle: String,
    val alertTitle: String = "Без осадков",
    val alertSubtitle: String = "В ближайшие 24 часа",
    val adviceTip: String,
    val cloudCoverPercent: Int,
    val pressureHpa: Double,
    val windSpeedKmh: Double,
    val windDirectionDeg: Int,
    val visibilityKm: Double = 10.0,
    val uvIndexMax: Double,
    val aqiValue: Int?,
    val pm25: Double?,
    val pm10: Double?,
    val sunriseIso: String,
    val sunsetIso: String,
    val hourlyPoints: List<HourlyForecastPoint>,
    val dailyForecasts: List<DailyForecastItem>,
    val weekMinTempC: Double,
    val weekMaxTempC: Double
) {
    val mood: WeatherMood
        get() = runCatching { WeatherMood.valueOf(weatherMoodName) }
            .getOrDefault(if (isDay) WeatherMood.CLEAR_DAY else WeatherMood.CLEAR_NIGHT)
}

// --- Mapping & Formatting Helpers ---

object WeatherMapper {

    private val russianLocale = Locale.forLanguageTag("ru-RU")

    fun resolveMood(weatherCode: Int, isDay: Boolean): WeatherMood {
        return when (weatherCode) {
            0, 1 -> if (isDay) WeatherMood.CLEAR_DAY else WeatherMood.CLEAR_NIGHT
            2 -> if (isDay) WeatherMood.PARTLY_CLOUDY_DAY else WeatherMood.PARTLY_CLOUDY_NIGHT
            3 -> WeatherMood.OVERCAST
            45, 48 -> WeatherMood.FOG
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherMood.RAIN
            71, 73, 75, 77, 85, 86 -> WeatherMood.SNOW
            95, 96, 99 -> WeatherMood.THUNDERSTORM
            else -> if (isDay) WeatherMood.PARTLY_CLOUDY_DAY else WeatherMood.PARTLY_CLOUDY_NIGHT
        }
    }

    fun resolveConditionTitle(weatherCode: Int, isDay: Boolean): String {
        return when (weatherCode) {
            0 -> if (isDay) "Ясно" else "Ясная ночь"
            1 -> if (isDay) "Преимущественно ясно" else "Малооблачно"
            2 -> "Переменная облачность"
            3 -> "Пасмурно"
            45 -> "Туман"
            48 -> "Изморозь и туман"
            51 -> "Лёгкая морось"
            53 -> "Умеренная морось"
            55 -> "Плотная морось"
            56, 57 -> "Ледяная морось"
            61 -> "Небольшой дождь"
            63 -> "Умеренный дождь"
            65 -> "Сильный дождь"
            66, 67 -> "Ледяной дождь"
            71 -> "Небольшой снег"
            73 -> "Умеренный снег"
            75 -> "Сильный снегопад"
            77 -> "Снежная крупа"
            80 -> "Кратковременный дождь"
            81 -> "Ливневый дождь"
            82 -> "Сильный ливень"
            85 -> "Небольшой снегопад"
            86 -> "Снежный шквал"
            95 -> "Гроза"
            96, 99 -> "Гроза с градом"
            else -> "Переменная облачность"
        }
    }

    fun resolvePrecipitationSummary(
        hourlyPoints: List<HourlyForecastPoint>,
        currentCode: Int
    ): Pair<String, String> {
        val maxProb = hourlyPoints.maxOfOrNull { it.precipitationProb } ?: 0
        val firstWet = hourlyPoints.firstOrNull {
            it.precipitationProb >= 40 || it.weatherCode in setOf(51, 53, 55, 61, 63, 65, 71, 73, 75, 80, 81, 82, 85, 86, 95, 96, 99)
        }
        return when {
            currentCode in setOf(95, 96, 99) ->
                "Гроза" to "Порывистый ветер и осадки сейчас"
            currentCode in setOf(71, 73, 75, 77, 85, 86) ->
                "Снегопад" to "Ожидается снег в ближайшие часы"
            currentCode in setOf(51, 53, 55, 61, 63, 65, 80, 81, 82) ->
                "Идёт дождь" to "Захватите зонт при выходе"
            firstWet != null && maxProb >= 40 ->
                "Ожидаются осадки" to "Около ${firstWet.hourLabel} (вероятность до $maxProb%)"
            else ->
                "Без осадков" to "В ближайшие 24 часа"
        }
    }

    fun resolveAdviceTip(weatherCode: Int, tempC: Double, windKmh: Double, uvMax: Double, isDay: Boolean): String {
        return when {
            weatherCode in setOf(95, 96, 99) ->
                "Ожидается гроза — лучше остаться в помещении"
            weatherCode in setOf(61, 63, 65, 66, 67, 80, 81, 82) ->
                "Захватите зонт и непромокаемую обувь"
            weatherCode in setOf(71, 73, 75, 77, 85, 86) ->
                "На улицах возможен снег — одевайтесь теплее"
            weatherCode in setOf(45, 48) ->
                "Снижена видимость из-за тумана"
            tempC <= -10.0 ->
                "Морозная погода — не забудьте шарф и перчатки"
            tempC <= 5.0 ->
                "Прохладно, пригодится тёплая куртка"
            windKmh >= 32.0 ->
                "Порывистый ветер — выбирайте ветрозащитную одежду"
            isDay && uvMax >= 6.0 ->
                "Высокий УФ-индекс — используйте солнцезащиту"
            weatherCode in setOf(0, 1) && isDay && tempC >= 18.0 ->
                "Прекрасная погода для прогулки"
            !isDay && weatherCode in setOf(0, 1) ->
                "Спокойный звёздный вечер и чистое небо"
            else ->
                "Комфортные условия для прогулок и дел"
        }
    }

    fun mapToReport(
        city: SavedCity,
        forecast: OpenMeteoForecastResponse,
        airQuality: AirQualityResponse?
    ): WeatherReport {
        val current = forecast.current
        val isDay = current.isDay == 1
        val mood = resolveMood(current.weatherCode, isDay)
        val uvMaxToday = forecast.daily.uvIndexMax.firstOrNull() ?: 0.0

        val currentHourPrefix = current.time.take(13)
        val startIndex = forecast.hourly.time.indexOfFirst { it.startsWith(currentHourPrefix) }
            .takeIf { it >= 0 } ?: 0

        val hourlyList = mutableListOf<HourlyForecastPoint>()
        val endIndex = minOf(startIndex + 24, forecast.hourly.time.size)
        for (i in startIndex until endIndex) {
            val iso = forecast.hourly.time[i]
            val hourStr = iso.substringAfter("T").take(5)
            hourlyList.add(
                HourlyForecastPoint(
                    timeIso = iso,
                    hourLabel = hourStr,
                    tempC = forecast.hourly.temperature2m.getOrElse(i) { current.temperature2m },
                    precipitationProb = forecast.hourly.precipitationProbability.getOrNull(i) ?: 0,
                    weatherCode = forecast.hourly.weatherCode.getOrElse(i) { current.weatherCode },
                    isDay = forecast.hourly.isDay.getOrElse(i) { current.isDay } == 1
                )
            )
        }

        val dailyList = mutableListOf<DailyForecastItem>()
        val dailyCount = minOf(7, forecast.daily.time.size)
        for (i in 0 until dailyCount) {
            val dateIso = forecast.daily.time[i]
            val (dayName, dateLabel) = formatDailyLabels(dateIso, index = i)
            dailyList.add(
                DailyForecastItem(
                    dateIso = dateIso,
                    dayName = dayName,
                    dateLabel = dateLabel,
                    weatherCode = forecast.daily.weatherCode.getOrElse(i) { current.weatherCode },
                    tempMinC = forecast.daily.temperature2mMin.getOrElse(i) { current.temperature2m },
                    tempMaxC = forecast.daily.temperature2mMax.getOrElse(i) { current.temperature2m },
                    precipitationProbMax = forecast.daily.precipitationProbabilityMax.getOrNull(i) ?: 0,
                    uvIndexMax = forecast.daily.uvIndexMax.getOrNull(i) ?: 0.0,
                    windSpeedMaxKmh = forecast.daily.windSpeed10mMax.getOrNull(i) ?: current.windSpeed10m
                )
            )
        }

        val weekMin = dailyList.minOfOrNull { it.tempMinC } ?: current.temperature2m
        val weekMax = dailyList.maxOfOrNull { it.tempMaxC } ?: current.temperature2m
        val (alertTitle, alertSubtitle) = resolvePrecipitationSummary(hourlyList, current.weatherCode)
        val visibilityKm = ((current.visibility ?: 10000.0) / 1000.0).coerceIn(0.1, 50.0)

        return WeatherReport(
            city = city,
            updatedAtMillis = System.currentTimeMillis(),
            currentLocalIso = current.time,
            temperatureC = current.temperature2m,
            apparentTemperatureC = current.apparentTemperature,
            humidityPercent = current.relativeHumidity2m,
            isDay = isDay,
            weatherCode = current.weatherCode,
            weatherMoodName = mood.name,
            conditionTitle = resolveConditionTitle(current.weatherCode, isDay),
            alertTitle = alertTitle,
            alertSubtitle = alertSubtitle,
            adviceTip = resolveAdviceTip(
                weatherCode = current.weatherCode,
                tempC = current.temperature2m,
                windKmh = current.windSpeed10m,
                uvMax = uvMaxToday,
                isDay = isDay
            ),
            cloudCoverPercent = current.cloudCover,
            pressureHpa = current.pressureMsl,
            windSpeedKmh = current.windSpeed10m,
            windDirectionDeg = current.windDirection10m,
            visibilityKm = visibilityKm,
            uvIndexMax = uvMaxToday,
            aqiValue = airQuality?.current?.europeanAqi,
            pm25 = airQuality?.current?.pm25,
            pm10 = airQuality?.current?.pm10,
            sunriseIso = forecast.daily.sunrise.firstOrNull() ?: "",
            sunsetIso = forecast.daily.sunset.firstOrNull() ?: "",
            hourlyPoints = hourlyList,
            dailyForecasts = dailyList,
            weekMinTempC = weekMin,
            weekMaxTempC = weekMax
        )
    }

    private fun formatDailyLabels(dateIso: String, index: Int): Pair<String, String> {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date: Date = parser.parse(dateIso) ?: return Pair(dateIso, "")
            val cal = Calendar.getInstance().apply { time = date }
            val dateFormatted = SimpleDateFormat("d MMM", russianLocale).format(date)
            val dayName = when (index) {
                0 -> "Сегодня"
                else -> {
                    when (cal.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.MONDAY -> "Пн"
                        Calendar.TUESDAY -> "Вт"
                        Calendar.WEDNESDAY -> "Ср"
                        Calendar.THURSDAY -> "Чт"
                        Calendar.FRIDAY -> "Пт"
                        Calendar.SATURDAY -> "Сб"
                        Calendar.SUNDAY -> "Вс"
                        else -> ""
                    }
                }
            }
            Pair(dayName, dateFormatted)
        } catch (_: Exception) {
            Pair(dateIso, "")
        }
    }
}

object WeatherFormatters {

    fun convertTemp(tempC: Double, unit: TemperatureUnit): Int {
        return when (unit) {
            TemperatureUnit.CELSIUS -> tempC.roundToInt()
            TemperatureUnit.FAHRENHEIT -> (tempC * 9.0 / 5.0 + 32.0).roundToInt()
        }
    }

    fun formatTemp(tempC: Double, unit: TemperatureUnit): String {
        val value = convertTemp(tempC, unit)
        return if (value > 0) "+$value°" else "$value°"
    }

    fun formatWind(windKmh: Double, unit: WindSpeedUnit): String {
        return when (unit) {
            WindSpeedUnit.MS -> {
                val ms = (windKmh / 3.6).roundToInt()
                "$ms ${unit.label}"
            }
            WindSpeedUnit.KMH -> "${windKmh.roundToInt()} ${unit.label}"
        }
    }

    fun formatPressureValue(pressureHpa: Double, unit: PressureUnit): String {
        return when (unit) {
            PressureUnit.MMHG -> "${(pressureHpa * 0.750062).roundToInt()}"
            PressureUnit.HPA -> "${pressureHpa.roundToInt()}"
        }
    }

    fun formatPressure(pressureHpa: Double, unit: PressureUnit): String {
        return "${formatPressureValue(pressureHpa, unit)} ${unit.label}"
    }

    fun formatVisibility(visibilityKm: Double): String {
        return "${visibilityKm.roundToInt()} км"
    }

    fun windDirectionCompass(deg: Int): String {
        val dirs = listOf("С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ")
        val idx = (((deg % 360) + 360) % 360 / 45.0).roundToInt() % 8
        return dirs[idx]
    }

    fun windDirectionFull(deg: Int): String {
        val dirs = listOf(
            "Северный", "Северо-восточный", "Восточный", "Юго-восточный",
            "Южный", "Юго-западный", "Западный", "Северо-западный"
        )
        val idx = (((deg % 360) + 360) % 360 / 45.0).roundToInt() % 8
        return dirs[idx]
    }

    fun uvIndexDescription(uv: Double): String {
        return when {
            uv < 3.0 -> "Низкий"
            uv < 6.0 -> "Умеренный"
            uv < 8.0 -> "Высокий"
            uv < 11.0 -> "Очень высокий"
            else -> "Экстремальный"
        }
    }

    fun aqiDescription(aqi: Int?): String {
        return when {
            aqi == null -> "Нет данных"
            aqi <= 20 -> "Отличное"
            aqi <= 40 -> "Хорошее"
            aqi <= 60 -> "Умеренное"
            aqi <= 80 -> "Пониженное"
            aqi <= 100 -> "Плохое"
            else -> "Опасное"
        }
    }

    fun extractTimeHm(iso: String): String {
        return if (iso.contains("T")) iso.substringAfter("T").take(5) else "--:--"
    }

    fun computeDayProgress(currentIso: String, sunriseIso: String, sunsetIso: String): Float {
        fun toMinutes(iso: String): Int? {
            val hm = iso.substringAfter("T", "").take(5)
            val parts = hm.split(":")
            if (parts.size != 2) return null
            val h = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            return h * 60 + m
        }
        val now = toMinutes(currentIso) ?: return 0.5f
        val rise = toMinutes(sunriseIso) ?: return 0.5f
        val set = toMinutes(sunsetIso) ?: return 0.5f
        if (set <= rise) return 0.5f
        return ((now - rise).toFloat() / (set - rise).toFloat()).coerceIn(0f, 1f)
    }

    fun humidityComfortLabel(humidity: Int): String {
        return when {
            humidity < 30 -> "Сухой воздух"
            humidity <= 60 -> "Комфортная"
            humidity <= 80 -> "Повышенная"
            else -> "Высокая влажность"
        }
    }
}
