package com.dailydevchallenge.devstreaks.tts

import android.content.Context
import android.media.MediaPlayer
import com.dailydevchallenge.devstreaks.R // Place confetti.mp3 in `androidApp/src/main/res/raw`
import com.dailydevchallenge.devstreaks.session.getAppContext



class AndroidConfettiSoundPlayer(private val context: Context) : ConfettiSoundPlayer {
    override fun play() {
        val mediaPlayer = MediaPlayer.create(context, R.raw.success)
        mediaPlayer?.start()
        mediaPlayer.setOnCompletionListener {
            it.release()
        }
    }
}


actual fun getConfettiSoundPlayer(): ConfettiSoundPlayer {
    val context = getAppContext()
    return AndroidConfettiSoundPlayer(context) // use actual application context
}

