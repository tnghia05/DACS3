package com.eritlab.jexmon.presentation.screens.admin.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eritlab.jexmon.domain.model.OrderModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class OrderManagementState(
    val orders: List<OrderModel> = emptyList(),
    val filteredOrders: List<OrderModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedStatus: String? = null
)

class OrderManagementViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val _state = MutableStateFlow(OrderManagementState())
    val state: StateFlow<OrderManagementState> = _state

    init {
        fetchOrders()
    }

    fun fetchOrders() {
        _state.value = _state.value.copy(isLoading = true)
        
        firestore.collection("orders")
            .get()
            .addOnSuccessListener { result ->
                val orders = result.documents.mapNotNull { doc ->
                    doc.toObject(OrderModel::class.java)?.copy(id = doc.id)
                }
                _state.value = _state.value.copy(
                    orders = orders,
                    filteredOrders = orders,
                    isLoading = false
                )
            }
            .addOnFailureListener { exception ->
                _state.value = _state.value.copy(
                    error = exception.message,
                    isLoading = false
                )
            }
    }

    fun filterOrdersByStatus(status: String?) {
        _state.value = _state.value.copy(selectedStatus = status)
        if (status == null || status.isEmpty()) {
            _state.value = _state.value.copy(filteredOrders = _state.value.orders)
        } else {
            _state.value = _state.value.copy(
                filteredOrders = _state.value.orders.filter { it.status == status }
            )
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: String) {
        try {
            firestore.collection("orders").document(orderId)
                .update("status", newStatus)
                .await()
            
            // Refresh orders after update
            fetchOrders()
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = e.message)
        }
    }

    suspend fun updatePaymentStatus(orderId: String, newPaymentStatus: String) {
        try {
            firestore.collection("orders").document(orderId)
                .update("paymentStatus", newPaymentStatus)
                .await()
            
            // Refresh orders after update
            fetchOrders()
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = e.message)
        }
    }

    fun deleteOrder(orderId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("orders").document(orderId)
            .delete()
            .addOnSuccessListener {
                onSuccess()
                fetchOrders() // Refresh the list after deletion
            }
            .addOnFailureListener { onFailure(it) }
    }
} 