package com.eritlab.jexmon.presentation.screens.don_da_mua

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.eritlab.jexmon.domain.model.OrderModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor() : ViewModel() {
    private val _orders = mutableStateOf<List<OrderModel>>(emptyList())
    val orders: State<List<OrderModel>> = _orders

    private val _filteredOrders = mutableStateOf<List<OrderModel>>(emptyList())
    val filteredOrders: State<List<OrderModel>> = _filteredOrders

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        loadOrders()
    }

    fun loadOrders() {
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true

        db.collection("orders")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val ordersList = result.documents.mapNotNull { doc ->
                    doc.toObject(OrderModel::class.java)?.copy(id = doc.id)
                }
                _orders.value = ordersList
                _filteredOrders.value = ordersList // Mặc định hiển thị tất cả đơn hàng
                _isLoading.value = false
            }
            .addOnFailureListener { exception ->
                _error.value = exception.message
                _isLoading.value = false
            }
    }

    fun filterOrdersByStatus(status: String?) {
        if (status == null || status.isEmpty()) {
            _filteredOrders.value = _orders.value // Hiển thị tất cả nếu không có bộ lọc
        } else {
            _filteredOrders.value = _orders.value.filter { it.status == status }
        }
    }
}