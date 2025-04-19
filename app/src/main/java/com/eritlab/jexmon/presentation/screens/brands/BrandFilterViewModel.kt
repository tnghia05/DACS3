package com.eritlab.jexmon.presentation.screens.brands

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.eritlab.jexmon.domain.model.BrandModel


data class BrandFilterState(
    val brands: List<BrandModel> = emptyList(),
    val filteredBrands: List<BrandModel> = emptyList(),
    val selectedCategory: String = "all"
)

class BrandFilterViewModel : ViewModel() {
    private val _state = MutableStateFlow(BrandFilterState())
    val state: StateFlow<BrandFilterState> = _state

    fun filterBrandsByCategory(brands: List<BrandModel>, category: String) {
        val filteredBrands = when (category) {
            "men" -> brands.filter { it.categoryId == "men" }
            "women" -> brands.filter { it.categoryId == "women" }
            "shoes" -> brands.filter { it.categoryId == "shoes" }
            "kids" -> brands.filter { it.categoryId == "kids" }
            else -> brands
        }
        _state.value = _state.value.copy(
            brands = brands,
            filteredBrands = filteredBrands,
            selectedCategory = category
        )
    }
}