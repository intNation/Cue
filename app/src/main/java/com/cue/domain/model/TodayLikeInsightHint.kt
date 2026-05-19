package com.cue.domain.model

data class TodayLikeInsightHint(
    val insightType: InsightType,
    val message: String,
    val matchedPatternMessage : String,
    val confidence: Float,
    val matchedSnapshotTimeStamp: Long
)
