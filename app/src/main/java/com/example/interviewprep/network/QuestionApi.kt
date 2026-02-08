package com.example.interviewprep.network

import com.example.interviewprep.data.QuestionResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface QuestionApi {
    @POST("/api/generate-questions")
    suspend fun generateQuestions(
        @Body body: Map<String, Any>
    ): Response<QuestionResponse>

    @Multipart
    @POST("/api/generate-questions-file")
    suspend fun generateQuestionsFile(
        @Part resume: MultipartBody.Part,
        @Part("role") role: RequestBody?,
        @Part("count") count: RequestBody?
    ): Response<QuestionResponse>
}