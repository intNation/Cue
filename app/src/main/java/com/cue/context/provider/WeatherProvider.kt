package com.cue.context.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/**
 * Provider for collecting weather signals using Open-Meteo API.
 */
class WeatherProvider {

    /**
     * Fetches current weather for a given location.
     * @return "Sunny", "Rainy", "Cloudy", or "Unknown"
     */
    suspend fun getWeatherData(lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
            val response = URL(url).readText()
            val json = JSONObject(response)
            val currentWeather = json.getJSONObject("current_weather")
            val weatherCode = currentWeather.getInt("weathercode")

            // WMO Weather interpretation codes
            // https://open-meteo.com/en/docs
            when (weatherCode) {
                0 -> "Sunny" // Clear sky
                1, 2, 3 -> "Cloudy" // Mainly clear, partly cloudy, and overcast
                45, 48 -> "Cloudy" // Fog
                51, 53, 55, 61, 63, 65, 80, 81, 82 -> "Rainy" // Rain & Drizzle
                71, 73, 75, 77, 85, 86 -> "Rainy" // Snow
                95, 96, 99 -> "Rainy" // Thunderstorm
                else -> "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
