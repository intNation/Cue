package com.cue.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.cue.data.local.entity.InsightEntity
import com.cue.domain.model.Insight

@Dao
interface InsightDao {

    @Insert(InsightEntity::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsight(insight: InsightEntity) : Long

    @Query("SELECT * FROM Insight WHERE user_id = :userId")
    suspend fun getUserInsights(userId: Long): List<InsightEntity>?

    @Query("SELECT * FROM Insight WHERE id = :insightId")
    suspend fun getInsightById(insightId: Long): InsightEntity?

    /**
     * Get insights by type
     * This will allow the engine to look back at how a specific pattern for histroy purposes
     */
    suspend fun getInsightsByType(userId: Long, insightType: String): List<InsightEntity> {
        return getUserInsights(userId)?.filter { it.type == insightType } ?: emptyList()
    }

}