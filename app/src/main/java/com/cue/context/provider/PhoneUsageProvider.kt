package com.cue.context.provider

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import java.util.*

/**
 * Provider for collecting phone usage signals.
 * Calculates total screen time in the 2 hours preceding the capture.
 */
class PhoneUsageProvider(private val context: Context) {

    /**
     * Captures the normalized phone usage status.
     * @return "High", "Medium", "Low", or "Unknown"
     */
    fun getPhoneUsageStatus(): String {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return "Unknown"

        val endTime = System.currentTimeMillis()
        val startTime = endTime - (2 * 60 * 60 * 1000) // 2 hours ago

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        if (stats.isNullOrEmpty()) {
            return "Low" // Or "Unknown" if we can't tell if it's zero usage or missing permission
        }

        var totalTimeVisible: Long = 0
        for (usageStat in stats) {
            totalTimeVisible += usageStat.totalTimeInForeground
        }

        val totalMinutes = totalTimeVisible / (1000 * 60)

        return when {
            totalMinutes > 60 -> "High"
            totalMinutes > 20 -> "Medium"
            else -> "Low"
        }
    }
}
