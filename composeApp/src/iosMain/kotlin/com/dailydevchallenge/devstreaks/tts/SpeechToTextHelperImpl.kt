package com.dailydevchallenge.devstreaks.tts

// shared/src/iosMain/kotlin/com/dailydevchallenge/devstreaks/tts/SpeechToTextHelperImpl.kt

class IosSpeechToTextHelper : SpeechToTextHelper {
    override val isListening: Boolean
        get() = false // Provide actual state via Swift/ObjectiveC bridge

    override fun startListening(onResult: (String) -> Unit) {
        // Call a platform method (SFSpeechRecognizer, Swift)
        // Pass result back to Compose via the callback
    }
    override fun stopListening() {
        // Stop iOS recognition
    }
}
