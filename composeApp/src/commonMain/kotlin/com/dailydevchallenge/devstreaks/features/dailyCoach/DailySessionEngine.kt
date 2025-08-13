package com.dailydevchallenge.devstreaks.features.dailyCoach

import com.dailydevchallenge.devstreaks.model.ChallengeTask

data class DailySession(
    val title: String,               // "Logic Combat: Day 3"
    val coachIntro: String,          // "We're optimizing loops today. Ready to break patterns?"
    val targetXp: Int,               // e.g. 40
    val microWins: List<String>,    // ["Completed all checklists", "Solved logic puzzle"]
    val radarBoost: Map<String, Int>, // {"Logic": +2, "Debugging": +1}
    val challenges: List<ChallengeTask>
)
