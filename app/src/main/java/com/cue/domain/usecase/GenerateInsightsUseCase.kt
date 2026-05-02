package com.cue.domain.usecase

import com.cue.context.contracts.WhetherSignal
import com.cue.domain.model.ContextSnapshot
import com.cue.domain.model.Insight
import com.cue.domain.model.InsightType
import com.cue.domain.repository.ContextSnapShotRepository
import com.cue.domain.repository.DailyCheckinRepository
import com.cue.domain.repository.InsightRepository
import com.cue.domain.repository.StudySessionRepository
import com.cue.domain.repository.UserRepository
import java.util.Calendar
import kotlin.math.abs


class GenerateInsightsUseCase(
    private val userRepository: UserRepository,
    private val sessionRepository: StudySessionRepository,
    private val checkinRepository: DailyCheckinRepository,
    private val snapshotRepository: ContextSnapShotRepository,
    private val insightRepository: InsightRepository
) {

    data class PatternOccurences(
        var totalFailures: Int = 0,
        var matchingOccurrences: Int = 0
    )


    suspend operator fun invoke() {
        val user = userRepository.getCurrentUser() ?: return
        val checkins = checkinRepository.getAllCheckIns()
        val snapshots = snapshotRepository.getAllSnapshots()
        val sessions = sessionRepository.getAllSessions()

        //get all the timestamps of failed study sessions

        val failureTimestamps = mutableListOf<Long>()

        //add  all manual failed checkins
        failureTimestamps.addAll(checkins.filter { !it.didStudy }.map { it.timestamp })

        //add silent failures to the list

        failureTimestamps.addAll(getSilentFailureTimeStamps(user.weeklySchedule,sessions))

        //initialize the insight type x occurrences map
        val insightTypeOccurrencesMap = mutableMapOf<InsightType, PatternOccurences>().apply{
                InsightType.entries.forEach{
                    put(it, PatternOccurences())
                }
        }


        // analysis loop, relate failures with context

        failureTimestamps.forEach { timestamp ->
            val closestSnapshot = snapshots.minByOrNull { abs(it.timestamp - timestamp) }

            // 1. Phone usage rule
            //count total failures of the phone usage insight
            if (closestSnapshot != null) {
                insightTypeOccurrencesMap[InsightType.PHONE_USAGE]?.let {
                    it.totalFailures++

                    if (closestSnapshot.phoneUsage == "High") {
                        it.matchingOccurrences++
                    }
                }

                //2. connectivity rule
                insightTypeOccurrencesMap[InsightType.CONNECTIVITY]?.let {
                    it.totalFailures++

                    if (closestSnapshot.connectivity == "None") {
                        it.matchingOccurrences++
                    }
                }

                //sleep rule - no sleep api intergrated just yet
                insightTypeOccurrencesMap[InsightType.SLEEP]?.let {
                    it.totalFailures++
                    if(closestSnapshot.sleep < 5) {
                        it.matchingOccurrences++
                    }
                }
            }
        }

        //threshold filter: only save when the insight is relevant(occured atleast 3 times and frequency is above 60%)

        insightTypeOccurrencesMap.forEach {( insightType, occurences) ->
            //if occured more than 3 times get the frequency it ocurred for
            if(occurences.totalFailures >= 3){
                val frequency =   occurences.matchingOccurrences / occurences.totalFailures.toFloat()
                if(frequency >= 0.6f){
                    val message = createMessageForInsightType(insightType)
                    createUniqueInsight(user.id, message, insightType)
                }
            }

        }


        // Rule 1: High Phone Usage before Failure
        val negativeCheckins = checkins.filter { !it.didStudy }
        negativeCheckins.forEach { checkin ->
            val closestSnapshot = snapshots.minByOrNull { abs(it.timestamp - checkin.timestamp) }
            applyRules(user.id, closestSnapshot)
        }
        
        // Silent Failure Detection
        detectSilentFailures(user.id, user.weeklySchedule, sessions, snapshots)
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
                    applyRules(userId, it)
                }
            }
        }
    }

    private suspend fun applyRules(userId: Long, snapshot: com.cue.domain.model.ContextSnapshot?) {
        if (snapshot == null) return

        if (snapshot.phoneUsage == "High") {
            createUniqueInsight(
                userId,
                createMessageForInsightType(InsightType.PHONE_USAGE),
                InsightType.PHONE_USAGE
            )
        }
        
        if (snapshot.sleep < 6) {
            createUniqueInsight(
                userId,
                createMessageForInsightType(InsightType.SLEEP),
                InsightType.SLEEP
            )
        }

        if (snapshot.connectivity == "None") {
            createUniqueInsight(
                userId,
                createMessageForInsightType(InsightType.CONNECTIVITY),
                InsightType.CONNECTIVITY
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
}
