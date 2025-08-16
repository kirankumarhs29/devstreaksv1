package com.dailydevchallenge.devstreaks.storage

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import kotlinx.coroutines.tasks.await

actual fun getFirebaseStorageService(): FirebaseStorageService {
    return FirebaseStorageServiceAndroid()
}

class FirebaseStorageServiceAndroid : FirebaseStorageService {
    private val storage = FirebaseStorage.getInstance("gs://devsteaks.firebasestorage.app")
    private val storageRef = storage.reference
    private val auth = FirebaseAuth.getInstance()

    private fun checkAuthentication(): Boolean {
        val currentUser = auth.currentUser
        println("Current user: ${currentUser?.uid}")
        println("User is authenticated: ${currentUser != null}")
        return currentUser != null
    }

    override suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): Result<String> {
        return try {
            // Check authentication first
            if (!checkAuthentication()) {
                return Result.failure(IllegalStateException("User must be authenticated to upload files"))
            }

            // Validate inputs
            if (userId.isBlank()) {
                return Result.failure(IllegalArgumentException("User ID cannot be blank"))
            }
            if (imageBytes.isEmpty()) {
                return Result.failure(IllegalArgumentException("Image bytes cannot be empty"))
            }

            // Create a unique filename for the avatar
            val fileName = "avatars/$userId/avatar_${UUID.randomUUID()}.jpg"
            val avatarRef = storageRef.child(fileName)

            println("Attempting to upload avatar to: $fileName")
            println("Image size: ${imageBytes.size} bytes")
            println("Storage bucket: ${storage.app.options.storageBucket}")

            // Upload the image with metadata
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            val uploadTask = avatarRef.putBytes(imageBytes, metadata).await()
            println("Upload completed successfully")

            // Get the download URL
            val downloadUrl = avatarRef.downloadUrl.await()
            val urlString = downloadUrl.toString()
            println("Download URL obtained: $urlString")

            Result.success(urlString)
        } catch (e: Exception) {
            println("Upload failed with error: ${e.message}")
            println("Error type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun deleteAvatar(userId: String): Result<Unit> {
        return try {
            // Delete all avatars for this user (in case there are multiple)
            val avatarFolder = storageRef.child("avatars/$userId")

            try {
                val listResult = avatarFolder.listAll().await()

                // Only delete if there are files to delete
                if (listResult.items.isNotEmpty()) {
                    listResult.items.forEach { item ->
                        try {
                            item.delete().await()
                        } catch (deleteException: Exception) {
                            // Log but don't fail if individual file deletion fails
                            println("Failed to delete file ${item.path}: ${deleteException.message}")
                        }
                    }
                }

                Result.success(Unit)
            } catch (listException: Exception) {
                // If listing fails (e.g., folder doesn't exist), that's fine - nothing to delete
                if (listException.message?.contains("Object does not exist") == true) {
                    // Folder doesn't exist, which means no avatars to delete - this is success
                    Result.success(Unit)
                } else {
                    // Some other error occurred
                    Result.failure(listException)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
