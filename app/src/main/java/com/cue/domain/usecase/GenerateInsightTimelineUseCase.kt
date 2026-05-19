package com.cue.domain.usecase

import com.cue.domain.model.Insight
import com.cue.domain.model.InsightTimeLineEntry
import com.cue.domain.model.InsightTimeLineEventType
import com.cue.domain.repository.InsightRepository
import com.cue.domain.repository.UserRepository

/**
 *  Use case behavior:
 *   1. Get current user.
 *   2. Return empty list if there is no user.
 *   3. Fetch all user insights.
 *   4. Keep only insights from the last 14 days.
 *   5. Sort from oldest to newest internally.
 *   6. Mark the first time each message appears as NEWLY_DETECTED.
 *   7. Mark later entries with the same message as REINFORCED.
 *   8. Return the final timeline newest-first for display.
 *   9. Limit to a reasonable number, such as 20 items.
 */
class GenerateInsightTimelineUseCase (private val userRepo : UserRepository, private val insightRepo : InsightRepository){

    suspend operator fun invoke(): List <InsightTimeLineEntry>{
        //get the current User
        val currUser = userRepo.getCurrentUser()?: return emptyList()
        //get the user insights
        return insightRepo.getUserInsights(currUser.id).orEmpty()
            .keepInsightsFromTwoWeeksAgo()
            .sortedBy { it.timestamp }
            .toInsightTimelineEntries()
            .sortedByDescending { it.timestamp }
            .take(20)
    }

    /**
     * function to keep insights from the last 14 days
     */
    private fun  List<Insight>.keepInsightsFromTwoWeeksAgo(): List<Insight> {
        val twoWeeksAgo = System.currentTimeMillis() - (14L * 24 * 60 * 60 * 1000)
        return filter { it.timestamp >= twoWeeksAgo }
    }

    /**
     *
     * function to mark the first time each message appears as NEWLY_DETECTED
     * and mark later entries with the same message as REINFORCED
     * and return the final timeline newest-first for display
     */

    private fun List<Insight>.toInsightTimelineEntries(): List<InsightTimeLineEntry>{
        val seenMessages = mutableSetOf<String>()

        return map { insight ->
            val eventType = if (insight.message in seenMessages) {
                InsightTimeLineEventType.REINFORCED
            } else {
                seenMessages.add(insight.message)
                InsightTimeLineEventType.NEWLY_DETECTED
            }

            InsightTimeLineEntry(
                id = insight.id,
                insightType = insight.type,
                message = insight.message,
                confidence = insight.confidenceScore,
                timestamp = insight.timestamp,
                eventType = eventType
            )
        }
    }
}