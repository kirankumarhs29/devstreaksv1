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
import com.dailydevchallenge.devstreaks.model.ActivityType
import com.dailydevchallenge.devstreaks.model.ChallengeActivity
import kotlinx.coroutines.launch

@Composable
fun ActivityRenderer(activity: ChallengeActivity, onComplete: (String) -> Unit = {}) {
    Spacer(Modifier.height(16.dp))
    when (activity.type) {
        ActivityType.QUIZ -> QuizCard(activity, onComplete = { onComplete(activity.id) })
        ActivityType.CODE -> CodeChallengeCard(activity, onComplete = { onComplete(activity.id) })
        ActivityType.FLASHCARD -> FlashcardActivityCard(activity, onComplete = { onComplete(activity.id) })
        ActivityType.PROJECT -> ProjectActivityCard(activity, onComplete = { onComplete(activity.id) })
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
    isChallengeCompleted: Boolean
) {
    val pageCount = items.size
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })
    var currentPage by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val completedIds = remember { mutableStateListOf<String>() }
    val allDone = completedIds.size == items.size

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
            ActivityCardRenderer(item, onComplete = {
                if (!completedIds.contains(item.id)) completedIds.add(item.id)
                if (!isLast) {
                    coroutineScope.launch { pagerState.animateScrollToPage(page + 1) }
                }
            }, isLast = isLast)
        }
//            when (item.type) {
//                ActivityType.QUIZ.toString() -> item.challenge.let { challenge ->
//                    challenge?.let {
//                        QuizCard(challenge, onViewed = {
//                            onActivityViewed(challenge.id)
//                        })
//                    } ?: Text(
//                        "Missing " +
//                                "quiz data", Modifier.padding(20.dp)
//                    )
//                }
//
//                ActivityType.CODE.toString() -> {
//                    item.challenge.let { challenge ->
//                        challenge?.let {
//                            CodeChallengeCard(
//                                challenge,
//                                onViewed = { onActivityViewed(challenge.id) })
//                        }
//                    } ?: Text("Missing code data", Modifier.padding(20.dp))
//                }
//
//                ActivityType.FLASHCARD.toString() -> {
//                    item.challenge.let { challenge ->
//                        challenge?.let {
//                            FlashcardActivityCard(challenge, onViewed = {
//                                onActivityViewed(challenge.id)
//                            })
//                        }
//                    } ?: Text("Missing flashcard data", Modifier.padding(20.dp))
//                }
//
//                ActivityType.PROJECT.toString() -> {
//                    item.challenge.let { challenge ->
//                        challenge?.let {
//                            ProjectActivityCard(
//                                challenge,
//                                onViewed = { onActivityViewed(challenge.id) })
//                        }
//                    } ?: Text("Missing project data", Modifier.padding(20.dp))
//                }
//
//                "why" -> WhyItMattersCard(item.content ?: "")
//                "tip" -> TipCard(item.content ?: "")
//                "bonus" -> BonusCard(item.content ?: "")
//                "aiBreakdown" -> AIBreakdownCard(item.content ?: "")
//                else -> Text(item.content ?: "Unknown card", Modifier.padding(20.dp))
//            }
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
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(item.content ?: "")
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (isLast) "Finish" else "Next") }
    }
}


