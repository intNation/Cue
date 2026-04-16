package com.cue.context.provider

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.cue.context.contracts.ConnectivityProvider as ConnectivitySignalProvider
import com.cue.context.contracts.ConnectivitySignal
import com.cue.context.contracts.ProviderResult
import com.cue.context.contracts.UnavailableReason

/**
 * Provider for collecting network connectivity signals.
 */
class ConnectivityProvider(private val context: Context) : ConnectivitySignalProvider {

    /**
     * Detects the current connectivity type.
     * @return A ProviderResult indicating the current connectivity type.
     */
    override fun getConnectivitySignal(): ProviderResult<ConnectivitySignal> {
        // Get the system ConnectivityManager
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return ProviderResult.Unavailable(UnavailableReason.TEMPORARY_ERROR)

        // Get the active network and its capabilities
        val activeNetwork = connectivityManager.activeNetwork
            ?: return ProviderResult.Available(ConnectivitySignal.NONE)
        // Check the network capabilities to determine the connectivity type
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            ?: return ProviderResult.Unavailable(UnavailableReason.DATA_NOT_AVAILABLE)

        // Return the appropriate ConnectivitySignal based on the network capabilities
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                ProviderResult.Available(ConnectivitySignal.WIFI)
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                ProviderResult.Available(ConnectivitySignal.CELLULAR)
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
                ProviderResult.Available(ConnectivitySignal.UNKNOWN)
            else -> ProviderResult.Available(ConnectivitySignal.UNKNOWN)
        }
    }
}
