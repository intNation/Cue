package com.cue.domain.usecase

import com.cue.domain.model.ContextSnapshot
import com.cue.domain.model.InsightPatternSummary
import com.cue.domain.model.InsightType
import com.cue.domain.model.TodayLikeInsightHint
import com.cue.domain.repository.ContextSnapShotRepository
import com.cue.presentation.insights.model.PatternSummary
import com.cue.presentation.insights.model.TodayLikeHint
import kotlin.collections.emptyList

class GenerateTodayLikeHintUseCase(private val contextRepo: ContextSnapShotRepository, private val patternSummaryUseCase: GenerateInsightSummaryUseCase) {

    suspend operator fun invoke(): TodayLikeInsightHint? {
        //get the current snapshot
        val latestSnapshot = contextRepo.getAllSnapshots().maxByOrNull { it.timestamp }
        //get the pattern summaries or null if empty
        val patternSummaries = patternSummaryUseCase().filter { it.averageConfidence >= 0.7f}

        if (patternSummaries.isEmpty() || latestSnapshot == null) return null

        //check if any pattern summary matches the latest snapshot and return a hint if so
        return patternSummaries
            .mapNotNull{summary -> summary.createTodayLikeHintIfMatchesKnownPattern(latestSnapshot)}
            .maxWithOrNull (compareBy<TodayLikeInsightHint> {it.confidence}.thenBy({ latestSnapshot.timestamp }) )
    }

    /**
     * creates a hint message and returns a TodayLikeHint if a similar pattern is found in the latest snapshot
     */
    private  fun  InsightPatternSummary.createTodayLikeHintIfMatchesKnownPattern(latestSnapshot: ContextSnapshot?) : TodayLikeInsightHint?{
        if (latestSnapshot == null) return null

        if(!latestSnapshot.hasMatchingSignal(insightType)) return null

        return TodayLikeInsightHint(
            insightType = insightType,
            message = createHintMessage(insightType),
            matchedPatternMessage = message,
            confidence = averageConfidence,
            matchedSnapshotTimeStamp = latestSnapshot.timestamp
        )
    }

    /**
     * checks whether the latest snapshot has a signal that matches a pattern summary.
     */
    private fun ContextSnapshot.hasMatchingSignal(insightType: InsightType): Boolean {
        return when (insightType) {
            InsightType.SLEEP ->  sleep in 1..4
            InsightType.PHONE_USAGE -> phoneUsage == "High"
            InsightType.WEATHER -> weather == "Rainy"
            InsightType.CONNECTIVITY -> connectivity == "None"
        }
    }

    /**
     * creates a hint message when a similar pattern is found in the latest snapshot.
     */

    private fun createHintMessage(type: InsightType): String {
        return when (type) {
            InsightType.SLEEP -> "On days like today when you sleep for less than 4 hours, you tend to miss/delay study sessions."
            InsightType.PHONE_USAGE -> "On days like today with high phone usage, you tend to miss/delay a study sessions."
            InsightType.WEATHER -> "On days like today when the weather is rainy, you tend to miss/delay study sessions."
            InsightType.CONNECTIVITY -> "On days like today when you have no connectivity, you tend to miss/delay study sessions."
        }
    }

}