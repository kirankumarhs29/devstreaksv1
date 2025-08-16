package com.dailydevchallenge.devstreaks.repository

import com.dailydevchallenge.devstreaks.model.AdaptiveChallenge
import com.dailydevchallenge.database.ChallengePathQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChallengeRepositoryImpl(private val challengePathQueries: ChallengePathQueries) {
    suspend fun insertGeneratedChallenge(challenge: AdaptiveChallenge) = withContext(Dispatchers.Default) {
        challengePathQueries.insertAdaptiveChallenge(
            id = challenge.id,
            title = challenge.title,
            description = challenge.description,
            difficulty = challenge.difficulty,
            pathId = challenge.pathId,
            userId = challenge.userId
        )
    }

    suspend fun getAdaptiveChallengesForUser(userId: String): List<AdaptiveChallenge> = withContext(Dispatchers.Default) {
        challengePathQueries.selectAdaptiveChallengesByUser(userId).executeAsList().map {
            AdaptiveChallenge(
                id = it.id,
                title = it.title,
                description = it.description,
                difficulty = it.difficulty,
                pathId = it.pathId,
                userId = it.userId
            )
        }
    }
}

