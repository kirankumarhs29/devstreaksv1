package com.dailydevchallenge.devstreaks.cache

import android.annotation.SuppressLint
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

actual class PersistentImageCache(private val context: Context) {

    // Use filesDir instead of cacheDir for better persistence
    private val cacheDir = File(context.filesDir, "avatar_images")

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        println("PersistentImageCache: Initialized with directory: ${cacheDir.absolutePath}")
    }

    actual suspend fun getCachedImage(url: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = generateFileName(url)
                val file = File(cacheDir, fileName)

                println("PersistentImageCache: Looking for cached image: ${file.absolutePath}")
                println("PersistentImageCache: File exists: ${file.exists()}")
                if (file.exists()) {
                    println("PersistentImageCache: File size: ${file.length()} bytes")
                    println("PersistentImageCache: File age: ${(System.currentTimeMillis() - file.lastModified()) / 1000} seconds")
                }

                if (file.exists() && isFileValid(file)) {
                    println("PersistentImageCache: Loading image from cache")
                    file.readBytes()
                } else {
                    println("PersistentImageCache: Image not found in cache or invalid")
                    null
                }
            } catch (e: Exception) {
                println("PersistentImageCache: Error reading cached image: ${e.message}")
                null
            }
        }
    }

    actual suspend fun cacheImage(url: String, imageBytes: ByteArray) {
        withContext(Dispatchers.IO) {
            try {
                val fileName = generateFileName(url)
                val file = File(cacheDir, fileName)
                println("PersistentImageCache: Caching image to: ${file.absolutePath}")
                println("PersistentImageCache: Image size: ${imageBytes.size} bytes")
                file.writeBytes(imageBytes)
                println("PersistentImageCache: Image cached successfully")
            } catch (e: Exception) {
                println("PersistentImageCache: Error caching image: ${e.message}")
            }
        }
    }

    actual suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            try {
                cacheDir.listFiles()?.forEach { file ->
                    file.delete()
                }
            } catch (e: Exception) {
                // Ignore clear errors
            }
        }
    }

    private fun generateFileName(url: String): String {
        val md5 = MessageDigest.getInstance("MD5")
        val hash = md5.digest(url.toByteArray())
        return hash.joinToString("") { "%02x".format(it) } + ".jpg"
    }

    private fun isFileValid(file: File): Boolean {
        // Check if file is not too old (7 days) and has content
        val maxAge = 7 * 24 * 60 * 60 * 1000L // 7 days in milliseconds
        return file.length() > 0 && (System.currentTimeMillis() - file.lastModified()) < maxAge
    }
}

// Singleton instance for Android
@SuppressLint("StaticFieldLeak")
private var cacheInstance: PersistentImageCache? = null

actual fun getPlatformImageCache(): PersistentImageCache {
    return cacheInstance ?: throw IllegalStateException("PersistentImageCache not initialized. Call initializeImageCache() first.")
}

fun initializeImageCache(context: Context) {
    cacheInstance = PersistentImageCache(context)
}
