package com.eritlab.jexmon.presentation.screens.product_detail_screen

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eritlab.jexmon.common.Constrains
import com.eritlab.jexmon.common.Resource
import com.eritlab.jexmon.domain.model.CartItem
import com.eritlab.jexmon.domain.use_case.get_product_detail.GetProductDetailUseCase
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
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

    fun addToCart(cartItem: CartItem): Flow<Result<Unit>> = flow {
        try {
            _state.value = _state.value.copy(isAddingToCart = true, addToCartSuccess = false, addToCartError = "")

            val db = FirebaseFirestore.getInstance()
            val querySnapshot = db.collection("carts")
                .whereEqualTo("userId", cartItem.userId)
                .whereEqualTo("productId", cartItem.productId)
                .whereEqualTo("size", cartItem.size)
                .whereEqualTo("color", cartItem.color)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                // Đã có item tương tự trong giỏ -> cập nhật số lượng
                val doc = querySnapshot.documents.first()
                val existingItem = doc.toObject(CartItem::class.java)
                val updatedQuantity = (existingItem?.quantity ?: 0) + cartItem.quantity

                val updatedItem = cartItem.copy(quantity = updatedQuantity)

                db.collection("carts")
                    .document(doc.id)
                    .set(updatedItem)
                    .await()
            } else {
                // Chưa có -> thêm mới
                db.collection("carts")
                    .add(cartItem)
                    .await()
            }

            _state.value = _state.value.copy(isAddingToCart = false, addToCartSuccess = true)
            emit(Result.success(Unit))

        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isAddingToCart = false,
                addToCartSuccess = false,
                addToCartError = e.message ?: "Không thể thêm vào giỏ hàng"
            )
            emit(Result.failure(e))
        }
    }

}
