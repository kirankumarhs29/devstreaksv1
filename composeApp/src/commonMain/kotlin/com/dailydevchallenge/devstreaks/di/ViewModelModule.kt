package com.dailydevchallenge.devstreaks.di
// viewModelModule

import com.dailydevchallenge.devstreaks.features.challenge.ChallengeDetailViewModel
import com.dailydevchallenge.devstreaks.features.devcoach.DevChatViewModel
import com.dailydevchallenge.devstreaks.features.devcoach.ResumeChatViewModel
import com.dailydevchallenge.devstreaks.features.home.HomeViewModel
import com.dailydevchallenge.devstreaks.features.onboarding.OnboardingViewModel
import com.dailydevchallenge.devstreaks.features.profile.ProfileEditViewModel
import com.dailydevchallenge.devstreaks.model.LeaderboardViewModel
import com.dailydevchallenge.devstreaks.model.ProfileViewModel
import com.dailydevchallenge.devstreaks.model.UserInfoViewModel
import org.koin.dsl.module

val viewModelModule = module {
    single { OnboardingViewModel(get(), get(), get()) }
    single {ChallengeDetailViewModel(get())}
    single { HomeViewModel(get(), get(), get(),get()) }
    single { DevChatViewModel(get(), get(), get()) }
    single {ProfileViewModel(repo = get())}
    single { ResumeChatViewModel(get(), get(), get()) }
    single { LeaderboardViewModel(get(),get()) }
    single{ UserInfoViewModel(get())}
    single{ProfileEditViewModel(get(),get())}

}

