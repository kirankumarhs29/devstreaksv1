package com.dailydevchallenge.devstreaks.database

fun com.dailydevchallenge.database.ResumeAnalysis.toModel(): com.dailydevchallenge.devstreaks.model.ResumeAnalysis {
    return com.dailydevchallenge.devstreaks.model.ResumeAnalysis(
        id = this.id,
        userId = this.userId,
        summary = this.summary.orEmpty(),
        skillsMatched = this.skillsParsed?.split(",") ?: emptyList(),
        skillsMissing = this.skillsGaps?.split(",") ?: emptyList(),
        jobMatchScore = this.jobMatchScore ?: 0, // handle nullability if present
        recommendations = this.recommendations.orEmpty(),
        createdAt = this.createdAt
    )
}
// Extension function in database package
fun com.dailydevchallenge.database.InterviewQuestion.toModel(): com.dailydevchallenge.devstreaks.model.InterviewQuestion =
    com.dailydevchallenge.devstreaks.model.InterviewQuestion(
        id = id,
        question = questionText,
        topic = topic.orEmpty(),
        difficulty = difficulty?.toInt() ?: 0,
        followUp = followUp.orEmpty()
    )



