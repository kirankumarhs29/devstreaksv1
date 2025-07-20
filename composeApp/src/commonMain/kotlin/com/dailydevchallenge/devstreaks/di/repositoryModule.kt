// di/RepositoryModule.kt
package com.dailydevchallenge.devstreaks.di

import com.dailydevchallenge.devstreaks.repository.JournalRepository
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import com.dailydevchallenge.devstreaks.repository.InterviewRepository
import org.koin.dsl.module
import com.dailydevchallenge.devstreaks.repository.JournalRepositoryImpl
import com.dailydevchallenge.devstreaks.repository.MemoryRepository
import com.dailydevchallenge.devstreaks.repository.MemoryRepositoryImpl
import com.dailydevchallenge.devstreaks.repository.ProfileRepository
import com.dailydevchallenge.devstreaks.repository.ProfileRepositoryImpl
import com.dailydevchallenge.devstreaks.repository.ResumeAnalysisRepository


val repositoryModule = module {
    single { ChallengeRepository(get(),get()) }
    single<JournalRepository> { JournalRepositoryImpl(get()) }
    single <MemoryRepository>{MemoryRepositoryImpl(get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(get()) }
    single<ResumeAnalysisRepository> { ResumeAnalysisRepository(get())}
    single<InterviewRepository> { InterviewRepository(get())}

}

