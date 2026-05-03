package com.cue.data.repository

import com.cue.data.local.dao.InsightDao
import com.cue.data.local.entity.InsightEntity
import com.cue.data.local.mappers.toDomain
import com.cue.domain.model.Insight
import com.cue.domain.repository.InsightRepository

class InsightRepositoryImpl(val dao : InsightDao) : InsightRepository  {
    override suspend fun insertInsight(insight: Insight): Long {
        val insight  = InsightEntity(
            id = insight.id,
            userId = insight.userId,
            message = insight.message,
            type = insight.type.name,
            confidenceScore = insight.confidenceScore,
            timestamp = insight.timestamp
        )

       return  dao.insertInsight(insight)
    }

    override suspend fun getUserInsights(userId: Long): List<Insight> {
        return dao.getUserInsights(userId)?.map { it.toDomain() } ?: emptyList()
    }

    override suspend fun getInsightById(insightId: Long): Insight? {
        return dao.getInsightById(insightId)?.toDomain()
    }

    override suspend fun getInsightByType(userId: Long, insightType: String): List<Insight> {
        return dao.getInsightsByType(userId, insightType).map { it.toDomain() }
    }

}