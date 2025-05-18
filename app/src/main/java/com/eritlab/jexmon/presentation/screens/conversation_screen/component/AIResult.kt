package com.eritlab.jexmon.presentation.screens.conversation_screen.component

sealed class AIResult<out T> {
    data class Success<T>(val data: T) : AIResult<T>()
    data class Error(val exception: Exception) : AIResult<Nothing>()
} 