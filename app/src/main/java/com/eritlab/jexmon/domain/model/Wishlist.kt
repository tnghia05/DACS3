package com.eritlab.jexmon.domain.model

data class Wishlist(
    val id: String = "", // ID của danh sách yêu thích (có thể là userId)
    val userId: String = "", // ID của user
    val productIds: List<String> = emptyList() // Danh sách sản phẩm yêu thích
)
