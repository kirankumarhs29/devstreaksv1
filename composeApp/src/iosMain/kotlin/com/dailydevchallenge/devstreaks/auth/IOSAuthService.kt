package com.dailydevchallenge.devstreaks.auth
import com.dailydevchallenge.devstreaks.auth.AuthService
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.*
import platform.darwin.*

class IOSAuthService : AuthService {
    override suspend fun login(email: String, password: String): AuthResult =
        suspendCancellableCoroutine { cont ->
            FIRAuth.auth().signInWithEmail(email, password) { user, error ->
                if (error != null) {
                    cont.resume(AuthResult.Error(error.localizedDescription ?: "Unknown error"))
                } else {
                    val uid = user?.user?.uid ?: "UID_NULL"
                    cont.resume(AuthResult.Success(uid))
                }
            }
        }

    override suspend fun signup(email: String, password: String): AuthResult =
        suspendCancellableCoroutine { cont ->
            FIRAuth.auth().createUserWithEmail(email, password) { user, error ->
                if (error != null) {
                    cont.resume(AuthResult.Error(error.localizedDescription ?: "Signup failed"))
                } else {
                    val uid = user?.user?.uid ?: "UID_NULL"
                    cont.resume(AuthResult.Success(uid))
                }
            }
        }

    override fun logout() {
        FIRAuth.auth().signOut(nil)
    }

    override fun isLoggedIn(): Boolean {
        return FIRAuth.auth().currentUser != null
    }

    override suspend fun sendResetEmail(email: String): AuthResult =
        suspendCancellableCoroutine { cont ->
            FIRAuth.auth().sendPasswordResetWithEmail(email) { error ->
                if (error != null) {
                    cont.resume(AuthResult.Error(error.localizedDescription ?: "Failed to send reset email"))
                } else {
                    cont.resume(AuthResult.Success("reset"))
                }
            }
        }
}
