package com.dailydevchallenge.devstreaks.settings

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object UserPreferences {
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_EMAIL_ID = "email_id"  // Fixed: was "user_id"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_REMINDER_HOUR = "reminder_hour"
    private const val KEY_REMINDER_MINUTE = "reminder_minute"
    private const val LATESTRESUMEANALYSIS = "resume_analysis_id"
    // store pending request for course Gemini or OpenAI
    private const val KEY_PENDING_REQUEST = "pending_request"
    private const val KEY_AVATAR_URL = "avatar_url"

    // Learning Intent Keys
    private const val KEY_PRIMARY_GOAL = "primary_goal"
    private const val KEY_SKILL_FOCUS = "skill_focus"
    private const val KEY_CAREER_TRACK = "career_track"
    private const val KEY_EXPERIENCE = "experience"
    private const val KEY_TIME_PER_DAY = "time_per_day"
    private const val KEY_LEARNING_STYLE = "learning_style"
    private const val KEY_FEARS = "fears"
    private const val KEY_MOTIVATIONS = "motivations"

    // Onboarding Keys
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    private const val KEY_PENDING_REQUEST_ID = "pending_request_id"

    private val settings: Settings = Settings()

    fun setLoggedIn(loggedIn: Boolean) {
        settings[KEY_IS_LOGGED_IN] = loggedIn
    }

    fun isLoggedIn(): Boolean {
        return settings[KEY_IS_LOGGED_IN, false]
    }

    fun setUserId(id: String) {
        settings[KEY_USER_ID] = id
    }
    fun setEmailId(email: String) {
        settings[KEY_EMAIL_ID] = email
    }

    private fun getUserId(): String? {
        return settings.getStringOrNull(KEY_USER_ID)
    }
    fun getEmailId(): String? {
        return settings.getStringOrNull(KEY_EMAIL_ID)
    }
    fun setNotificationsEnabled(enabled: Boolean) {
        settings[KEY_NOTIFICATIONS_ENABLED] = enabled
    }

    fun isNotificationsEnabled(): Boolean {
        return settings[KEY_NOTIFICATIONS_ENABLED, true]
    }

    fun setReminderTime(hour: Int, minute: Int) {
        settings[KEY_REMINDER_HOUR] = hour
        settings[KEY_REMINDER_MINUTE] = minute
    }

    fun getReminderTime(): Pair<Int, Int> {
        val hour = settings[KEY_REMINDER_HOUR, 9]
        val minute = settings[KEY_REMINDER_MINUTE, 0]
        return hour to minute
    }

    fun logout() {
        settings[KEY_IS_LOGGED_IN] = false
        settings.remove(KEY_USER_ID)
        settings.remove(KEY_EMAIL_ID)
        settings.remove(KEY_NOTIFICATIONS_ENABLED)
        settings.remove(KEY_AVATAR_URL) // Clear avatar URL on logout
    }
    fun getSafeUserId(): String {
        return getUserId() ?: throw IllegalStateException("User ID not found in preferences")
    }
    fun getGetLatestResume(): String? {
        return settings.getStringOrNull(LATESTRESUMEANALYSIS)
    }
    fun savePendingRequest(request: String) {
        settings[KEY_PENDING_REQUEST] = request
    }
    fun getPendingRequest(): String? {
        return settings.getStringOrNull(KEY_PENDING_REQUEST)
    }
    fun savePendingRequestId(requestId: String) {
        val currentRequest = getPendingRequest() ?: ""
        settings[KEY_PENDING_REQUEST] = "$currentRequest/$requestId"
    }
    fun getPendingRequestId(): String? {
        val raw = settings.getStringOrNull(KEY_PENDING_REQUEST) ?: return null
        return try {
            kotlinx.serialization.json.Json.parseToJsonElement(raw)
                .jsonObject["requestId"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    fun clearPendingRequest() {
        settings.remove(KEY_PENDING_REQUEST)
    }

    fun setAvatarUrl(avatarUrl: String?) {
        if (avatarUrl.isNullOrBlank()) {
            settings.remove(KEY_AVATAR_URL)
        } else {
            settings[KEY_AVATAR_URL] = avatarUrl
        }
    }

    fun getAvatarUrl(): String? {
        return settings.getStringOrNull(KEY_AVATAR_URL)
    }

    // Learning Intent Methods
    fun setUserLearningIntent(
        primaryGoal: String,
        skillFocus: String,
        careerTrack: String,
        experience: String,
        timePerDay: Int,
        learningStyle: String,
        fears: String,
        motivations: String
    ) {
        settings[KEY_PRIMARY_GOAL] = primaryGoal
        settings[KEY_SKILL_FOCUS] = skillFocus
        settings[KEY_CAREER_TRACK] = careerTrack
        settings[KEY_EXPERIENCE] = experience
        settings[KEY_TIME_PER_DAY] = timePerDay
        settings[KEY_LEARNING_STYLE] = learningStyle
        settings[KEY_FEARS] = fears
        settings[KEY_MOTIVATIONS] = motivations
    }

    fun getUserPrimaryGoal(): String? = settings.getStringOrNull(KEY_PRIMARY_GOAL)
    fun getUserSkillFocus(): String? = settings.getStringOrNull(KEY_SKILL_FOCUS)
    fun getUserCareerTrack(): String? = settings.getStringOrNull(KEY_CAREER_TRACK)
    fun getUserExperience(): String? = settings.getStringOrNull(KEY_EXPERIENCE)
    fun getUserTimePerDay(): Int? = settings.getIntOrNull(KEY_TIME_PER_DAY)
    fun getUserLearningStyle(): String? = settings.getStringOrNull(KEY_LEARNING_STYLE)
    fun getUserFears(): String? = settings.getStringOrNull(KEY_FEARS)
    fun getUserMotivations(): String? = settings.getStringOrNull(KEY_MOTIVATIONS)

    // Individual setter methods for user preferences
    fun setUserPrimaryGoal(value: String) = settings.set(KEY_PRIMARY_GOAL, value)
    fun setUserSkillFocus(value: String) = settings.set(KEY_SKILL_FOCUS, value)
    fun setUserCareerTrack(value: String) = settings.set(KEY_CAREER_TRACK, value)
    fun setUserExperience(value: String) = settings.set(KEY_EXPERIENCE, value)
    fun setUserTimePerDay(value: Int) = settings.set(KEY_TIME_PER_DAY, value)
    fun setUserLearningStyle(value: String) = settings.set(KEY_LEARNING_STYLE, value)
    fun setUserFears(value: String) = settings.set(KEY_FEARS, value)
    fun setUserMotivations(value: String) = settings.set(KEY_MOTIVATIONS, value)

    // Onboarding Methods
    fun hasCompletedOnboarding(): Boolean = settings.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    fun setOnboardingCompleted(completed: Boolean) = settings.set(KEY_ONBOARDING_COMPLETED, completed)

    fun setPendingRequestId(requestId: String) = settings.set(KEY_PENDING_REQUEST_ID, requestId)
    fun getPendingRequestIdSimple(): String? = settings.getStringOrNull(KEY_PENDING_REQUEST_ID)

}
