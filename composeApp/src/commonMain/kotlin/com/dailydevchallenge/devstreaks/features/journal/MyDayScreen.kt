package com.dailydevchallenge.devstreaks.features.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import kotlinx.datetime.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import kotlinx.coroutines.launch
import com.dailydevchallenge.devstreaks.data.Journal
import com.dailydevchallenge.devstreaks.repository.JournalRepository
import org.koin.compose.koinInject
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import com.dailydevchallenge.devstreaks.utils.getLogger
import com.dailydevchallenge.devstreaks.utils.logAnalyticsEvent


@Composable
fun MyDayScreen(repository: JournalRepository = koinInject()) {
    val focusManager = LocalFocusManager.current
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var selectedSentiment by remember { mutableStateOf<String?>(null) }
    var journalEntries by remember { mutableStateOf(listOf<Journal>()) }
    val logger = remember { getLogger() }

    val sentiments = listOf(
        "🚀 I made progress today",
        "🌿 I showed up, and that’s enough",
        "🌧️ Today was tough, but I tried"
    )
    LaunchedEffect(Unit) {
        journalEntries = repository.getAllJournals()
        logger.log("MyDayScreen viewed, loaded ${journalEntries.size} entries")
    }
    fun saveEntry() {
        scope.launch {
            val journal = Journal(
                title = selectedSentiment ?: "",
                content = text
            )
            repository.addJournal(journal)
            journalEntries = repository.getAllJournals()
            text = ""
            selectedSentiment = null
            snackBarHostState.showSnackbar("🎉 Saved to journal!",
                withDismissAction = true,
                duration = SnackbarDuration.Short)
            logAnalyticsEvent("journal_entry_saved", mapOf(
                "sentiment" to (selectedSentiment ?: "none"),
                "content_length" to text.length.toString(),
                "timestamp" to Clock.System.now().toEpochMilliseconds().toString()
            ))
            logger.log("Journal entry saved: ${journal.title}, content length: ${text.length}")

        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Text(
                        "🧠 Reflect + Track Your Day",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        sentiments.forEach { sentiment ->
                            Card(
                                onClick = { selectedSentiment = sentiment },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedSentiment == sentiment)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (selectedSentiment == sentiment)
                                        MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    sentiment,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(16.dp)
                    ) {
                        if (text.isEmpty()) {
                            Text(
                                "Write about something you learned, struggled with, or felt today...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BasicTextField(
                            value = text,
                            onValueChange = { text = it },
                            textStyle = TextStyle.Default.copy(color = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                item {
                    Button(
                        onClick = {
                            if (text.isNotBlank() && selectedSentiment != null) {
                                saveEntry()
                                focusManager.clearFocus()
                                logger.log("Save button clicked with sentiment: $selectedSentiment, content length: ${text.length}")
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Reflection")
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text(
                        "📜 Your Past Entries",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        "➡️ Tip: Swipe an entry left or right to delete it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                }

                items(journalEntries.reversed(), key = { it.id }) { entry ->
                    SwipeToDeleteCard(journal = entry) {
                        scope.launch {
                            val deletedJournal = entry
                            repository.deleteJournal(deletedJournal.id)
                            journalEntries = repository.getAllJournals()

                            logger.log("Journal entry deleted: ${deletedJournal.title}, id: ${deletedJournal.id}")

                            val result = snackBarHostState.showSnackbar(
                                message = "Entry deleted",
                                withDismissAction = true,
                                duration = SnackbarDuration.Short
                            )

                            if (result == SnackbarResult.ActionPerformed) {
                                repository.addJournal(deletedJournal)
                                journalEntries = repository.getAllJournals()
                            }

                        }
                    }
                }

            }
        }
    }
}


@Composable
fun SwipeToDeleteCard(
    journal: Journal,
    onDelete: () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val threshold = 300f // Swipe threshold to trigger delete
    val deleted = remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Delete Entry?") },
            text = { Text("Are you sure you want to delete this journal entry? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    if (deleted.value) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 15.dp)
            .background(MaterialTheme.colorScheme.errorContainer) // red background while swiping
    ) {
        // Background content while swiping
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Swipe to delete",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onErrorContainer),
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
        }

        Card(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.toInt(), 0) }
                .pointerInput(journal.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX.value > threshold || offsetX.value < -threshold) {
//                                scope.launch {
//                                    offsetX.animateTo(
//                                        targetValue = 1000f * if (offsetX.value > 0) 1 else -1,
//                                        animationSpec = tween(300)
//                                    )
//                                    deleted.value = true
//                                    onDelete()
//                                }
                                showDialog = true // just show dialog
                            } else {
                                scope.launch {
                                    offsetX.animateTo(0f, tween(300))
                                }
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount)
                            }
                        }
                    )
                }
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(journal.title, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                val localDateTime = Instant.fromEpochMilliseconds(journal.timestamp)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                val formattedDate = "${localDateTime.dayOfMonth} ${
                    localDateTime.month.name.lowercase().replaceFirstChar { it.uppercase() }
                } ${localDateTime.year}"

                Text(formattedDate, style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(6.dp))
                Text(journal.content, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}



//@Composable
//fun JournalEntryCard(entry: Journal, onDelete: (String) -> Unit) {
//    val localDateTime = Instant.fromEpochMilliseconds(entry.timestamp)
//        .toLocalDateTime(TimeZone.currentSystemDefault())
//    val formattedDate = "${localDateTime.dayOfMonth} ${localDateTime.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${localDateTime.year}"
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 4.dp),
//        shape = MaterialTheme.shapes.medium,
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surface,
//            contentColor = MaterialTheme.colorScheme.onSurface
//        )
//    ) {
//        Column(modifier = Modifier.padding(16.dp)) {
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.SpaceBetween,
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Column {
//                    Text(entry.title, style = MaterialTheme.typography.labelLarge)
//                    Text(formattedDate, style = MaterialTheme.typography.labelSmall)
//                }
//                IconButton(onClick = { onDelete(entry.id) }) {
//                    Icon(Icons.Default.Delete, contentDescription = "Delete")
//                }
//            }
//            Spacer(modifier = Modifier.height(6.dp))
//            Text(entry.content, style = MaterialTheme.typography.bodyMedium)
//        }
//    }
//}

