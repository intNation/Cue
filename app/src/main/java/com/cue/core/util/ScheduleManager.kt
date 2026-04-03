package com.cue.core.util

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.cue.data.context.ContextPollingWorker
import com.cue.domain.model.DaySchedule
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ScheduleManager(private val context: Context) {

    /**
     * Schedules the next context polling event based on the user's weekly schedule.
     * If the schedule is empty, cancels any existing polling.
     *
     */
    fun updateSchedule(weeklySchedule: List<DaySchedule>) {
        if (weeklySchedule.isEmpty()) {
            cancelAllPolling()
            return
        }

        val nextDelay = calculateNextStartDelay(weeklySchedule) ?: return

        val workRequest = OneTimeWorkRequestBuilder<ContextPollingWorker>()
            .setInitialDelay(nextDelay, TimeUnit.MILLISECONDS)
            .addTag(TAG_POLLING)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME_GHOST_SNAPSHOT,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelAllPolling() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_GHOST_SNAPSHOT)
    }

    /**
     * Calculates the delay until the next context polling event based on the user's weekly schedule.
     * The schedule is a list of DaySchedule objects, each containing a day of the week (1=Mon...7=Sun) and an optional start time (e.g. "14:00").
     */
    private fun calculateNextStartDelay(weeklySchedule: List<DaySchedule>): Long? {
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
        
        // Map Calendar.DAY_OF_WEEK to our 1=Mon...7=Sun
        val ourCurrentDay = when(currentDay) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }

        // Find all potential next start times of polling
        val potentialStarts = mutableListOf<Long>()

        weeklySchedule.forEach { schedule ->
            if (schedule.startTime != null) {
                val parts = schedule.startTime.split(":")
                val hour = parts[0].toInt()
                val min = parts[1].toInt()

                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, min)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // Adjust day
                val dayDiff = schedule.dayOfWeek - ourCurrentDay
                calendar.add(Calendar.DAY_OF_YEAR, dayDiff)

                // If time already passed today, move to next week
                // If time already passed this week, move to next week
                if (calendar.before(now)) {
                    calendar.add(Calendar.DAY_OF_YEAR, 7)
                }

                potentialStarts.add(calendar.timeInMillis)
            }
        }

        return potentialStarts.minOrNull()?.let { it - now.timeInMillis }
    }

    companion object {
        private const val TAG_POLLING = "context_polling"
        private const val WORK_NAME_GHOST_SNAPSHOT = "ghost_snapshot_work"
    }
}
