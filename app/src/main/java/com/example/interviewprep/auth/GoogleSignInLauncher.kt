package com.example.interviewprep.auth

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun rememberGoogleSignInLauncher(
    context: Context,
    webClientId: String,
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit
): () -> Unit {

    val gso = remember(webClientId) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }
    val googleClient = remember(webClientId) { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let(onIdToken) ?: onError("Missing ID token")
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Google sign-in failed")
        }
    }

    return remember(googleClient, launcher) {
        {

            googleClient.signOut().addOnCompleteListener {
                launcher.launch(googleClient.signInIntent)
            }

        }
    }
}