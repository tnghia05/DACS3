package com.eritlab.jexmon.presentation.screens.cart_screen

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
        val subtotal = _cartItems.value.sumOf { it.price * it.quantity }
        _totalAmount.value = subtotal - _discountAmount.value*subtotal/100
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
                            document.reference.update("quantity", quantity - 1)
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

        db.collection("carts")
            .document(cartItem.productId)
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
