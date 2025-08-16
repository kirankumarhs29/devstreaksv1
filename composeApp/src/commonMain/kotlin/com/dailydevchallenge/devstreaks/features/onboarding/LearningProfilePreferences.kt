package com.dailydevchallenge.devstreaks.features.onboarding

import com.dailydevchallenge.devstreaks.settings.getSettings
import com.russhwolf.settings.Settings

data class LearningProfile(
    val goal: String,
    val skills: List<String>,
    val experience: String,
    val timePerDay: String,
    val days: String,
    val style: String,
    val fear: String

)

object LearningProfilePreferences {
    private val settings: Settings = getSettings()

    private const val KEY_GOAL = "goal"
    private const val KEY_SKILLS = "skills"
    private const val KEY_EXPERIENCE = "experience"
    private const val KEY_TIME_PER_DAY = "time_per_day"
    private const val KEY_HAS_NEW_CHALLENGE = "has_new_challenge"
    private const val KEY_DAYS = "days"
    private const val KEY_STYLE = "style"
    private const val KEY_FEAR = "fear"


    fun setNewChallengeAvailable(value: Boolean) {
        settings.putBoolean(KEY_HAS_NEW_CHALLENGE, value)
    }

    fun isNewChallengeAvailable(): Boolean {
        return settings.getBoolean(KEY_HAS_NEW_CHALLENGE, false)
    }


    fun saveProfile(profile: LearningProfile) {
        settings.putString(KEY_GOAL, profile.goal)
        settings.putString(KEY_SKILLS, profile.skills.joinToString(","))
        settings.putString(KEY_EXPERIENCE, profile.experience)
        settings.putString(KEY_TIME_PER_DAY, profile.timePerDay)
        settings.putString(KEY_DAYS, profile.days)
        settings.putString(KEY_STYLE, profile.style)
        settings.putString(KEY_FEAR, profile.fear)
    }
    fun clearProfile() {
        settings.remove(KEY_GOAL)
        settings.remove(KEY_SKILLS)
        settings.remove(KEY_EXPERIENCE)
        settings.remove(KEY_TIME_PER_DAY)
        settings.remove(KEY_DAYS)
        settings.remove(KEY_STYLE)
        settings.remove(KEY_FEAR)
    }

    fun getProfile(): LearningProfile? {
        val goal = settings.getString(KEY_GOAL, "Kotlin development") ?: return null // No goal
        val skills = settings.getString(KEY_SKILLS, "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        val experience = settings.getString(KEY_EXPERIENCE, "") ?: ""
        val timePerDay = settings.getString(KEY_TIME_PER_DAY, "") ?: ""
        val days = settings.getString(KEY_DAYS, "") ?: ""
        val style = settings.getString(KEY_STYLE, "") ?: ""
        val fear = settings.getString(KEY_FEAR, "") ?: ""

        return LearningProfile(
            goal = goal,
            skills = skills,
            experience = experience,
            timePerDay = timePerDay,
            days = days,
            style = style,
            fear = fear
        )
    }
    fun isOnboardingCompleted(): Boolean {
        return settings.getBoolean("onboarding_completed", false)
    }
    fun setOnboardingCompleted(completed: Boolean) {
        settings.putBoolean("onboarding_completed", completed)
    }
    fun clearOnboarding() {
        settings.remove("onboarding_completed")
        clearProfile()
    }



}
