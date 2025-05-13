package com.eritlab.jexmon.domain.model

data class ProductFilter(
    val priceRange: ClosedFloatingPointRange<Float> = 0f..Float.MAX_VALUE,
    val rating: Float = 0f,
    val sortBy: SortOption = SortOption.RELEVANCE,
    val categories: List<String> = emptyList()
)

enum class SortOption {
    RELEVANCE,
    PRICE_LOW_TO_HIGH,
    PRICE_HIGH_TO_LOW,
    RATING,
    NEWEST
} 