package com.cue.domain.usecase

import com.cue.domain.model.Insight
import com.cue.domain.model.InsightPatternSummary
import com.cue.domain.model.User
import com.cue.domain.repository.InsightRepository
import com.cue.domain.repository.UserRepository


class GenerateInsightSummaryUseCase(private val insightRepo : InsightRepository, private val userRepo : UserRepository)
{
    suspend operator fun  invoke() : List<InsightPatternSummary>{
        val user = userRepo.getCurrentUser()?: return emptyList()
        val insights = insightRepo.getUserInsights(user.id).orEmpty()

        //return only recent insights from the history, grouped by message
        return insights
            .filterRecent()
            .groupBy { it.message }
            .mapNotNull { (_,GroupedInsights) -> GroupedInsights.toPatternSummary()}
            .sortedWith(compareByDescending<InsightPatternSummary> {
                it.recurrenceCount }
                .thenByDescending { it.averageConfidence }
                .thenBy { it.latestTimestamp })
            .take(MAX_PATTERN_SUMMARIES)
        }

    /**
     * converts a List of insight history to pattern summaries
     */

    private fun List<Insight>.toPatternSummary(): InsightPatternSummary? {
        if (isEmpty()) return null

        val latestInsight = maxBy{ it.timestamp }

        return InsightPatternSummary (
            insightType = latestInsight.type,
            message = latestInsight.message,
            averageConfidence = map { it.confidenceScore }.average().toFloat(),
            recurrenceCount = size,
            latestTimestamp =  latestInsight.timestamp
        )

    }


    /**
     * get the latest insight
     * @return the latest insight
     */
    private fun List<Insight>.filterRecent(): List<Insight>{
        val cutoffTime = System.currentTimeMillis() - PATTERN_WINDOW_MS
        return this.filter { it.timestamp >= cutoffTime }
    }


    private  companion object {
        const val  MAX_PATTERN_SUMMARIES = 3
        const val PATTERN_WINDOW_MS = 14L * 24 * 60 * 60 * 1000
    }



}


