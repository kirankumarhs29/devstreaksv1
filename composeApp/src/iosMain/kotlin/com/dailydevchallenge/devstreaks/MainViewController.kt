package com.dailydevchallenge.devstreaks

import androidx.compose.ui.window.ComposeUIViewController
import com.dailydevchallenge.devstreaks.di.appModule
import com.dailydevchallenge.devstreaks.di.iosModule
import org.koin.compose.KoinContext
import org.koin.core.context.startKoin
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter


//fun MainViewController() = ComposeUIViewController { App() }
fun MainViewController() = ComposeUIViewController {

    startKoinIfNeeded()
    requestNotificationPermission()
    KoinContext {
        App()
    }


}

    private var koinStarted = false

    private fun startKoinIfNeeded() {
        if (!koinStarted) {
        startKoin {
            printLogger()
//            initKoin()
            modules(iosModule, appModule) // Make sure `appModule` exists in shared code
        }
            koinStarted = true
        }
    }

private fun requestNotificationPermission() {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    center.requestAuthorizationWithOptions(
        options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
        completionHandler = { granted, error ->
            println("🔔 Notification permission granted: $granted")
            error?.let { println("⚠️ Error requesting permission: ${it.localizedDescription}") }
        }
    )
    center.delegate = null // Optional: if you want to handle foreground notifications later
}




