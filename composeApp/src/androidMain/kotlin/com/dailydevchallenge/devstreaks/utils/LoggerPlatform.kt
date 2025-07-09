package com.dailydevchallenge.devstreaks.utils

import android.content.Context
import com.dailydevchallenge.devstreaks.logger.DevLogger
import com.dailydevchallenge.devstreaks.logger.AndroidDevLogger

private lateinit var appContext: Context

actual fun getLogger(): DevLogger = AndroidDevLogger(appContext)

fun initLogger(context: Context) {
    appContext = context.applicationContext
}
