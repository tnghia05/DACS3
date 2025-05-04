package com.eritlab.jexmon.presentation.screens.cart_screen

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.eritlab.jexmon.domain.model.CartItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor() : ViewModel() {
    private val _cartItems = mutableStateOf<List<CartItem>>(emptyList())
    val cartItems: State<List<CartItem>> = _cartItems

    private val _totalAmount = mutableStateOf(0.0)
    val totalAmount: State<Double> = _totalAmount

    private val _discountAmount = mutableStateOf(0.0)
    val discountAmount: State<Double> = _discountAmount

    private val _appliedVoucher = mutableStateOf<String?>(null)
    val appliedVoucher: State<String?> = _appliedVoucher

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        loadCartItems()
    }

    fun loadCartItems() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("carts")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val items = result.documents.mapNotNull { doc ->
                    doc.toObject(CartItem::class.java)?.copy(id = doc.id) // 👈 chỗ này
                }
                _cartItems.value = items
                calculateTotal()
            }
            .addOnFailureListener {
                _cartItems.value = emptyList()
            }
    }

    private fun calculateTotal() {

        val total1 = _cartItems.value.sumOf { (1 - it.discount.toFloat() / 100)*it.price *it.quantity }

        Log.d("CartViewModel", "Total1: $total1")
//        val subtotal = _cartItems.value.sumOf { total1 * it.quantity }
//        Log.d("CartViewModel", "Subtotal: $subtotal")
        _totalAmount.value = total1 - _discountAmount.value*total1/100
    }
    fun resetAllVouchersSelection(onSuccess: () -> Unit, onError: (String) -> Unit) {
        db.collection("vouchers")
            .whereEqualTo("Chon", true) // Chỉ lấy những voucher đang được chọn
            .get()
            .addOnSuccessListener { result ->
                val batch = db.batch()
                for (document in result.documents) {
                    batch.update(document.reference, "Chon", false)
                }
                batch.commit()
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        onError("Không thể reset voucher: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                onError("Lỗi khi lấy vouchers: ${e.message}")
            }
    }

    fun checkVoucher(code: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        db.collection("vouchers")
            .document(code)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val discount = document.getDouble("discount") ?: 0.0
                    val expiryDate = document.getTimestamp("expiryDate")?.toDate()
                    val quantity = document.getLong("quantity")?.toInt() ?: 0
                    val currentDate = java.util.Date()

                    when {
                        expiryDate != null && currentDate.after(expiryDate) -> {
                            onError("Voucher đã hết hạn")
                        }
                        quantity <= 0 -> {
                            onError("Voucher đã hết lượt sử dụng")
                        }
                        else -> {
                            // Cập nhật số lượng voucher
                            document.reference.update(
                                mapOf(
                                    "quantity" to quantity - 1,
                                    "Chon" to true
                                )
                            )
                                .addOnSuccessListener {
                                    _discountAmount.value = discount
                                    _appliedVoucher.value = code
                                    calculateTotal()
                                    onSuccess()
                                }
                                .addOnFailureListener {
                                    onError("Không thể áp dụng voucher")
                                }
                        }
                    }
                } else {
                    onError("Mã voucher không hợp lệ")
                }
            }
            .addOnFailureListener {
                onError("Đã xảy ra lỗi khi kiểm tra voucher")
            }
    }

    fun removeVoucher() {
        _discountAmount.value = 0.0
        _appliedVoucher.value = null
        calculateTotal()
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _totalAmount.value = 0.0
    }
    fun removeItem(cartItem: CartItem) {
        val userId = auth.currentUser?.uid ?: return
        Log.d("CartViewModel", "Removed item: ${cartItem.id}")
        db.collection("carts")
            .document(cartItem.id)
            .delete()
            .addOnSuccessListener {
                loadCartItems()
            }
            .addOnFailureListener {

                // Handle failure
            }
    }
    fun updateQuantity(cartId: String, newQuantity: Int) {
        val userId = auth.currentUser?.uid ?: return
        val cartItem = _cartItems.value.find { it.id == cartId } ?: return // 🔥 dùng id, không dùng productId

        if (newQuantity == 0) {
            removeItem(cartItem)
            return
        }
        val updatedItem = cartItem.copy(quantity = newQuantity)

        db.collection("carts")
            .document(cartItem.id ?: return) // nếu id null thì bỏ luôn
            .set(updatedItem)
            .addOnSuccessListener {
                loadCartItems()
            }
            .addOnFailureListener {
                // TODO: handle failure
            }
    }

}
