package com.eritlab.jexmon.domain.model

data class Message(
    val text: String = "",
    val isFromUser: Boolean = true,
    val timestamp: Long = 0L
)
