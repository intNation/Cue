package com.cue.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cue.data.local.dao.StudySessionDao
import com.cue.data.local.entity.StudySessionEntity

@Database(entities = [StudySessionEntity::class], version = 1)
abstract class CueDatabase : RoomDatabase() {
    abstract fun studySessionDao(): StudySessionDao
}