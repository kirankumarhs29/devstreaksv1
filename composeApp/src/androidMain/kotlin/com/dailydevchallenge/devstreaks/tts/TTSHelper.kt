// shared/src/androidMain/kotlin/com/dailydevchallenge/devstreaks/tts/TTSHelper.kt
package com.dailydevchallenge.devstreaks.tts

import android.content.Context
import android.os.Build
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.annotation.RequiresApi
import com.dailydevchallenge.devstreaks.utils.getLogger
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.util.InternalAPI
// base64 decoding
import java.io.File
import java.util.Base64

actual class TTSHelper actual constructor(context: Any) {
    private val androidContext = context as Context
    private lateinit var tts: TextToSpeech
    private var isReady = false
    private val client by lazy { HttpClient() }
    private var mediaPlayer: MediaPlayer? = null
    private val audioCache: MutableMap<String, File> = mutableMapOf()


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

    @RequiresApi(Build.VERSION_CODES.O)
    actual fun speak(text: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val cacheFile = audioCache[text]
            if (cacheFile != null && cacheFile.exists()) {
                playMp3(cacheFile)
            } else {
                val bytes = try { fetchCloudTTS(text) } catch (_: Exception) { null }
                if (bytes != null) {
                    val tempFile = File.createTempFile("cloud_tts", ".mp3", androidContext.cacheDir)
                    tempFile.writeBytes(bytes)
                    audioCache[text] = tempFile
                    playMp3(tempFile)
                } else {
                    // Fallback to local TTS
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
        }
    }

    actual fun shutdown() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        audioCache.values.forEach { it.delete() }
        audioCache.clear()
        tts.shutdown()
    }
    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(InternalAPI::class)
    private suspend fun fetchCloudTTS(text: String): ByteArray? {
        val apiKey = "AIzaSyA7WGPl8-i1p1Ja1avxjZeKvrR0BlbxvEo" // Replace with your key
        val url = "https://texttospeech.googleapis.com/v1/text:synthesize?key=$apiKey"
        val bodyJson = """
            {
                "input": {"text":"$text"},
                "voice": {
                    "languageCode":"en-US",
                    "name": "en-US-Wavenet-F",
                    "ssmlGender":"FEMALE"
                    },
                "audioConfig": {"audioEncoding":"MP3"}
            }
        """.trimIndent()
        val response: HttpResponse = client.post(url) {
            body = TextContent(bodyJson, ContentType.Application.Json)
        }
        val logger = getLogger()
        if (response.status.value != 200) {
            logger.e("TTS request failed: ${response.status.value} - ${response.bodyAsText()}")
            return null
        }
        val bodyStr = response.bodyAsText()
        logger.d("TTS response: $bodyStr")
        val base64 = Regex("\"audioContent\"\\s*:\\s*\"([^\"]+)\"").find(bodyStr)?.groupValues?.get(1)
        return base64?.let { Base64.getDecoder().decode(it) }
    }

    // Play MP3 bytes using MediaPlayer
    private fun playMp3(file: File) {
        // Save to temp file then play
        mediaPlayer?.stop()
        mediaPlayer?.release()
//        val tempFile = File.createTempFile("cloud_tts", ".mp3", androidContext.cacheDir)
//        tempFile.writeBytes(bytes)
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            start()
        }
//        player.setOnCompletionListener {
//            it.release()
//            tempFile.delete()
//        }
    }
}
