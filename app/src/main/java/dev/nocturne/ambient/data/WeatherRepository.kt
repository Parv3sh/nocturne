package dev.nocturne.ambient.data

import android.content.Context
import dev.nocturne.ambient.model.DayForecast
import dev.nocturne.ambient.model.HourForecast
import dev.nocturne.ambient.model.Settings
import dev.nocturne.ambient.model.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Locale

class WeatherRepository(context: Context) {
    private val cacheFile = File(context.filesDir, "weather-cache.json")
    private val mutex = Mutex()
    private var cached: WeatherData? = null
    private var cacheLoaded = false

    suspend fun get(settings: Settings, force: Boolean = false): WeatherData? = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (!cacheLoaded) {
                cached = readCache()
                cacheLoaded = true
            }
            val now = System.currentTimeMillis()
            val fresh = cached != null &&
                now - cached!!.updatedAt.toEpochMilli() < REFRESH_MS &&
                !force
            if (fresh) {
                return@withContext cached
            }
            val previousAqi = cached?.aqi
            val previousPm25 = cached?.pm25
            try {
                val weatherJson = fetchWeather(settings)
                val aqi = try {
                    fetchAqi(settings)
                } catch (_: Exception) {
                    null
                }
                val parsed = parseWeather(
                    weatherJson,
                    aqiJson = aqi,
                    fallbackAqi = previousAqi,
                    fallbackPm25 = previousPm25
                )
                cached = parsed
                writeCache(weatherJson, aqi, parsed.updatedAt)
                parsed
            } catch (error: Exception) {
                android.util.Log.w("NocturneWeather", "Weather refresh failed", error)
                cached?.copy(stale = true)
            }
        }
    }

    fun peek(): WeatherData? = cached

    private suspend fun fetchWeather(settings: Settings): JSONObject {
        val lat = String.format(Locale.US, "%.4f", settings.latitude)
        val lon = String.format(Locale.US, "%.4f", settings.longitude)
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_probability_max" +
            "&hourly=temperature_2m,precipitation_probability,weather_code" +
            "&timezone=auto&forecast_days=7&wind_speed_unit=kmh"
        return JSONObject(Http.getText(url, timeoutMs = 12_000))
    }

    private suspend fun fetchAqi(settings: Settings): JSONObject? {
        val lat = String.format(Locale.US, "%.4f", settings.latitude)
        val lon = String.format(Locale.US, "%.4f", settings.longitude)
        val url = "https://air-quality-api.open-meteo.com/v1/air-quality" +
            "?latitude=$lat&longitude=$lon&current=us_aqi,pm2_5&timezone=auto"
        return JSONObject(Http.getText(url, timeoutMs = 9_000))
    }

    private fun parseWeather(
        root: JSONObject,
        aqiJson: JSONObject?,
        fallbackAqi: Int?,
        fallbackPm25: Double?
    ): WeatherData {
        val current = root.getJSONObject("current")
        val daily = root.getJSONObject("daily")
        val hourly = root.getJSONObject("hourly")

        val currentTime = current.optString("time").let {
            runCatching { LocalDateTime.parse(it) }.getOrNull()
        }

        val dates = daily.getJSONArray("time")
        val dayCodes = daily.getJSONArray("weather_code")
        val highs = daily.getJSONArray("temperature_2m_max")
        val lows = daily.getJSONArray("temperature_2m_min")
        val sunrises = daily.optJSONArray("sunrise")
        val sunsets = daily.optJSONArray("sunset")
        val precip = daily.optJSONArray("precipitation_probability_max")

        val days = (0 until dates.length()).mapNotNull { i ->
            val date = runCatching { LocalDate.parse(dates.getString(i)) }.getOrNull() ?: return@mapNotNull null
            DayForecast(
                date = date,
                code = dayCodes.optInt(i, 0),
                high = highs.optDouble(i, 0.0),
                low = lows.optDouble(i, 0.0),
                precipitation = precip?.optInt(i, 0) ?: 0,
                sunrise = parseTime(sunrises?.optString(i)),
                sunset = parseTime(sunsets?.optString(i))
            )
        }

        val hourTimes = hourly.getJSONArray("time")
        val hourTemps = hourly.getJSONArray("temperature_2m")
        val hourPrecip = hourly.optJSONArray("precipitation_probability")
        val hourCodes = hourly.getJSONArray("weather_code")
        val nowHour = currentTime?.truncatedTo(ChronoUnit.HOURS) ?: LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
        val hours = (0 until hourTimes.length()).mapNotNull { i ->
            val dt = runCatching { LocalDateTime.parse(hourTimes.getString(i)) }.getOrNull()
                ?: return@mapNotNull null
            if (dt.isBefore(nowHour)) return@mapNotNull null
            HourForecast(
                time = dt,
                temperature = hourTemps.optDouble(i, 0.0),
                precipitation = hourPrecip?.optInt(i, 0) ?: 0,
                code = hourCodes.optInt(i, 0)
            )
        }.take(24)

        val aqiCurrent = aqiJson?.optJSONObject("current")
        val aqi = aqiCurrent?.optInt("us_aqi", -1)?.takeIf { it >= 0 } ?: fallbackAqi
        val pm25 = aqiCurrent?.optDouble("pm2_5", Double.NaN)?.takeIf { !it.isNaN() } ?: fallbackPm25

        return WeatherData(
            temperature = current.optDouble("temperature_2m", 0.0),
            apparent = current.optDouble("apparent_temperature", 0.0),
            humidity = current.optInt("relative_humidity_2m", 0),
            code = current.optInt("weather_code", 0),
            isDay = current.optInt("is_day", 1) == 1,
            windKph = current.optDouble("wind_speed_10m", 0.0),
            precipitation = current.optDouble("precipitation", 0.0),
            high = days.firstOrNull()?.high ?: current.optDouble("temperature_2m", 0.0),
            low = days.firstOrNull()?.low ?: current.optDouble("temperature_2m", 0.0),
            sunrise = days.firstOrNull()?.sunrise,
            sunset = days.firstOrNull()?.sunset,
            daily = days,
            hourly = hours,
            aqi = aqi,
            pm25 = pm25,
            updatedAt = Instant.now()
        )
    }

    private fun parseTime(value: String?): LocalTime? {
        if (value.isNullOrBlank()) return null
        val timePart = value.substringAfter('T', value)
        return runCatching { LocalTime.parse(timePart) }.getOrNull()
    }

    private fun readCache(): WeatherData? {
        return try {
            if (!cacheFile.exists()) return null
            val root = JSONObject(cacheFile.readText())
            val weather = root.optJSONObject("weather") ?: return null
            val aqi = root.optJSONObject("aqi")
            parseWeather(
                weather,
                aqi,
                fallbackAqi = null,
                fallbackPm25 = null
            ).copy(
                updatedAt = Instant.ofEpochMilli(root.optLong("savedAt", System.currentTimeMillis())),
                stale = true
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun writeCache(weather: JSONObject, aqi: JSONObject?, savedAt: Instant) {
        try {
            val root = JSONObject().apply {
                put("savedAt", savedAt.toEpochMilli())
                put("weather", weather)
                if (aqi != null) put("aqi", aqi)
            }
            val temp = File(cacheFile.parentFile, "weather-cache.tmp")
            temp.writeText(root.toString())
            if (!temp.renameTo(cacheFile)) {
                cacheFile.writeText(root.toString())
                temp.delete()
            }
        } catch (_: Exception) {
            // Cache is best-effort.
        }
    }

    companion object {
        private const val REFRESH_MS = 15 * 60 * 1000L
    }
}
