package com.dailydevchallenge.devstreaks

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.FirebaseApp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.graphics.toArgb
import com.dailydevchallenge.devstreaks.session.initSessionManager
import com.dailydevchallenge.devstreaks.settings.DarkModeSettings
import com.dailydevchallenge.devstreaks.settings.initSettings
import com.dailydevchallenge.devstreaks.theme.DevStreakTheme
import com.dailydevchallenge.devstreaks.utils.initLogger
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import org.koin.compose.KoinContext


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Override splash theme with actual app theme
        setTheme(R.style.Theme_DevStreak)

        enableEdgeToEdge()
        installSplashScreen()
        Firebase.analytics

//        initLogger(this)
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        initLogger(this)
        initSessionManager(applicationContext)
        initSettings(this)


        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        val launchDestination = intent?.getStringExtra("navigateTo")
       // getLogger().d("MainActivity", "MainActivity created!")


        setContent {
            KoinContext {
                DevStreakTheme {
                    // Only now we are inside a composable context
                    val view = window.decorView
                    val isDark = DarkModeSettings.darkModeFlow.collectAsState().value
                    val color = MaterialTheme.colorScheme.background.toArgb()

                    window.setDecorFitsSystemWindows(false) // for edge-to-edge
                    WindowInsetsControllerCompat(window, view).apply {
                        isAppearanceLightStatusBars = !isDark
                        isAppearanceLightNavigationBars = !isDark
                    }

                    // Deprecated, but still fallback for API < 33
                    @Suppress("DEPRECATION")
                    window.statusBarColor = color
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = color

                    App(launchDestination = launchDestination)
                }
            }

        }

    }
}


@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
