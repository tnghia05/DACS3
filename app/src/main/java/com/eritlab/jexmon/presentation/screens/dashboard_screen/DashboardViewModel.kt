package com.eritlab.jexmon.presentation.screens.dashboard_screen

import android.util.Log
import androidx.lifecycle.ViewModel
import com.eritlab.jexmon.domain.model.ProductModel
import com.eritlab.jexmon.domain.model.ProductStock
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class DashboardViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val _state = MutableStateFlow(ProductState())
    val state: StateFlow<ProductState> = _state

    init {
        fetchProducts()
        getFavoriteProducts()
    }

    private fun updateProductLists(products: List<ProductModel>) {
        // Lấy 10 sản phẩm mới nhất
        val latest = products.sortedByDescending { it.createdAt.seconds }.take(10)
        
        // Lấy 10 sản phẩm bán chạy nhất
        val bestSelling = products.sortedByDescending { it.sold }.take(10)

        _state.value = _state.value.copy(
            allProducts = products,
            latestProducts = latest,
            bestSellingProducts = bestSelling,
            isLoading = false
        )
    }

    fun toggleFavorite(productId: String) {
        firestore.collection("favorites")
            .document(productId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Nếu sản phẩm đã yêu thích, xóa khỏi danh sách
                    firestore.collection("favorites")
                        .document(productId)
                        .delete()
                        .addOnSuccessListener {
                            getFavoriteProducts() // Refresh danh sách yêu thích
                        }
                } else {
                    // Nếu sản phẩm chưa yêu thích, thêm vào danh sách
                    firestore.collection("favorites")
                        .document(productId)
                        .set(mapOf(
                            "productId" to productId,
                            "timestamp" to com.google.firebase.Timestamp.now()
                        ))
                        .addOnSuccessListener {
                            getFavoriteProducts() // Refresh danh sách yêu thích
                        }
                }
            }
    }

    private fun getFavoriteProducts() {
        firestore.collection("favorites")
            .get()
            .addOnSuccessListener { result ->
                val favoriteIds = result.documents.mapNotNull { it.id }
                // Cập nhật trạng thái yêu thích cho các sản phẩm
                val updatedProducts = _state.value.allProducts.map { product ->
                    product.copy(isFavourite = favoriteIds.contains(product.id))
                }
                updateProductLists(updatedProducts)
            }
    }

    fun fetchProducts() {
        _state.value = _state.value.copy(isLoading = true)

        firestore.collection("products")
            .get()
            .addOnSuccessListener { result ->
                val products = mutableListOf<ProductModel>()

                result.documents.forEach { document ->
                    val product = document.toObject(ProductModel::class.java)?.copy(id = document.id)
                    Log.d("Product", "Product ID: ${product?.id}, Name: ${product?.name}")


                    product?.let { prod ->
                        firestore.collection("products")
                            .document(document.id)
                            .collection("stock") // Fetch stock từ subcollection
                            .get()
                            .addOnSuccessListener { stockResult ->
                                val stockItems = stockResult.documents.mapNotNull { stockDoc ->
                                    stockDoc.toObject(ProductStock::class.java)
                                }
                                products.add(prod.copy(stock = stockItems))
                                
                                // Cập nhật state khi đã lấy đủ data
                                if (products.size == result.documents.size) {
                                    updateProductLists(products)
                                    getFavoriteProducts() // Cập nhật trạng thái yêu thích sau khi lấy sản phẩm
                                }
                            }
                    }
                }
            }
            .addOnFailureListener { exception ->
                _state.value = _state.value.copy(
                    errorMessage = exception.message ?: "Unknown error",
                    isLoading = false
                )
            }
    }


}
