package com.example.interviewprep.network

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.interviewprep.data.QuestionItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class QuestionViewModel(
    private val repository: QuestionRepository
): ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    sealed interface UiState {
        object Idle : UiState
        object Loading : UiState
        data class Success(val items: List<QuestionItem>) : UiState
        data class Error(val message: String) : UiState
    }

    fun uploadFileAndGenerate(context: Context, uri: Uri, role: String?, count: Int = 8) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val result = repository.generateFromFile(context, uri, role, count)
            _uiState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Failed") }
            )
        }
    }

}