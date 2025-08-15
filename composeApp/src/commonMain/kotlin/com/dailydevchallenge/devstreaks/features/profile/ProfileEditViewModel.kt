package com.dailydevchallenge.devstreaks.features.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydevchallenge.devstreaks.model.User
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import com.dailydevchallenge.devstreaks.storage.getFirebaseStorageService
import com.dailydevchallenge.devstreaks.sync.FirebaseUserHelper
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class ProfileEditViewModel(
    private val userRepository: ChallengeRepository,
    private val firebaseUserHelper: FirebaseUserHelper
) : ViewModel() {
    private val userId = UserPreferences.getSafeUserId()
    private val storageService = getFirebaseStorageService()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> get() = _user

    // State: Form fields
    var username by mutableStateOf("")
        private set
    var email by mutableStateOf("")
        private set

    var avatarBytes by mutableStateOf<ByteArray?>(null)
        private set

    private var isSaving by mutableStateOf(false)

    private var isUploadingAvatar by mutableStateOf(false)

    private var errorMessage by mutableStateOf<String?>(null)

    // Add validation properties
    val isUsernameValid: Boolean
        get() = username.isNotBlank() && username.length >= 3

    val isEmailValid: Boolean
        get() = email.isNotBlank() && email.contains("@") && email.contains(".") &&
                email.indexOf("@") > 0 && email.indexOf("@") < email.lastIndexOf(".")

    val isLoading: Boolean
        get() = isSaving || isUploadingAvatar

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
                // Safely fetch user with proper error handling
                val fetchedUser = try {
                    userRepository.getUserById(userId)
                } catch (e: IndexOutOfBoundsException) {
                    getLogger().e("ProfileEditViewModel", e, "IndexOutOfBoundsException when fetching user: ${e.message}")
                    null
                } catch (e: Exception) {
                    getLogger().e("ProfileEditViewModel", e, "Exception when fetching user: ${e.message}")
                    null
                }

                getLogger().d("ProfileEditViewModel", "Fetched user: $fetchedUser")
                println("ProfileEditViewModel: Fetched user from repository: $fetchedUser")
                println("ProfileEditViewModel: User avatarUrl: ${fetchedUser?.avatarUrl}")

                // Safely get Firebase data with proper error handling
                val dataPresent = try {
                    firebaseUserHelper.getCurrentUserdata(userId, email)
                } catch (e: Exception) {
                    getLogger().e("ProfileEditViewModel", e, "Failed to get Firebase user data: ${e.message}")
                    // Create a default user if Firebase data fails
                    User(
                        userId = userId,
                        username = "User",
                        email = email.takeIf { it.isNotEmpty() } ?: "unknown@example.com",
                        passwordHash = "",
                        avatarUrl = null,
                        createdAt = Clock.System.now().toEpochMilliseconds(),
                        lastLogin = Clock.System.now().toEpochMilliseconds(),
                        xp = 0,
                        level = 1L, // Long type
                        dailyStreak = 0L, // Long type
                        streakStartDate = null, // Instant? type
                        preferences = emptyMap(), // Map<String, String> type
                        role = "user"
                    )
                }

                getLogger().d("ProfileEditViewModel", "Data present: $dataPresent")

                if (fetchedUser == null) {
                    errorMessage = null // Clear any previous error since we have fallback data
                    _user.value = dataPresent
                    username = dataPresent.username
                    this@ProfileEditViewModel.email = dataPresent.email
                    println("ProfileEditViewModel: Using Firebase data, avatarUrl: ${dataPresent.avatarUrl}")
                } else {
                    // If user exists, use the fetched data but check UserPreferences for avatarUrl backup
                    val avatarUrl = fetchedUser.avatarUrl ?: UserPreferences.getAvatarUrl()
                    println("ProfileEditViewModel: Database avatarUrl: ${fetchedUser.avatarUrl}")
                    println("ProfileEditViewModel: UserPreferences avatarUrl: ${UserPreferences.getAvatarUrl()}")
                    println("ProfileEditViewModel: Final avatarUrl: $avatarUrl")

                    val userWithAvatar = fetchedUser.copy(avatarUrl = avatarUrl)
                    _user.value = userWithAvatar
                    username = fetchedUser.username
                    this@ProfileEditViewModel.email = fetchedUser.email
                    println("ProfileEditViewModel: Using repository data with fallback, final avatarUrl: $avatarUrl")
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load user: ${e.message}"
                println("ProfileEditViewModel: Error loading user: ${e.message}")
                getLogger().e("ProfileEditViewModel", e, "General error in loadUser: ${e.message}")
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

    // Add simple save method that calls saveProfile
    fun save() {
        saveProfile {}
    }

    private fun saveProfile(onSuccess: () -> Unit) {
        if (username.isBlank()) {
            errorMessage = "Username cannot be empty"
            return
        }

        viewModelScope.launch {
            isSaving = true
            try {
                val currentUser = _user.value
                if (currentUser == null) {
                    errorMessage = "User data not loaded"
                    return@launch
                }

                var avatarUrl = currentUser.avatarUrl

                // Upload avatar to Firebase Storage if a new one was selected
                if (avatarBytes != null) {
                    isUploadingAvatar = true
                    try {
                        val uploadResult = storageService.uploadAvatar(userId, avatarBytes!!)
                        if (uploadResult.isSuccess) {
                            avatarUrl = uploadResult.getOrNull()
                            getLogger().d("ProfileEditViewModel", "Avatar uploaded successfully: $avatarUrl")
                        } else {
                            getLogger().e("ProfileEditViewModel", uploadResult.exceptionOrNull(),
                                "Failed to upload avatar: ${uploadResult.exceptionOrNull()?.message}")
                            errorMessage = "Failed to upload avatar: ${uploadResult.exceptionOrNull()?.message}"
                            return@launch
                        }
                    } catch (e: Exception) {
                        getLogger().e("ProfileEditViewModel", e,"Avatar upload error: ${e.message}")
                        errorMessage = "Failed to upload avatar: ${e.message}"
                        return@launch
                    } finally {
                        isUploadingAvatar = false
                    }
                }

                val updatedUser = currentUser.copy(
                    username = username,
                    email = email,
                    avatarUrl = avatarUrl
                )

                // Save user to Firestore and database
                try {
                    // Store avatarUrl in UserPreferences as backup
                    UserPreferences.setAvatarUrl(avatarUrl)
                    println("ProfileEditViewModel: Saved avatarUrl to UserPreferences: $avatarUrl")

                    userRepository.updateUser(updatedUser)
                    _user.value = updatedUser
                    errorMessage = null
                    avatarBytes = null // Clear selected avatar after successful save

                    getLogger().d("ProfileEditViewModel", "Profile updated successfully for user: ${updatedUser.userId}")
                    onSuccess()
                } catch (updateError: Exception) {
                    getLogger().e("ProfileEditViewModel", updateError, "Failed to update user in repository: ${updateError.message}")
                    errorMessage = "Failed to save profile: ${updateError.message}"
                }

            } catch (e: Exception) {
                getLogger().e("ProfileEditViewModel", e, "Profile save error: ${e.message}")
                errorMessage = "Failed to update profile: ${e.message}"
            } finally {
                isSaving = false
                isUploadingAvatar = false
            }
        }
    }

}
