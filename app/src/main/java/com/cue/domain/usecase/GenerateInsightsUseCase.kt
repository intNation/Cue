package com.cue.domain.usecase

import com.cue.domain.model.Insight
import com.cue.domain.model.InsightType
import com.cue.domain.model.StudySession
import com.cue.domain.model.ContextSnapshot
import com.cue.domain.repository.ContextSnapShotRepository
import com.cue.domain.repository.DailyCheckinRepository
import com.cue.domain.repository.InsightRepository
import com.cue.domain.repository.StudySessionRepository
import com.cue.domain.repository.UserRepository
import java.util.Calendar
import kotlin.collections.forEach
import kotlin.math.abs
import kotlin.ranges.contains

data class PatternOccurences(
    var totalFailures: Int = 0,
    var matchingOccurrences: Int = 0
)

data class InsightCandidate(
    var type: InsightType,
    var message: String,
    var confidenceScore: Float = 0.0f,
    var priorityScore: Float = 0.0f
)

private data class SnapshotSignalState(
    val hasPhoneUsage: Boolean,
    val hasConnectivity: Boolean,
    val hasSleep: Boolean,
    val hasWeather: Boolean,
    val phoneUsageMatched: Boolean,
    val connectivityMatched: Boolean,
    val sleepMatched: Boolean,
    val weatherMatched: Boolean
)

enum class TimeBuckets(val label:String)
{
    MORNING("Morning"),
    AFTERNOON("Afternoon"),
    EVENING("Evening"),
    NIGHT("Night")
}

enum class MultiSignalRule(val primaryType: InsightType) {
    PHONE_USAGE_AND_SLEEP(InsightType.PHONE_USAGE),
    PHONE_USAGE_AND_CONNECTIVITY(InsightType.PHONE_USAGE),
    SLEEP_AND_CONNECTIVITY(InsightType.SLEEP)
}


