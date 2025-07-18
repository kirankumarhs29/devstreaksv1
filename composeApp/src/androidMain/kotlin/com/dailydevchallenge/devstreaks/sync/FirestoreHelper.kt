package com.dailydevchallenge.devstreaks.sync

import android.annotation.SuppressLint
import com.dailydevchallenge.database.UserProgress
import com.dailydevchallenge.devstreaks.model.CompletedChallenge
import com.dailydevchallenge.devstreaks.model.TaskReflection
import com.dailydevchallenge.devstreaks.utils.getLogger
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirestoreHelper {
    @SuppressLint("StaticFieldLeak")
    private val db = FirebaseFirestore.getInstance()

    suspend fun uploadUserProgress(userProgress: UserProgress) {
        try {
            db.collection("user_progress")
                .document(userProgress.id)
                .set(userProgress).await()
            getLogger().d("FirebaseUpload", "Upload successful!")
        } catch  (e: Exception) {
            getLogger().e("FirebaseUpload", e , "Upload failed!")
        }
    }

    suspend fun uploadReflection(reflection: TaskReflection) {
        try {
            db.collection("reflections")
                .document(reflection.id)
                .set(reflection).await()
        } catch  (e: Exception) {
            getLogger().e("FirebaseUpload", e , "Upload failed!")
        }
    }

    suspend fun uploadCompletedChallenge(entry: CompletedChallenge) {
        val id = "${entry.pathId}_${entry.completedDate}"
        try {
            db.collection("completed_challenges")
                .document(id)
                .set(entry).await()
        } catch  (e: Exception) {
            getLogger().e("FirebaseUpload", e , "Upload failed!")
        }
    }
}