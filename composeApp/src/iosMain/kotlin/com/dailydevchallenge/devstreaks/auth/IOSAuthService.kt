package com.dailydevchallenge.devstreaks.auth

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.cinterop.ExperimentalForeignApi
import cocoapods.FirebaseAuth.FIRAuth
import platform.Foundation.*
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
object IOSAuthService : AuthService {

    override suspend fun login(email: String, password: String): AuthResult =
        suspendCancellableCoroutine { cont ->
            FIRAuth.auth().signInWithEmail(email = email, password = password) { result, error ->
                if (error != null) {
                    cont.resume(AuthResult.Error(error.localizedDescription))
                } else {
                    val uid = result?.user()?.uid()?: "UID_NULL"
                    cont.resume(AuthResult.Success(uid, "dummy_token"))
                }
            }
        }

    override suspend fun signup(email: String, password: String): AuthResult =
        suspendCancellableCoroutine { cont ->
            FIRAuth.auth().createUserWithEmail(email = email, password = password) { result, error ->
                if (error != null) {
                    cont.resume(AuthResult.Error(error.localizedDescription ?: "Signup failed"))
                } else {
                    val uid = result?.user()?.uid() ?: "UID_NULL"
                    cont.resume(AuthResult.Success(uid, "dummy_token"))
                }
            }
        }

    override fun logout() {
        FIRAuth.auth().signOut(null)
    }

    override fun isLoggedIn(): Boolean {
        return FIRAuth.auth().currentUser() != null
    }

    override fun sendPasswordResetEmail(email: String, callback: (Boolean, String?) -> Unit) {
        FIRAuth.auth().sendPasswordResetWithEmail(email) { error ->
            if (error != null) {
                callback(false, error.localizedDescription ?: "Reset email failed")
            } else {
                callback(true, null)
            }
        }
    }
}
