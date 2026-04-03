package com.cue.domain.model


enum class insightType {
    PHONE_USAGE,
    SLEEP,
    CONNECTIVITY,
    WEATHER
}

data class Insight(
    val id: Long = 0,
    val userId: Long,
    val message: String,
    val type: String
)
