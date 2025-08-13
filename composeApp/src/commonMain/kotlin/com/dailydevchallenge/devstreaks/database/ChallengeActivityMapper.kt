// ChallengeActivityMapper.kt
package com.dailydevchallenge.devstreaks.database

import com.dailydevchallenge.devstreaks.model.ActivityType
import com.dailydevchallenge.devstreaks.model.ChallengeActivity

import com.dailydevchallenge.database.ChallengeActivity as ChallengeActivityEntity

fun ChallengeActivityEntity.toModel(): ChallengeActivity {
    return ChallengeActivity(
        id = this.id,
        type = ActivityType.valueOf(this.type),
        prompt = this.prompt,
        options = this.options?.split("|"),
        correctAnswer = this.correctAnswer,
        language = this.language,
        starterCode = this.starterCode,
        explanation = this.explanation,
        videoUrl = this.videoUrl
    )
}


fun Map<String, String>.toPreferenceString(): String =
    this.entries.joinToString(";") { "${it.key}=${it.value}" }

fun parsePreferences(prefString: String?): Map<String, String> =
    prefString?.split(";")?.associate {
        val (k, v) = it.split("="); k to v
    } ?: emptyMap()
