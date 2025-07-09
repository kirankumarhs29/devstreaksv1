package com.dailydevchallenge.devstreaks.utils

actual fun generateUUID(): String = java.util.UUID.randomUUID().toString()
