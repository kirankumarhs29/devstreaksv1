package com.dailydevchallenge.devstreaks.model

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydevchallenge.devstreaks.repository.PublicUserProfile
import com.dailydevchallenge.devstreaks.repository.UserInfoRepository
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class UserInfoViewModel(private val repo: UserInfoRepository): ViewModel() {
    val userId = UserPreferences.getSafeUserId()

    val profile = MutableStateFlow<PublicUserProfile?>(null)

    init {
        viewModelScope.launch {
            profile.value = repo.getCurrentUserProfile()
        }
    }
}
