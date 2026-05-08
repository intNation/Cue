package com.cue.presentation.insights.model

import com.cue.domain.model.InsightType

data class PatternSummary(
    val insightType: InsightType,
    val message: String,
    val averageConfidence: Float,
    val recurrenceCount: Int,
    val latestTimestamp: Long,
    val signalStrength: SignalStrength
)


data class TodayLikeHint(
    val insightType: InsightType,
    val message: String,
    val signalStrength: SignalStrength,
    val matchedTimeStamp: Long
)

data class InsightTimelineItem(
    val id: Long,
    val insightType: InsightType,
    val message: String,
    val timestamp: Long,
    val isNew: TimelineEvent,
    val signalStrength: SignalStrength
)
enum class SignalStrength {
    EMERGING,
    MODERATE,
    STRONG
}

enum class TimelineEvent{
    NEWLY_DETECTED,
    REINFORCED
}