package com.eritlab.jexmon.domain.model

data class VoucherModel(
    val code: String? = null,
    val value: String? = null,
    val valid_from: Long? = null,
    val created_at: Long? = null,
    val status: String? = null,
    val customerId : String
)
