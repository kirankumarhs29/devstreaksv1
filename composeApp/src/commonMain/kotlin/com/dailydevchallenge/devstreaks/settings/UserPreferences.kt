package com.dailydevchallenge.devstreaks.settings

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set

object UserPreferences {
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_REMINDER_HOUR = "reminder_hour"
    private const val KEY_REMINDER_MINUTE = "reminder_minute"


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

    private fun getUserId(): String? {
        return settings.getStringOrNull(KEY_USER_ID)
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
        settings.remove(KEY_NOTIFICATIONS_ENABLED)
    }
    fun getSafeUserId(): String {
        return getUserId() ?: throw IllegalStateException("User ID not found in preferences")
    }

}




