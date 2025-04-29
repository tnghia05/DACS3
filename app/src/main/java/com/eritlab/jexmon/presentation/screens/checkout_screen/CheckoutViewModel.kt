package com.eritlab.jexmon.presentation.screens.checkout_screen

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.eritlab.jexmon.domain.model.AddressModel
import com.eritlab.jexmon.domain.model.CartItem
import com.eritlab.jexmon.domain.model.OrderModel
import com.eritlab.jexmon.domain.model.VoucherModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class CheckoutViewModel @Inject constructor() : ViewModel() {
    private val _cartItems = mutableStateOf<List<CartItem>>(emptyList())
    val cartItems: State<List<CartItem>> = _cartItems

    private val _shippingAddress = mutableStateOf<AddressModel?>(null)
    val shippingAddress: State<AddressModel?> = _shippingAddress

    private val _paymentMethod = mutableStateOf<String?>(null)
    val paymentMethod: State<String?> = _paymentMethod
    private val _vouchers = mutableStateOf<List<VoucherModel>>(emptyList())
    val vouchers: State<List<VoucherModel>> = _vouchers

    private val _subtotal = mutableStateOf(0.0)
    val subtotal: State<Double> = _subtotal

    private val _shippingFee = mutableStateOf(30000.0) // Phí ship mặc định 30k
    val shippingFee: State<Double> = _shippingFee

    private val _discount = mutableStateOf(0.0)
    val discount: State<Double> = _discount

    private val _total = mutableStateOf(0.0)
    val total: State<Double> = _total
    private  val  _discountproduct = mutableStateOf(0.0)
    val discountproduct: State<Double> = _discountproduct

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun setCartItems(items: List<CartItem>) {
        _cartItems.value = items
        calculateTotal()
        calculateProductDiscountTotal()

    }

    fun setShippingAddress(address: AddressModel) {
        _shippingAddress.value = address
    }
    fun setVoucher(voucher: VoucherModel) {
        _discount.value = voucher.discount
        calculateTotal()
    }

    fun setPaymentMethod(method: String) {
        _paymentMethod.value = method
    }

    fun setDiscount(discount: Double) {
        _discount.value = discount
        calculateTotal()
        calculateProductDiscountTotal()

    }
    fun setdiscountproduct(discount: Double) {
        _discountproduct.value = discount
        calculateProductDiscountTotal()
    }
    fun calculateProductDiscountTotal(): Double {
        // Tổng giá sau khi đã áp dụng giảm giá từng sản phẩm
        val discountedTotal = _cartItems.value.sumOf { item ->
            val discountRate = item.discount / 100.0
            val discountedPrice = item.price * (1 - discountRate)
            discountedPrice * item.quantity
        }
        _discountproduct.value = discountedTotal
        return discountedTotal
    }

    private fun calculateTotal() {
        val subtotal = _cartItems.value.sumOf { it.price * it.quantity }
        _subtotal.value = subtotal
        _total.value = subtotal + _shippingFee.value - (_discount.value * subtotal / 100)
    }


    fun fetchShippingAddress() {
        val userId = auth.currentUser?.uid ?: return

        _isLoading.value = true

        db.collection("address")
            .whereEqualTo("iduser", userId)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val document = result.documents.first()
                    val address = AddressModel(
                        id = document.id,
                        diachi = document.getString("diachi") ?: "",
                        sdt = document.getString("sdt") ?: "",
                        name = document.getString("name") ?: "",
                        iduser = document.getString("iduser") ?: ""
                    )
                    setShippingAddress(address) // <<< THÊM DÒNG NÀY

                } else {
                    _error.value = "Không tìm thấy địa chỉ giao hàng"
                }
                _isLoading.value = false
            }
            .addOnFailureListener { exception ->
                _error.value = exception.message
                _isLoading.value = false
            }
    }
    fun selectVoucher(code: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _isLoading.value = true

        db.collection("vouchers")
            .get()
            .addOnSuccessListener { result ->
                val batch = db.batch()
                for (document in result.documents) {
                    val isSelected = document.id == code
                    batch.update(document.reference, "Chon", isSelected)
                }
                batch.commit()
                    .addOnSuccessListener {
                        _isLoading.value = false
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        _isLoading.value = false
                        onError("Không thể cập nhật voucher: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                onError("Lỗi khi lấy vouchers: ${e.message}")
            }
    }

    fun fetchVouchers() {
        _isLoading.value = true

        db.collection("vouchers")
            .get()
            .addOnSuccessListener { result ->
                val vouchersList = result.documents.map { document ->
                    VoucherModel(
                        code = document.id,
                        detail = document.getString("detail") ?: "",
                        discount = document.getDouble("discount") ?: 0.0,
                        quantity = document.getLong("quantity")?.toInt() ?: 0,
                        expiryDate = document.getDate("expiryDate"),
                        chon = document.getBoolean("Chon") ?: false // Mặc định không chọn voucher

                    )
                }
                // Cập nhật danh sách vouchers vào state
                _vouchers.value = vouchersList
                _isLoading.value = false
            }
            .addOnFailureListener { exception ->
                _error.value = exception.message
                _isLoading.value = false
            }
    }
    private val _selectedVoucher = MutableStateFlow<VoucherModel?>(null)
    val selectedVoucher: StateFlow<VoucherModel?> = _selectedVoucher

    // check voucher true
    fun fetchSelectedVoucher() {
        _isLoading.value = true

        db.collection("vouchers")
            .whereEqualTo("Chon", true)
            .limit(1) // <-- Chỉ lấy 1 voucher được chọn
            .get()
            .addOnSuccessListener { result ->
                val selectedVoucher = result.documents.firstOrNull()?.let { document ->
                    VoucherModel(
                        code = document.id,
                        detail = document.getString("detail") ?: "",
                        discount = document.getDouble("discount") ?: 0.0,
                        quantity = document.getLong("quantity")?.toInt() ?: 0,
                        expiryDate = document.getDate("expiryDate"),
                        chon = document.getBoolean("Chon") ?: false
                    )
                }
                _selectedVoucher.value = selectedVoucher // <-- Chỉ lưu 1 voucher đang chọn
                _isLoading.value = false
            }
            .addOnFailureListener { exception ->
                _error.value = exception.message
                _isLoading.value = false
            }
    }




    fun placeOrder(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val paymentMethod = _paymentMethod.value ?: return

        _isLoading.value = true

        val order = OrderModel(
            userId = userId,
            items = _cartItems.value,
            paymentMethod = paymentMethod,
            subtotal = _subtotal.value,
            shippingFee = _shippingFee.value,
            discount = _discount.value,
            total = _total.value
        )

        db.collection("orders")
            .add(order)
            .addOnSuccessListener { documentRef ->
                // Cập nhật ID cho đơn hàng
                documentRef.update("id", documentRef.id)
                    .addOnSuccessListener {
                        // Xóa giỏ hàng sau khi đặt hàng thành công
                        clearCart(userId) {
                            _isLoading.value = false
                            onSuccess()
                        }
                    }
                    .addOnFailureListener { e ->
                        _isLoading.value = false
                        onError(e.message ?: "Đã xảy ra lỗi khi tạo đơn hàng")
                    }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                onError(e.message ?: "Đã xảy ra lỗi khi tạo đơn hàng")
            }
    }
    fun fetchCartItems() {
        val userId = auth.currentUser?.uid ?: return

        _isLoading.value = true

        db.collection("carts")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val cartList = result.documents.map { doc ->
                    CartItem(
                        id = doc.getString("id") ?: "",
                        name = doc.getString("name") ?: "",
                        price = (doc.getDouble("price") ?: 0.0),
                        quantity = (doc.getLong("quantity")?.toInt() ?: 1),
                        imageUrl = doc.getString("imageUrl") ?: "",
                        productId = doc.getString("productId") ?: "",
                        discount = (doc.getDouble("discount") ?: 0.0),
                        size = (doc.getLong("size")?.toInt() ?: 0),
                        color = convertToColorName(doc.getString("color") ?: "#FFFFFF") // Mặc định là trắng
                    )
                }
                setCartItems(cartList)
                _isLoading.value = false
            }
            .addOnFailureListener { exception ->
                _error.value = exception.message
                _isLoading.value = false
            }
    }
    fun convertToHex(color: String): String {
        val colorMap = mapOf(
            "red" to "#FF0000",
            "blue" to "#0000FF",
            "green" to "#008000",
            "yellow" to "#FFFF00",
            "black" to "#000000",
            "white" to "#FFFFFF",
            "gray" to "#808080",
            "purple" to "#800080",
            "orange" to "#FFA500",
            "pink" to "#FFC0CB"
        )

        return colorMap[color.lowercase()] ?: color // Nếu không tìm thấy, giả định nó là mã hex
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

        return colorMap[hex.uppercase()] ?: hex // Nếu không tìm thấy, giữ nguyên mã hex
    }


    private fun clearCart(userId: String, onComplete: () -> Unit) {
        db.collection("carts")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val batch = db.batch()
                result.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit()
                    .addOnSuccessListener {
                        _cartItems.value = emptyList()
                        onComplete()
                    }
                    .addOnFailureListener {
                        onComplete()
                    }
            }
            .addOnFailureListener {
                onComplete()
            }
    }
    fun getSelectedVouchers(): List<VoucherModel> {
        return _vouchers.value.filter { it.chon }
    }
    fun setSelectedVouchers(selectedVouchers: List<VoucherModel>) {
        _vouchers.value = _vouchers.value.map { voucher ->
            if (selectedVouchers.contains(voucher)) {
                voucher.copy(chon = true)
            } else {
                voucher.copy(chon = false)
            }
        }
    }



}