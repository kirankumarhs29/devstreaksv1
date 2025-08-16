package com.dailydevchallenge.devstreaks.di


import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfilePreferences
import org.koin.dsl.module

val sharedModule = module {
    // Learning Profile Preferences - now properly configured since Settings dependency is available
    single { LearningProfilePreferences } // depends on Settings from platform modules
}
