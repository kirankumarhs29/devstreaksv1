package com.dailydevchallenge.devstreaks.utils

// shared/src/commonMain/kotlin/com/dailydevchallenge/utils/AnalyticsPlatform.kt

expect fun logAnalyticsEvent(name: String, params: Map<String, Any>? = null)
