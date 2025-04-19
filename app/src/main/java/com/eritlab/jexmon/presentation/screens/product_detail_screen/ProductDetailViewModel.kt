package com.eritlab.jexmon.presentation.screens.product_detail_screen

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eritlab.jexmon.common.Constrains
import com.eritlab.jexmon.domain.use_case.get_product_detail.GetProductDetailUseCase
import com.eritlab.jexmon.common.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val getProductDetailUseCase: GetProductDetailUseCase,
    stateHandle: SavedStateHandle
) : ViewModel() {
    // State
    private val _state = mutableStateOf(ProductDetailState())
    val state: State<ProductDetailState> = _state

    init {
        val productId = stateHandle.get<String>(Constrains.PRODUCT_ID_PARAM) ?: ""
        Log.d("ProductDetailViewModel", "Received prsoductId: $productId")
        if (productId.isNotBlank()) {
            getProductDetail(productId)
        } else {
            _state.value = ProductDetailState(errorMessage = "Invalid product ID")
        }
    }

    fun getProductDetail(productId: String) {
        if (productId.isBlank()) {
            _state.value = ProductDetailState(errorMessage = "Product ID cannot be empty")
            return
        }

        getProductDetailUseCase(productId).onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = ProductDetailState(isLoading = true)
                }
                is Resource.Success -> {
                    _state.value = if (result.data != null) {
                        ProductDetailState(productDetail = result.data)
                    } else {
                        ProductDetailState(errorMessage = "Product not found")
                    }
                }
                is Resource.Error -> {
                    _state.value = ProductDetailState(errorMessage = result.message ?: "Unknown error")
                }
            }
        }.launchIn(viewModelScope)
    }
}
