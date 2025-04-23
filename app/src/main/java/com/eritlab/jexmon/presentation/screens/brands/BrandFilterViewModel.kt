package com.eritlab.jexmon.presentation.screens.brands

import androidx.lifecycle.ViewModel
import com.eritlab.jexmon.domain.model.BrandModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


data class BrandFilterState(
    val brands: List<BrandModel> = emptyList(),
    val filteredBrands: List<BrandModel> = emptyList(),
    val selectedCategory: String = "all"
)

class BrandFilterViewModel : ViewModel() {
    private val _state = MutableStateFlow(BrandFilterState())
    val state: StateFlow<BrandFilterState> = _state

    fun filterBrandsByCategory(brands: List<BrandModel>, category: String) {
        val normalizedCategory = category.lowercase().trim()
        android.util.Log.d("BrandFilter", "Filtering for category: $normalizedCategory")
        android.util.Log.d("BrandFilter", "Available brands: ${brands.map { "${it.name}:${it.categoryId}" }}")
        
        val filteredBrands = when (normalizedCategory) {
            "Nam","nam" -> brands.filter {
                val brandCategory = it.categoryId?.trim()
                android.util.Log.d("BrandFilter", "Checking brand ${it.name} with category $brandCategory")
                brandCategory == "Nam"
            }
            "Nữ", "nu" -> brands.filter { 
                val brandCategory = it.categoryId?.trim()
                android.util.Log.d("BrandFilter", "Checking brand ${it.name} with category $brandCategory")
                brandCategory == "Nữ"
            }
            "dép", "dep" -> brands.filter { 
                val brandCategory = it.categoryId?.trim()
                android.util.Log.d("BrandFilter", "Checking brand ${it.name} with category $brandCategory")
                brandCategory == "dép"
            }
            "trẻ em", "tre em" -> brands.filter { 
                val brandCategory = it.categoryId?.trim()
                android.util.Log.d("BrandFilter", "Checking brand ${it.name} with category $brandCategory")
                brandCategory == "trẻ em"
            }
            "all" -> brands
            else -> emptyList()
        }
        
        android.util.Log.d("BrandFilter", "Filtered brands count: ${filteredBrands.size}")
        android.util.Log.d("BrandFilter", "Filtered brands: ${filteredBrands.map { it.name }}")

        _state.value = _state.value.copy(
            brands = brands,
            filteredBrands = filteredBrands,
            selectedCategory = category
        )
    }

    fun updateFilteredBrands(filteredBrands: List<BrandModel>) {
        _state.value = _state.value.copy(
            filteredBrands = filteredBrands
        )
    }
}