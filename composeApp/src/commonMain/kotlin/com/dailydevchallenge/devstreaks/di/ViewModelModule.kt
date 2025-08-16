package com.dailydevchallenge.devstreaks.di
// viewModelModule

import com.dailydevchallenge.devstreaks.ai.AIContextManager
import com.dailydevchallenge.devstreaks.ai.UnifiedAICoachService
import com.dailydevchallenge.devstreaks.ai.UserContextManager
import com.dailydevchallenge.devstreaks.features.challenge.ChallengeDetailViewModel
import com.dailydevchallenge.devstreaks.features.challenge.AdaptiveChallengeViewModel
import com.dailydevchallenge.devstreaks.features.devcoach.DevChatViewModel
import com.dailydevchallenge.devstreaks.features.devcoach.ResumeChatViewModel
import com.dailydevchallenge.devstreaks.features.devcoach.PersonalizedDevChatViewModel
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
import com.dailydevchallenge.devstreaks.features.social.SocialViewModel
import com.dailydevchallenge.devstreaks.features.skilltree.SkillTreeViewModel
import com.dailydevchallenge.devstreaks.features.projects.ProjectViewModel
import com.dailydevchallenge.devstreaks.features.auth.LoginViewModel
import com.dailydevchallenge.devstreaks.features.analytics.PredictiveInsightsViewModel
import org.koin.dsl.module

val viewModelModule = module {
    // Core AI Services - keep only AI-specific services here
    single { AIFeedbackService(get(), get()) }
    single { AIContextManager() }
    single { UserContextManager(get(), get(), get(), get(), get(), get()) }
    single { UnifiedAICoachService(get(), get(), get(), get()) }

    // Repositories - these should stay in ViewModelModule as they're used by ViewModels
    single { ResumeAnalysisRepository(get()) }
    single { InterviewRepository(get()) }

    // ViewModels - Updated to include adaptive features
    single { OnboardingViewModel(get(), get(), get(), get()) }
    single { ChallengeDetailViewModel(get(), get(), get(), get(), get()) }
    single { AdaptiveChallengeViewModel(get(), get(), get(), get()) }
    single { HomeViewModel(get(), get(), get(), get(), get(), get()) }
    single { DevChatViewModel(get(), get(), get(), get(), get()) }
    single { PersonalizedDevChatViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { ProfileViewModel(repo = get()) }
    single { ResumeChatViewModel(get(), get(), get(), get(), get()) }
    single { LeaderboardViewModel(get()) }
    single { UserInfoViewModel(get()) }
    single { ProfileEditViewModel(get(), get()) }
    single { SubscriptionViewModel(get(), get()) }

    // Missing ViewModels - Add all discovered ViewModels
    single { SocialViewModel(get(), get()) }
    single { SkillTreeViewModel(get(), get()) }
    single { ProjectViewModel(get(), get()) }
    single { LoginViewModel() }
    single { PredictiveInsightsViewModel(get(), get()) }
}
