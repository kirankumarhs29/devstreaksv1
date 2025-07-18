// shared/src/iosMain/kotlin/com/dailydevchallenge/devstreaks/tts/TTSHelper.kt
package com.dailydevchallenge.devstreaks.tts

import platform.AVFoundation.AVSpeechSynthesizer
import platform.AVFoundation.AVSpeechUtterance

actual class TTSHelper actual constructor(context: Any) {
    private val synthesizer = AVSpeechSynthesizer()

    actual fun speak(text: String) {
        val utterance = AVSpeechUtterance(text)
        synthesizer.speakUtterance(utterance)
    }

    actual fun shutdown() {
        // Optional: stop speech immediately
        if (synthesizer.speaking) {
            synthesizer.stopSpeakingAtBoundary(0) // 0 = AVSpeechBoundaryImmediate
        }
    }
}
