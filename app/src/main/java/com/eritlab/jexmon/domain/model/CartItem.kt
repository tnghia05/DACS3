// Trong file CartItem.kt (nằm trong thư mục domain/model)

package com.eritlab.jexmon.domain.model

import android.os.Parcelable // <-- DÒNG NÀY CẦN CÓ
import kotlinx.parcelize.Parcelize // <-- DÒNG NÀY CẦN CÓ

@Parcelize // <-- DÒNG NÀY CẦN CÓ, ĐẶT NGAY TRÊN data class
data class CartItem(
    val id: String = "",  // ID của sản phẩm trong giỏ hàng
    val userId: String = "",  // Liên kết với User
    val productId: String = "",  // Liên kết với Product
    val name: String = "",
    val price: Double = 0.0,
    val priceBeforeDiscount: Double = 0.0,
    val size: Int = 0,
    val color: String = "",
    val quantity: Int = 1,
    val imageUrl: String = "",
    val discount: Double = 0.0
) : Parcelable // <-- DÒNG NÀY CẦN CÓ (implement interface Parcelable)