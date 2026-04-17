package com.cue.data.context

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cue.CueApplication
import com.cue.data.repository.ContextSnapShotRepositoryImpl

import com.cue.core.util.ScheduleManager
import com.cue.data.repository.UserRepositoryImpl

class ContextPollingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = (applicationContext as CueApplication).database
        val snapshotDao = database.contextSnapshotDao()
        val snapshotRepo = ContextSnapShotRepositoryImpl(snapshotDao)
        val userRepo = UserRepositoryImpl(database, database.userDao())
        
        val contextEngine = com.cue.context.impl.ContextEngineImpl(
            userRepo,
            com.cue.context.provider.PhoneUsageProvider(applicationContext),
            com.cue.context.provider.ConnectivityProvider(applicationContext),
            com.cue.context.provider.LocationProvider(applicationContext),
            com.cue.context.provider.WeatherProvider()
        )

        // Capture context without a session ID (Ghost Snapshot)
        val snapshot = contextEngine.captureSnapshot(sessionId = null)
        snapshotRepo.insertSnapshot(snapshot)

        // Reschedule next capture after this one completes
        userRepo.getCurrentUser()?.let { user ->
            ScheduleManager(applicationContext).updateSchedule(user.weeklySchedule)
        }

        return Result.success()
    }
}
