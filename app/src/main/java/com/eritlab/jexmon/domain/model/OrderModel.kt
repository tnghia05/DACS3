package com.eritlab.jexmon.domain.model

import com.google.firebase.Timestamp

data class OrderModel(
    val id: String = "",
    val userId: String = "",
    val items: List<CartItem> = emptyList(),
    val shippingAddress: AddressModel = AddressModel(),
    val paymentMethod: String = "", // COD, MOMO, ZALOPAY, etc.
    val status: String = "PENDING", // Chua xac nhan, Da xac nhan, Dang giao, Da giao, Da huy
    val paymentStatus: String = "UNPAID", // UNPAID, PAID, CANCELED
    val subtotal: Double = 0.0,
    val shippingFee: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
    val voucherId: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)


