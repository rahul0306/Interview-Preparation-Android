package com.example.interviewprep.network

import android.content.Context
import android.net.Uri
import com.example.interviewprep.data.QuestionItem
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class QuestionRepository(val api: QuestionApi) {
    suspend fun generateFromFile(
        context: Context,
        uri: Uri,
        role: String?,
        count: Int
    ): Result<List<QuestionItem>> {
        val temp = File.createTempFile("resume-", null, context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { i ->
                FileOutputStream(temp).use { o -> i.copyTo(o) }
            } ?: error("Unable to open resume")

            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val displayName = resolveName(context, uri) ?: defaultNameFor(mime)

            val filePart = MultipartBody.Part.createFormData(
                name = "resume",
                filename = displayName,
                body = temp.asRequestBody(mime.toMediaTypeOrNull())
            )

            val rolePart  = role?.toRequestBody("text/plain".toMediaTypeOrNull())
            val countPart = count.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            val res = api.generateQuestionsFile(filePart, rolePart, countPart)

            if (res.isSuccessful) {
                return Result.success(res.body()?.questions.orEmpty())
            } else {
                return Result.failure(IllegalStateException(res.errorBody()?.string() ?: "Upload failed"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        } finally {
            temp.delete()
        }
    }

    private fun resolveName(ctx: Context, uri: Uri): String? {
        val c = ctx.contentResolver.query(uri, null, null, null, null) ?: return null
        c.use {
            val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && it.moveToFirst()) return it.getString(idx)
        }
        return null
    }
}
private fun defaultNameFor(mime: String) = when (mime) {
    "application/pdf" -> "resume.pdf"
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "resume.docx"
    "text/plain" -> "resume.txt"
    else -> "resume"
}