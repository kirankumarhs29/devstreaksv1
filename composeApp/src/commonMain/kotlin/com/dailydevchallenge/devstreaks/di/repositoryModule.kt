// di/RepositoryModule.kt
package com.dailydevchallenge.devstreaks.di

import com.dailydevchallenge.devstreaks.repository.JournalRepository
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import org.koin.dsl.module
import com.dailydevchallenge.devstreaks.repository.JournalRepositoryImpl
import com.dailydevchallenge.devstreaks.repository.MemoryRepository
import com.dailydevchallenge.devstreaks.repository.MemoryRepositoryImpl
import com.dailydevchallenge.devstreaks.repository.ProfileRepository
import com.dailydevchallenge.devstreaks.repository.ProfileRepositoryImpl


val repositoryModule = module {
    single { ChallengeRepository(get(),get()) }
    single<JournalRepository> { JournalRepositoryImpl(get()) }
    single <MemoryRepository>{MemoryRepositoryImpl(get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(get()) }

}
