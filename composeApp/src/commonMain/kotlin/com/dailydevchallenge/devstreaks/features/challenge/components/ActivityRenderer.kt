package com.dailydevchallenge.devstreaks.features.challenge.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.features.challenge.ActivityPagerItem
import com.dailydevchallenge.devstreaks.features.challenge.ChallengeMiniIntro
import com.dailydevchallenge.devstreaks.features.dailyCoach.AILessonCard
import com.dailydevchallenge.devstreaks.features.dailyCoach.DevCoachSpeechVisualizer
import com.dailydevchallenge.devstreaks.features.dailyCoach.DevCoachVoiceScreen
import com.dailydevchallenge.devstreaks.features.dailyCoach.ReadOutput
import com.dailydevchallenge.devstreaks.features.dailyCoach.TTSController
import com.dailydevchallenge.devstreaks.llm.GeminiLLMService
import com.dailydevchallenge.devstreaks.llm.LLMService
import com.dailydevchallenge.devstreaks.llm.PromptBuilder
import com.dailydevchallenge.devstreaks.model.ActivityType
import com.dailydevchallenge.devstreaks.model.ChallengeActivity
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import com.mohamedrejeb.calf.core.LocalPlatformContext
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.compose.koinInject

@Composable
fun ActivityRenderer(activity: ChallengeActivity, onComplete: (String) -> Unit = {}) {
    Spacer(Modifier.height(16.dp))
    when (activity.type) {
        ActivityType.QUIZ -> QuizCard(activity, onComplete = { onComplete(activity.id) })
        ActivityType.CODE -> CodeChallengeCard(activity, onComplete = { onComplete(activity.id) })
        ActivityType.FLASHCARD -> FlashcardActivityCard(activity, onComplete = { onComplete(activity.id) })
        ActivityType.PROJECT -> ProjectActivityCard(activity, onComplete = { onComplete(activity.id) })
        ActivityType.AI_LESSON -> AILessonCard(activity, onComplete = { onComplete(activity.id) })
    }
    activity.videoUrl?.let {
        Spacer(Modifier.height(12.dp))
        Text("🎥 Learn More:", style = MaterialTheme.typography.labelMedium)
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }

    activity.explanation?.let {
        Spacer(Modifier.height(8.dp))
        Text("ℹ️ Explanation:", style = MaterialTheme.typography.labelSmall)
        Text(it, style = MaterialTheme.typography.bodySmall)
    }
}


@Composable
fun ActivityPager(
    items: List<ActivityPagerItem>,
    onAllCompleted: () -> Unit,
    isChallengeCompleted: Boolean,
    challengeRepository: ChallengeRepository
) {
    val pageCount = items.size
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })
    var currentPage by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val completedIds = remember { mutableStateListOf<String>() }
    val allDone = completedIds.size == items.size
    var startTime by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    var engagementLogged by remember { mutableStateOf(false) }



    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
       HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(bottom = 80.dp). fillMaxSize(),
                    userScrollEnabled = false
        ) { page ->
           val item = items[page]
           val isLast = page == items.lastIndex
           Column(
               modifier = Modifier
                   .fillMaxSize()
                   .padding(4.dp),
               verticalArrangement = Arrangement.spacedBy(12.dp)
           ) {
               ChallengeMiniIntro(
                   title = "Focus: ${item.challenge?.skillFocus ?: "Skill Focus"}",
                   insight = item.challenge?.insight ?: "Slow down and reason through each line.",
                   goal = item.challenge?.goal ?: "Avoid common traps by spotting logical flaws."
               )
               ActivityCardRenderer(item, onComplete = {
                   if (!completedIds.contains(item.id)) completedIds.add(item.id)

                   if (isLast && !engagementLogged && !isChallengeCompleted) {
                       val endTime = Clock.System.now().toEpochMilliseconds()
                       val taskId = item.challenge?.id
                       val userId = UserPreferences.getSafeUserId()

                       if (taskId != null) {
                           coroutineScope.launch {
                               challengeRepository.logEngagementTime(
                                   taskId = taskId,
                                   userId = userId,
                                   startTime = startTime,
                                   endTime = endTime
                               )
                               engagementLogged = true
                           }
                       }
                   }

                   if (!isLast) {
                       coroutineScope.launch { pagerState.animateScrollToPage(page + 1) }
                   }
               }, isLast = isLast)

           }
       }
    PagerIndicator(
        pageCount = items.size,
        currentPageIndex = pagerState.currentPage,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp)
    )
    if (pagerState.currentPage == 0) {
        Text(
            "Swipe left to continue",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
        if (allDone && !isChallengeCompleted) {
           Button(
                onClick = onAllCompleted,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Text("✅ Mark as Done")
            }

        }
}
}


