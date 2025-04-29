package com.eritlab.jexmon.domain.model

import java.util.Date

data class VoucherModel(
    val code: String = "",
    val discount: Double = 0.0,
    val expiryDate: Date? = null,
    val quantity: Int = 0,
    val detail: String = "",
    val chon: Boolean = false // Trạng thái cho voucher được chọn

    )
