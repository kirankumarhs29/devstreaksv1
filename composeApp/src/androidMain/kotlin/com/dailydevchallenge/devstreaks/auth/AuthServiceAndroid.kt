package com.dailydevchallenge.devstreaks.auth

import com.dailydevchallenge.devstreaks.model.User
import com.dailydevchallenge.devstreaks.session.SessionManager
import com.dailydevchallenge.devstreaks.session.SessionManager1
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

class AuthServiceAndroid : AuthService {

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()
    private val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
    override suspend fun login(email: String, password: String): AuthResult =
        suspendCancellableCoroutine { cont ->
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.getIdToken(true)
                            ?.addOnSuccessListener { result ->
                                val token = result.token
                                val uid = user.uid
                                UserPreferences.setUserId(uid)
                                UserPreferences.setLoggedIn(true)
                                coroutineScope.launch {
                                    val userdata = getUserProfile(user.uid)
                                    SessionManager1.setUser(userdata)
                                }


                                if (token != null) {
                                    cont.resume(AuthResult.Success(uid, token))
                                } else {
                                    cont.resume(AuthResult.Error("Token is null"))
                                }
                            }
                            ?.addOnFailureListener { ex ->
                                cont.resume(AuthResult.Error("Token fetch failed: ${ex.localizedMessage}"))
                            }
                    } else {
                        cont.resume(AuthResult.Error(task.exception?.message ?: "Login failed"))
                    }
                }
        }

    override suspend fun signup(name : String,email: String, password: String): AuthResult =
        suspendCancellableCoroutine { cont ->
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.getIdToken(true)
                            ?.addOnSuccessListener { result ->
                                val token = result.token
                                val uid = user.uid
                                if (token != null) {
                                    cont.resume(AuthResult.Success(uid, token))
                                    val user = FirebaseAuth.getInstance().currentUser
                                    if (user != null) {
                                        val db = FirebaseFirestore.getInstance()
                                        val userDoc = db.collection("users").document(user.uid)
                                        val userData = mapOf(
                                            "name" to name,
                                            "email" to user.email,
                                            "xp" to 0,
                                            "streak" to 0
                                        )
                                        userDoc.set(userData)
                                    }

                                } else {
                                    cont.resume(AuthResult.Error("Token is null"))
                                }
                            }
                    } else {
                        cont.resume(AuthResult.Error(task.exception?.message ?: "Signup failed"))
                    }
                }
        }
    override fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    override fun logout() {
        auth.signOut()
    }
    override fun sendPasswordResetEmail(email: String, callback: (Boolean, String?) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, null)
                } else {
                    callback(false, task.exception?.localizedMessage)
                }
            }
    }
    private suspend fun getUserProfile(userId: String): User = suspendCancellableCoroutine { cont ->
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    try {
                        val user = User(
                            userId = userId,
                            email = snapshot.getString("email") ?: "",
                            passwordHash = "", // You usually don’t retrieve this
                            username = snapshot.getString("username") ?: "",
                            avatarUrl = snapshot.getString("avatarUrl"),
                            createdAt = snapshot.getLong("createdAt") ?: 0L,
                            lastLogin = System.currentTimeMillis(), // or from DB if stored
                            xp = (snapshot.getLong("xp") ?: 0L).toInt(),
                            level = snapshot.getLong("level") ?: 1,
                            dailyStreak = snapshot.getLong("dailyStreak") ?: 0,
                            streakStartDate = snapshot.getLong("streakStartDate")?.let { Instant
                                .fromEpochSeconds(it) },
                            preferences = snapshot.get("preferences") as? Map<String, String> ?: emptyMap(),
                            role = snapshot.getString("role") ?: "user",
                            badges = snapshot.get("badges") as? List<String> ?: emptyList()
                        )
                        cont.resume(user)
                    } catch (e: Exception) {
                        cont.resumeWith(Result.failure(e))
                    }
                } else {
                    cont.resumeWith(Result.failure(Exception("User profile not found.")))
                }
            }
            .addOnFailureListener { e ->
                cont.resumeWith(Result.failure(e))
            }
    }


}

data class FirestoreUser(
    val email: String = "",
    val passwordHash: String = "", // Store a placeholder or omit in Firestore
    val username: String = "",
    val avatarUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis(),
    val xp: Int = 0,
    val level: Long? = 1,
    val dailyStreak: Long? = 0,
    val streakStartDate: Long? = null, // Store Instant as epoch millis
    val preferences: Map<String, String> = emptyMap(),
    val role: String = "user",
    val badges: List<String> = emptyList()
)

