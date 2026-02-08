package com.example.interviewprep.screens.upload

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.interviewprep.BuildConfig
import com.example.interviewprep.data.Question
import com.example.interviewprep.di.provideRetrofit
import com.example.interviewprep.network.QuestionApi
import com.example.interviewprep.network.QuestionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.nio.charset.Charset

class UploadViewModel: ViewModel() {
    private val _ui = MutableStateFlow(UploadUiState())
    val ui: StateFlow<UploadUiState> = _ui

    fun setSelected(uri: Uri?) {
        _ui.value = _ui.value.copy(selected = uri, error = null, scannedText = null)
    }

    fun scanToText(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            if (uri == null) {
                _ui.value = _ui.value.copy(error = "Please choose a file first", scannedText = null)
                return@launch
            }
            _ui.value = _ui.value.copy(isScanning = true, error = null, scannedText = null)

            try {
                val text = extractTextBestEffort(context, uri)
                if (text.isNullOrBlank()) {
                    _ui.value = _ui.value.copy(
                        isScanning = false,
                        scannedText = null,
                        error = "Could not read text from the resume. Try a .txt or text-based PDF."
                    )
                } else {
                    _ui.value = _ui.value.copy(
                        isScanning = false,
                        scannedText = text.trim(),
                        error = null
                    )
                }
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(
                    isScanning = false,
                    scannedText = null,
                    error = t.message ?: "Failed to read the resume."
                )
            }
        }
    }

    fun uploadToBackend(
        context: Context,
        uri: Uri,
        onQuestionsReady: (List<String>) -> Unit
    ) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isScanning = true, error = null)
            try {
                val retrofit = provideRetrofit()
                val api = retrofit.create(QuestionApi::class.java)
                val repo = QuestionRepository(api)

                val result = repo.generateFromFile(context, uri, role = null, count = 8)
                result.fold(
                    onSuccess = { questions ->
                        onQuestionsReady(questions.map { it.question })
                    },
                    onFailure = { e ->
                        _ui.value = _ui.value.copy(error = e.message ?: "Upload failed")
                    }
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(error = e.message ?: "Something went wrong")
            } finally {
                _ui.value = _ui.value.copy(isScanning = false)
            }
        }
    }
}
private suspend fun extractTextBestEffort(context: Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
        val cr = context.contentResolver
        val mime = cr.getType(uri) ?: ""

        cr.openInputStream(uri)?.use { inRaw ->
            BufferedInputStream(inRaw).use { input ->
                val bytes = input.readBytes()


                if (mime.startsWith("text/")) {
                    return@withContext bytes.toString(Charset.forName("UTF-8"))
                }


                val printable = bytes.count { b ->
                    val c = b.toInt() and 0xFF
                    c == 0x0A || c == 0x0D || (c in 0x09..0x7E)
                }
                val ratio = if (bytes.isNotEmpty()) printable.toDouble() / bytes.size else 0.0

                if (ratio > 0.9) {
                    return@withContext bytes.toString(Charset.forName("UTF-8"))
                } else {

                    throw IllegalArgumentException(
                        "This file doesn't look like plain text. " +
                                "Please upload a .txt or a text-based PDF (not scanned images)."
                    )
                }
            }
        }
        null
    }