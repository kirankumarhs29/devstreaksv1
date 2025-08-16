package com.dailydevchallenge.devstreaks.cache

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathDirectory
import platform.Foundation.NSUserDomainMask
import platform.darwin.ByteVar

actual class PersistentImageCache {

    private val cacheDir: String by lazy {
        val manager = NSFileManager.defaultManager
        val urls = manager.URLsForDirectory(NSSearchPathDirectory.NSCachesDirectory, NSUserDomainMask)
        val cacheUrl = urls.firstOrNull() as? NSURL
        val avatarCacheDir = cacheUrl?.path + "/avatar_images"

        // Create directory if it doesn't exist
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(avatarCacheDir)) {
            fileManager.createDirectoryAtPath(
                avatarCacheDir,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
        }

        avatarCacheDir
    }

    actual suspend fun getCachedImage(url: String): ByteArray? {
        return withContext(Dispatchers.Default) {
            try {
                val fileName = generateFileName(url)
                val filePath = "$cacheDir/$fileName"

                val fileManager = NSFileManager.defaultManager
                if (fileManager.fileExistsAtPath(filePath) && isFileValid(filePath)) {
                    val data = NSData.dataWithContentsOfFile(filePath)
                    data?.toByteArray()
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    actual suspend fun cacheImage(url: String, imageBytes: ByteArray) {
        withContext(Dispatchers.Default) {
            try {
                val fileName = generateFileName(url)
                val filePath = "$cacheDir/$fileName"
                val data = imageBytes.toNSData()
                data.writeToFile(filePath, atomically = true)
            } catch (e: Exception) {
                // Ignore cache write errors
            }
        }
    }

    actual suspend fun clearCache() {
        withContext(Dispatchers.Default) {
            try {
                val fileManager = NSFileManager.defaultManager
                val contents = fileManager.contentsOfDirectoryAtPath(cacheDir, error = null)
                contents?.forEach { fileName ->
                    val filePath = "$cacheDir/$fileName"
                    fileManager.removeItemAtPath(filePath, error = null)
                }
            } catch (e: Exception) {
                // Ignore clear errors
            }
        }
    }

    private fun generateFileName(url: String): String {
        // Simple hash function for filename generation
        val hash = url.hashCode().toString().replace("-", "")
        return "$hash.jpg"
    }

    private fun isFileValid(filePath: String): Boolean {
        val fileManager = NSFileManager.defaultManager
        val attributes = fileManager.attributesOfItemAtPath(filePath, error = null)
        val modificationDate = attributes?.get(NSFileModificationDate) as? NSDate

        if (modificationDate == null) return false

        val maxAge = 7 * 24 * 60 * 60.0 // 7 days in seconds
        val now = NSDate()
        return now.timeIntervalSinceDate(modificationDate) < maxAge
    }

    private fun ByteArray.toNSData(): NSData {
        return NSData.create(bytes = this.refTo(0), length = this.size.toULong())
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun NSData.toByteArray(): ByteArray {
        return ByteArray(this.length.toInt()) { index ->
            this.subdataWithRange(NSMakeRange(index.toULong(), 1u)).bytes!!.reinterpret<ByteVar>()[0]
        }
    }
}

// Singleton instance for iOS
private val cacheInstance = PersistentImageCache()

actual fun getPlatformImageCache(): PersistentImageCache {
    return cacheInstance
}
