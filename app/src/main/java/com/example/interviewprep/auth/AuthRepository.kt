package com.example.interviewprep.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) {
    val authState: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser != null) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun currentUserDisplayName(): String? = auth.currentUser?.displayName
    fun currentUserEmailVerified(): Boolean = auth.currentUser?.isEmailVerified == true

    suspend fun signUpWithEmail(fullName: String, email: String, password: String): AuthResult {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            val profile = UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build()
            auth.currentUser?.updateProfile(profile)?.await()

            auth.currentUser?.sendEmailVerification()?.await()
            auth.signOut()
            AuthResult(true)
        } catch (e: Exception) {
            AuthResult(false, e.localizedMessage ?: "Sign up failed")
        }
    }

    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            auth.currentUser?.reload()?.await()
            if (auth.currentUser?.isEmailVerified == true) {
                AuthResult(true)
            } else {
                auth.signOut()
                AuthResult(false, "Please verify your email before logging in.")
            }
        } catch (e: Exception) {
            AuthResult(false, e.localizedMessage ?: "Login failed")
        }
    }

    suspend fun signInWithGoogleIdToken(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            // Google accounts are already verified
            AuthResult(true)
        } catch (e: Exception) {
            AuthResult(false, e.localizedMessage ?: "Google sign-in failed")
        }
    }

    fun signOut() { auth.signOut() }
}