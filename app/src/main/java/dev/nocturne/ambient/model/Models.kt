package dev.nocturne.ambient.model

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

enum class Face(val title: String) {
    MINIMAL("Minimal"),
    FLIP("Flip"),
    ANALOG("Analog"),
    DASHBOARD("Dashboard"),
    AMBIENT("Ambient");

    fun next(direction: Int): Face {
        val values = entries
        return values[(ordinal + direction + values.size) % values.size]
    }

    companion object {
        fun from(value: String?): Face = entries.firstOrNull { it.name == value } ?: MINIMAL
    }
}

data class Settings(
    val face: Face = Face.MINIMAL,
    val use24Hour: Boolean = true,
    val showSeconds: Boolean = false,
    val celsius: Boolean = true,
    val brightness: Float = 1f,
    val nightDim: Boolean = true,
    val motion: Boolean = true,
    val showWeather: Boolean = true,
    val showCalendar: Boolean = true,
    val showWorldClocks: Boolean = true,
    val noteEnabled: Boolean = true,
    val locationAuto: Boolean = true,
    val locationResolved: Boolean = false,
    val locationLabel: String = "Melbourne",
    val latitude: Double = -37.8136,
    val longitude: Double = 144.9631,
    val calendarUrls: String = "",
    val launchOnBoot: Boolean = true,
    val onboardingSeen: Boolean = false,
    val worldClockCities: List<ZoneCity> = defaultWorldCities
) {
    fun toJson(): String = JSONObject().apply {
        put("v", 1)
        put("face", face.name)
        put("use24Hour", use24Hour)
        put("showSeconds", showSeconds)
        put("celsius", celsius)
        put("brightness", brightness.toDouble())
        put("nightDim", nightDim)
        put("motion", motion)
        put("showWeather", showWeather)
        put("showCalendar", showCalendar)
        put("showWorldClocks", showWorldClocks)
        put("noteEnabled", noteEnabled)
        put("locationAuto", locationAuto)
        put("locationResolved", locationResolved)
        put("locationLabel", locationLabel)
        put("latitude", latitude)
        put("longitude", longitude)
        put("calendarUrls", calendarUrls)
        put("launchOnBoot", launchOnBoot)
        put("onboardingSeen", onboardingSeen)
        put("worldClockCities", JSONArray().apply {
            worldClockCities.forEach { city ->
                put(JSONObject().apply {
                    put("label", city.label)
                    put("zone", city.zone)
                })
            }
        })
    }.toString()

    companion object {
        fun fromJson(raw: String?): Settings {
            if (raw.isNullOrBlank()) return Settings()
            return try {
                val o = JSONObject(raw)
                Settings(
                    face = Face.from(o.optString("face")),
                    use24Hour = o.optBoolean("use24Hour", true),
                    showSeconds = o.optBoolean("showSeconds", false),
                    celsius = o.optBoolean("celsius", true),
                    brightness = o.optDouble("brightness", 1.0).toFloat().coerceIn(0.2f, 1f),
                    nightDim = o.optBoolean("nightDim", true),
                    motion = o.optBoolean("motion", true),
                    showWeather = o.optBoolean("showWeather", true),
                    showCalendar = o.optBoolean("showCalendar", true),
                    showWorldClocks = o.optBoolean("showWorldClocks", true),
                    noteEnabled = o.optBoolean("noteEnabled", true),
                    locationAuto = o.optBoolean("locationAuto", true),
                    locationResolved = o.optBoolean("locationResolved", false),
                    locationLabel = o.optString("locationLabel", "Melbourne"),
                    latitude = o.optDouble("latitude", -37.8136),
                    longitude = o.optDouble("longitude", 144.9631),
                    calendarUrls = o.optString("calendarUrls", ""),
                    launchOnBoot = o.optBoolean("launchOnBoot", true),
                    onboardingSeen = o.optBoolean("onboardingSeen", false),
                    worldClockCities = o.optJSONArray("worldClockCities")?.let { arr ->
                        (0 until arr.length()).mapNotNull { k ->
                            arr.optJSONObject(k)?.let { obj ->
                                val label = obj.optString("label")
                                val zone = obj.optString("zone")
                                if (label.isNotBlank() && zone.isNotBlank()) ZoneCity(label, zone) else null
                            }
                        }
                    }.orEmpty().let { saved ->
                        (saved + defaultWorldCities).distinctBy { it.zone + it.label }.take(3)
                    }
                )
            } catch (_: Exception) {
                Settings()
            }
        }
    }
}

data class City(
    val label: String,
    val region: String,
    val latitude: Double,
    val longitude: Double
)

val cityPresets = listOf(
    City("Melbourne", "AU", -37.8136, 144.9631),
    City("Sydney", "AU", -33.8688, 151.2093),
    City("Brisbane", "AU", -27.4698, 153.0251),
    City("Perth", "AU", -31.9523, 115.8613),
    City("Adelaide", "AU", -34.9285, 138.6007),
    City("Auckland", "NZ", -36.8509, 174.7645),
    City("London", "UK", 51.5072, -0.1276),
    City("New York", "US", 40.7128, -74.0060),
    City("Los Angeles", "US", 34.0522, -118.2437),
    City("Tokyo", "JP", 35.6762, 139.6503),
    City("Singapore", "SG", 1.3521, 103.8198),
    City("Mumbai", "IN", 19.0760, 72.8777)
)

