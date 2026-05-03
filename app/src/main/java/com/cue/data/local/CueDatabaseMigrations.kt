package com.cue.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object CueDatabaseMigrations {

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE ContextSnapshot
                ADD COLUMN study_location TEXT NOT NULL DEFAULT 'UNKNOWN'
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS StudyLocation_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    user_id INTEGER NOT NULL,
                    label TEXT NOT NULL,
                    location TEXT NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    radius_meters INTEGER NOT NULL,
                    is_active INTEGER NOT NULL,
                    FOREIGN KEY(user_id) REFERENCES User(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO StudyLocation_new (
                    id,
                    user_id,
                    label,
                    location,
                    latitude,
                    longitude,
                    radius_meters,
                    is_active
                )
                SELECT
                    id,
                    user_id,
                    location,
                    location,
                    0.0,
                    0.0,
                    100,
                    1
                FROM StudyLocation
                """.trimIndent()
            )

            db.execSQL("DROP TABLE StudyLocation")
            db.execSQL("ALTER TABLE StudyLocation_new RENAME TO StudyLocation")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE Insight
                ADD COLUMN confidence_score REAL NOT NULL DEFAULT 0.0
                """.trimIndent()
            )
        }
    }
}
