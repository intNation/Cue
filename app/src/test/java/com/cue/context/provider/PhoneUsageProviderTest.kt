package com.cue.context.provider

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import com.cue.context.contracts.PhoneUsageLevelSignal
import com.cue.context.contracts.ProviderResult
import com.cue.context.contracts.UnavailableReason
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PhoneUsageProviderTest {

    private val context = mockk<Context>()
    private val usageStatsManager = mockk<UsageStatsManager>()
    private val appOpsManager = mockk<AppOpsManager>()
    private lateinit var provider: PhoneUsageProvider

    @Before
    fun setup() {
        every { context.getSystemService(Context.USAGE_STATS_SERVICE) } returns usageStatsManager
        every { context.getSystemService(Context.APP_OPS_SERVICE) } returns appOpsManager
        every { context.packageName } returns "com.cue"
        provider = PhoneUsageProvider(context)
    }

    @Test
    fun `when usage permission is disabled, return SYSTEM_SETTING_DISABLED`() = runTest {
        // Arrange
        stubUsageStatsPermission(AppOpsManager.MODE_IGNORED)

        // Act
        val result = provider.getPhoneUsageLevel()

        // Assert
        assert(result is ProviderResult.Unavailable)
        assertEquals(UnavailableReason.SYSTEM_SETTING_DISABLED, (result as ProviderResult.Unavailable).reason)
    }

    @Test
    fun `when usage is over 60 minutes, return HIGH`() = runTest {
        // Arrange
        stubUsageStatsPermission(AppOpsManager.MODE_ALLOWED)

        val mockStat = mockk<UsageStats>()
        every { mockStat.totalTimeInForeground } returns 70 * 60 * 1000L // 70 minutes
        
        every { 
            usageStatsManager.queryUsageStats(any(), any(), any()) 
        } returns listOf(mockStat)

        // Act
        val result = provider.getPhoneUsageLevel()

        // Assert
        assert(result is ProviderResult.Available)
        assertEquals(PhoneUsageLevelSignal.HIGH, (result as ProviderResult.Available).data)
    }

    @Test
    fun `when usage is between 20 and 60 minutes, return MEDIUM`() = runTest {
        // Arrange
        stubUsageStatsPermission(AppOpsManager.MODE_ALLOWED)

        val mockStat = mockk<UsageStats>()
        every { mockStat.totalTimeInForeground } returns 30 * 60 * 1000L // 30 minutes
        
        every { 
            usageStatsManager.queryUsageStats(any(), any(), any()) 
        } returns listOf(mockStat)

        // Act
        val result = provider.getPhoneUsageLevel()

        // Assert
        assert(result is ProviderResult.Available)
        assertEquals(PhoneUsageLevelSignal.MEDIUM, (result as ProviderResult.Available).data)
    }

    @Test
    fun `when usage is low, return LOW`() = runTest {
        // Arrange
        stubUsageStatsPermission(AppOpsManager.MODE_ALLOWED)

        val mockStat = mockk<UsageStats>()
        every { mockStat.totalTimeInForeground } returns 5 * 60 * 1000L // 5 minutes
        
        every { 
            usageStatsManager.queryUsageStats(any(), any(), any()) 
        } returns listOf(mockStat)

        // Act
        val result = provider.getPhoneUsageLevel()

        // Assert
        assert(result is ProviderResult.Available)
        assertEquals(PhoneUsageLevelSignal.LOW, (result as ProviderResult.Available).data)
    }

    @Suppress("DEPRECATION")
    private fun stubUsageStatsPermission(mode: Int) {
        every {
            appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, any(), "com.cue")
        } returns mode
        every {
            appOpsManager.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, any(), "com.cue")
        } returns mode
    }
}
