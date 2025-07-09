package com.dailydevchallenge.devstreaks.features.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevStreakTopBar(
    title: String = "DevStreak",
    onBack: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                start = 16.dp,
                end = 16.dp,
                bottom = 12.dp
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "DevStreak Icon",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = "Code. Learn. Repeat.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            actions?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    content = it
                )
            }
        }
    }
}
