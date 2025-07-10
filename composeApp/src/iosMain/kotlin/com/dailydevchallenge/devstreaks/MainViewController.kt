package com.dailydevchallenge.devstreaks

import androidx.compose.ui.window.ComposeUIViewController



fun MainViewController() = ComposeUIViewController { App() }

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





