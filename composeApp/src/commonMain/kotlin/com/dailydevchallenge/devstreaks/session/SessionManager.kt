package com.dailydevchallenge.devstreaks.session

interface SessionManager {
    suspend fun saveToken(token: String)
    suspend fun getToken(): String?
    suspend fun clearToken()
}

expect fun getSessionManager(): SessionManager
