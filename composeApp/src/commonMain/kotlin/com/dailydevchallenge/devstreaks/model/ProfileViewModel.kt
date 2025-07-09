package com.dailydevchallenge.devstreaks.model


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfile
import com.dailydevchallenge.devstreaks.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repo: ProfileRepository,
    private val userId: String
) : ViewModel() {

    private val _profile = MutableStateFlow<LearningProfile?>(null)
    val profile: StateFlow<LearningProfile?> = _profile.asStateFlow()

    fun loadProfile() = viewModelScope.launch {
        _profile.value = repo.getProfile(userId)
    }

    fun save(profile: LearningProfile) = viewModelScope.launch {
        repo.saveProfile(profile, userId)
        _profile.value = profile
    }

    fun clearProfile() = viewModelScope.launch {
        repo.clearProfile(userId)
        _profile.value = null
    }
}
