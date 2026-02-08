package com.example.interviewprep.screens.upload

import android.net.Uri

data class UploadUiState(
    val isScanning: Boolean = false,
    val selected: Uri? = null,
    val scannedText: String? = null,
    val error: String? = null
)
