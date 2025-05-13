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
    // Kiểm tra số lượng sản phẩm trong giỏ hàng với số lượng trong kho
    fun checkCartStock(CartItem: CartItem, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        val productId = CartItem.productId
        val color = CartItem.color
        val size = CartItem.size

        // Truy vấn đến collection products -> stock -> size_color để lấy thông tin số lượng trong kho
        db.collection("products")
            .document(productId)
            .collection("stock")
            .document("size_${size}_${color}")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Lấy số lượng trong kho
                    val stockQuantity = document.getLong("quantity")?.toInt() ?: 0
                    
                    // So sánh với số lượng trong giỏ hàng
                    if (CartItem.quantity <= stockQuantity) {
                        // Nếu đủ số lượng, cập nhật lại số lượng trong kho
                        val newStockQuantity = stockQuantity - CartItem.quantity
                        
                        // Cập nhật số lượng mới vào Firestore
                        document.reference.update("quantity", newStockQuantity)
                            .addOnSuccessListener {
                                // Callback thành công với số lượng còn lại trong kho
                                onSuccess(newStockQuantity)
                            }
                            .addOnFailureListener { e ->
                                onError("Không thể cập nhật số lượng trong kho: ${e.message}")
                            }
                    } else {
                        // Nếu không đủ số lượng, thông báo lỗi
                        onError("Số lượng sản phẩm trong kho không đủ (còn $stockQuantity)")
                    }
                } else {
                    onError("Không tìm thấy thông tin sản phẩm trong kho")
                }
            }
            .addOnFailureListener { e ->
                onError("Lỗi khi kiểm tra kho: ${e.message}")
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
    fun convertToColorName(hex: String): String {
        val colorMap = mapOf(
            "#FF0000" to "Red",
            "#0000FF" to "Blue",
            "#008000" to "Green",
            "#FFFF00" to "Yellow",
            "#000000" to "Black",
            "#FFFFFF" to "White",
            "#808080" to "Gray",
            "#800080" to "Purple",
            "#FFA500" to "Orange",
            "#FFC0CB" to "Pink"
        )
        return colorMap[hex] ?: "Unknown"
    }

    // Hàm kiểm tra tất cả sản phẩm trong giỏ hàng
    fun checkAllCartItems(onSuccess: () -> Unit, onError: (String) -> Unit) {
        // Lấy danh sách hiện tại từ state
        val currentCartItems = _cartItems.value
        Log.d("CartViewModel", "Current cart items: $currentCartItems")
        
        // Nếu giỏ hàng trống
        if (currentCartItems.isEmpty()) {
            onError("Giỏ hàng trống")
            return
        }

        // Sử dụng batch để thực hiện nhiều thao tác cùng lúc
        val batch = db.batch()
        var hasError = false
        var itemsProcessed = 0
        
        // Kiểm tra từng sản phẩm trong giỏ hàng
        currentCartItems.forEach { cartItem ->
            Log.d("CartViewModel", "Checking item: ${cartItem.productId}, size: ${cartItem.size}, color: ${cartItem.color}, quantity: ${cartItem.quantity}")
            Log.d("CartViewModel", "Document path: products/${cartItem.productId}/stock/size_${cartItem.size}_${cartItem.color}")
        val color = convertToColorName(cartItem.color)
            db.collection("products")
                .document(cartItem.productId)
                .collection("stock")
                .document("size_${cartItem.size}_$color")
                .get()
                .addOnSuccessListener { document ->

                    itemsProcessed++
                    
                    if (document.exists()) {
                        val stockQuantity = document.getLong("quantity")?.toInt() ?: 0
                        
                        if (cartItem.quantity <= stockQuantity) {
                            // Nếu đủ số lượng, thêm operation cập nhật vào batch
                            val newStockQuantity = stockQuantity - cartItem.quantity
                            batch.update(document.reference, "quantity", newStockQuantity)
                            
                            // Nếu đã xử lý tất cả sản phẩm và không có lỗi
                            if (itemsProcessed == currentCartItems.size && !hasError) {
                                // Thực hiện tất cả các thay đổi
                                batch.commit()
                                    .addOnSuccessListener {
                                        onSuccess()
                                    }
                                    .addOnFailureListener { e ->
                                        onError("Lỗi khi cập nhật số lượng: ${e.message}")
                                    }
                            }
                        } else {
                            hasError = true
                            onError("Sản phẩm '${cartItem.name}' không đủ số lượng trong kho (yêu cầu: ${cartItem.quantity}, còn lại: $stockQuantity)")
                        }
                    } else {
                        hasError = true
                        onError("Không tìm thấy thông tin sản phẩm '${cartItem.name}' trong kho")
                        Log.d("CartViewModel", "Không tìm thấy thông tin sản phẩm '${cartItem.name}' trong kho")
                    }
                }
                .addOnFailureListener { e ->
                    hasError = true
                    onError("Lỗi khi kiểm tra kho: ${e.message}")
                }
        }
    }

}
