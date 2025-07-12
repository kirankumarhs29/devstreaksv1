// di/AppModule.kt
package com.dailydevchallenge.devstreaks.di

import com.dailydevchallenge.database.ConversationMemoryQueries
import com.dailydevchallenge.database.JournalQueries
import com.dailydevchallenge.devstreaks.database.ChallengeDatabase
import com.dailydevchallenge.devstreaks.database.DatabaseDriverFactory
import com.dailydevchallenge.devstreaks.llm.GeminiLLMService
import com.dailydevchallenge.devstreaks.llm.LLMService
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import org.koin.dsl.module
import com.dailydevchallenge.devstreaks.network.getHttpClient
import com.dailydevchallenge.devstreaks.auth.AuthService

import com.dailydevchallenge.devstreaks.auth.getAuthService
import com.dailydevchallenge.devstreaks.features.devcoach.DevChatViewModel
import com.dailydevchallenge.devstreaks.features.devcoach.ResumeChatViewModel
import com.dailydevchallenge.devstreaks.features.home.HomeViewModel
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfilePreferences
import com.dailydevchallenge.devstreaks.features.onboarding.OnboardingViewModel
import com.dailydevchallenge.devstreaks.model.ProfileViewModel
import com.dailydevchallenge.devstreaks.repository.JournalRepository
import com.dailydevchallenge.devstreaks.repository.JournalRepositoryImpl
import com.dailydevchallenge.devstreaks.repository.MemoryRepository
import com.dailydevchallenge.devstreaks.repository.MemoryRepositoryImpl
import com.dailydevchallenge.devstreaks.repository.ProfileRepository
import com.dailydevchallenge.devstreaks.repository.ProfileRepositoryImpl

val appModule = module {

    single<AuthService> { getAuthService() }

    // DB + Repo
    single {
        val driver = get<DatabaseDriverFactory>().createDriver()
        ChallengeDatabase(driver)
    }
    single { get<ChallengeDatabase>().challengePathQueries }
    single { ChallengeRepository(get(),get()) }
    single<JournalRepository> { JournalRepositoryImpl(get()) }
    single <MemoryRepository>{ MemoryRepositoryImpl(get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(get()) }
    single<JournalQueries> {
        get<ChallengeDatabase>().journalQueries
    }
    single<ConversationMemoryQueries> { get<ChallengeDatabase>().conversationMemoryQueries }
    single { get<ChallengeDatabase>().userProfileQueries }

    // Network + AI
    single { getHttpClient() }
    single<LLMService> { GeminiLLMService(get(), apiKey = "AIzaSyA6RewW_nJoIvrbm_BujSGmtVmMJ_HYot4") }

    single { LearningProfilePreferences }

    // ViewModels
    single { OnboardingViewModel(get(), get() , get()) }
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
