package com.cue.context.impl

import com.cue.context.contracts.*
import com.cue.domain.model.User
import com.cue.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ContextEngineImplTest {

    private val userRepository = mockk<UserRepository>()
    private val phoneProvider = mockk<PhoneUsageLevelProvider>()
    private val connectivityProvider = mockk<ConnectivityProvider>()
    private val locationProvider = mockk<StudyLocationProvider>()
    private val weatherProvider = mockk<WhetherProvider>()
    
    private lateinit var contextEngine: ContextEngineImpl

    @Before
    fun setup() {
        contextEngine = ContextEngineImpl(
            userRepository,
            phoneProvider,
            connectivityProvider,
            locationProvider,
            weatherProvider
        )
    }

    @Test
    fun `when all providers return data, confidence score is 1_0`() = runTest {
        // Arrange
        coEvery { userRepository.getCurrentUser() } returns User(id = 1)
        
        coEvery { phoneProvider.getPhoneUsageLevel() } returns ProviderResult.Available(PhoneUsageLevelSignal.HIGH)
        every { connectivityProvider.getConnectivitySignal() } returns ProviderResult.Available(ConnectivitySignal.WIFI)
        coEvery { locationProvider.getStudyLocationSignal(any()) } returns ProviderResult.Available(StudyLocationSignal.AT_USUAL_LOCATION)
        coEvery { locationProvider.getWhetherLocation() } returns ProviderResult.Available(CoarseLocation(0.0, 0.0))
        coEvery { weatherProvider.getWhetherSignal(any()) } returns ProviderResult.Available(WhetherSignal.SUNNY)

        // Act
        val snapshot = contextEngine.captureSnapshot(sessionId = 123L)

        // Assert
        assertEquals(1.0f, snapshot.confidenceScore)
        assertEquals("HIGH", snapshot.phoneUsage)
        assertEquals("WIFI", snapshot.connectivity)
        assertEquals("SUNNY", snapshot.weather)
        assertEquals("AT_USUAL_LOCATION", snapshot.studyLocation)
    }

    @Test
    fun `when two providers fail, confidence score is 0_5`() = runTest {
        // Arrange
        coEvery { userRepository.getCurrentUser() } returns User(id = 1)
        
        coEvery { phoneProvider.getPhoneUsageLevel() } returns ProviderResult.Available(PhoneUsageLevelSignal.LOW)
        every { connectivityProvider.getConnectivitySignal() } returns ProviderResult.Available(ConnectivitySignal.CELLULAR)
        
        // Failures
        coEvery { locationProvider.getStudyLocationSignal(any()) } returns ProviderResult.Unavailable(UnavailableReason.PERMISSION_DENIED)
        coEvery { locationProvider.getWhetherLocation() } returns ProviderResult.Unavailable(UnavailableReason.PERMISSION_DENIED)
        // Weather will also fail because location failed in ContextEngineImpl logic

        // Act
        val snapshot = contextEngine.captureSnapshot(sessionId = null)

        // Assert
        assertEquals(0.5f, snapshot.confidenceScore)
        assertEquals("UNKNOWN", snapshot.studyLocation)
        assertEquals("UNKNOWN", snapshot.weather)
        assertEquals("LOW", snapshot.phoneUsage)
    }
}
