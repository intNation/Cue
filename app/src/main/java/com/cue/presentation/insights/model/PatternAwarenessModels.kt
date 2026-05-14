package com.cue.presentation.insights.model

import com.cue.domain.model.InsightType

/*
 * domain models for the insight pattern awareness screen
 * these sit between the presentation layer and the insight model
 * these will be used to populate the insight screen without having to touch the existing insight model
 */

/**
 * Pattern summary class for the insight screen.
 * it represents a repeated pattern in the database
 * @param insightType the type of insight
 * @param message the message of the insight
 * @param averageConfidence the average confidence of the insight
 * @param recurrenceCount the number of times the insight has been repeated
 * @param latestTimestamp the timestamp of the latest insight
 * @param signalStrength the strength of the signal
 */
data class PatternSummary(
    val insightType: InsightType,
    val message: String,
    val averageConfidence: Float,
    val recurrenceCount: Int,
    val latestTimestamp: Long,
    val signalStrength: SignalStrength
)

/**
 * Hint class for the insight screen.
 * it represents a hint represent an "On Days Like..." hint
 * @param insightType the type of insight(SLEEP,WHETHER,LOCATION,PHONE USAGE, ETC)
 * @param message the message of the insight(i.e "On Days Like Today with High phone usage, you tend to miss Study session")
 * @param signalStrength the strength of the signal
 * @param matchedTimeStamp the timestamp of the matched pattern insight
 */
data class TodayLikeHint(
    val insightType: InsightType,
    val message: String,
    val signalStrength: SignalStrength,
    val matchedTimeStamp: Long
)

/**
 * Timeline item class for the insight screen.
 * it represents a timeline of insights in the insight screen
 * it shows the latest pattern evolution
 * @param id the id of the insight
 * @param insightType the type of insight
 * @param message the message of the insight
 * @param timestamp the timestamp of the insight
 * @param isNew whether the insight is new or  has been reinforced
 * @param signalStrength the strength of the signal
 */

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