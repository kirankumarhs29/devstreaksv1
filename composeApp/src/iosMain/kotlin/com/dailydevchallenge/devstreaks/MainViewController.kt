package com.dailydevchallenge.devstreaks

import androidx.compose.ui.window.ComposeUIViewController

//import com.dailydevchallenge.devstreaks.di.appModule
//import com.dailydevchallenge.devstreaks.di.iosModule

fun MainViewController() = ComposeUIViewController {
    // Start Koin only once
    startKoinIfNeeded()
    App()
}

private var koinStarted = false

private fun startKoinIfNeeded() {
    if (!koinStarted) {
//        startKoin {
//            printLogger()
//            modules(iosModule, appModule) // Make sure `appModule` exists in shared code
//        }
        koinStarted = true
    }
}
