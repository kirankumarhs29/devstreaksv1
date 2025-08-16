package com.dailydevchallenge.devstreaks.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.cache.rememberCachedImage

@Composable
fun CachedAvatarImage(
    avatarUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    loadingSize: androidx.compose.ui.unit.Dp = 24.dp,
    fallbackSize: androidx.compose.ui.unit.Dp = 40.dp
) {
    var isLoading by remember(avatarUrl) { mutableStateOf(false) }
    var hasError by remember(avatarUrl) { mutableStateOf(false) }

    // Debug: Log when avatarUrl changes
    LaunchedEffect(avatarUrl) {
        println("CachedAvatarImage: avatarUrl changed to: $avatarUrl")
    }

    val cachedImage = rememberCachedImage(
        url = avatarUrl,
        onLoadingChange = { loading ->
            isLoading = loading
        },
        onError = { error ->
            hasError = true
            println("Avatar loading error: ${error?.message}")
        }
    )

    when {
        cachedImage != null -> {
            // Successfully loaded and cached image
            Image(
                bitmap = cachedImage,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }

        isLoading -> {
            // Loading state
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(loadingSize),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        hasError || avatarUrl.isNullOrBlank() -> {
            // Error or no avatar state
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = contentDescription,
                    tint = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(fallbackSize)
                )
            }
        }
    }
}
