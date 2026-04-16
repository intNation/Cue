package com.cue.context.provider

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.cue.context.contracts.PhoneUsageLevelProvider
import com.cue.context.contracts.PhoneUsageLevelSignal
import com.cue.context.contracts.ProviderResult
import com.cue.context.contracts.UnavailableReason

/**
 * Refactored PhoneUsageProvider that explicitly checks for system settings
 * and normalizes data into the required signal levels.
 */
class PhoneUsageProvider(private val context: Context) : PhoneUsageLevelProvider {

    override suspend fun getPhoneUsageLevel(): ProviderResult<PhoneUsageLevelSignal> {
        // 1. Explicit System Setting / AppOps Check
        if (!hasUsageStatsPermission()) {
            return ProviderResult.Unavailable(UnavailableReason.SYSTEM_SETTING_DISABLED)
        }

            // 2. Query Usage Stats
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return ProviderResult.Unavailable(UnavailableReason.TEMPORARY_ERROR)

        // 3. Normalized Calculation
        // Calculate the total time spent in the foreground in a 2hour window
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (2 * 60 * 60 * 1000) // 2 hour window

        // 4. Query Usage Stats
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        // Handle the case where no stats are available
        if (stats.isNullOrEmpty()) {
            return ProviderResult.Unavailable(UnavailableReason.DATA_NOT_AVAILABLE)
        }

        // 5. Normalized Calculation
        var totalTimeForeground: Long = 0
        stats.forEach { 
            // We only care about usage that falls within our 2-hour window
            // totalTimeInForeground can be larger if it's daily interval, 
            // but queryUsageStats with startTime/endTime helps filter.
            totalTimeForeground += it.totalTimeInForeground 
        }

        val usageMinutes = totalTimeForeground / (1000 * 60)

        // Phone usage thresholds
        return when {
            usageMinutes > 60 -> ProviderResult.Available(PhoneUsageLevelSignal.HIGH)
            usageMinutes > 20 -> ProviderResult.Available(PhoneUsageLevelSignal.MEDIUM)
            else -> ProviderResult.Available(PhoneUsageLevelSignal.LOW)
        }
    }

    /**
     * Checks if the app has usage stats permission.
     * @return True if the permission is granted, false otherwise.
     */
    private fun hasUsageStatsPermission(): Boolean {

        // Check if the app has usage stats permissios
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager // Context.APP_OPS_MANAGER
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) { // android.os.Build.VERSION_CODES.Q
            appOps.unsafeCheckOpNoThrow(
                //
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {

            @Suppress("DEPRECATION")

            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }

        // Return true if the permission is granted, false otherwise
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
