package com.dailydevchallenge.devstreaks.features.onboarding

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object OnboardingPreferences {
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    private val settings: Settings = Settings()

    // Backed by Settings value
    private val _onboardingFlow = MutableStateFlow(settings[KEY_ONBOARDING_COMPLETED, false])
    val onboardingFlow: StateFlow<Boolean> = _onboardingFlow

    fun setOnboardingCompleted(completed: Boolean) {
        settings[KEY_ONBOARDING_COMPLETED] = completed
        _onboardingFlow.value = completed
    }

    fun isOnboardingCompleted(): Boolean = settings[KEY_ONBOARDING_COMPLETED, false]
}
