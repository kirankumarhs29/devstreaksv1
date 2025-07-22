package com.dailydevchallenge.devstreaks.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.google.firebase.firestore.FirebaseFirestore

class AuthServiceAndroid : AuthService {
    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()
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

}
