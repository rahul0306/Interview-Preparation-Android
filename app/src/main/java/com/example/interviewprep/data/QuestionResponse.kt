package com.example.interviewprep.data

data class QuestionResponse(
    val uid: String,
    val role: String?,
    val questions: List<QuestionItem>
)
