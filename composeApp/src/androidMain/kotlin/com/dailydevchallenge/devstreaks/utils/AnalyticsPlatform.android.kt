package com.dailydevchallenge.devstreaks.utils

import android.os.Bundle
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

actual fun logAnalyticsEvent(name: String, params: Map<String, Any>?) {
    val bundle = Bundle().apply {
        params?.forEach { (key, value) ->
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Double -> putDouble(key, value)
                is Float -> putFloat(key, value)
                is Boolean -> putBoolean(key, value)
                else -> putString(key, value.toString())
            }
        }
    }
    Firebase.analytics.logEvent(name, bundle)
}
