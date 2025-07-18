// sharedMain
package com.dailydevchallenge.devstreaks.sync

import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfile

interface CloudSyncService {
    suspend fun syncPushAll(
        profile: LearningProfile,
        xp: Int,
        streak: Int,
        lastCompletedDate: String?
    )
}

// sharedMain
expect fun getCloudSyncService(): CloudSyncService
