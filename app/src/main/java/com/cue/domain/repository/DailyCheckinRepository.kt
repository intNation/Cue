package com.cue.domain.repository

import com.cue.domain.model.DailyCheckIn

interface DailyCheckinRepository {

    abstract  suspend fun  insertCheckIn(checkIn: DailyCheckIn): Long
    abstract  suspend fun  getAllCheckIns(): List<DailyCheckIn>
    abstract  suspend fun  getRecentCheckIn(startOfDay: Long): DailyCheckIn?
}