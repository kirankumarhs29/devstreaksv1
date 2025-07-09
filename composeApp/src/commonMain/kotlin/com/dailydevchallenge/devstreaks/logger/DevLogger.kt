package com.dailydevchallenge.devstreaks.logger

interface DevLogger {
    fun d(message: String, tag: String? = null)
    fun i(message: String, tag: String? = null)
    fun w(message: String, tag: String? = null)
    fun e(message: String, throwable: Throwable? = null, tag: String? = null)

    fun log(message: String) // Legacy support
    fun clear()
}


