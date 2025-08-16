package com.dailydevchallenge.devstreaks

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.FirebaseApp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dailydevchallenge.devstreaks.session.initSessionManager
import com.dailydevchallenge.devstreaks.settings.DarkModeSettings
import com.dailydevchallenge.devstreaks.settings.initSettings
import com.dailydevchallenge.devstreaks.theme.DevStreakTheme
import com.dailydevchallenge.devstreaks.utils.initLogger
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import org.koin.compose.KoinContext


class MainActivity : ComponentActivity() {
    // Register the permission launcher
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, you can proceed with notifications
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            // Permission denied, handle accordingly
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Register the microphone permission launcher
    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Microphone permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Microphone permission denied. Speech-to-text features won't work.", Toast.LENGTH_LONG).show()
        }
    }

    // Function to request notification permission
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission is already granted
                    Toast.makeText(this, "Notification permission already granted", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // Request the permission
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
    // Function to request microphone permission (modern approach)
    private fun requestMicrophonePermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted
                return
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.RECORD_AUDIO
            ) -> {
                // Show rationale and request permission
                Toast.makeText(this, "Microphone access is needed for speech-to-text features", Toast.LENGTH_LONG).show()
                microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            else -> {
                // Request the permission directly
                microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Updated function to check if the microphone permission is granted (fixed API level check)
    private fun isMicrophonePermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Public method to check microphone permission (can be called from anywhere in the app)
    fun checkMicrophonePermission(): Boolean {
        return isMicrophonePermissionGranted()
    }

    // Public method to request microphone permission (can be called from anywhere in the app)
    fun requestMicrophonePermissionIfNeeded() {
        if (!isMicrophonePermissionGranted()) {
            requestMicrophonePermission()
        }
    }
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Override splash theme with actual app theme
        setTheme(R.style.Theme_DevStreak)

        enableEdgeToEdge()
        installSplashScreen()
        Firebase.analytics

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        initLogger(this)
        initSessionManager(applicationContext)
        initSettings(this)

        // Initialize image cache for persistent avatar storage
        com.dailydevchallenge.devstreaks.cache.initializeImageCache(this)

        // Request notification permission using modern approach
        requestNotificationPermission()

        // Request microphone permission using modern approach
        requestMicrophonePermissionIfNeeded()

        val launchDestination = intent?.getStringExtra("navigateTo")

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
