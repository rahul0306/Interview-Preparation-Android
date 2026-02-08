package com.example.interviewprep.navigation

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.interviewprep.R
import com.example.interviewprep.auth.AuthViewModel
import com.example.interviewprep.auth.rememberGoogleSignInLauncher
import com.example.interviewprep.screens.interview.InterviewScreen
import com.example.interviewprep.screens.login.LoginScreen
import com.example.interviewprep.screens.recordings.RecordingsScreen
import com.example.interviewprep.screens.review.ReviewScreen
import com.example.interviewprep.screens.signup.SignUpScreen
import com.example.interviewprep.screens.upload.UploadScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.serialization.json.Json

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun InterviewPrepNavigation(modifier: Modifier = Modifier) {

    val navController = rememberNavController()
    var lastVideoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var lastQuestionStarts by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var sessionQuestions by rememberSaveable(saver = questionsTextSaver) { mutableStateOf(emptyList<String>()) }
    val vm: AuthViewModel = hiltViewModel()
    val ui by vm.ui.collectAsState()
    val startDestination = remember {
        val u = FirebaseAuth.getInstance().currentUser
        val isGoogle = u?.providerData?.any { it.providerId == "google.com" } == true
        if (u != null && (u.isEmailVerified || isGoogle))
            InterviewPrepScreens.Upload.route
        else
            InterviewPrepScreens.Login.route
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(InterviewPrepScreens.Login.route) {
            val context = LocalContext.current
            val webClientId = stringResource(R.string.default_web_client_id)


            val launchGoogle = rememberGoogleSignInLauncher(
                context = context,
                webClientId = webClientId,
                onIdToken = { token -> vm.signInWithGoogleIdToken(token) },
                onError   = { msg -> vm.clearError()  }
            )

            val isVerified by rememberFirebaseLoggedInState()
            LaunchedEffect(isVerified) {
                if (isVerified) {
                    navController.navigate(InterviewPrepScreens.Upload.route) {
                        popUpTo(InterviewPrepScreens.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            val showVerifyPrompt = navController.currentBackStackEntry
                ?.savedStateHandle
                ?.remove<Boolean>("showVerifyPrompt") == true

            LoginScreen(
                onLogin = { email, pass ->
                    vm.onEmail(email); vm.onPassword(pass); vm.signIn()
                },
                onForgotPassword = {  },
                onGoogleClick = launchGoogle,
                onSignUpClick = { navController.navigate(InterviewPrepScreens.Signup.route) },
                loading = ui.loading,
                errorText = ui.error ?: if (showVerifyPrompt)
                "We sent a verification email. Please verify and logi in." else null
            )
        }
        composable(InterviewPrepScreens.Signup.route) {

            val context = LocalContext.current
            val webClientId = stringResource(R.string.default_web_client_id)

            val launchGoogle = rememberGoogleSignInLauncher(
                context = context,
                webClientId = webClientId,
                onIdToken = { token -> vm.signInWithGoogleIdToken(token) },
                onError   = { vm.clearError() }
            )

            LaunchedEffect(ui.verificationSent) {
                if (ui.verificationSent) {
                    navController.navigate(InterviewPrepScreens.Login.route) {
                        popUpTo(InterviewPrepScreens.Signup.route) { inclusive = true }
                        launchSingleTop = true
                    }

                    navController.getBackStackEntry(InterviewPrepScreens.Login.route)
                        .savedStateHandle["showVerifyPrompt"] = true
                }
            }
            LaunchedEffect(ui.signedIn) {
                if (ui.signedIn || FirebaseAuth.getInstance().currentUser != null) {
                    navController.navigate(InterviewPrepScreens.Upload.route) {
                        popUpTo(InterviewPrepScreens.Signup.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            SignUpScreen(
                onSignUp = { name, email, pass, confirm ->
                    vm.onFullName(name)
                    vm.onEmail(email)
                    vm.onPassword(pass)
                    vm.onConfirmPassword(confirm)
                    vm.signUp()
                },
                onGoogleClick = launchGoogle,
                onLoginClick = { navController.popBackStack() },
                loading = ui.loading,
                errorText = ui.error
            )
        }
        composable(InterviewPrepScreens.Upload.route) {
            UploadScreen(
                onQuestionReady = { qs: List<String> ->
                    sessionQuestions = qs
                    navController.navigate(InterviewPrepScreens.Interview.route)
                },
                onRecordingList = {
                    navController.navigate(InterviewPrepScreens.Recordings.route)
                },
                onLogout = {
                    vm.signOut()
                    navController.navigate(InterviewPrepScreens.Login.route) {
                        popUpTo(InterviewPrepScreens.Upload.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(InterviewPrepScreens.Interview.route) {
            InterviewScreen(
                questions = sessionQuestions.ifEmpty {
                    listOf(
                        "Walk me through a challenging Android bug you fixed recently.",
                        "How do you structure a Jetpack Compose + MVVM project?",
                        "Explain your experience with CameraX and media recording.",
                        "How would you optimize startup time in an Android app?"
                    )
                },
                onFinish = { recordedUri: Uri?, starts: List<Int> ->
                    lastVideoUriString = recordedUri?.toString()
                    lastQuestionStarts = starts
                    navController.navigate(InterviewPrepScreens.Review.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(InterviewPrepScreens.Review.route) {
            val parsed: Uri? = remember(lastVideoUriString) {
                lastVideoUriString?.toUri()
            }
            ReviewScreen(
                videoUri = parsed,
                questions = sessionQuestions,
                questionStartSeconds = lastQuestionStarts,
                onDone = {
                    navController.popBackStack(
                        InterviewPrepScreens.Upload.route,
                        inclusive = false
                    )
                }
            )
        }
        composable(InterviewPrepScreens.Recordings.route) {
            RecordingsScreen(
                onBack = { navController.popBackStack() },
                onOpenRecording = { uri ->
                    lastVideoUriString = uri.toString()
                    navController.navigate(InterviewPrepScreens.Review.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

    }
}

private val json = Json { ignoreUnknownKeys = true }

private val questionsTextSaver: Saver<MutableState<List<String>>, String> =
    Saver(
        save = { state: MutableState<List<String>> ->
            json.encodeToString(state.value)
        },
        restore = { raw: String ->
            val list = json.decodeFromString<List<String>>(raw)
            mutableStateOf(list)
        }
    )

@Composable
private fun rememberFirebaseLoggedInState(): State<Boolean> {
    val verified = remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { a ->
            val u = a.currentUser
            val isGoogle = u?.providerData?.any { it.providerId == "google.com" } == true
            verified.value = u != null && (u.isEmailVerified || isGoogle)
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }
    return verified
}


