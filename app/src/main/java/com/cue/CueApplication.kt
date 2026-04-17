package com.cue

import android.app.Application
import androidx.room.Room
import com.cue.data.local.CueDatabase
import com.cue.data.local.CueDatabaseMigrations

class CueApplication : Application() {
    
    lateinit var database: CueDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            CueDatabase::class.java,
            "cue.db"
        )
        .addMigrations(CueDatabaseMigrations.MIGRATION_6_7)
        .build()
    }
}
