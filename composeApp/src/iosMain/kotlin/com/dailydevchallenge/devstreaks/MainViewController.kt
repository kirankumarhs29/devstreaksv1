package com.dailydevchallenge.devstreaks

import androidx.compose.ui.window.ComposeUIViewController
import com.dailydevchallenge.devstreaks.di.appModule
import com.dailydevchallenge.devstreaks.di.iosModule
import com.dailydevchallenge.devstreaks.di.databaseModule
import com.dailydevchallenge.devstreaks.di.repositoryModule
import com.dailydevchallenge.devstreaks.di.serviceModule
import com.dailydevchallenge.devstreaks.di.viewModelModule
import com.dailydevchallenge.devstreaks.di.sharedModule
import com.dailydevchallenge.devstreaks.llm.llmModule
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
            modules(
                iosModule,
                appModule,
                databaseModule,
                repositoryModule,
                llmModule,
                serviceModule,
                viewModelModule,
                sharedModule
            )
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
