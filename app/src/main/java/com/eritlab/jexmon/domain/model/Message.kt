package com.eritlab.jexmon.domain.model

data class Message(
    val messageId: String = "",  // Unique identifier for each message
    val text: String = "",
    val isFromUser: Boolean = true,
    val timestamp: Long = 0L,
    val type: String = "text" // Can be "text" or "product"
)
