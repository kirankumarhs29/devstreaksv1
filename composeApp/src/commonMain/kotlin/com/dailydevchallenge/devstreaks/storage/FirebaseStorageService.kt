package com.dailydevchallenge.devstreaks.storage

interface FirebaseStorageService {
    suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): Result<String>
    suspend fun deleteAvatar(userId: String): Result<Unit>
}

expect fun getFirebaseStorageService(): FirebaseStorageService

data class StorageResult<T>(
    val data: T?,
    val error: String? = null
) {
    val isSuccess: Boolean get() = data != null && error == null
    val isFailure: Boolean get() = !isSuccess
}
