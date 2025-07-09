package com.dailydevchallenge.devstreaks.utils

actual object PlatformUtils {
    actual fun isAndroid(): Boolean = false

    // No-op function for commonMain compatibility. Actual logic is in the composable below.
    actual fun pickPdfAndExtract(onExtracted: (String) -> Unit) {
        // This should be triggered from the Composable below
    }
}