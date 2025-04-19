package com.eritlab.jexmon.domain.model

data class  BrandModel(
    val id: String = "",
    val name: String = "",
    val categoryId: String = "",
    val imageUrl: String = "",
    val description: String = ""
) {
    // No-argument constructor for Firestore deserialization
    constructor() : this("", "", "", "", "")
}
