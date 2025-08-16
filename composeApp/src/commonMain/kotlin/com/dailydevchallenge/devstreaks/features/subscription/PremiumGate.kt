package com.dailydevchallenge.devstreaks.features.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun PremiumGate(
    feature: String,
    description: String,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val subscriptionViewModel: SubscriptionViewModel = org.koin.compose.koinInject()
    val isPremium = subscriptionViewModel.isPremiumUser()

    if (isPremium) {
        content()
    } else {
        PaywallDialog(
            feature = feature,
            description = description,
            onUpgrade = onUpgrade,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun PaywallDialog(
    feature: String,
    description: String,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Premium icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(40.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Diamond,
                        contentDescription = "Premium",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Feature title
                Text(
                    text = "Unlock $feature",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Premium features preview
                PremiumFeaturesList()

                Spacer(modifier = Modifier.height(24.dp))

                // Upgrade button
                Button(
                    onClick = onUpgrade,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Upgrade,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Upgrade to Pro",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dismiss button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Maybe Later")
                }
            }
        }
    }
}

@Composable
private fun PremiumFeaturesList() {
    val features = listOf(
        "🤖 AI-powered challenges" to "Personalized to your skill level",
        "📊 Advanced analytics" to "Track your coding progress",
        "🏆 Friend leaderboards" to "Compete with colleagues",
        "�� Unlimited content" to "Access all challenge packs"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "DevStreak Pro includes:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            features.forEach { (title, description) ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Composable function to easily gate premium features
@Composable
fun PremiumFeature(
    feature: String,
    description: String = "This feature requires DevStreak Pro",
    content: @Composable () -> Unit
) {
    val subscriptionViewModel: SubscriptionViewModel = org.koin.compose.koinInject()
    var showPaywall by remember { mutableStateOf(false) }

    if (subscriptionViewModel.isPremiumUser()) {
        content()
    } else {
        // Show locked state with upgrade prompt
        PremiumLockedCard(
            feature = feature,
            onUnlock = { showPaywall = true }
        )

        if (showPaywall) {
            PaywallDialog(
                feature = feature,
                description = description,
                onUpgrade = {
                    subscriptionViewModel.showPaywall()
                    showPaywall = false
                },
                onDismiss = { showPaywall = false }
            )
        }
    }
}

@Composable
private fun PremiumLockedCard(
    feature: String,
    onUnlock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$feature (Pro Feature)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Upgrade to unlock this feature",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onUnlock,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        Icons.Default.Diamond,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Unlock")
                }
            }
        }
    }
}

// Utility composable for inline premium badges
@Composable
fun PremiumBadge() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFD700),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Diamond,
                contentDescription = "Premium",
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "PRO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
