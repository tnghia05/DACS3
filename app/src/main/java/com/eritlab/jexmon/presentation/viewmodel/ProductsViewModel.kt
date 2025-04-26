package com.eritlab.jexmon.presentation.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eritlab.jexmon.domain.model.CartItem
import com.eritlab.jexmon.domain.model.ProductModel
import com.eritlab.jexmon.domain.repository.ProductRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _products = mutableStateOf<List<ProductModel>>(emptyList())
    val products: State<List<ProductModel>> = _products

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    init {
        getProducts()
    }

    private fun getProducts() {
        productRepository.getProduct().onEach { result: List<ProductModel> ->
            _products.value = result
            _isLoading.value = false
        }.launchIn(viewModelScope)
    }

    fun addToCart(cartItem: CartItem): Flow<Result<Unit>> = flow {
        try {
            FirebaseFirestore.getInstance().collection("carts")
                .add(cartItem)
                .await()
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}