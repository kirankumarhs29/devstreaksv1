package com.dailydevchallenge.devstreaks.features.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydevchallenge.devstreaks.model.User
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import com.dailydevchallenge.devstreaks.sync.FirebaseUserHelper
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class ProfileEditViewModel(
    private val userRepository: ChallengeRepository,
    private val firebaseUserHelper: FirebaseUserHelper
) : ViewModel() {
    private val userId  =  UserPreferences.getSafeUserId()


    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> get() = _user

    // State: Form fields
    var username by mutableStateOf("")
        private set
    var email by mutableStateOf("")
        private set

    var avatarBytes by mutableStateOf<ByteArray?>(null)
        private set

    var isSaving by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            getLogger().d("ProfileEditViewModel", "Initialized with userId: $userId")
            loadUser()
            getLogger().d("ProfileEditViewModel", "User loaded: ${_user.value}")
        }
    }

    private suspend fun loadUser() {
            getLogger().d("ProfileEditViewModel", "Loading user with userId: $userId, email: $email")
            try {
                val fetchedUser = userRepository.getUserById(userId)
                getLogger().d("ProfileEditViewModel", "Fetched user: $fetchedUser")
                val dataPresent = firebaseUserHelper.getCurrentUserdata(userId, email)
                getLogger().d("ProfileEditViewModel", "Data present: $dataPresent")
                if (fetchedUser == null) {
                    errorMessage = "User not found"
                    _user.value = dataPresent
                    username = dataPresent.username ?: ""
                    this@ProfileEditViewModel.email = dataPresent.email ?: ""
                } else {
                    // If user exists, use the fetched data
                    _user.value = fetchedUser
                    username = fetchedUser.username ?: ""
                    this@ProfileEditViewModel.email = fetchedUser.email ?: ""
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load user: ${e.message}"
            }
    }

    fun onUsernameChange(newUsername: String) {
        username = newUsername
    }
    fun onEmailChange(newEmail: String) {
        email = newEmail
    }


    fun onAvatarSelected(bytes: ByteArray) {
        avatarBytes = bytes
    }

    fun saveProfile(onSuccess: () -> Unit) {
        if (username.isBlank()) {
            errorMessage = "Username cannot be empty"
            return
        }

        viewModelScope.launch {
            isSaving = true
            try {
                val currentUser = _user.value ?: return@launch
                val updatedUser = currentUser.copy(
                    username = username,
                    email = email,
                    avatarUrl = avatarBytes?.let { encodeAvatar(it) } ?: currentUser.avatarUrl
                )
                userRepository.updateUser(updatedUser)
                _user.value = updatedUser
                errorMessage = null
                getLogger().d("ProfileEditViewModel", "Profile updated successfully: $updatedUser")
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Failed to update profile: ${e.message}"
            } finally {
                isSaving = false
            }
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun encodeAvatar(bytes: ByteArray): String {
        return Base64.encode(bytes)
    }

}
