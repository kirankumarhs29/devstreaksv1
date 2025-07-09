package com.dailydevchallenge.devstreaks.repository
//
//import com.dailydevchallenge.devstreaks.model.Challenge
//import kotlinx.datetime.DateTimeUnit
//import kotlinx.datetime.LocalDate
//import kotlinx.datetime.Clock
//import kotlinx.datetime.TimeZone
//import kotlinx.datetime.toLocalDateTime
//import kotlinx.datetime.minus
////import com.dailydevchallenge.app.data.MockChallengeProvider
//
//
//class InMemoryUserProgressRepository : UserProgressRepository {
//    private val completedChallenges = mutableSetOf<String>()
//    private var currentXp = 0
//    private var streak = 0
//    private var lastCompletedDate: LocalDate? = null
//    private var currentDay = 1
//
//    override fun markChallengeCompleted(id: String) {
//        if (completedChallenges.contains(id)) return
//
//        completedChallenges.add(id)
//        currentXp += 50
//
//        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
//        val yesterday = today.minus(1, DateTimeUnit.DAY)
//
//        streak = when (lastCompletedDate) {
//            yesterday -> streak + 1
//            today -> streak // already completed today
//            else -> 1
//        }
//
//        lastCompletedDate = today
//    }
//
//    override fun isChallengeCompleted(id: String) = completedChallenges.contains(id)
//
////    override fun getTodayChallenge(): String = "Today's Challenge" // Placeholder for today's challenge logic
//
//    override fun getXp() = currentXp
//
//    override fun getStreak() = streak
//
//    override fun resetProgress() {
//        completedChallenges.clear()
//        currentXp = 0
//        streak = 0
//        currentDay = 1
//        lastCompletedDate = null
//    }
//
//    override fun advanceDay() {
//        currentDay += 1
//    }
//}
