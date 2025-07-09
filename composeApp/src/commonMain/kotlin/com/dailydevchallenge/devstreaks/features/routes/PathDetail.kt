package com.dailydevchallenge.devstreaks.features.routes

import kotlinx.serialization.Serializable

@Serializable
data class PathDetail(val pathId: String)

@Serializable
data class LearnRoute(val taskId: String)

@Serializable
data class ChallengeFlowRoute(val taskId: String)



