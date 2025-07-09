package com.dailydevchallenge.devstreaks.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class Challenge

@Serializable
@SerialName("CODE")
data class CodeChallenge(
    val title: String,
    val description: String,
    val starterCode: String,
    val solution: String
) : Challenge()

@Serializable
@SerialName("QUIZ")
data class QuizChallenge(
    val title: String,
    val questions: List<QuizQuestion>
) : Challenge()

@Serializable
@SerialName("FLASHCARD")
data class FlashcardChallenge(
    val title: String,
    val cards: List<Flashcard>
) : Challenge()


@Serializable
data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val answer: String
)

@Serializable
data class Flashcard(
    val front: String,
    val back: String
)