package com.dailydevchallenge.devstreaks.tts

expect class TTSHelper(context: Any) {
    fun speak(text: String)
    fun shutdown()
}

// shared/src/commonMain/kotlin/SpeechToTextHelper.kt
interface SpeechToTextHelper {
    fun startListening(onResult: (String) -> Unit)
    fun stopListening()
    val isListening: Boolean
}
