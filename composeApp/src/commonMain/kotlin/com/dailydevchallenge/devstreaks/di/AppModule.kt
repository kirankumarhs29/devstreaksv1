// di/AppModule.kt
package com.dailydevchallenge.devstreaks.di

import com.dailydevchallenge.devstreaks.database.ChallengeDatabase
import com.dailydevchallenge.devstreaks.database.DatabaseDriverFactory
import com.dailydevchallenge.devstreaks.llm.GeminiLLMService
import com.dailydevchallenge.devstreaks.llm.LLMService
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import org.koin.dsl.module
import com.dailydevchallenge.devstreaks.network.getHttpClient
import com.dailydevchallenge.devstreaks.auth.AuthService

import com.dailydevchallenge.devstreaks.auth.getAuthService
import com.dailydevchallenge.devstreaks.features.home.HomeViewModel
import com.dailydevchallenge.devstreaks.features.onboarding.OnboardingViewModel

val appModule = module {

    single<AuthService> { getAuthService() }

    // DB + Repo
    single {
        val driver = get<DatabaseDriverFactory>().createDriver()
        ChallengeDatabase(driver)
    }
    single { get<ChallengeDatabase>().challengePathQueries }
    single { ChallengeRepository(get(),get()) }

    // Network + AI
    single { getHttpClient() }
    single<LLMService> { GeminiLLMService(get(), apiKey = "AIzaSyA6RewW_nJoIvrbm_BujSGmtVmMJ_HYot4") }


    // ViewModels
    single { OnboardingViewModel(get(), get() , get()) }
    single { HomeViewModel(get(), get(), get()) }

}
