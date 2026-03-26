package com.cue.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cue.data.local.dao.StudySessionDao
import com.cue.data.local.dao.UserDao
import com.cue.data.local.entity.StudySessionEntity

@Database(entities = [StudySessionEntity::class , UserEntity::class ] , version = 2)
abstract class CueDatabase : RoomDatabase() {
    abstract fun studySessionDao(): StudySessionDao
    abstract fun userDao() : UserDao
}