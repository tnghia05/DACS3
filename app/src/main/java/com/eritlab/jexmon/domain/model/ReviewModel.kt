package com.eritlab.jexmon.domain.model

import com.google.firebase.Timestamp

data class ReviewModel(
    val id: String = "",
    val productId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val createdAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    val images: List<String>? = null,
    val productVariant: String? = null, // e.g. "CP1 Pro 4MP"
    val helpfulCount: Int = 0,
    val userAvatar: String? = null
)