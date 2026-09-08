package com.cue.presentation.insights.mappers

import com.cue.domain.model.InsightPatternSummary
import com.cue.domain.model.InsightTimeLineEntry
import com.cue.domain.model.TodayLikeInsightHint
import com.cue.presentation.insights.model.InsightTimelineItem
import com.cue.presentation.insights.model.PatternSummary
import com.cue.presentation.insights.model.TodayLikeHint

//map an TodayLikeInsightHint to a TodayLikeHint model
fun TodayLikeInsightHint.toTodayLikeHintModel(): TodayLikeHint
{
    return TodayLikeHint(
        insightType = insightType,
        message = message,
        signalStrength = confidence.toInsightsStrength(),
        matchedTimeStamp = matchedSnapshotTimeStamp
    )
}

//map an InsightPatternSummary to a patternSummary model
fun InsightPatternSummary.toPatternSummary(): PatternSummary {
    return PatternSummary(
        insightType = insightType,
        message = message,
        averageConfidence = averageConfidence,
        recurrenceCount = recurrenceCount,
        latestTimestamp = latestTimestamp,
        signalStrength = averageConfidence.toInsightsStrength()
    )
}


fun TodayLikeInsightHint.toTodayLikeHint(): TodayLikeHint {
    return TodayLikeHint(
        insightType = insightType,
        message = message,
        signalStrength = confidence.toInsightStrength(),
        matchedTimeStamp = matchedSnapshotTimeStamp
    )
}


fun InsightTimeLineEntry.toInsightTimelineItem(): InsightTimelineItem {
    return InsightTimelineItem(
        id = id,
        insightType = insightType,
        message = message,
        timestamp = timestamp,
        isNew = eventType.to(),
        signalStrength = confidence.toInsightStrength()
    )
}