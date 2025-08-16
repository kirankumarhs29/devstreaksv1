package com.dailydevchallenge.devstreaks.tts

expect class TTSHelper(context: Any) {
    fun speak(text: String, onDone: () -> Unit = {})
    fun shutdown()
}

// shared/src/commonMain/kotlin/SpeechToTextHelper.kt
interface SpeechToTextHelper {
    fun startListening(onResult: (String) -> Unit)
    fun stopListening()
    val isListening: Boolean
}

interface ConfettiSoundPlayer {
    fun play(toneType: String)
}
expect fun getConfettiSoundPlayer(): ConfettiSoundPlayer
