// androidMain/kotlin/com/dailydevchallenge/session/AndroidSessionManager.kt
package com.dailydevchallenge.devstreaks.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.datastore.preferences.core.stringPreferencesKey

private lateinit var appContext: Context

fun initSessionManager(context: Context) {
    appContext = context.applicationContext
}

actual fun getSessionManager(): SessionManager = AndroidSessionManager(appContext)


private val Context.dataStore by preferencesDataStore(name = "secure_prefs")

class AndroidSessionManager(private val context: Context) : SessionManager {
    private val TOKEN_KEY = stringPreferencesKey("auth_token")

    override suspend fun saveToken(token: String) {
        runBlocking {
            context.dataStore.edit { prefs ->
                prefs[TOKEN_KEY] = token
            }
        }
    }

    override suspend fun getToken(): String? {
        return runBlocking {
            context.dataStore.data.first()[TOKEN_KEY]
        }
    }

    override suspend fun clearToken() {
        runBlocking {
            context.dataStore.edit { prefs ->
                prefs.remove(TOKEN_KEY)
            }
        }
    }
}

