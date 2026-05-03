package com.cue.domain.usecase

import com.cue.domain.model.Insight
import com.cue.domain.model.InsightType
import com.cue.domain.repository.ContextSnapShotRepository
import com.cue.domain.repository.DailyCheckinRepository
import com.cue.domain.repository.InsightRepository
import com.cue.domain.repository.StudySessionRepository
import com.cue.domain.repository.UserRepository
import java.util.Calendar
import kotlin.collections.forEach
import kotlin.math.abs
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

        /*
        Noise Filtering
         */

        //get the cleaned sessions
        val cleanedSessions = rawSessions.filter { session ->
            val startTime = session.startTime
            val endTime = session.endTime ?: 0L
            val durationMS = endTime - startTime
            val durationMins = durationMS / (1000 * 60)

            //filter 1: ignore sessions with less than 5 minutes duration and > 12 hour duration
            durationMins in 5..(12 * 60)

        }


        /**
         * Occurences + frequency filtering
         */
        //get all the timestamps of failed study sessions
        val failureTimestamps = mutableListOf<Long>()

        //add  all manual failed checkins
        failureTimestamps.addAll(checkins.filter { !it.didStudy }.map { it.timestamp })

        //add silent failures to the list
        failureTimestamps.addAll(getSilentFailureTimeStamps(user.weeklySchedule, cleanedSessions))

        //initialize the insight type x occurrences map
        val insightTypeOccurrencesMap =
            mutableMapOf<Pair<InsightType, TimeBuckets>, PatternOccurences>().apply {
                InsightType.entries.forEach { type ->
                    TimeBuckets.entries.forEach { bucket ->
                        put(Pair(type, bucket), PatternOccurences())
                    }
                }
            }

        val multiSignalOccurrencesMap =
            mutableMapOf<Pair<MultiSignalRule, TimeBuckets>, PatternOccurences>().apply {
                MultiSignalRule.entries.forEach { rule ->
                    TimeBuckets.entries.forEach { bucket ->
                        put(Pair(rule, bucket), PatternOccurences())
                    }
                }
            }

        // analysis loop, relate failures with context
        failureTimestamps.forEach { timestamp ->
            val closestSnapshot = rawSnapshots
                .minByOrNull { abs(it.timestamp - timestamp) }
                ?.takeIf { abs(it.timestamp - timestamp) <= MAX_SNAPSHOT_CORRELATION_WINDOW_MS }
            val timeBucket = getTimeBucket(timestamp)
            // 1. Phone usage rule
            //count total failures of the phone usage insight
            if (closestSnapshot != null) {
                val hasPhoneUsage = closestSnapshot.phoneUsage != "UNKNOWN"
                val hasConnectivity = closestSnapshot.connectivity != "UNKNOWN"
                val hasSleep = closestSnapshot.sleep in 1..18
                val phoneUsageMatched = closestSnapshot.phoneUsage == "High"
                val connectivityMatched = closestSnapshot.connectivity == "None"
                val sleepMatched = closestSnapshot.sleep < 5

                if (hasPhoneUsage) {
                    insightTypeOccurrencesMap[Pair(InsightType.PHONE_USAGE, timeBucket)]?.let {
                        it.totalFailures++

                        if (phoneUsageMatched) {
                            it.matchingOccurrences++
                        }
                    }
                }

                //2. connectivity rule
                if (hasConnectivity) {
                    insightTypeOccurrencesMap[Pair(InsightType.CONNECTIVITY, timeBucket)]?.let {
                        it.totalFailures++

                        if (connectivityMatched) {
                            it.matchingOccurrences++
                        }
                    }
                }

                //whether signal
                if (closestSnapshot.weather != "UNKNOWN") {
                    insightTypeOccurrencesMap[Pair(InsightType.WEATHER, timeBucket)]?.let {
                        it.totalFailures++
                        if (closestSnapshot.weather == "Rainy") {
                            it.matchingOccurrences++
                        }
                    }
                }

                //sleep rule - no sleep api integrated just yet
                if (hasSleep) {
                    insightTypeOccurrencesMap[Pair(InsightType.SLEEP, timeBucket)]?.let {
                        it.totalFailures++
                        if (sleepMatched) {
                            it.matchingOccurrences++
                        }
                    }
                }

                updateMultiSignalOccurrences(
                    occurrences = multiSignalOccurrencesMap[Pair(MultiSignalRule.PHONE_USAGE_AND_SLEEP, timeBucket)],
                    hasAllSignals = hasPhoneUsage && hasSleep,
                    allSignalsMatch = phoneUsageMatched && sleepMatched
                )
                updateMultiSignalOccurrences(
                    occurrences = multiSignalOccurrencesMap[Pair(MultiSignalRule.PHONE_USAGE_AND_CONNECTIVITY, timeBucket)],
                    hasAllSignals = hasPhoneUsage && hasConnectivity,
                    allSignalsMatch = phoneUsageMatched && connectivityMatched
                )
                updateMultiSignalOccurrences(
                    occurrences = multiSignalOccurrencesMap[Pair(MultiSignalRule.SLEEP_AND_CONNECTIVITY, timeBucket)],
                    hasAllSignals = hasSleep && hasConnectivity,
                    allSignalsMatch = sleepMatched && connectivityMatched
                )
            }
        }

        // list of insights that meet the 0.6 confidence score
        val insightCandidates  = mutableListOf<InsightCandidate>()

        //threshold filter: only save when the insight is relevant(occured at least 3 times and frequency is above 60%)
        //confidence scoring
        // formula: cs = (frequency * 0.5) + (occurencWeight * 0.3) + (consistency * 0.2)
        //          occurenceWeight = min(totalOccurences/10,1.0)
        //          consistency = if frequency > 0.8 then 1.0 else 0.5
        insightTypeOccurrencesMap.forEach {( key, occurences) -> val (type,timeBucket) = key
            //if occured more than 3 times get the frequency it ocurred for
            if(occurences.totalFailures >= 3){
                val frequency =   occurences.matchingOccurrences / occurences.totalFailures.toFloat()
                if(frequency >= 0.6f){
                    // occurence weight : more data = more weight
                    val occurenceWeight = minOf(occurences.totalFailures / 10f, 1.0f)

                    //consistency : higher frequency = higher confidence
                    val consistency = if (frequency > 0.8f) 1.0f else 0.5f

                    val cs = (frequency * 0.5f) + (occurenceWeight * 0.3f) + (consistency * 0.2f)
                    val message = createMessageForInsightTypeWithTime(type,timeBucket)
                    if(cs >= 0.6f) {
                        //insight prioritization: calculate priorityScore
                        val impactWeight = getInsightImpactWeightForType(type)
                        val priorityScore = cs * impactWeight
                        insightCandidates.add(InsightCandidate(type, message, cs,priorityScore))
                    }

                }
            }

        }

        multiSignalOccurrencesMap.forEach { (key, occurences) ->
            val (rule, timeBucket) = key
            if (occurences.totalFailures >= 3) {
                val frequency = occurences.matchingOccurrences / occurences.totalFailures.toFloat()
                if (frequency >= 0.6f) {
                    val occurenceWeight = minOf(occurences.totalFailures / 10f, 1.0f)
                    val consistency = if (frequency > 0.8f) 1.0f else 0.5f
                    val cs = (frequency * 0.5f) + (occurenceWeight * 0.3f) + (consistency * 0.2f)

                    if (cs >= 0.6f) {
                        val message = createMessageForMultiSignalRule(rule, timeBucket)
                        val priorityScore = cs * getMultiSignalImpactWeight(rule)
                        insightCandidates.add(
                            InsightCandidate(
                                type = rule.primaryType,
                                message = message,
                                confidenceScore = cs,
                                priorityScore = priorityScore
                            )
                        )
                    }
                }
            }
        }

        //sort the insight candidates by priority score and take the top 3
        insightCandidates.sortByDescending { it.priorityScore }
        val top3Insights = insightCandidates.take(3)

        //add the top 3 to the insights table
        top3Insights.forEach {
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