@Composable
fun PagerIndicator(pageCount: Int, currentPageIndex: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pageCount) { index ->
//            val color = if (currentPageIndex == index) Color.DarkGray else Color.LightGray
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .clip(CircleShape)
                    .size(if (currentPageIndex == index) 10.dp else 8.dp)
                    .background(
                        if (currentPageIndex == index) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
            )
        }
    }
}

@Composable
fun ActivityCardRenderer(
    item: ActivityPagerItem,
    onComplete: () -> Unit,
    isLast: Boolean
) {
    when (item.type) {
        ActivityType.QUIZ.toString() -> {
            QuizCard(item.challenge!!, onComplete)
        }
        ActivityType.CODE.toString() -> {
            CodeChallengeCard(item.challenge!!, onComplete)
        }
        ActivityType.FLASHCARD.toString() -> {
            FlashcardActivityCard(item.challenge!!, onComplete)
        }
        ActivityType.PROJECT.toString() -> {
            ProjectActivityCard(item.challenge!!, onComplete)
        }
        // For info/tip/bonus/AI cards, simple button
        "why", "tip", "bonus", "aiBreakdown" -> {
            InfoCardWithNext(item, onComplete, isLast)
        }
        else -> {
            // Fallback
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = onComplete) { Text(if (isLast) "Finish" else "Next") }
            }
        }
    }
}

@Composable
fun InfoCardWithNext(item: ActivityPagerItem, onComplete: () -> Unit, isLast: Boolean) {
    val llmService: LLMService = koinInject()
    val coroutineScope = rememberCoroutineScope()
    var aiFeedback by remember { mutableStateOf<String?>(null) }
    var isDisplayed by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    val localContext = LocalPlatformContext.current
    LaunchedEffect(Unit) {
        TTSController.init(localContext)
    }
    DisposableEffect(Unit) {
        onDispose {
            TTSController.stop() // Stops on screen exit
        }
    }
    LaunchedEffect(aiFeedback) {
        if (!aiFeedback.isNullOrBlank()) {
            isSpeaking = true
            TTSController.speak(aiFeedback!!) {
                isSpeaking = false // triggers UI update
            }
        }
    }



    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(item.content ?: "")
        Spacer(Modifier.height(16.dp))
        if (item.type == "aiBreakdown") {
            if (!isDisplayed) {
                Button(
                    onClick = {
                        val prompt = item.content?.let { PromptBuilder.getSystemResponse(it) }
                        coroutineScope.launch {
                            if (prompt != null) {
                                isLoading = true
                                val response = llmService.generateResponse(prompt)
                                aiFeedback = response
                                isDisplayed = true // Hide button after response
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ask DevCoach to Speak")
                }
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(12.dp)
                            .size(32.dp),
                        strokeWidth = 3.dp
                    )
                }

            }
            Spacer(Modifier.height(8.dp))
            // display AI response here if needed
            if (!aiFeedback.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                isDisplayed = true
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("🧠 DevCoach says:", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        if (isSpeaking) {
                            DevCoachVoiceScreen(aiFeedback!! , isSpeaking = true , onNext =
                                onComplete)
                            Spacer(Modifier.height(8.dp))
                        }
//                        TTSController.speak(aiFeedback!!)
//                        Text(aiFeedback!!, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

        }
        if (item.type == "aiBreakdown") {
            if (isDisplayed) {
                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (isLast) "Finish" else "Next") }
            }
        } else if (item.type != "aiBreakdown") {
            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (isLast) "Finish" else "Next") }
        }
    }
}


