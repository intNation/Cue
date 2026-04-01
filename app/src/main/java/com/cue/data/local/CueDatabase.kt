package com.cue.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cue.data.local.dao.ContextSnapshotDao
import com.cue.data.local.dao.DailyCheckInDao
import com.cue.data.local.dao.StudySessionDao
import com.cue.data.local.dao.UserDao
import com.cue.data.local.entity.ContextSnapshotEntity
import com.cue.data.local.entity.DailyCheckInEntity
import com.cue.data.local.entity.StudyLocationEntity
import com.cue.data.local.entity.StudySessionEntity
import com.cue.data.local.entity.UserEntity
import com.cue.data.local.entity.WeeklyScheduleEntity

@Database(
    entities = [
        StudySessionEntity::class,
        UserEntity::class,
        ContextSnapshotEntity::class,
        DailyCheckInEntity::class,
        StudyLocationEntity::class,
        WeeklyScheduleEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class CueDatabase : RoomDatabase() {
    abstract fun studySessionDao(): StudySessionDao
    abstract fun userDao(): UserDao
    abstract fun contextSnapshotDao(): ContextSnapshotDao
    abstract fun dailyCheckInDao(): DailyCheckInDao
}
