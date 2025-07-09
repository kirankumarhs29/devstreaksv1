// sharedMain/commonMain/kotlin/com/dailydevchallenge/settings/DarkModeSettings.kt
package com.dailydevchallenge.devstreaks.settings

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object DarkModeSettings {
    private const val KEY_DARK_MODE = "dark_mode_enabled"

    private val settings: Settings = Settings() // default implementation

    private val _darkModeFlow = MutableStateFlow(settings.getBoolean(KEY_DARK_MODE, false))
    val darkModeFlow: StateFlow<Boolean> = _darkModeFlow
//    private val _darkModeFlow = MutableStateFlow(true) // <- force default to dark


    fun toggleDarkMode(enabled: Boolean) {
        settings[KEY_DARK_MODE] = enabled
        _darkModeFlow.value = enabled
    }

    fun current(): Boolean = settings.getBoolean(KEY_DARK_MODE, true)
}
