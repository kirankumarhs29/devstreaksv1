package com.dailydevchallenge.devstreaks.utils

import platform.Foundation.*


actual fun logAnalyticsEvent(name: String, params: Map<String, Any>?) {
    val nsDict = params?.let {
        val dict = NSMutableDictionary()
        it.forEach { (key, value) ->
            dict.setValue(value, forKey = key)
        }
        dict
    }
}
