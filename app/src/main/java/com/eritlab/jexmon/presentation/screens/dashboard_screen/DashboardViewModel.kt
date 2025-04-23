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
                                _state.value = _state.value.copy(
                                    product = products,
                                    isLoading = false
                                )
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
