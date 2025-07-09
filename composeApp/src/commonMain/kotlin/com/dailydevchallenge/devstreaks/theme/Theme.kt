package com.dailydevchallenge.devstreaks.theme


import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
//import com.dailydevchallenge.devstreaks.settings.DarkModeSettings

private val LightColors = lightColorScheme(
    primary = Color(0xFF3366FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9EEFF),
    onPrimaryContainer = Color(0xFF1A237E),
    secondary = Color(0xFF6D9886),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F3ED),
    onSecondaryContainer = Color(0xFF2F4F4F),
    background = Color(0xFFFDFDFC),
    onBackground = Color(0xFF1C1C1C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1C1C),
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFF44464F)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF91B4F3),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF2B3D5B),
    onPrimaryContainer = Color(0xFFD0E2FF),
    secondary = Color(0xFF9BD1B2),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2C463B),
    onSecondaryContainer = Color(0xFFE1F8EA),
    background = Color(0xFF121212),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFD8D8D8),
    surfaceVariant = Color(0xFF2E2E30),
    onSurfaceVariant = Color(0xFFCFCFD1)
)

@Composable
fun DevStreakTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
//    val isDark by DarkModeSettings.darkModeFlow.collectAsState()
    val colors = if (false) DarkColors else LightColors

        MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}
