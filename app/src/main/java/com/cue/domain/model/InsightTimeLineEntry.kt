package com.cue.domain.model

data class InsightTimeLineEntry(
    val id : Long,
    val insightType: InsightType,
    val message : String,
    val confidence : Float,
    val timestamp : Long,
    val eventType: InsightTimeLineEventType
)

enum class InsightTimeLineEventType {
    NEWLY_DETECTED,
    REINFORCED
}