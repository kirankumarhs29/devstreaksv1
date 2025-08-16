package com.dailydevchallenge.devstreaks.cache

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import io.github.alexzhirkevich.compottie.internal.platform.fromBytes
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

expect class PersistentImageCache {
    suspend fun getCachedImage(url: String): ByteArray?
    suspend fun cacheImage(url: String, imageBytes: ByteArray)
    suspend fun clearCache()
}

expect fun getPlatformImageCache(): PersistentImageCache

@OptIn(ExperimentalEncodingApi::class)
@Composable
fun rememberCachedImage(
    url: String?,
    onLoadingChange: (Boolean) -> Unit = {},
    onError: (Throwable?) -> Unit = {}
): ImageBitmap? {
    var imageBitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        if (url.isNullOrBlank()) {
            imageBitmap = null
            return@LaunchedEffect
        }

        println("rememberCachedImage: Starting load for URL: $url")

        try {
            isLoading = true
            onLoadingChange(true)

            val cache = getPlatformImageCache()

            // Try to get from cache first
            println("rememberCachedImage: Checking cache for URL: $url")
            val cachedBytes = cache.getCachedImage(url)
            if (cachedBytes != null) {
                println("rememberCachedImage: Found cached image, converting to bitmap")
                imageBitmap = withContext(Dispatchers.Default) {
                    try {
                        ImageBitmap.fromBytes(cachedBytes)
                    } catch (e: Exception) {
                        println("rememberCachedImage: Error converting cached bytes to bitmap: ${e.message}")
                        null
                    }
                }
                if (imageBitmap != null) {
                    println("rememberCachedImage: Successfully loaded from cache")
                    isLoading = false
                    onLoadingChange(false)
                    return@LaunchedEffect
                }
            }

            // If not in cache or cache is corrupted, download and cache
            if (url.startsWith("https://") || url.startsWith("gs://")) {
                println("rememberCachedImage: Cache miss, downloading from: $url")
                val httpClient = HttpClient()
                try {
                    val imageBytes = httpClient.get(url).readBytes()

                    // Cache the downloaded image
                    cache.cacheImage(url, imageBytes)

                    // Convert to ImageBitmap
                    imageBitmap = withContext(Dispatchers.Default) {
                        try {
                            ImageBitmap.fromBytes(imageBytes)
                        } catch (e: Exception) {
                            println("rememberCachedImage: Error converting downloaded bytes to bitmap: ${e.message}")
                            null
                        }
                    }
                    println("rememberCachedImage: Successfully downloaded and cached")
                } catch (e: Exception) {
                    println("rememberCachedImage: Download error: ${e.message}")
                    onError(e)
                } finally {
                    httpClient.close()
                }
            } else {
                // Handle Base64 data
                try {
                    val imageBytes = Base64.Default.decode(url)
                    imageBitmap = withContext(Dispatchers.Default) {
                        try {
                            ImageBitmap.fromBytes(imageBytes)
                        } catch (e: Exception) {
                            null
                        }
                    }
                } catch (e: Exception) {
                    onError(e)
                }
            }
        } catch (e: Exception) {
            println("rememberCachedImage: General error: ${e.message}")
            onError(e)
        } finally {
            isLoading = false
            onLoadingChange(false)
        }
    }

    return imageBitmap
}
