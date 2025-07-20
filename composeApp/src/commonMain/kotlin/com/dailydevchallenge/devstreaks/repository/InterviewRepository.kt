package com.dailydevchallenge.devstreaks.repository

import com.dailydevchallenge.database.*
import com.dailydevchallenge.devstreaks.database.toModel
import com.dailydevchallenge.devstreaks.model.InterviewQuestion
import com.dailydevchallenge.devstreaks.utils.generateUUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InterviewRepository(
    private val queries: ResumeAnalysisQueries
) {
    suspend fun createSession(userId: String, resumeAnalysisId: String, summary: String, sessionDate: Long): String =
        withContext(Dispatchers.Default) {
            val id = generateUUID()
            queries.insertInterviewSession(id, userId, resumeAnalysisId, sessionDate, summary)
            id
        }

    suspend fun getSessionById(id: String) = withContext(Dispatchers.Default) {
        queries.selectInterviewSessionById(id).executeAsOneOrNull()
    }

    suspend fun saveQuestion(id: String, questionText: String, topic: String?, difficulty: Int?,
                             followUp: String, lastAskedAt: Long?, timesAsked: Int) =
        withContext(Dispatchers.Default) {
                queries.insertInterviewQuestion(id, questionText, topic, difficulty?.toLong(),
                    followUp,
                    lastAskedAt,
                    timesAsked.toLong())
        }

    suspend fun getAnswersForUser(userId: String): List<UserAnswer> = withContext(Dispatchers.Default) {
        queries.selectUserAnswersForUser(userId).executeAsList()
    }
    suspend fun getAllQuestions(): List<InterviewQuestion> = withContext(Dispatchers.Default) {
        queries.selectAllInterviewQuestions().executeAsList().map { it.toModel() }
    }

    suspend fun updateUserAnswerFeedback(
        questionId: String,
        sessionId: String,
        feedback: String
    ) = withContext(Dispatchers.Default) {
        queries.updateFeedbackForAnswer(feedback, questionId, sessionId)
    }


    suspend fun saveUserAnswer(
        questionId: String,
        sessionId: String,
        answerText: String,
        feedback: String?,
        score: Int?,
        topic: String?,
        timestamp: Long
    ): String = withContext(Dispatchers.Default) {
        val id = generateUUID()
        queries.insertUserAnswer(id, questionId, sessionId, answerText, feedback, score?.toLong()
            ,topic, timestamp)
        id
    }

    suspend fun getAnswersForSession(sessionId: String) = withContext(Dispatchers.Default) {
        queries.selectUserAnswersBySession(sessionId).executeAsList()
    }
    suspend fun getSessionsForUser(userId: String): List<InterviewSession> =
        withContext(Dispatchers.Default) {
            queries.selectInterviewSessionsForUser(userId).executeAsList()
        }

}
