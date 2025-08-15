package com.dailydevchallenge.devstreaks.storage

import cocoapods.FirebaseStorage.*
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.*
import kotlin.coroutines.resume

actual fun getFirebaseStorageService(): FirebaseStorageService {
    return FirebaseStorageServiceIOS()
}

class FirebaseStorageServiceIOS : FirebaseStorageService {
    private val storage = FIRStorage.storage()
    private val storageRef = storage.reference()

    override suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            try {
                // Create a unique filename for the avatar
                val fileName = "avatars/$userId/avatar_${NSUUID.UUID().UUIDString}.jpg"
                val avatarRef = storageRef.child(fileName)

                // Convert ByteArray to NSData
                val data = imageBytes.toNSData()

                // Upload the image
                val uploadTask = avatarRef.putData(data) { metadata, error ->
                    if (error != null) {
                        continuation.resume(Result.failure(Exception(error.localizedDescription)))
                    } else {
                        // Get the download URL
                        avatarRef.downloadURLWithCompletion { url, urlError ->
                            if (urlError != null) {
                                continuation.resume(Result.failure(Exception(urlError.localizedDescription)))
                            } else {
                                continuation.resume(Result.success(url?.absoluteString ?: ""))
                            }
                        }
                    }
                }

                continuation.invokeOnCancellation {
                    uploadTask.cancel()
                }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    override suspend fun deleteAvatar(userId: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            try {
                val avatarFolder = storageRef.child("avatars/$userId")

                avatarFolder.listAllWithCompletion { result, error ->
                    if (error != null) {
                        // Check if the error is because the folder doesn't exist
                        val errorMessage = error.localizedDescription ?: ""
                        if (errorMessage.contains("Object does not exist", ignoreCase = true)) {
                            // Folder doesn't exist, which means no avatars to delete - this is success
                            continuation.resume(Result.success(Unit))
                        } else {
                            // Some other error occurred
                            continuation.resume(Result.failure(Exception(errorMessage)))
                        }
                    } else {
                        // Successfully listed files, now delete them
                        val items = result?.items
                        if (items?.isNotEmpty() == true) {
                            var deletedCount = 0
                            val totalItems = items.size

                            items.forEach { item ->
                                item.deleteWithCompletion { deleteError ->
                                    deletedCount++
                                    if (deleteError != null) {
                                        // Log individual delete errors but don't fail the entire operation
                                        println("Failed to delete file ${item.fullPath}: ${deleteError.localizedDescription}")
                                    }

                                    // Check if all deletions are complete
                                    if (deletedCount == totalItems) {
                                        continuation.resume(Result.success(Unit))
                                    }
                                }
                            }
                        } else {
                            // No items to delete
                            continuation.resume(Result.success(Unit))
                        }
                    }
                }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    private fun ByteArray.toNSData(): NSData {
        return NSData.create(bytes = this.refTo(0), length = this.size.toULong())
    }
}
