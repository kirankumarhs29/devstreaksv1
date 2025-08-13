package com.dailydevchallenge.devstreaks.session

import com.dailydevchallenge.devstreaks.model.User

interface SessionManager {
    suspend fun saveToken(token: String)
    suspend fun getToken(): String?

    suspend fun clearToken()
}


object SessionManager1 {
    private var currentUser: User? = null

    fun setUser(user: User) {
        currentUser = user
    }

    fun getUser(): User? = currentUser

    fun clear() {
        currentUser = null
    }
}

expect fun getSessionManager(): SessionManager
