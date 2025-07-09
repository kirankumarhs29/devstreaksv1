// ChallengeTaskMapper.kt
package com.dailydevchallenge.devstreaks.database

import com.dailydevchallenge.devstreaks.model.ChallengeActivity
import com.dailydevchallenge.devstreaks.model.ChallengeTask
import com.dailydevchallenge.database.ChallengeTask as ChallengeTaskEntity


fun ChallengeTaskEntity.toModel(activities: List<ChallengeActivity>): ChallengeTask {
    return ChallengeTask(
        id = this.id,
        pathId = this.pathId,
        day = this.day.toInt(),
        title = this.title,
        type = this.type,
        content = this.content,
        xp = this.xp.toInt(),
        checklist = this.checklist?.split("|") ?: emptyList(),
        whyItMatters = this.whyItMatters,
        bonus = this.bonus,
        tip = this.tip,
        aiBreakdown = this.aiBreakdown,
        videoUrl = this.videoUrl,
        codeExample = this.codeExample,
        challenges = activities
    )
}