data class ZoneCity(val label: String, val zone: String)

val zonePresets = listOf(
    ZoneCity("Karachi", "Asia/Karachi"),
    ZoneCity("Dubai", "Asia/Dubai"),
    ZoneCity("Mumbai", "Asia/Kolkata"),
    ZoneCity("Singapore", "Asia/Singapore"),
    ZoneCity("Hong Kong", "Asia/Hong_Kong"),
    ZoneCity("Tokyo", "Asia/Tokyo"),
    ZoneCity("Sydney", "Australia/Sydney"),
    ZoneCity("Auckland", "Pacific/Auckland"),
    ZoneCity("Istanbul", "Europe/Istanbul"),
    ZoneCity("Moscow", "Europe/Moscow"),
    ZoneCity("London", "Europe/London"),
    ZoneCity("Paris", "Europe/Paris"),
    ZoneCity("New York", "America/New_York"),
    ZoneCity("Miami", "America/New_York"),
    ZoneCity("Chicago", "America/Chicago"),
    ZoneCity("Nashville", "America/Chicago"),
    ZoneCity("Los Angeles", "America/Los_Angeles"),
    ZoneCity("Sao Paulo", "America/Sao_Paulo")
)

val defaultWorldCities = listOf(
    ZoneCity("Karachi", "Asia/Karachi"),
    ZoneCity("Miami", "America/New_York"),
    ZoneCity("Nashville", "America/Chicago")
)

enum class WeatherKind {
    CLEAR, PARTLY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, STORM
}

data class DayForecast(
    val date: LocalDate,
    val code: Int,
    val high: Double,
    val low: Double,
    val precipitation: Int,
    val sunrise: LocalTime?,
    val sunset: LocalTime?
) {
    val kind: WeatherKind get() = weatherKind(code)
}

data class HourForecast(
    val time: LocalDateTime,
    val temperature: Double,
    val precipitation: Int,
    val code: Int
)

data class WeatherData(
    val temperature: Double,
    val apparent: Double,
    val humidity: Int,
    val code: Int,
    val isDay: Boolean,
    val windKph: Double,
    val precipitation: Double,
    val high: Double,
    val low: Double,
    val sunrise: LocalTime?,
    val sunset: LocalTime?,
    val daily: List<DayForecast>,
    val hourly: List<HourForecast>,
    val aqi: Int? = null,
    val pm25: Double? = null,
    val updatedAt: Instant = Instant.now(),
    val stale: Boolean = false
) {
    val kind: WeatherKind get() = weatherKind(code)
    val description: String get() = weatherDescription(code)
}

data class CalendarEvent(
    val title: String,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val allDay: Boolean,
    val location: String? = null,
    val calendarName: String? = null
)

fun weatherKind(code: Int): WeatherKind = when (code) {
    0, 1 -> WeatherKind.CLEAR
    2 -> WeatherKind.PARTLY
    3 -> WeatherKind.CLOUDY
    45, 48 -> WeatherKind.FOG
    51, 53, 55, 56, 57 -> WeatherKind.DRIZZLE
    61, 63, 65, 66, 67, 80, 81, 82 -> WeatherKind.RAIN
    71, 73, 75, 77, 85, 86 -> WeatherKind.SNOW
    95, 96, 99 -> WeatherKind.STORM
    else -> WeatherKind.CLOUDY
}

fun weatherDescription(code: Int): String = when (code) {
    0 -> "Clear"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45 -> "Fog"
    48 -> "Rime fog"
    51 -> "Light drizzle"
    53 -> "Drizzle"
    55 -> "Heavy drizzle"
    56, 57 -> "Freezing drizzle"
    61 -> "Light rain"
    63 -> "Rain"
    65 -> "Heavy rain"
    66, 67 -> "Freezing rain"
    71 -> "Light snow"
    73 -> "Snow"
    75 -> "Heavy snow"
    77 -> "Snow grains"
    80 -> "Light showers"
    81 -> "Showers"
    82 -> "Heavy showers"
    85 -> "Snow showers"
    86 -> "Heavy snow showers"
    95 -> "Thunderstorm"
    96, 99 -> "Storm with hail"
    else -> "Cloudy"
}

fun aqiLabel(aqi: Int?): String? = when {
    aqi == null -> null
    aqi <= 50 -> "Good"
    aqi <= 100 -> "Moderate"
    aqi <= 150 -> "Unhealthy for sensitive groups"
    aqi <= 200 -> "Unhealthy"
    aqi <= 300 -> "Very unhealthy"
    else -> "Hazardous"
}

fun aqiShortLabel(aqi: Int?): String? = when {
    aqi == null -> null
    aqi <= 50 -> "Good"
    aqi <= 100 -> "Moderate"
    aqi <= 150 -> "Sensitive"
    aqi <= 200 -> "Unhealthy"
    aqi <= 300 -> "Very unhealthy"
    else -> "Hazardous"
}
