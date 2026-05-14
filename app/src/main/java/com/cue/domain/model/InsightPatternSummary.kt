package com.cue.domain.model

data class InsightPatternSummary(
    val insightType: InsightType,
    val message: String,
    val averageConfidence: Float,
    val recurrenceCount: Int,
    val latestTimestamp: Long,
)
