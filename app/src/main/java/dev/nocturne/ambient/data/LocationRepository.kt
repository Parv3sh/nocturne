package dev.nocturne.ambient.data

import dev.nocturne.ambient.model.City
import org.json.JSONObject

class LocationRepository {
    suspend fun autoLocate(): City? {
        ipapi()?.let { return it }
        return ipinfo()
    }

    private suspend fun ipapi(): City? {
        return try {
            val root = JSONObject(Http.getText("https://ipapi.co/json/", timeoutMs = 9_000))
            if (root.optBoolean("error", false)) return null
            val lat = root.optDouble("latitude", Double.NaN)
            val lon = root.optDouble("longitude", Double.NaN)
            if (lat.isNaN() || lon.isNaN()) return null
            val city = root.optString("city").takeIf { it.isNotBlank() } ?: "Local weather"
            val region = root.optString("region_code").takeIf { it.isNotBlank() }
                ?: root.optString("country_code")
            City(city, region, lat, lon)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun ipinfo(): City? {
        return try {
            val root = JSONObject(Http.getText("https://ipinfo.io/json", timeoutMs = 9_000))
            val loc = root.optString("loc")
            val parts = loc.split(',')
            if (parts.size != 2) return null
            val lat = parts[0].toDoubleOrNull() ?: return null
            val lon = parts[1].toDoubleOrNull() ?: return null
            val city = root.optString("city").takeIf { it.isNotBlank() } ?: "Local weather"
            City(city, root.optString("country"), lat, lon)
        } catch (_: Exception) {
            null
        }
    }
}
