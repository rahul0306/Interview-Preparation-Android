package com.example.interviewprep.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui

    fun onFullName(v: String) {
        _ui.value = _ui.value.copy(fullName = v)
    }

    fun onEmail(v: String) {
        _ui.value = _ui.value.copy(email = v)
    }

    fun onPassword(v: String) {
        _ui.value = _ui.value.copy(password = v)
    }

    fun onConfirmPassword(v: String) {
        _ui.value = _ui.value.copy(confirmPassword = v)
    }

    fun clearError() {
        _ui.value = _ui.value.copy(error = null)
    }

    fun signUp() = viewModelScope.launch {
        val s = _ui.value
        if (s.fullName.isBlank() || s.email.isBlank() || s.password.isBlank() || s.confirmPassword.isBlank()) {
            _ui.value = s.copy(error = "Please fill all fields.")
            return@launch
        }
        if (s.password != s.confirmPassword) {
            _ui.value = s.copy(error = "Passwords do not match.")
            return@launch
        }
        _ui.value = s.copy(loading = true, error = null)
        val res = repository.signUpWithEmail(s.fullName.trim(), s.email.trim(), s.password)
        _ui.value = _ui.value.copy(
            loading = false,
            error = res.error,
            verificationSent = res.isSuccess && res.error == null
        )
    }

    fun signIn() = viewModelScope.launch {
        val s = _ui.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _ui.value = s.copy(error = "Email and password required.")
            return@launch
        }
        _ui.value = s.copy(loading = true, error = null)
        val res = repository.signInWithEmail(s.email.trim(), s.password)
        _ui.value = _ui.value.copy(loading = false, error = res.error, signedIn = res.isSuccess)
    }

    fun signInWithGoogleIdToken(idToken: String) = viewModelScope.launch {
        _ui.value = _ui.value.copy(loading = true, error = null)
        val res = repository.signInWithGoogleIdToken(idToken)
        _ui.value = _ui.value.copy(loading = false, error = res.error, signedIn = res.isSuccess)
    }

    fun signOut() = repository.signOut()

    fun currentUserName(): String = repository.currentUserDisplayName() ?: ""
}