// ChallengeActivityMapper.kt
package com.dailydevchallenge.devstreaks.database

import com.dailydevchallenge.devstreaks.model.ActivityType
import com.dailydevchallenge.devstreaks.model.ChallengeActivity

import com.dailydevchallenge.database.ChallengeActivity as ChallengeActivityEntity

fun ChallengeActivityEntity.toModel(): ChallengeActivity {
    return ChallengeActivity(
        id = this.id,
        type = ActivityType.valueOf(this.type.toString()),
        prompt = this.prompt,
        options = this.options?.toString()?.split("|"),
        correctAnswer = this.correctAnswer,
        language = this.language,
        starterCode = this.starterCode,
        explanation = this.explanation,
        videoUrl = this.videoUrl
    )
}
