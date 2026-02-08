package com.example.interviewprep.di

import com.example.interviewprep.network.QuestionApi
import com.example.interviewprep.network.QuestionRepository
import com.example.interviewprep.network.QuestionViewModel
import com.example.interviewprep.network.TranscriptionApi
import com.example.interviewprep.network.TranscriptionRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private const val CONNECT_TIMEOUT_SECONDS = 30L
private const val READ_TIMEOUT_SECONDS = 60L
private const val WRITE_TIMEOUT_SECONDS = 60L

fun provideRetrofit(): Retrofit {
    val client = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val token: String? = runBlocking {
                try {
                    FirebaseAuth.getInstance()
                        .currentUser
                        ?.getIdToken(false)
                        ?.await()
                        ?.token
                } catch (_: Exception) {
                    null
                }
            }

            val newReq = chain.request().newBuilder()
                .apply { if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token") }
                .build()

            chain.proceed(newReq)
        }
        .build()

    return Retrofit.Builder()
        .baseUrl("http://127.0.0.1:8080/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}

fun buildQuestionViewModel(): QuestionViewModel {
    val retrofit = provideRetrofit()
    val api = retrofit.create(QuestionApi::class.java)
    val repository = QuestionRepository(api)
    return QuestionViewModel(repository)
}

fun buildTranscriptionRepository(): TranscriptionRepository {
    val retrofit = provideRetrofit()
    val api = retrofit.create(TranscriptionApi::class.java)
    return TranscriptionRepository(api)
}