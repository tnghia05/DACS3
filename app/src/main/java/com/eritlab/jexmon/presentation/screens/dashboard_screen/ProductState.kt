package com.eritlab.jexmon.presentation.screens.dashboard_screen

import com.eritlab.jexmon.domain.model.ProductModel

data class ProductState(
    val product: List<ProductModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
}
