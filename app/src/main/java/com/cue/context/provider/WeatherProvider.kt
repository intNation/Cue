package com.cue.context.provider

import com.cue.context.contracts.CoarseLocation
import com.cue.context.contracts.ProviderResult
import com.cue.context.contracts.UnavailableReason
import com.cue.context.contracts.WhetherProvider
import com.cue.context.contracts.WhetherSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Provider for collecting weather signals using Open-Meteo API.
 */
class WeatherProvider : WhetherProvider {

    /**
     * Fetches current weather for a given coarse location.
     */
    override suspend fun getWhetherSignal(
        location: CoarseLocation
    ): ProviderResult<WhetherSignal> = withContext(Dispatchers.IO) {

        var connection: HttpURLConnection? = null
        try {
            //
            val url = URL(
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=${location.latitude}" +
                    "&longitude=${location.longitude}" +
                    "&current_weather=true"
            )

            //
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }

            if (connection.responseCode !in 200..299) {
                return@withContext ProviderResult.Unavailable(UnavailableReason.TEMPORARY_ERROR)
            }

            //
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val currentWeather = json.getJSONObject("current_weather")
            val weatherCode = currentWeather.getInt("weathercode")

            ProviderResult.Available(mapWeatherCode(weatherCode))
        } catch (_: java.net.SocketTimeoutException) {
            ProviderResult.Unavailable(UnavailableReason.TIMEOUT)
        } catch (e: Exception) {
            ProviderResult.Unavailable(UnavailableReason.TEMPORARY_ERROR)
        } finally {
            connection?.disconnect()
        }
    }

    private fun mapWeatherCode(weatherCode: Int): WhetherSignal {
        return when (weatherCode) {
            0 -> WhetherSignal.SUNNY
            1, 2, 3, 45, 48 -> WhetherSignal.CLOUDY
            51, 53, 55, 61, 63, 65, 71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 99 ->
                WhetherSignal.RAINY
            else -> WhetherSignal.UNKNOWN
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 5_000
        private const val READ_TIMEOUT_MS = 5_000
    }
}
