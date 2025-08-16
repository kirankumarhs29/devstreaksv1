package com.dailydevchallenge.devstreaks.features.auth

import androidx.lifecycle.ViewModel
import com.dailydevchallenge.database.UserProfileQueries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.dailydevchallenge.devstreaks.auth.AuthService
import com.dailydevchallenge.devstreaks.auth.AuthResult
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import com.dailydevchallenge.devstreaks.notification.getNotificationScheduler
import com.dailydevchallenge.devstreaks.session.getSessionManager
import com.dailydevchallenge.devstreaks.sync.FirebaseUserHelper
import com.dailydevchallenge.devstreaks.utils.logAnalyticsEvent
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.datetime.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// UI state for LoginScreen
 data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class LoginViewModel : ViewModel(), KoinComponent {
    private val authService: AuthService by inject()
    private val userProfileQueries : UserProfileQueries by inject()
    private val logger = getLogger()
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email, error = null)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun onTogglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(isPasswordVisible = !_uiState.value.isPasswordVisible)
    }

    fun login(onSuccess: (userId: String) -> Unit) {
        val email = _uiState.value.email
        val password = _uiState.value.password
        logger.log("Login button clicked: email=REDACTED")
        logAnalyticsEvent("login_attempted", mapOf("email_length" to email.length))
        when {
            !isValidEmail(email) -> _uiState.value = _uiState.value.copy(error = "Enter a valid email.")
            !isValidPassword(password) -> _uiState.value = _uiState.value.copy(error = "Password must be at least 8 characters.")
            else -> {
                _uiState.value = _uiState.value.copy(isLoading = true)
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        when (val result = authService.login(email, password)) {
                            is AuthResult.Success -> {
                                logger.log("Login successful for user=${result.userId}")
//                                val now = Clock.System.now().toEpochMilliseconds()
                                logAnalyticsEvent("login_success", mapOf("user_id_length" to result.userId.length))
                                UserPreferences.setLoggedIn(true)
                                UserPreferences.setUserId(result.userId)
                                UserPreferences.setEmailId(email)
                                syncUserFromRemote(result.userId, email)
                                getSessionManager().saveToken(result.token)
                                getNotificationScheduler().scheduleOneTimeNotification(
                                    "Welcome back to DevStreak!",
                                    "🎉 You logged in successfully",
                                    "login"
                                )
                                _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                                onSuccess(result.userId)
                            }
                            is AuthResult.Error -> {
                                _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                                logger.log("Login failed: ${result.message}")
                                logAnalyticsEvent("login_failed", mapOf("reason" to result.message))
                            }
                        }
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = "Unexpected error. Please try again.")
                        logger.log("Login exception: ${e.message}")
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    private suspend fun syncUserFromRemote(userId: String, email: String) {
        val firebaseUserHelper: FirebaseUserHelper by inject()
        val dataPresent = firebaseUserHelper.getCurrentUserdata(userId, email)

        // Check if exists locally
        val existing = userProfileQueries.getUserById(userId).executeAsOneOrNull()
        val now = Clock.System.now()

        if (existing == null) {
            // 🔼 Insert fresh
            userProfileQueries.insertUser(
                userId = userId,
                email = email,
                passwordHash = "",
                username = dataPresent.username,
                avatarUrl = dataPresent.avatarUrl,
                createdAt = now.toEpochMilliseconds(),
                lastLogin = now.toEpochMilliseconds(),
                xp = dataPresent.xp.toLong(),
                level = dataPresent.level,
                dailyStreak = dataPresent.dailyStreak,
                streakStartDate = null,
                role = dataPresent.role,
                badges = "",
                preferences = ""
            )
        } else {
            // 🔁 Update existing
            userProfileQueries.updateUser(
                userId = userId,
                email = email,
                username = dataPresent.username,
                xp = dataPresent.xp.toLong(),
                dailyStreak = dataPresent.dailyStreak,
                lastLogin = now.toEpochMilliseconds(),
                avatarUrl = dataPresent.avatarUrl,
                role = dataPresent.role,
                preferences = (dataPresent.preferences ?: "").toString(),
                badges = dataPresent.badges.joinToString(","),
                passwordHash = "", // Password is not updated during login
                level = dataPresent.level,
                streakStartDate = dataPresent.streakStartDate?.toEpochMilliseconds(),
            )
        }
    }

}

fun isValidEmail(email: String): Boolean {
    val emailPattern = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
    return emailPattern.matches(email)
}

fun isValidPassword(password: String): Boolean {
    return password.length >= 8
}

