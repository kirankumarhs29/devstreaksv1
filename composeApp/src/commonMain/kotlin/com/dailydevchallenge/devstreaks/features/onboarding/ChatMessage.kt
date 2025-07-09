package com.dailydevchallenge.devstreaks.features.onboarding

import kotlinx.datetime.Clock

enum class InputType {
    TEXT,
    MULTI_CHOICE,
    TIME_PICKER,
    NONE
}
@OptIn(kotlin.time.ExperimentalTime::class)
data class ChatMessage(
    val id: String = Clock.System.now().toEpochMilliseconds().toString(),
    val text: String,
    val isUser: Boolean,
    val inputType: InputType = InputType.NONE,
    val options: List<String> = emptyList(), // for MULTI_CHOICE
    val response: String? = null
)