class GenerateInsightsUseCase(
    private val userRepository: UserRepository,
    private val sessionRepository: StudySessionRepository,
    private val checkinRepository: DailyCheckinRepository,
    private val snapshotRepository: ContextSnapShotRepository,
    private val insightRepository: InsightRepository
) {

    suspend operator fun invoke() {
        val user = userRepository.getCurrentUser() ?: return
        val checkins = checkinRepository.getAllCheckIns()
        val rawSnapshots = snapshotRepository.getAllSnapshots()
        val rawSessions = sessionRepository.getAllSessions()

        val cleanedSessions = rawSessions.filter(::isValidSession)
        val failureTimestamps = buildFailureTimestamps(checkins, user.weeklySchedule, cleanedSessions)
        val insightTypeOccurrencesMap = createInsightTypeOccurrencesMap()
        val multiSignalOccurrencesMap = createMultiSignalOccurrencesMap()

        analyzeFailures(
            failureTimestamps = failureTimestamps,
            snapshots = rawSnapshots,
            insightTypeOccurrencesMap = insightTypeOccurrencesMap,
            multiSignalOccurrencesMap = multiSignalOccurrencesMap
        )

        buildInsightCandidates(
            insightTypeOccurrencesMap = insightTypeOccurrencesMap,
            multiSignalOccurrencesMap = multiSignalOccurrencesMap
        ).forEach {
            createOrUpdateInsight(user.id, it.message, it.type, it.confidenceScore)
        }
    }

    // modify logic to prevent insight overwriting, but instead everytime a pattern meets a threshold: (0.6 confidence),
    // always insert a new row in the Insight table
    // this will enable us to see how the insight behaved by keeping a "log of it"
    // this is used for history preservation
    private suspend fun createOrUpdateInsight(userId: Long, message: String, type: InsightType, confidence: Float) {
        val existing = insightRepository.getUserInsights(userId)
        val existingInsight = existing?.find { it.type == type && it.message == message }

        if (existingInsight == null && confidence >= 0.6f) {
            // new pattern found
            // Create a new insight only if the confidence level exceeds the .6 threshold
                insightRepository.insertInsight(
                    Insight(
                        userId = userId,
                        message = message,
                        type = type,
                        timestamp = System.currentTimeMillis(),
                        confidenceScore = confidence
                    )
                )
        } else if (existingInsight != null && confidence >= existingInsight.confidenceScore && confidence >= 0.6f ){

            //fetch the existing insight history
            val historyInsights = insightRepository.getInsightByType(userId, type.name)
            //calculate suppression window to prevent inserting same insight multiple times that was detected already in the past 3 days
            val threeDaysAgo = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000) // 3 days in milliseconds
            val recentSimilarInsight = historyInsights.find {
                it.message == message &&
                it.type == type &&
                it.timestamp >= threeDaysAgo
            }

            if(recentSimilarInsight != null) return
            //create a new log of the same insight with updated confidence score only if it was not created 3 days ago
            insightRepository.insertInsight(
                Insight(
                    userId = userId,
                    message = message,
                    type = type,
                    timestamp = System.currentTimeMillis(),
                    confidenceScore = confidence
                )
            )
        }
    }

    private fun isValidSession(session: StudySession): Boolean {
        val endTime = session.endTime ?: 0L
        val durationMins = (endTime - session.startTime) / (1000 * 60)
        return durationMins in 5..(12 * 60)
    }

    private fun buildFailureTimestamps(
        checkins: List<com.cue.domain.model.DailyCheckIn>,
        schedule: List<com.cue.domain.model.DaySchedule>,
        cleanedSessions: List<StudySession>
    ): List<Long> {
        return buildList {
            addAll(checkins.filter { !it.didStudy }.map { it.timestamp })
            addAll(getSilentFailureTimeStamps(schedule, cleanedSessions))
        }
    }

    private fun createInsightTypeOccurrencesMap(): Map<Pair<InsightType, TimeBuckets>, PatternOccurences> {
        return buildMap {
            InsightType.entries.forEach { type ->
                TimeBuckets.entries.forEach { bucket ->
                    put(Pair(type, bucket), PatternOccurences())
                }
            }
        }
    }

    private fun createMultiSignalOccurrencesMap(): Map<Pair<MultiSignalRule, TimeBuckets>, PatternOccurences> {
        return buildMap {
            MultiSignalRule.entries.forEach { rule ->
                TimeBuckets.entries.forEach { bucket ->
                    put(Pair(rule, bucket), PatternOccurences())
                }
            }
        }
    }

    private fun analyzeFailures(
        failureTimestamps: List<Long>,
        snapshots: List<ContextSnapshot>,
        insightTypeOccurrencesMap: Map<Pair<InsightType, TimeBuckets>, PatternOccurences>,
        multiSignalOccurrencesMap: Map<Pair<MultiSignalRule, TimeBuckets>, PatternOccurences>
    ) {
        failureTimestamps.forEach { timestamp ->
            analyzeFailure(
                timestamp = timestamp,
                snapshots = snapshots,
                insightTypeOccurrencesMap = insightTypeOccurrencesMap,
                multiSignalOccurrencesMap = multiSignalOccurrencesMap
            )
        }
    }

    private fun analyzeFailure(
        timestamp: Long,
        snapshots: List<ContextSnapshot>,
        insightTypeOccurrencesMap: Map<Pair<InsightType, TimeBuckets>, PatternOccurences>,
        multiSignalOccurrencesMap: Map<Pair<MultiSignalRule, TimeBuckets>, PatternOccurences>
    ) {
        val closestSnapshot = findClosestSnapshot(timestamp, snapshots) ?: return
        val timeBucket = getTimeBucket(timestamp)
        val signalState = buildSignalState(closestSnapshot)

        updateSingleSignalOccurrences(insightTypeOccurrencesMap, timeBucket, signalState)
        updateMultiSignalOccurrences(multiSignalOccurrencesMap, timeBucket, signalState)
    }

    private fun findClosestSnapshot(
        timestamp: Long,
        snapshots: List<ContextSnapshot>
    ): ContextSnapshot? {
        return snapshots
            .minByOrNull { abs(it.timestamp - timestamp) }
            ?.takeIf { abs(it.timestamp - timestamp) <= MAX_SNAPSHOT_CORRELATION_WINDOW_MS }
    }

    private fun buildSignalState(snapshot: ContextSnapshot): SnapshotSignalState {
        return SnapshotSignalState(
            hasPhoneUsage = snapshot.phoneUsage != "UNKNOWN",
            hasConnectivity = snapshot.connectivity != "UNKNOWN",
            hasSleep = snapshot.sleep in 1..18,
            hasWeather = snapshot.weather != "UNKNOWN",
            phoneUsageMatched = snapshot.phoneUsage == "High",
            connectivityMatched = snapshot.connectivity == "None",
            sleepMatched = snapshot.sleep < 5,
            weatherMatched = snapshot.weather == "Rainy"
        )
    }

    private fun updateSingleSignalOccurrences(
        insightTypeOccurrencesMap: Map<Pair<InsightType, TimeBuckets>, PatternOccurences>,
        timeBucket: TimeBuckets,
        signalState: SnapshotSignalState
    ) {
        incrementOccurrences(
            occurrences = insightTypeOccurrencesMap[Pair(InsightType.PHONE_USAGE, timeBucket)],
            hasSignal = signalState.hasPhoneUsage,
            signalMatched = signalState.phoneUsageMatched
        )
        incrementOccurrences(
            occurrences = insightTypeOccurrencesMap[Pair(InsightType.CONNECTIVITY, timeBucket)],
            hasSignal = signalState.hasConnectivity,
            signalMatched = signalState.connectivityMatched
        )
        incrementOccurrences(
            occurrences = insightTypeOccurrencesMap[Pair(InsightType.WEATHER, timeBucket)],
            hasSignal = signalState.hasWeather,
            signalMatched = signalState.weatherMatched
        )
        incrementOccurrences(
            occurrences = insightTypeOccurrencesMap[Pair(InsightType.SLEEP, timeBucket)],
            hasSignal = signalState.hasSleep,
            signalMatched = signalState.sleepMatched
        )
    }

    private fun incrementOccurrences(
        occurrences: PatternOccurences?,
        hasSignal: Boolean,
        signalMatched: Boolean
    ) {
        if (!hasSignal || occurrences == null) return

        occurrences.totalFailures++
        if (signalMatched) {
            occurrences.matchingOccurrences++
        }
    }

    private fun updateMultiSignalOccurrences(
        multiSignalOccurrencesMap: Map<Pair<MultiSignalRule, TimeBuckets>, PatternOccurences>,
        timeBucket: TimeBuckets,
        signalState: SnapshotSignalState
    ) {
        updateMultiSignalOccurrences(
            occurrences = multiSignalOccurrencesMap[Pair(MultiSignalRule.PHONE_USAGE_AND_SLEEP, timeBucket)],
            hasAllSignals = signalState.hasPhoneUsage && signalState.hasSleep,
            allSignalsMatch = signalState.phoneUsageMatched && signalState.sleepMatched
        )
        updateMultiSignalOccurrences(
            occurrences = multiSignalOccurrencesMap[Pair(MultiSignalRule.PHONE_USAGE_AND_CONNECTIVITY, timeBucket)],
            hasAllSignals = signalState.hasPhoneUsage && signalState.hasConnectivity,
            allSignalsMatch = signalState.phoneUsageMatched && signalState.connectivityMatched
        )
        updateMultiSignalOccurrences(
            occurrences = multiSignalOccurrencesMap[Pair(MultiSignalRule.SLEEP_AND_CONNECTIVITY, timeBucket)],
            hasAllSignals = signalState.hasSleep && signalState.hasConnectivity,
            allSignalsMatch = signalState.sleepMatched && signalState.connectivityMatched
        )
    }

    private fun buildInsightCandidates(
        insightTypeOccurrencesMap: Map<Pair<InsightType, TimeBuckets>, PatternOccurences>,
        multiSignalOccurrencesMap: Map<Pair<MultiSignalRule, TimeBuckets>, PatternOccurences>
    ): List<InsightCandidate> {
        val insightCandidates = mutableListOf<InsightCandidate>()

        insightTypeOccurrencesMap.forEach { (key, occurrences) ->
            val (type, timeBucket) = key
            createSingleSignalCandidate(type, timeBucket, occurrences)?.let(insightCandidates::add)
        }

        multiSignalOccurrencesMap.forEach { (key, occurrences) ->
            val (rule, timeBucket) = key
            createMultiSignalCandidate(rule, timeBucket, occurrences)?.let(insightCandidates::add)
        }

        return insightCandidates
            .sortedByDescending { it.priorityScore }
            .take(3)
    }

    private fun createSingleSignalCandidate(
        type: InsightType,
        timeBucket: TimeBuckets,
        occurrences: PatternOccurences
    ): InsightCandidate? {
        val confidenceScore = calculateConfidenceScore(occurrences) ?: return null
        val message = createMessageForInsightTypeWithTime(type, timeBucket)
        val priorityScore = confidenceScore * getInsightImpactWeightForType(type)

        return InsightCandidate(type, message, confidenceScore, priorityScore)
    }

    private fun createMultiSignalCandidate(
        rule: MultiSignalRule,
        timeBucket: TimeBuckets,
        occurrences: PatternOccurences
    ): InsightCandidate? {
        val confidenceScore = calculateConfidenceScore(occurrences) ?: return null
        val message = createMessageForMultiSignalRule(rule, timeBucket)
        val priorityScore = confidenceScore * getMultiSignalImpactWeight(rule)

        return InsightCandidate(
            type = rule.primaryType,
            message = message,
            confidenceScore = confidenceScore,
            priorityScore = priorityScore
        )
    }

    private fun calculateConfidenceScore(occurrences: PatternOccurences): Float? {
        if (occurrences.totalFailures < 3) return null

        val frequency = occurrences.matchingOccurrences / occurrences.totalFailures.toFloat()
        if (frequency < 0.6f) return null

        val occurrenceWeight = minOf(occurrences.totalFailures / 10f, 1.0f)
        val consistency = if (frequency > 0.8f) 1.0f else 0.5f
        val confidenceScore = (frequency * 0.5f) + (occurrenceWeight * 0.3f) + (consistency * 0.2f)

        return confidenceScore.takeIf { it >= 0.6f }
    }

    private fun updateMultiSignalOccurrences(
        occurrences: PatternOccurences?,
        hasAllSignals: Boolean,
        allSignalsMatch: Boolean
    ) {
        if (!hasAllSignals || occurrences == null) return

        occurrences.totalFailures++
        if (allSignalsMatch) {
            occurrences.matchingOccurrences++
        }
    }
    private fun getSilentFailureTimeStamps(
        schedule: List<com.cue.domain.model.DaySchedule>,
        sessions: List<com.cue.domain.model.StudySession>
    ): List<Long> {
        val failureTimestamps = mutableListOf<Long>()
        val now = Calendar.getInstance()

        //look back for the past 7 days for failure patterns
        for(i in 1 until 7) {
            val date = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dayofweek = getDayOfWeekInt(date)

            //find the matching scheduled day
            val scheduledDay = schedule.find { it.dayOfWeek == dayofweek } ?: continue

            //check for missed scheduled sessions
            val sessionToday = sessions.filter {
                val sessionDate = Calendar.getInstance().apply { timeInMillis = it.startTime }
                sessionDate.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
            }

            if (sessionToday.isEmpty()) {
                //missed scheduled session
                val startTime = scheduledDay.startTime?.let { startTimestr ->
                    val parts = startTimestr.split(":")
                    date.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                    date.set(Calendar.MINUTE, parts[1].toInt())
                    failureTimestamps.add(date.timeInMillis)
                }
            }
        }
            return failureTimestamps;
    }

    private fun getTimeBucket(timeStamp: Long) : TimeBuckets {
        val date = Calendar.getInstance().apply { timeInMillis = timeStamp }
        val hour = date.get(Calendar.HOUR_OF_DAY)

        return when(hour){
            in 5..11 -> TimeBuckets.MORNING
            in 12..17 -> TimeBuckets.AFTERNOON
            in 18..23 -> TimeBuckets.EVENING
            else -> TimeBuckets.NIGHT
        }
    }

    private fun getDayOfWeekInt(date: Calendar): Int {
        val  dayOfWeekInt = when (date.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
        return dayOfWeekInt
    }

    private fun getInsightImpactWeightForType (type: InsightType) : Float{
        return when(type){
            InsightType.PHONE_USAGE -> 1.5f
            InsightType.SLEEP -> 1.3f
            InsightType.CONNECTIVITY -> 1.0f
            InsightType.WEATHER -> 0.8f
        }
    }

    private fun getMultiSignalImpactWeight(rule: MultiSignalRule): Float {
        return when(rule) {
            MultiSignalRule.PHONE_USAGE_AND_SLEEP -> 1.6f
            MultiSignalRule.PHONE_USAGE_AND_CONNECTIVITY -> 1.4f
            MultiSignalRule.SLEEP_AND_CONNECTIVITY -> 1.3f
        }
    }

    companion object {
        private const val MAX_SNAPSHOT_CORRELATION_WINDOW_MS = 3 * 60 * 60 * 1000L
    }

    private  fun createMessageForInsightType(insightType: InsightType): String = when(insightType) {
        InsightType.PHONE_USAGE -> "You tend to miss study sessions after phone usage over 1 hour before scheduled study sessions."
        InsightType.SLEEP -> "You tend to miss study sessions on days of less than 6 hours of sleep."
        InsightType.CONNECTIVITY -> "Days with no internet connectivity often leads to study delays."
        InsightType.WEATHER -> "Certain whether conditions seems to affect your ability to initiate study sessions"
    }

    private fun createMessageForInsightTypeWithTime(insightType: InsightType, timeBucket: TimeBuckets): String {
        val timeLabel = timeBucket.label
        return when (insightType) {
            InsightType.PHONE_USAGE -> "You tend to miss study sessions after high phone usage over 1 hour before scheduled study sessions in the $timeLabel."
            InsightType.SLEEP -> "You tend to miss study sessions in the $timeLabel  on days of less than 6 hours of sleep "
            InsightType.CONNECTIVITY -> "In the $timeLabel, on days with no internet connectivity often leads to study delays."
            InsightType.WEATHER -> "A ${insightType.name.lowercase()} weather seems to affect your ability to initiate study sessions in the $timeLabel."
        }
    }

    private fun createMessageForMultiSignalRule(rule: MultiSignalRule, timeBucket: TimeBuckets): String {
        val timeLabel = timeBucket.label
        return when(rule) {
            MultiSignalRule.PHONE_USAGE_AND_SLEEP ->
                "In the $timeLabel, missed study sessions often follow both high phone usage and less than 5 hours of sleep."
            MultiSignalRule.PHONE_USAGE_AND_CONNECTIVITY ->
                "In the $timeLabel, missed study sessions often follow both high phone usage and no internet connectivity."
            MultiSignalRule.SLEEP_AND_CONNECTIVITY ->
                "In the $timeLabel, missed study sessions often follow both less than 5 hours of sleep and no internet connectivity."
        }
    }
}
