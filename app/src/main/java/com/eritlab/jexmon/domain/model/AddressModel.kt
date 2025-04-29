package com.eritlab.jexmon.domain.model

data class AddressModel(
    val id: String = "",
    val iduser: String = "",
    val diachi : String = "",
    val sdt : String = "",
    val name : String = "",
    val isDefault: Boolean = false, // Thêm field này
)
