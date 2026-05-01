package com.cue.context.provider

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.cue.context.contracts.ConnectivitySignal
import com.cue.context.contracts.ProviderResult
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ConnectivityProviderTest {

    private val context = mockk<Context>()
    private val connectivityManager = mockk<ConnectivityManager>()
    private lateinit var provider: ConnectivityProvider

    @Before
    fun setup() {
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        provider = ConnectivityProvider(context)
    }

    @Test
    fun `when no active network, return NONE`() {
        // Arrange
        every { connectivityManager.activeNetwork } returns null

        // Act
        val result = provider.getConnectivitySignal()

        // Assert
        assert(result is ProviderResult.Available)
        assertEquals(ConnectivitySignal.NONE, (result as ProviderResult.Available).data)
    }

    @Test
    fun `when WiFi is active, return WIFI`() {
        // Arrange
        val mockNetwork = mockk<android.net.Network>()
        val mockCapabilities = mockk<NetworkCapabilities>()
        
        every { connectivityManager.activeNetwork } returns mockNetwork
        every { connectivityManager.getNetworkCapabilities(mockNetwork) } returns mockCapabilities
        every { mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true

        // Act
        val result = provider.getConnectivitySignal()

        // Assert
        assert(result is ProviderResult.Available)
        assertEquals(ConnectivitySignal.WIFI, (result as ProviderResult.Available).data)
    }

    @Test
    fun `when Cellular is active, return CELLULAR`() {
        // Arrange
        val mockNetwork = mockk<android.net.Network>()
        val mockCapabilities = mockk<NetworkCapabilities>()
        
        every { connectivityManager.activeNetwork } returns mockNetwork
        every { connectivityManager.getNetworkCapabilities(mockNetwork) } returns mockCapabilities
        every { mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true

        // Act
        val result = provider.getConnectivitySignal()

        // Assert
        assert(result is ProviderResult.Available)
        assertEquals(ConnectivitySignal.CELLULAR, (result as ProviderResult.Available).data)
    }
}
