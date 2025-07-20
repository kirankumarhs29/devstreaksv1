package com.dailydevchallenge.devstreaks.tts

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import platform.AVFoundation.*
import platform.Foundation.*

class IOSConfettiSoundPlayer : ConfettiSoundPlayer {
    private var player: AVAudioPlayer? = null

    @OptIn(ExperimentalForeignApi::class)
    override fun play() {
        val path = NSBundle.mainBundle.pathForResource("confetti", ofType = "mp3")
        val url = NSURL.fileURLWithPath(path!!)
        player = AVAudioPlayer(contentsOfURL = url, error = null)
        player?.prepareToPlay()
        player?.play()
    }
}

actual fun getConfettiSoundPlayer(): ConfettiSoundPlayer {
    return IOSConfettiSoundPlayer()
}

