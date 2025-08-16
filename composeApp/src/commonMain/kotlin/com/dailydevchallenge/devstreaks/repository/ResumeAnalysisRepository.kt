package com.dailydevchallenge.devstreaks.repository

import com.dailydevchallenge.database.ResumeAnalysis
import com.dailydevchallenge.database.ResumeAnalysisQueries
import com.dailydevchallenge.devstreaks.utils.generateUUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ResumeAnalysisRepository(private val queries: ResumeAnalysisQueries) {
    suspend fun saveResumeAnalysis(
        id: String,
        userId: String,
        resumeText: String,
        summary: String?,
        skillsParsed: String?,
        skillsGaps: String?,
        recommendations: String?,
        jobMatchScore: Long? = null,
        createdAt: Long
    ): String = withContext(Dispatchers.Default) {
        queries.insertResumeAnalysis(id, userId, resumeText,summary , skillsParsed, skillsGaps,
             jobMatchScore, recommendations, createdAt)
        id
    }
    suspend fun getAllForUser(userId: String): List<ResumeAnalysis> = withContext(Dispatchers.Default) {
        queries.selectResumeAnalysesForUser(userId).executeAsList()
    }
    suspend fun getAnalysisById(id: String) = withContext(Dispatchers.Default) {
        queries.selectResumeAnalysisById(id).executeAsOneOrNull()
    }
}
