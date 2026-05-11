package me.rerere.awara.ui.component.common

import androidx.compose.runtime.Composable

sealed interface UiState {
    data object Initial : UiState
    data object Empty : UiState
    data object Loading : UiState
    data object Success : UiState

    data class Error(
        val throwable: Throwable? = null,
        val message: @Composable (() -> Unit)? = null,
        val messageText: String? = null,
    ) : UiState
}