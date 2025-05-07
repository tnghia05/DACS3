package com.eritlab.jexmon.presentation.screens.product_detail_screen

import com.eritlab.jexmon.domain.model.ProductModel
import com.eritlab.jexmon.domain.model.ReviewModel

data class ProductDetailState(
    val isLoading: Boolean = false,
    val productDetail: ProductModel? = null,
    val errorMessage: String = "",
    val isAddingToCart: Boolean = false,
    val addToCartSuccess: Boolean = false,
    val addToCartError: String = "",
    val reviews: List<ReviewModel> = emptyList(), // Sử dụng ReviewModel
    val isLoadingReviews: Boolean = false,
    val errorLoadingReviews: String? = null,
    val currentUserName: String? = null
)