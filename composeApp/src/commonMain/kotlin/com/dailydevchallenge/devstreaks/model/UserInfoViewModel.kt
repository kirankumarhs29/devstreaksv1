package com.dailydevchallenge.devstreaks.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydevchallenge.devstreaks.repository.PublicUserProfile
import com.dailydevchallenge.devstreaks.repository.UserInfoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class UserInfoViewModel(private val repo: UserInfoRepository): ViewModel() {
    val profile = MutableStateFlow<PublicUserProfile?>(null)

    init {
        viewModelScope.launch {
            profile.value = repo.getCurrentUserProfile()
        }
    }
}
