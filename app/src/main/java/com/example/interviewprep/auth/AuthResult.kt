package com.example.interviewprep.auth

data class AuthResult(
    val isSuccess: Boolean,
    val error: String? = null
)
