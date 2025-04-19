package com.eritlab.jexmon.domain.model

import com.google.firebase.Timestamp

data class ReviewModel (
    val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val rating: Int = 0, // 1 - 5 sao
    val comment: String = "",
    val createdAt: Timestamp = Timestamp.now()
)