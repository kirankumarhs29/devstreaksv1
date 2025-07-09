package com.dailydevchallenge.devstreaks.repository

import com.dailydevchallenge.devstreaks.model.Challenge

interface UserProgressRepository {
    fun markChallengeCompleted(id: String)
    fun isChallengeCompleted(id: String): Boolean
    fun getTodayChallenge(): Challenge
    fun getXp(): Int
    fun getStreak(): Int
    fun resetProgress()
    fun advanceDay()
}
