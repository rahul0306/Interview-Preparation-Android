package com.example.interviewprep.data

import kotlinx.serialization.Serializable

@Serializable
data class Question(
    val prompt: String,
    val category: String? = null
)