package com.dailydevchallenge.devstreaks.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.dailydevchallenge.devstreaks.settings.DarkModeSettings

// Semantic color tokens
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val achievement: Color,
    val onAchievement: Color,
    val achievementContainer: Color,
    val onAchievementContainer: Color,
)

private val LightExtendedColors = ExtendedColors(
    success = Color(0xFF2E7D32),
    onSuccess = Color.White,
    successContainer = Color(0xFFE8F5E9),
    onSuccessContainer = Color(0xFF1B5E20),
    warning = Color(0xFFF57C00),
    onWarning = Color.White,
    warningContainer = Color(0xFFFFF3E0),
    onWarningContainer = Color(0xFFE65100),
    info = Color(0xFF1976D2),
    onInfo = Color.White,
    infoContainer = Color(0xFFE3F2FD),
    onInfoContainer = Color(0xFF0D47A1),
    achievement = Color(0xFFFFB300),
    onAchievement = Color.Black,
    achievementContainer = Color(0xFFFFF8E1),
    onAchievementContainer = Color(0xFFFF8F00),
)

private val DarkExtendedColors = ExtendedColors(
    success = Color(0xFF4CAF50),
    onSuccess = Color.Black,
    successContainer = Color(0xFF2E5E32),
    onSuccessContainer = Color(0xFFC8E6C9),
    warning = Color(0xFFFF9800),
    onWarning = Color.Black,
    warningContainer = Color(0xFF5D4037),
    onWarningContainer = Color(0xFFFFE0B2),
    info = Color(0xFF2196F3),
    onInfo = Color.Black,
    infoContainer = Color(0xFF1565C0),
    onInfoContainer = Color(0xFFBBDEFB),
    achievement = Color(0xFFFFD700),
    onAchievement = Color.Black,
    achievementContainer = Color(0xFF6D4C00),
    onAchievementContainer = Color(0xFFFFF59D),
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

// Main Material3 color schemes
private val LightColors = lightColorScheme(
    primary = Color(0xFF3366FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9EEFF),
    onPrimaryContainer = Color(0xFF1A237E),
    secondary = Color(0xFF6D9886),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F3ED),
    onSecondaryContainer = Color(0xFF2F4F4F),
    tertiary = Color(0xFF7C4DFF),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE7FF),
    onTertiaryContainer = Color(0xFF4527A0),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFDFDFC),
    onBackground = Color(0xFF1C1C1C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1C1C),
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFF44464F),
    outline = Color(0xFF757575),
    outlineVariant = Color(0xFFE0E0E0),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF313131),
    inverseOnSurface = Color(0xFFF5F5F5),
    inversePrimary = Color(0xFF91B4F3),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF91B4F3),
    onPrimary = Color(0xFF0A1F3A),
    primaryContainer = Color(0xFF2B3D5B),
    onPrimaryContainer = Color(0xFFD0E2FF),
    secondary = Color(0xFF9BD1B2),
    onSecondary = Color(0xFF1A3A2B),
    secondaryContainer = Color(0xFF2C463B),
    onSecondaryContainer = Color(0xFFE1F8EA),
    tertiary = Color(0xFFB794FF),
    onTertiary = Color(0xFF2A1A4A),
    tertiaryContainer = Color(0xFF3F2A5F),
    onTertiaryContainer = Color(0xFFE7D9FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121212),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFD8D8D8),
    surfaceVariant = Color(0xFF2E2E30),
    onSurfaceVariant = Color(0xFFCFCFD1),
    outline = Color(0xFF9E9E9E),
    outlineVariant = Color(0xFF424242),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE6E6E6),
    inverseOnSurface = Color(0xFF313131),
    inversePrimary = Color(0xFF3366FF),
)

@Composable
fun DevStreakTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val isDark by DarkModeSettings.darkModeFlow.collectAsState(initial = darkTheme)
    val colors = if (isDark) DarkColors else LightColors
    val extendedColors = if (isDark) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = Typography(),
            shapes = Shapes(),
            content = content
        )
    }
}

// Extension properties for easy access to semantic colors
val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    get() = LocalExtendedColors.current
