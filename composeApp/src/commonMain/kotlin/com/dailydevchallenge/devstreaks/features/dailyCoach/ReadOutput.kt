package com.dailydevchallenge.devstreaks.features.dailyCoach

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.tts.TTSHelper
import com.mohamedrejeb.calf.core.LocalPlatformContext
import com.mohamedrejeb.calf.core.PlatformContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import devstreaks.composeapp.generated.resources.Res
import io.github.alexzhirkevich.compottie.LottieAnimation
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter


object TTSController {

    private var ttsHelper: TTSHelper? = null
    fun init(context: PlatformContext) {
        if (ttsHelper == null) {
            ttsHelper = TTSHelper(context)
        }
    }

    fun speak(text: String, onDone: () -> Unit) {
        ttsHelper?.speak(text, onDone)
    }

    fun stop() {
        ttsHelper?.shutdown()
    }

}

@Composable
fun ReadOutput(content: String) {
    val context = LocalPlatformContext.current
    val ttsHelper = remember { TTSHelper(context) }

    val reply = try {
        Json.parseToJsonElement(content).jsonObject["reply"]?.jsonPrimitive?.content
    } catch (e: Exception) {
        // If it's not JSON, maybe it's already the reply
        content
    }
    if (!reply.isNullOrBlank()) {
        ttsHelper.speak(reply)
    }
    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.shutdown()
        }
    }
}

@Composable
fun DevCoachVoiceScreen(
    responseText: String,
    isSpeaking: Boolean,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated glowing face or AI avatar
        AnimatedDevCoachFace(isSpeaking = isSpeaking)

        Spacer(modifier = Modifier.height(24.dp))

        // Speaking visualizer
        if (isSpeaking) {
            DevCoachSpeechVisualizer(isSpeaking = true)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Response text
//        Text(
//            text = responseText,
//            style = MaterialTheme.typography.titleMedium,
//            textAlign = TextAlign.Center
//        )

        Spacer(modifier = Modifier.height(32.dp))

        // "Next" button appears after speaking
        if (!isSpeaking) {
            Button(onClick = onNext) {
                Text("Next")
            }
        }
    }
}

@Composable
fun AnimatedDevCoachFace(isSpeaking: Boolean) {
    val pulse = rememberInfiniteTransition()
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (isSpeaking) 1.2f else 1f,
        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse)
    )

    Icon(
        imageVector = Icons.Filled.Face,
        contentDescription = "DevCoach AI",
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale),
        tint = MaterialTheme.colorScheme.primary
    )
}



@Composable
fun DevCoachSpeechVisualizer(isSpeaking: Boolean) {
    val pulse = rememberInfiniteTransition()
    val height1 by pulse.animateValue(
        initialValue = 12.dp,
        targetValue = if (isSpeaking) 28.dp else 12.dp,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        )
    )

    val height2 by pulse.animateValue(
        initialValue = 16.dp,
        targetValue = if (isSpeaking) 32.dp else 16.dp,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        )
    )

    val height3 by pulse.animateValue(
        initialValue = 10.dp,
        targetValue = if (isSpeaking) 24.dp else 10.dp,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.width(6.dp).height(height1).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)))
        Box(Modifier.width(6.dp).height(height2).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)))
        Box(Modifier.width(6.dp).height(height3).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)))
    }
}


@Composable
fun DevCoachLottieAvatar() {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/AnimaBot.json").decodeToString()
        )
    }
    var isPlaying by remember { mutableStateOf(false) }
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        restartOnPlay = true,
        speed = 1.0f
    )
    LaunchedEffect(progress) {
        if (progress == 1f) {
            isPlaying = false
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .clickable {
                    isPlaying = true
                }
        ) {
            Image(
                rememberLottiePainter(
                    composition = composition,
                    progress = { progress }
                ), null)
        }
    }
}

@Composable
fun DevCoachLottieSpeakingAvatar(isSpeaking: Boolean) {
    val context = LocalPlatformContext.current
    val infiniteTransition = rememberInfiniteTransition()

    LaunchedEffect(Unit) {
        TTSController.init(context)
    }
    DisposableEffect(
        key1 = Unit,
        effect = {
            onDispose {
                TTSController.stop()
            }
        }
    )
    var isPlaying by remember { mutableStateOf(false) }

    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/AnimaBot.json").decodeToString()
        )
    }

    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isSpeaking,
        restartOnPlay = true,
        speed = 1.1f
    )
    LaunchedEffect(progress) {
        if (progress == 1f) {
            isPlaying = isSpeaking
        }
    }


    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            rememberLottiePainter(
                composition = composition,
                progress = { progress }), null, modifier = Modifier.size(200.dp)
        )
    }
}
