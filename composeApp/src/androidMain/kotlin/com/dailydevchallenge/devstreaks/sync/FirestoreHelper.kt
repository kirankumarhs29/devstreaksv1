package com.dailydevchallenge.devstreaks.sync

import android.annotation.SuppressLint
import com.dailydevchallenge.database.UserProgress
import com.dailydevchallenge.devstreaks.model.ChallengePathResponse
import com.dailydevchallenge.devstreaks.model.CompletedChallenge
import com.dailydevchallenge.devstreaks.model.EngagementRecord
import com.dailydevchallenge.devstreaks.model.TaskReflection
import com.dailydevchallenge.devstreaks.utils.getLogger
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

object FirestoreHelper {
    @SuppressLint("StaticFieldLeak")
    private val db = FirebaseFirestore.getInstance()
    private val jsonFormatter = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }


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
    suspend fun fetchGeneratedCourse(requestId: String): ChallengePathResponse? {
        return try {
            getLogger().d("FirestoreHelper", "Fetching course for requestId: $requestId")
            val doc = Firebase.firestore
                .collection("courses")
                .document(requestId.trim()) // prevent trailing spaces
                .get()
                .await()

            if (doc.exists()) {
                getLogger().d("fetchCourse", "Document found!")
                val courseMap = doc.get("course") as? Map<String, Any>
                val json = courseMap?.let { convertMapToJsonString(it) }
                return json?.let { jsonFormatter.decodeFromString<ChallengePathResponse>(it) }

            } else {
                getLogger().d("fetchCourse", "Document does not exist")
                null
            }
        } catch (e: Exception) {
            getLogger().e("fetchCourse",e ,"Unknown error")
            e.printStackTrace()
            null
        }
    }
    private fun mapToJsonValue(value: Any?): String = when (value) {
        is String -> "\"${value.replace("\"", "\\\"")}\""
        is Number, is Boolean -> value.toString()
        is Map<*, *> -> (value as? Map<String, Any?>)?.let { convertMapToJsonString(it) } ?: "null"
        is List<*> -> "[" + value.joinToString(",") { mapToJsonValue(it) } + "]"
        null -> "null"
        else -> "\"$value\""
    }

    private fun convertMapToJsonString(map: Map<String, Any?>): String {
        return map.entries.joinToString(
            prefix = "{", postfix = "}"
        ) { (k, v) -> "\"$k\":${mapToJsonValue(v)}" }
    }
    suspend fun uploadEngagementData(record: EngagementRecord) {
        try {
            db.collection("engagement_data")
                .add(record)
                .await()
            println("✅ Engagement saved")
        } catch (e: Exception) {
            println("❌ Failed: ${e.message}")
        }
    }




}