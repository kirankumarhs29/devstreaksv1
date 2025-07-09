// di/DatabaseModule.kt
package com.dailydevchallenge.devstreaks.di

import com.dailydevchallenge.database.ConversationMemoryQueries
import com.dailydevchallenge.database.JournalQueries
import com.dailydevchallenge.devstreaks.database.DatabaseDriverFactory
import org.koin.dsl.module
import com.dailydevchallenge.devstreaks.database.ChallengeDatabase


val databaseModule = module {
    single { get<DatabaseDriverFactory>().createDriver() }
    single { ChallengeDatabase(get()) }
    single { get<ChallengeDatabase>().challengePathQueries }
    // journal
    single<JournalQueries> {
        get<ChallengeDatabase>().journalQueries
    }
    single<ConversationMemoryQueries> { get<ChallengeDatabase>().conversationMemoryQueries }
    single { get<ChallengeDatabase>().userProfileQueries }



}
