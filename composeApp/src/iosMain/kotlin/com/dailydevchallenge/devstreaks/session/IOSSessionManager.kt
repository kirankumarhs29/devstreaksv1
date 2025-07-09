// iosMain/kotlin/com/dailydevchallenge/session/iOSSessionManager.kt
package com.dailydevchallenge.devstreaks.session

import platform.Foundation.NSUserDefaults

class IOSSessionManager : SessionManager {
    private val defaults = NSUserDefaults.standardUserDefaults

    override suspend fun saveToken(token: String) {
        defaults.setObject(token, forKey = "auth_token")
    }

    override suspend fun getToken(): String? = defaults.stringForKey("auth_token")

    override suspend fun clearToken() {
        defaults.removeObjectForKey("auth_token")
    }
}

actual fun getSessionManager(): SessionManager = IOSSessionManager()

