package com.cue.domain.usecase

import com.cue.context.contracts.WhetherSignal
import com.cue.domain.model.ContextSnapshot
import com.cue.domain.model.Insight
import com.cue.domain.model.InsightType
import com.cue.domain.model.SessionStatus
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

enum class TimeBuckets(val label:String)
{
    MORNING("Morning"),
    AFTERNOON("Afternoon"),
    EVENING("Evening"),
    NIGHT("Night")
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


        // analysis loop, relate failures with context
        failureTimestamps.forEach { timestamp ->
            val closestSnapshot = rawSnapshots.minByOrNull { abs(it.timestamp - timestamp) }
            val timeBucket = getTimeBucket(timestamp)
            // 1. Phone usage rule
            //count total failures of the phone usage insight
            if (closestSnapshot != null) {
                if (closestSnapshot.phoneUsage != "UNKNOWN") {
                    insightTypeOccurrencesMap[Pair(InsightType.PHONE_USAGE, timeBucket)]?.let {
                        it.totalFailures++

                        if (closestSnapshot.phoneUsage == "High") {
                            it.matchingOccurrences++
                        }
                    }
                }

                //2. connectivity rule
                if (closestSnapshot.connectivity != "UNKNOWN") {
                    insightTypeOccurrencesMap[Pair(InsightType.CONNECTIVITY, timeBucket)]?.let {
                        it.totalFailures++

                        if (closestSnapshot.connectivity == "None") {
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
                if (closestSnapshot.sleep in 1..18) {
                    insightTypeOccurrencesMap[Pair(InsightType.SLEEP, timeBucket)]?.let {
                        it.totalFailures++
                        if (closestSnapshot.sleep < 5) {
                            it.matchingOccurrences++
                        }
                    }
                }
            }
        }

        //

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
                    createUniqueInsight(user.id, message, type)
                }
            }

        }

        val negativeCheckins = checkins.filter { !it.didStudy }
        negativeCheckins.forEach { checkin ->
            val closestSnapshot = rawSnapshots.minByOrNull { abs(it.timestamp - checkin.timestamp) }
            val timeBucket = getTimeBucket(checkin.timestamp)
            applyRules(user.id, closestSnapshot,timeBucket)
        }
        
        // Silent Failure Detection
        detectSilentFailures(user.id, user.weeklySchedule, rawSessions, rawSnapshots)
    }

    private suspend fun detectSilentFailures(
        userId: Long,
        schedule: List<com.cue.domain.model.DaySchedule>,
        sessions: List<com.cue.domain.model.StudySession>,
        snapshots: List<com.cue.domain.model.ContextSnapshot>
    ) {
        // Look at the last 7 days
        val now = Calendar.getInstance()
        for (i in 0 until 7) {
            val date = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dayOfWeek = getDayOfWeekInt(date)

            val scheduledDay = schedule.find { it.dayOfWeek == dayOfWeek } ?: continue
            
            // If we had a scheduled session but no study session was started that day
            val sessionsToday = sessions.filter { 
                val sessionDate = Calendar.getInstance().apply { timeInMillis = it.startTime }
                sessionDate.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
            }

            if (sessionsToday.isEmpty()) {
                // Find ghost snapshot for this day
                val ghostSnapshot = snapshots.find {
                    val snapDate = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    snapDate.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR) && it.sessionId == null
                }
                
                ghostSnapshot?.let {
                    val timeBucket = getTimeBucket(it.timestamp)
                    applyRules(userId, it, timeBucket)
                }
            }
        }
    }

    private suspend fun applyRules(userId: Long, snapshot: com.cue.domain.model.ContextSnapshot?, timeBucket: TimeBuckets) {
        if (snapshot == null) return

        if (snapshot.phoneUsage == "High") {
            createUniqueInsight(
                userId,
                createMessageForInsightTypeWithTime(InsightType.PHONE_USAGE, timeBucket),
                InsightType.PHONE_USAGE
            )
        }
        
        if (snapshot.sleep < 6) {
            createUniqueInsight(
                userId,
                createMessageForInsightTypeWithTime(InsightType.SLEEP,timeBucket),
                InsightType.SLEEP
            )
        }

        if (snapshot.connectivity == "None") {
            createUniqueInsight(
                userId,
                createMessageForInsightTypeWithTime(InsightType.CONNECTIVITY,timeBucket),
                InsightType.CONNECTIVITY
            )
        }
    }

    // modify logic to prevent insight overwriting, but instead everytime a pattern meets a threshold: (0.6 confidence),
    // always insert a new row in the Insight table
    // this will enable us to see how the insight behaved by keeping a "log of it"
    // this is used for history preservation

    private suspend fun createOrUpdateInsight(userId: Long, message: String, type: InsightType, confidence: Float) {
        val existing = insightRepository.getUserInsights(userId)
        val existingInsight = existing?.find { it.type == type }

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
                    id = existingInsight.id,
                    userId = userId,
                    message = message,
                    type = type,
                    timestamp = System.currentTimeMillis(),
                    confidenceScore = confidence
                )
            )
        }
    }
    private suspend fun createUniqueInsight(userId: Long, message: String, type: InsightType) {
        val existing = insightRepository.getUserInsights(userId)
        if (existing!!.none { it.type == type }) {
            insightRepository.insertInsight(
                Insight(
                    userId = userId,
                    message = message,
                    type = type,
                    timestamp = System.currentTimeMillis()
                )
            )
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
            in 18..22 -> TimeBuckets.EVENING
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
}
