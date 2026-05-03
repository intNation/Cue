package com.cue.domain.repository

import com.cue.data.local.entity.InsightEntity
import com.cue.domain.model.Insight

interface InsightRepository{
    suspend fun insertInsight(insight: Insight) : Long
    suspend fun getUserInsights(userId: Long): List<Insight>?
    suspend fun getInsightById(insightId: Long): Insight?
    suspend fun getInsightByType(userId: Long, insightType: String): List<Insight>

}