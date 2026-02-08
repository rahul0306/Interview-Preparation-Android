package com.example.interviewprep.data

data class QuestionItem(
    val question: String,
    val category: String? = null,
    val difficulty: String? = null,
    val answerHints: List<String>? = null
)
