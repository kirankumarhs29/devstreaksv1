package com.dailydevchallenge.devstreaks.tts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale
import kotlin.properties.Delegates

class AndroidSpeechToTextHelper(private val context: Context) : SpeechToTextHelper {
    // Backed by a mutable variable to allow Compose to observe state
    private var _isListening: Boolean by Delegates.observable(false) { _, _, _ -> }
    override val isListening: Boolean get() = _isListening

    private var speechRecognizer: SpeechRecognizer? = null
    private var resultCallback: ((String) -> Unit)? = null

    override fun startListening(onResult: (String) -> Unit) {
        if (_isListening) return

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        resultCallback = onResult
        _isListening = true

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now…")
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                _isListening = false
                resultCallback?.invoke("") // Or handle cancelled/error differently
            }

            override fun onResults(results: Bundle?) {
                _isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                resultCallback?.invoke(matches?.firstOrNull() ?: "")
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    override fun stopListening() {
        _isListening = false
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
