package com.dailydevchallenge.devstreaks.auth

interface AuthService {
    suspend fun login(email: String, password: String): AuthResult
    suspend fun signup(name : String,email: String, password: String): AuthResult
    fun logout()
    fun isLoggedIn(): Boolean
    fun sendPasswordResetEmail(email: String, callback: (Boolean, String?) -> Unit)
}

sealed class AuthResult {
    data class Success(val userId: String, val token: String) : AuthResult() // 🔧 changed from object to data class
    data class Error(val message: String) : AuthResult()
}
