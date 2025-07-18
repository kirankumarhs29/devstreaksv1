package com.dailydevchallenge.devstreaks.di

import android.content.Context
import com.dailydevchallenge.devstreaks.database.DatabaseDriverFactory
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfilePreferences
import com.dailydevchallenge.devstreaks.notification.AndroidNotificationScheduler
import com.dailydevchallenge.devstreaks.notification.NotificationScheduler
import com.dailydevchallenge.devstreaks.notification.getNotificationScheduler
import com.dailydevchallenge.devstreaks.tts.AndroidSpeechToTextHelper
import com.dailydevchallenge.devstreaks.tts.SpeechToTextHelper
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.dsl.module

fun platformModule(appContext: Context) = module {
    single { DatabaseDriverFactory(appContext) }
    single<NotificationScheduler> { AndroidNotificationScheduler(appContext) }
    single<NotificationScheduler> { getNotificationScheduler() }
    single<Settings> {
        SharedPreferencesSettings(
            appContext.getSharedPreferences("DevStreakPrefs", Context.MODE_PRIVATE)
        )
    }
    single { LearningProfilePreferences }
    single<SpeechToTextHelper> { AndroidSpeechToTextHelper(get()) }


}