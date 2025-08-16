// di/AppModule.kt
package com.dailydevchallenge.devstreaks.di

import com.dailydevchallenge.database.ConversationMemoryQueries
import com.dailydevchallenge.database.JournalQueries
import com.dailydevchallenge.devstreaks.database.ChallengeDatabase
import com.dailydevchallenge.devstreaks.database.DatabaseDriverFactory
import com.dailydevchallenge.devstreaks.llm.GeminiLLMService
import com.dailydevchallenge.devstreaks.llm.LLMService
import com.dailydevchallenge.devstreaks.llm.AIFeedbackService
import com.dailydevchallenge.devstreaks.ai.UnifiedAICoachService
import com.dailydevchallenge.devstreaks.ai.AIContextManager
import com.dailydevchallenge.devstreaks.ai.UserContextManager
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import org.koin.dsl.module
import com.dailydevchallenge.devstreaks.network.getHttpClient
import com.dailydevchallenge.devstreaks.auth.AuthService
import com.dailydevchallenge.devstreaks.features.home.UserStatsManager

import com.dailydevchallenge.devstreaks.auth.getAuthService
import com.dailydevchallenge.devstreaks.features.devcoach.DevChatViewModel
import com.dailydevchallenge.devstreaks.features.devcoach.ResumeChatViewModel
import com.dailydevchallenge.devstreaks.features.home.HomeViewModel
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfilePreferences
import com.dailydevchallenge.devstreaks.features.onboarding.OnboardingViewModel
import com.dailydevchallenge.devstreaks.features.leaderboard.LeaderboardViewModel
import com.dailydevchallenge.devstreaks.model.ProfileViewModel
import com.dailydevchallenge.devstreaks.repository.JournalRepository
import com.dailydevchallenge.devstreaks.repository.JournalRepositoryImpl
import com.dailydevchallenge.devstreaks.repository.MemoryRepository
import com.dailydevchallenge.devstreaks.repository.MemoryRepositoryImpl
import com.dailydevchallenge.devstreaks.repository.ProfileRepository
import com.dailydevchallenge.devstreaks.repository.ProfileRepositoryImpl
import com.dailydevchallenge.devstreaks.repository.ResumeAnalysisRepository
import com.dailydevchallenge.devstreaks.repository.InterviewRepository
import com.dailydevchallenge.devstreaks.service.AdaptiveIntelligenceOrchestrator
import com.dailydevchallenge.devstreaks.service.DifficultyCalculator
import com.dailydevchallenge.devstreaks.service.PersonalizedAICoachingService
import com.dailydevchallenge.devstreaks.service.RealTimeWeakAreaMonitoringService
import com.dailydevchallenge.devstreaks.service.WeakAreaDetectionService

val appModule = module {

    single<AuthService> { getAuthService() }

    // DB + Repo
    single {
        val driver = get<DatabaseDriverFactory>().createDriver()
        ChallengeDatabase(driver)
    }
    single { get<ChallengeDatabase>().challengePathQueries }
    single { get<ChallengeDatabase>().resumeAnalysisQueries }
    single { get<ChallengeDatabase>().userProfileQueries }
    single<JournalQueries> {
        get<ChallengeDatabase>().journalQueries
    }
    single<ConversationMemoryQueries> { get<ChallengeDatabase>().conversationMemoryQueries }

    // Network + AI
    single { getHttpClient() }
    single<LLMService> { GeminiLLMService(get()) }

    // Core Repositories
    single { ChallengeRepository(get(), get(), get()) }
    single<JournalRepository> { JournalRepositoryImpl(get()) }
    single<MemoryRepository> { MemoryRepositoryImpl(get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(get()) }
    single { ResumeAnalysisRepository(get()) }
    single { InterviewRepository(get()) }

    // AI Services
    single { AIContextManager() }
    single { AIFeedbackService(get(), get()) }
    single { UserStatsManager(get()) }
    single { UserContextManager(get(), get(), get(), get(), get(), get()) }
    single { UnifiedAICoachService(get(), get(), get(), get()) }

    // Learning Profile Preferences (was commented out in SharedKoinModule)
    single { LearningProfilePreferences }
}
