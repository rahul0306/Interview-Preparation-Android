package com.example.interviewprep.network

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class TranscriptionRepository(
    private val api: TranscriptionApi
) {
    suspend fun transcribeFromUri(
        context: Context,
        uri: Uri
    ): Result<String> {
        val temp = File.createTempFile("interview-video-", ".mp4", context.cacheDir)

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(temp).use { output ->
                    input.copyTo(output)
                }
            } ?: error("Unable to open recording")

            val mime = context.contentResolver.getType(uri) ?: "video/mp4"

            val filePart = MultipartBody.Part.createFormData(
                name = "video",
                filename = "answer.mp4",
                body = temp.asRequestBody(mime.toMediaTypeOrNull())
            )

            val res = api.transcribe(filePart)

            if (res.isSuccessful) {
                val body = res.body()
                val text = body?.text
                if (!text.isNullOrBlank()) {
                    Result.success(text)
                } else {
                    Result.failure(IllegalStateException("Empty transcript"))
                }
            } else {
                return if (res.code() == 413) {
                    Result.failure<String>(
                        IllegalStateException(
                            "Recording is too long to transcribe in one go. Please record a shorter answer."
                        )
                    )
                } else {
                    val err = res.errorBody()?.string()
                    Result.failure<String>(
                        IllegalStateException(
                            err ?: "Transcription failed (${res.code()})"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            temp.delete()
        }
    }
}