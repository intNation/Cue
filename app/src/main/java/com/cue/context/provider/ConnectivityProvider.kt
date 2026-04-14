package com.cue.context.provider

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Provider for collecting network connectivity signals.
 */
class ConnectivityProvider(private val context: Context) {

    /**
     * Detects the current connectivity type.
     * @return "WiFi", "Cellular", or "None"
     */
    fun getConnectivityStatus(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "None"

        val activeNetwork = connectivityManager.activeNetwork ?: return "None"
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return "None"

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "WiFi" // Treat Ethernet as WiFi
            else -> "None"
        }
    }
}
