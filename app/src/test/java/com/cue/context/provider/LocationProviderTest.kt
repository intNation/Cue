package com.cue.context.provider

import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.cue.context.contracts.ProviderResult
import com.cue.context.contracts.StudyLocationSignal
import com.cue.context.contracts.UnavailableReason
import com.cue.domain.model.StudyLocation
import com.cue.domain.model.StudyPlace
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocationProviderTest {

    private val context = mockk<Context>(relaxed = true)
    private val locationManager = mockk<LocationManager>()
    private lateinit var provider: LocationProvider

    @Before
    fun setup() {
        every { context.getSystemService(Context.LOCATION_SERVICE) } returns locationManager
        every { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns true
        every { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } returns true
        provider = LocationProvider(context)
        
        // Mock permission check to always return true for this unit test
        // In a real app, this would be handled by the ActivityResultLauncher

    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getWhetherLocation should quantize coordinates to 1 decimal place`() = runTest {
        // Arrange
        val mockLocation = mockk<Location>()
        every { mockLocation.latitude } returns -26.123456
        every { mockLocation.longitude } returns 28.654321
        every { mockLocation.hasAccuracy() } returns true
        every { mockLocation.accuracy } returns 10f
        
        every { locationManager.getProviders(true) } returns listOf(LocationManager.GPS_PROVIDER)
        every { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } returns mockLocation
        
        // Act
        val result = provider.getWhetherLocation()

        // Assert
        assert(result is ProviderResult.Available)
        val data = (result as ProviderResult.Available).data
        
        // Should be rounded to 1 decimal place (-26.1, 28.7)
        assertEquals(-26.1, data.latitude, 0.001)
        assertEquals(28.7, data.longitude, 0.001)
    }

    @Test
    fun `getStudyLocationSignal should return AT_USUAL_LOCATION when within radius`() = runTest {
        // Arrange
        val lat = -26.0
        val lon = 28.0
        
        val mockLocation = mockk<Location>()
        every { mockLocation.latitude } returns lat
        every { mockLocation.longitude } returns lon
        every { mockLocation.hasAccuracy() } returns true
        every { mockLocation.accuracy } returns 10f
        
        every { locationManager.getProviders(true) } returns listOf(LocationManager.GPS_PROVIDER)
        every { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } returns mockLocation

        // Distance matching
        mockkStatic(Location::class)
        every { 
            Location.distanceBetween(any(), any(), any(), any(), any()) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 50f // 50 meters away
        }

        val studyPlaces = listOf(
            StudyPlace(label = "Library", category = StudyLocation.LIBRARY, latitude = lat, longitude = lon, radiusMeters = 100)
        )

        // Act
        val result = provider.getStudyLocationSignal(studyPlaces)

        // Assert
        assert(result is ProviderResult.Available)
        assertEquals(StudyLocationSignal.AT_USUAL_LOCATION, (result as ProviderResult.Available).data)
    }

    @Test
    fun `when location is disabled, return SYSTEM_SETTING_DISABLED`() = runTest {
        // Arrange
        every { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns false
        every { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } returns false

        // Act
        val result = provider.getWhetherLocation()

        // Assert
        assert(result is ProviderResult.Unavailable)
        assertEquals(UnavailableReason.SYSTEM_SETTING_DISABLED, (result as ProviderResult.Unavailable).reason)
    }
}
