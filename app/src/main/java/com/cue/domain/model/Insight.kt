package com.cue.domain.model


enum class InsightType {
    PHONE_USAGE,
    SLEEP,
    CONNECTIVITY,
    WEATHER
}

data class Insight(
    val id: Long = 0,
    val userId: Long,
    val message: String,
    val type: InsightType,
    val timestamp: Long,
    val confidenceScore: Float = 0.0 //Added confidence score field
)
