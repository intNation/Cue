package com.cue.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.cue.data.local.entity.InsightEntity

@Dao
interface InsightDao {

    @Upsert
    suspend fun insertInsight(insight: InsightEntity) : Long

    @Query("SELECT * FROM Insight WHERE user_id = :userId")
    suspend fun getUserInsights(userId: Long): List<InsightEntity>?

    @Query("SELECT * FROM Insight WHERE id = :insightId")
    suspend fun getInsightById(insightId: Long): InsightEntity?

}