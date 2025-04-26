package com.eritlab.jexmon.domain.model

data class CartItem(
    val id: String = "",  // ID của sản phẩm trong giỏ hàng
    val userId: String = "",  // Liên kết với User
    val productId: String = "",  // Liên kết với Product
    val name: String = "",
    val price: Double = 0.0,
    val size: Int = 0,
    val color: String = "",
    val quantity: Int = 1,
    val imageUrl: String = ""
)
