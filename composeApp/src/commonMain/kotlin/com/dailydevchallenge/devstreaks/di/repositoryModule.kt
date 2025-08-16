// di/RepositoryModule.kt
package com.dailydevchallenge.devstreaks.di

import com.dailydevchallenge.devstreaks.repository.JournalRepository
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import com.dailydevchallenge.devstreaks.repository.ChallengeRepositoryImpl
import com.dailydevchallenge.devstreaks.repository.InterviewRepository
import org.koin.dsl.module
import com.dailydevchallenge.devstreaks.repository.JournalRepositoryImpl
import com.dailydevchallenge.devstreaks.repository.LeaderboardRepository
import com.dailydevchallenge.devstreaks.repository.MemoryRepository
import com.dailydevchallenge.devstreaks.repository.MemoryRepositoryImpl
import com.dailydevchallenge.devstreaks.repository.ProfileRepository
import com.dailydevchallenge.devstreaks.repository.ProfileRepositoryImpl
import com.dailydevchallenge.devstreaks.repository.ResumeAnalysisRepository
import com.dailydevchallenge.devstreaks.repository.UserInfoRepository
import com.dailydevchallenge.devstreaks.repository.UserInfoRepositoryImpl
import com.dailydevchallenge.devstreaks.sync.FirebaseUserHelper
import com.dailydevchallenge.devstreaks.sync.getPlatformFirebaseUserHelper
import com.dailydevchallenge.devstreaks.repository.UserProgressRepositoryImpl
import com.dailydevchallenge.devstreaks.features.home.UserStatsManager
import com.dailydevchallenge.devstreaks.features.subscription.SubscriptionRepository
import com.dailydevchallenge.devstreaks.features.subscription.FirebaseSubscriptionRepository
import com.dailydevchallenge.devstreaks.service.AdaptiveChallengeGenerator
import com.dailydevchallenge.devstreaks.service.LLMApi
import com.dailydevchallenge.devstreaks.model.PerformanceMetrics
import com.dailydevchallenge.devstreaks.llm.LLMService
import com.dailydevchallenge.devstreaks.llm.ChatMessage


val repositoryModule = module {
    single { ChallengeRepository(get(),get(), get()) }
    single<ChallengeRepositoryImpl> { ChallengeRepositoryImpl(get()) }
    single<JournalRepository> { JournalRepositoryImpl(get()) }
    single <MemoryRepository>{MemoryRepositoryImpl(get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(get()) }
    single<ResumeAnalysisRepository> { ResumeAnalysisRepository(get())}
    single<InterviewRepository> { InterviewRepository(get())}
    single<FirebaseUserHelper> { getPlatformFirebaseUserHelper() }

    // Use the platform-specific Firebase helper
    single { LeaderboardRepository(get<FirebaseUserHelper>()) }
    single<SubscriptionRepository> { FirebaseSubscriptionRepository() }

    single { UserInfoRepositoryImpl(get()) as UserInfoRepository }

    // Define the performance metrics provider function
    single<suspend (String) -> PerformanceMetrics?> {
        ::defaultPerformanceMetricsProvider
    }

    // Create LLMApi implementation that wraps LLMService
    single<LLMApi> {
        object : LLMApi {
            override suspend fun generateChallenges(prompt: String): String {
                val llmService = get<LLMService>()
                // Use generateResponse method with a simple chat message
                return llmService.generateResponse(listOf(ChatMessage(role = "user", content = prompt)))
            }
        }
    }

    // Define AdaptiveChallengeGenerator with LLMApi
    single<AdaptiveChallengeGenerator> {
        AdaptiveChallengeGenerator(get<LLMApi>())
    }

    // Fix UserProgressRepositoryImpl with proper dependencies
    single<UserProgressRepositoryImpl> {
        UserProgressRepositoryImpl(
            get(), // userProfileQueries
            get<suspend (String) -> PerformanceMetrics?>(), // performanceMetricsProvider
            get(), // challengeRepository
            get()  // adaptiveChallengeGenerator
        )
    }

    single<UserStatsManager> { UserStatsManager(get()) }
}

// Helper function outside the module
private suspend fun defaultPerformanceMetricsProvider(taskId: String): PerformanceMetrics? {
    // Default implementation - returns null for now
    // This can be enhanced later to fetch actual performance metrics
    return null
}
