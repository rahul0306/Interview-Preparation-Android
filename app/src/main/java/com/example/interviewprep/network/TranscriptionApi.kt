package com.example.interviewprep.network

import com.example.interviewprep.data.TranscriptionResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface TranscriptionApi {
    @Multipart
    @POST("/api/transcribe")
    suspend fun transcribe(
        @Part video: MultipartBody.Part
    ): Response<TranscriptionResponse>
}