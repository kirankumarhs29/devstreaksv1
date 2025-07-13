package com.dailydevchallenge.devstreaks.di
// viewModelModule

import com.dailydevchallenge.devstreaks.features.devcoach.DevChatViewModel
import com.dailydevchallenge.devstreaks.features.devcoach.ResumeChatViewModel
import com.dailydevchallenge.devstreaks.features.home.HomeViewModel
import com.dailydevchallenge.devstreaks.features.onboarding.OnboardingViewModel

import com.dailydevchallenge.devstreaks.model.ProfileViewModel
import org.koin.dsl.module

val viewModelModule = module {
    single { OnboardingViewModel(get(), get(), get()) }
    single { HomeViewModel(get(), get(), get()) }
    single { DevChatViewModel(get(), get(), get()) }
    single { (userId: String) ->
        ProfileViewModel(
            repo = get(),
            userId = userId
        )
    }
    single { ResumeChatViewModel(get()) }

}
