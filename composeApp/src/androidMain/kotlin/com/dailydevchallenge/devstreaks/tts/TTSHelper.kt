// shared/src/androidMain/kotlin/com/dailydevchallenge/devstreaks/tts/TTSHelper.kt
package com.dailydevchallenge.devstreaks.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

actual class TTSHelper actual constructor(context: Any) {
    private lateinit var tts: TextToSpeech
    private var isReady = false


    init {
        val androidContext = context as Context
        tts = TextToSpeech(androidContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.getDefault()
                tts.setSpeechRate(0.95f) // slightly slower for clarity than default (1.0)
                tts.setPitch(1.1f)       // slightly higher pitch to sound friendlier
                isReady = true
            }
        }
    }

    actual fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    actual fun shutdown() {
        tts.shutdown()
    }
}
