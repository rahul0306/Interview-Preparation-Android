package com.example.interviewprep.auth

data class AuthUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val signedIn: Boolean = false,
    val verificationSent: Boolean = false
)
