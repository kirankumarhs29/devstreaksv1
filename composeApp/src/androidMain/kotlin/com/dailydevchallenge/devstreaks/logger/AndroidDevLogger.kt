package com.dailydevchallenge.devstreaks.logger

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

class AndroidDevLogger(private val context: Context) : DevLogger {
    private val logFile: File = File(context.filesDir, "devstreak_log.txt")
    private val defaultTag = "DevLogger"

    private fun logToFile(level: String, tag: String?, message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        logFile.appendText("[$timestamp][$level][${tag ?: defaultTag}] $message\n")
    }

    override fun d(message: String, tag: String?) {
        Log.d(tag ?: defaultTag, message)
        logToFile("DEBUG", tag, message)
    }

    override fun i(message: String, tag: String?) {
        Log.i(tag ?: defaultTag, message)
        logToFile("INFO", tag, message)
    }

    override fun w(message: String, tag: String?) {
        Log.w(tag ?: defaultTag, message)
        logToFile("WARN", tag, message)
    }

    override fun e(message: String, throwable: Throwable?, tag: String?) {
        Log.e(tag ?: defaultTag, message, throwable)
        logToFile("ERROR", tag, message)// Log the error message and stack trace to the file
        logToFile("ERROR", tag, "$message\n${throwable?.stackTraceToString() ?: "No throwable provided"}")
    }

    override fun log(message: String) {
        d(message)
    }

    override fun clear() {
        logFile.writeText("")
    }
}

