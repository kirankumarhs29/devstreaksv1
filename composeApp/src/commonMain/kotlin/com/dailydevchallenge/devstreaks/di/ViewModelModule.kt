package com.dailydevchallenge.devstreaks.di
// viewModelModule

import com.dailydevchallenge.devstreaks.ai.AIContextManager
import com.dailydevchallenge.devstreaks.ai.UnifiedAICoachService
import com.dailydevchallenge.devstreaks.ai.UserContextManager
import com.dailydevchallenge.devstreaks.features.challenge.ChallengeDetailViewModel
import com.dailydevchallenge.devstreaks.features.devcoach.DevChatViewModel
import com.dailydevchallenge.devstreaks.features.devcoach.ResumeChatViewModel
import com.dailydevchallenge.devstreaks.features.home.HomeViewModel
import com.dailydevchallenge.devstreaks.features.home.UserStatsManager
import com.dailydevchallenge.devstreaks.features.onboarding.OnboardingViewModel
import com.dailydevchallenge.devstreaks.features.profile.ProfileEditViewModel
import com.dailydevchallenge.devstreaks.features.leaderboard.LeaderboardViewModel
import com.dailydevchallenge.devstreaks.features.subscription.SubscriptionViewModel
import com.dailydevchallenge.devstreaks.llm.AIFeedbackService
import com.dailydevchallenge.devstreaks.model.ProfileViewModel
import com.dailydevchallenge.devstreaks.model.UserInfoViewModel
import com.dailydevchallenge.devstreaks.repository.ResumeAnalysisRepository
import com.dailydevchallenge.devstreaks.repository.InterviewRepository
import org.koin.dsl.module

val viewModelModule = module {
    // AI Services
    single { AIFeedbackService(get(), get()) }
    single { UserStatsManager(get()) }
    single { AIContextManager() }
    single { UserContextManager(get(), get(), get(), get(), get(), get()) } // New unified context manager
    single { UnifiedAICoachService(get(), get(), get(), get()) }

    // Missing repositories for AI features
    single { ResumeAnalysisRepository(get()) }
    single { InterviewRepository(get()) }

    // ViewModels
    single { OnboardingViewModel(get(), get(), get(), get()) }
    single { ChallengeDetailViewModel(get(), get(), get()) }
    single { HomeViewModel(get(), get(), get(), get(), get()) }
    single { DevChatViewModel(get(), get(), get(), get(), get()) } // Updated to include UserContextManager
    single { ProfileViewModel(repo = get()) }
    single { ResumeChatViewModel(get(), get(), get(), get(), get()) } // Updated to include UserContextManager
    single { LeaderboardViewModel(get()) }
    single { UserInfoViewModel(get()) }
    single { ProfileEditViewModel(get(), get()) }
    single { SubscriptionViewModel(get(), get()) }
}
