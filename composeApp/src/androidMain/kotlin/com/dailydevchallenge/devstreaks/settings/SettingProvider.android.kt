package com.dailydevchallenge.devstreaks.settings


import com.russhwolf.settings.Settings
import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings

private lateinit var appContext: Context

fun initSettings(context: Context) {
    appContext = context.applicationContext
}

actual fun getSettings(): Settings {
    val sharedPreferences = appContext.getSharedPreferences("learning_profile_prefs", Context.MODE_PRIVATE)
    return SharedPreferencesSettings(sharedPreferences)
}