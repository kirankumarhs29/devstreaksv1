package com.dailydevchallenge.devstreaks.logger

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.Clock
import platform.Foundation.*
import com.dailydevchallenge.devstreaks.utils.toNSData

class IOSDevLogger : DevLogger {

    private val defaultTag = "DevLogger"
    private val logFilePath: String = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory, NSUserDomainMask, true
    ).first().toString() + "/devstreak_log.txt"

    private fun writeLogToFile(level: String, tag: String?, message: String) {
        val timestamp = Clock.System.now().toString()
        val fullMessage = "[$timestamp][$level][${tag ?: defaultTag}] $message\n"
        val fileManager = NSFileManager.defaultManager

        if (!fileManager.fileExistsAtPath(logFilePath)) {
            fileManager.createFileAtPath(logFilePath, null, null)
        }

        val fileHandle = NSFileHandle.fileHandleForWritingAtPath(logFilePath)
        fileHandle?.seekToEndOfFile()
        fileHandle?.writeData(fullMessage.encodeToByteArray().toNSData())
        fileHandle?.closeFile()
    }

    override fun d(message: String, tag: String?) {
        NSLog("[DEBUG] [${tag ?: defaultTag}] $message")
        writeLogToFile("DEBUG", tag, message)
    }

    override fun i(message: String, tag: String?) {
        NSLog("[INFO] [${tag ?: defaultTag}] $message")
        writeLogToFile("INFO", tag, message)
    }

    override fun w(message: String, tag: String?) {
        NSLog("[WARN] [${tag ?: defaultTag}] $message")
        writeLogToFile("WARN", tag, message)
    }

    override fun e(message: String, throwable: Throwable?, tag: String?) {
        val fullMessage = buildString {
            appendLine(message)
            throwable?.let { appendLine(it.stackTraceToString()) }
        }
        NSLog("[ERROR] [${tag ?: defaultTag}] $fullMessage")
        writeLogToFile("ERROR", tag, fullMessage)
    }

    override fun log(message: String) {
        d(message) // Default to debug
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun clear() {
        NSFileManager.defaultManager.removeItemAtPath(logFilePath, null)
    }
}
