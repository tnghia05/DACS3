package com.eritlab.jexmon.presentation.screens.checkout_screen

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eritlab.jexmon.data.zalopay.Api.CreateOrder
import com.eritlab.jexmon.domain.model.AddressModel
import com.eritlab.jexmon.domain.model.CartItem
import com.eritlab.jexmon.domain.model.OrderModel
import com.eritlab.jexmon.domain.model.VoucherModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import vn.zalopay.sdk.ZaloPayError
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    @ApplicationContext private val context: Context // Thêm annotation @ApplicationContext
) : ViewModel() {

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

    private val _aftertotal = mutableStateOf(0.0)
    val aftertotal: State<Double> = _aftertotal
    private val _discountproduct = mutableStateOf(0.0)
    val discountproduct: State<Double> = _discountproduct

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error
    // State để giữ mã giao dịch ZaloPay (ViewModel chỉ lưu token, không gọi SDK)
    private val _zaloPayToken = mutableStateOf<String?>(null)
    val zaloPayToken: State<String?> = _zaloPayToken
    sealed class PaymentEvent {
        object Success : PaymentEvent() // Sự kiện thanh toán thành công
        data class Error(val message: String) : PaymentEvent() // Sự kiện lỗi (chứa thông báo)
        object Cancelled : PaymentEvent() // Sự kiện hủy
        // Bạn có thể thêm các loại sự kiện khác nếu cần, ví dụ NavigateToOrderDetails(val orderId: String)
    }

    private val _paymentEvents = MutableSharedFlow<PaymentEvent>()
    val paymentEvents: SharedFlow<PaymentEvent> = _paymentEvents.asSharedFlow()
    // State để giữ AppTransID hoặc OrderId sau khi order được tạo thành công
    private val _currentOrderId = mutableStateOf<String?>(null)
    val currentOrderId: State<String?> = _currentOrderId
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun setCartItems(items: List<CartItem>) {
        _cartItems.value = items
        calculateTotal()
        calculateProductDiscountTotal()
        calculateProductDiscountTotalnodis()

    }

    fun setShippingAddress(address: AddressModel) {
        _shippingAddress.value = address
    }

    fun setVoucher(voucher: VoucherModel) {
        _discount.value = voucher.discount
        calculateTotal()
        calculateProductDiscountTotal()
        calculateProductDiscountTotalnodis()

    }

    fun setPaymentMethod(method: String) {
        _paymentMethod.value = method
    }

    fun setDiscount(discount: Double) {
        _discount.value = discount
        calculateTotal()
        calculateProductDiscountTotal()
        calculateProductDiscountTotalnodis()

    }

    fun setdiscountproduct(discount: Double) {
        _discountproduct.value = discount
        calculateProductDiscountTotal()
        calculateProductDiscountTotalnodis()

    }

    fun calculateProductDiscountTotal(): Double {
        fetchSelectedVoucher()
        // Tổng giá sau khi đã áp dụng giảm giá từng sản phẩm
        val discountedTotal = _cartItems.value.sumOf { item ->
            val discountRate = item.discount / 100.0
            val discountedPrice = item.price * (1 - discountRate)
            discountedPrice * item.quantity
        }
        Log.d("CheckoutViewModel", "Discounted Total: ${_discountproduct.value}")
        // Ghi log tổng giá sau khi đã áp dụng giảm giá
        if (_selectedVoucher.value != null) {
            Log.d("CheckoutViewModel", "Discounted Total1: ${_discountproduct.value}")

            Log.d("CheckoutViewModel", "Selected Voucher: ${_selectedVoucher.value!!.discount}")
            _discountproduct.value = discountedTotal - (_selectedVoucher.value!!.discount * discountedTotal / 100)
            Log.d("CheckoutViewModel", "Discounted Total: ${_discountproduct.value}")
        } else {
            _discountproduct.value = discountedTotal
        }
        return discountedTotal
    }  fun calculateProductDiscountTotalnodis(): Double {
        fetchSelectedVoucher()
        val discountedTotal = _cartItems.value.sumOf { item ->
            val discountedPrice = item.price
            discountedPrice * item.quantity
        }
        // Ghi log tổng giá sau khi đã áp dụng giảm giá
        _aftertotal.value = discountedTotal

        Log.d("CheckoutViewModel", "Discounted Total: ${_aftertotal.value}")
        return discountedTotal
    }


    private fun calculateTotal() {
        val subtotal = _cartItems.value.sumOf { it.price * it.quantity }
        _subtotal.value = subtotal
        Log.d("CheckoutViewModel", "Subtotal: ${formatPrice(subtotal)}")
        Log.d("CheckoutViewModel", "Discount: ${_discount.value}")
        _total.value = subtotal  - (_discount.value * subtotal / 100)
        Log.d("CheckoutViewModel", "Total: ${formatPrice(_total.value)}")
    }
    // Đảm bảo hàm formatPrice không thay đổi
    private fun formatPrice(price: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        return format.format(price)
    }

    fun fetchDefaultShippingAddress() {
        val userId = auth.currentUser?.uid ?: return

        _isLoading.value = true

        db.collection("address")
            .whereEqualTo("iduser", userId)
            .whereEqualTo("isDefault", true)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val document = result.documents.first()
                    val address = AddressModel(
                        id = document.id,
                        diachi = document.getString("diachi") ?: "",
                        sdt = document.getString("sdt") ?: "",
                        name = document.getString("name") ?: "",
                        iduser = document.getString("iduser") ?: "",
                        isDefault = document.getBoolean("isDefault") ?: false
                    )
                    _shippingAddress.value = address
                } else {
                    _error.value = "Không tìm thấy địa chỉ mặc định"
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

                _discount.value = selectedVoucher?.discount ?: 0.0
                if (_selectedVoucher.value != null) {
                    Log.d("CheckoutViewModel", "Selected Voucher1: ${_selectedVoucher.value!!.discount}")
                } else {
                    Log.d("CheckoutViewModel", "No voucher selected1")
                }

                _isLoading.value = false
            }
            .addOnFailureListener { exception ->
                _error.value = exception.message
                _isLoading.value = false
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
                        priceBeforeDiscount = (doc.getDouble("priceBeforeDiscount") ?: 0.0),
                        size = (doc.getLong("size")?.toInt() ?: 0),
                        color = convertToColorName(
                            doc.getString("color") ?: "#FFFFFF"
                        ) // Mặc định là trắng
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


    // *** Hàm xử lý quy trình thanh toán (Tạo Order -> Lấy Token) ***
    fun processPayment(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onError("Người dùng chưa đăng nhập")
            return
        }
        val paymentMethod = _paymentMethod.value
        if (paymentMethod == null) {
            onError("Vui lòng chọn phương thức thanh toán")
            return
        }
        val shippingAddress = _shippingAddress.value
        if (shippingAddress == null) {
            onError("Vui lòng chọn địa chỉ giao hàng")
            return
        }
        val cartItems = _cartItems.value
        if (cartItems.isEmpty()) {
            onError("Giỏ hàng trống")
            return
        }
        if (paymentMethod != "ZaloPay") {
            // Xử lý các phương thức không phải ZaloPay ở đây
            _isLoading.value = true // Bắt đầu loading cho COD
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val order = OrderModel(
                        userId = userId,
                        items = cartItems,
                        shippingAddress = shippingAddress,
                        paymentMethod = paymentMethod,
                        subtotal = _subtotal.value,
                        discount = discount.value,
                        shippingFee = _shippingFee.value,
                        total = total.value,
                        status = "Chờ xác nhận", // Trạng thái chờ xác nhận cho COD
                        paymentStatus = "Chưa thanh toán" // Trạng thái chưa thanh toán cho COD
                    )
                    val documentRef = db.collection("orders").add(order).await()
                    db.collection("orders").document(documentRef.id).update("id", documentRef.id).await()
                    
                    // Xóa voucher đã chọn nếu có
                    _selectedVoucher.value?.let { voucher ->
                        db.collection("vouchers").document(voucher.code).update("Chon", false).await()
                    }

                    // Xóa giỏ hàng sau khi đặt hàng thành công
                    clearCart(userId) {
                        // Chuyển về Main thread để gọi callback UI
                        viewModelScope.launch(Dispatchers.Main) {
                            onSuccess() // Gọi onSuccess cho COD
                            _isLoading.value = false // Kết thúc loading cho COD
                            _selectedVoucher.value = null // Xóa voucher đã chọn khỏi state UI
                        }
                    }
                } catch (e: Exception) {
                    launch(Dispatchers.Main) {
                        onError("Lỗi đặt hàng COD: ${e.message}") // Gọi onError cho COD
                        _isLoading.value = false // Kết thúc loading cho COD
                    }
                }
            }
            return // Kết thúc hàm nếu không phải ZaloPay
        }

        // *** LOGIC CHỈ DÀNH CHO ZALOPAY ***
        _isLoading.value = true // Bắt đầu loading cho ZaloPay

        viewModelScope.launch(Dispatchers.IO) { // Chạy trên IO thread
            try {
                // Tạo đơn hàng trên Firestore trước để có ID
                val order = OrderModel(
                    userId = userId,
                    items = cartItems,
                    shippingAddress = shippingAddress,
                    paymentMethod = paymentMethod,
                    subtotal = _subtotal.value, // Tổng tiền hàng gốc
                    discount = discount.value, // Số tiền giảm giá voucher
                    shippingFee = _shippingFee.value, // Phí vận chuyển
                    total = total.value, // Tổng tiền cuối cùng
                    status = "Chua xac nhan", // Trạng thái đơn hàng
                    paymentStatus = "Chua thanh toan", // Trạng thái thanh toán
                )

                val documentRef = db.collection("orders").add(order).await()
                val orderId = documentRef.id
                db.collection("orders").document(orderId).update("id", orderId).await()

                // Lưu OrderId vào state để sử dụng sau này (ví dụ: khi xử lý kết quả trả về)
                _currentOrderId.value = orderId


                // *** BƯỚC QUAN TRỌNG: GỌI BACKEND ĐỂ LẤY ZALOPAY TOKEN ***
                // Sử dụng suspend function createOrder() của bạn

                val zpTransToken = createOrder(
                    amount = _total.value.toLong(), // Sử dụng tổng tiền cuối cùng để tạo order ZaloPay
                    description = "Thanh toán đơn hàng ${orderId}"
                )
                Log.d("ZaloPayRequest", "amount: ${_discountproduct.value.toLong()}")


                // *** Cập nhật state zaloPayToken ***
                // Việc cập nhật này sẽ được Composable quan sát và kích hoạt gọi SDK
                _zaloPayToken.value = zpTransToken

                // Không gọi onSuccess/onError ở đây.
                // loading = true vẫn giữ cho đến khi ZaloPay listener trả về kết quả.

            } catch (e: Exception) {
                // Xử lý lỗi trong quá trình tạo order trên Firestore hoặc gọi backend
                _error.value = "Lỗi xử lý thanh toán: ${e.message}"
                Log.d("CheckoutViewModel", "Error processing ZaloPay payment", e)
                // Chuyển về Main thread để gọi callback UI
                launch(Dispatchers.Main) {
                    onError("Lỗi xử lý thanh toán ZaloPay: ${e.message}")
                    _isLoading.value = false // Kết thúc loading nếu có lỗi trước khi gọi SDK
                }
            }
        }
    }

    // Trong hàm handleZaloPayResult trong CheckoutViewModel.kt

    fun handleZaloPayResult(errorCode: ZaloPayError?, transToken: String?, appTransID: String?) = viewModelScope.launch(Dispatchers.IO) {
        _isLoading.value = false // Kết thúc trạng thái loading khi có kết quả từ ZaloPay

        val orderId = _currentOrderId.value // Lấy OrderId đang xử lý
        _currentOrderId.value = null // Xóa OrderId hiện tại sau khi xử lý

        try {
            when (errorCode) {
                ZaloPayError.EMPTY_RESULT -> {
                    Log.d("ZaloPay Result", "CANCELED: $transToken, AppTransID: $appTransID, OrderId: $orderId")
                    orderId?.let {
                        db.collection("orders").document(it).update("status", "Chờ xác nhận").await()
                    }
                    launch(Dispatchers.Main) { _paymentEvents.emit(PaymentEvent.Cancelled) }
                }
                ZaloPayError.FAIL -> { // Lỗi chung từ ZaloPay
                    Log.e("ZaloPay Result", "FAIL: $transToken, AppTransID: $appTransID, OrderId: $orderId, ErrorCode: $errorCode")
                    orderId?.let {
                        db.collection("orders").document(it).update("status", "Payment failed").await()
                    }
                    val errorMessage = "Thanh toán ZaloPay thất bại."
                    launch(Dispatchers.Main) { _paymentEvents.emit(PaymentEvent.Error(errorMessage)) }
                }
                null -> { // Thành công (errorCode == null trong onPaymentSucceeded)
                    Log.d("ZaloPay Result", "SUCCESS: TransactionId: $transToken, AppTransID: $appTransID, OrderId: $orderId")
                    orderId?.let {
                        db.collection("orders").document(it).update("status", "Đang giao").await()
                        db.collection("orders").document(it).update("paymentStatus", "ZaloPay").await()

                        _selectedVoucher.value?.let { voucher ->
                            db.collection("vouchers").document(voucher.code).update("Chon", false).await()
                        }
                        launch(Dispatchers.Main) { _selectedVoucher.value = null } // Reset voucher state UI
                    }
                    launch(Dispatchers.Main) { _paymentEvents.emit(PaymentEvent.Success) }
                }

                // *** THÊM CÁC NHÁNH BỊ THIẾU THEO YÊU CẦU CỦA TRÌNH BIÊN DỊCH ***
                ZaloPayError.UNKNOWN -> { // Lỗi không xác định
                    Log.e("ZaloPay Result", "UNKNOWN ERROR: $transToken, AppTransID: $appTransID, OrderId: $orderId")
                    orderId?.let {
                        db.collection("orders").document(it).update("status", "Payment failed (Unknown)").await()
                    }
                    launch(Dispatchers.Main) { _paymentEvents.emit(PaymentEvent.Error("Lỗi thanh toán không xác định từ ZaloPay.")) }
                }
                ZaloPayError.PAYMENT_APP_NOT_FOUND -> { // Không tìm thấy ứng dụng ZaloPay
                    Log.e("ZaloPay Result", "APP NOT FOUND: $transToken, AppTransID: $appTransID, OrderId: $orderId")
                    // Không cần cập nhật status order thành failed nếu chỉ do app không tìm thấy, user có thể thử lại
                    // Gửi event báo lỗi và gợi ý cài app
                    launch(Dispatchers.Main) { _paymentEvents.emit(PaymentEvent.Error("Không tìm thấy ứng dụng ZaloPay. Vui lòng cài đặt.")) }
                }
                ZaloPayError.INPUT_IS_INVALID -> { // Dữ liệu đầu vào không hợp lệ
                    Log.e("ZaloPay Result", "INPUT INVALID: $transToken, AppTransID: $appTransID, OrderId: $orderId")
                    orderId?.let {
                        db.collection("orders").document(it).update("status", "Payment failed (Input Invalid)").await()
                    }
                    launch(Dispatchers.Main) { _paymentEvents.emit(PaymentEvent.Error("Dữ liệu thanh toán không hợp lệ. Vui lòng thử lại.")) }
                }
                ZaloPayError.EMPTY_RESULT -> { // Kết quả trả về rỗng
                    Log.e("ZaloPay Result", "EMPTY RESULT: $transToken, AppTransID: $appTransID, OrderId: $orderId")
                    orderId?.let {
                        db.collection("orders").document(it).update("status", "Payment failed (Empty Result)").await()
                    }
                    launch(Dispatchers.Main) { _paymentEvents.emit(PaymentEvent.Error("ZaloPay trả về kết quả rỗng. Vui lòng thử lại.")) }
                }
            }
        } catch (e: Exception) {
            Log.e("CheckoutViewModel", "Error during post ZaloPay processing", e)
            launch(Dispatchers.Main) { _paymentEvents.emit(PaymentEvent.Error("Lỗi xử lý sau thanh toán: ${e.message}")) }
        }
    }
    // suspend function tạo order ZaloPay (gọi backend)
    private suspend fun createOrder(amount: Long, description: String): String {
        Log.d("ZaloPayRequest", "amount: $amount, description: $description")
        return withContext(Dispatchers.IO) {
            var retryCount = 0
            val maxRetries = 3
            var lastException: Exception? = null

            while (retryCount < maxRetries) {
                try {
                    val createOrder = CreateOrder()
                    val jsonResult = createOrder.createOrder(amount.toString())
                    Log.d("ZaloPay API Response", "Response from ZaloPay API: $jsonResult")

                    when {
                        jsonResult == null -> {
                            Log.e("ZaloPay API Response", "API response is null")
                            throw Exception("Không thể tạo đơn hàng ZaloPay: Kết quả trả về null")
                        }
                        !jsonResult.has("return_code") || jsonResult.getInt("return_code") != 1 -> {
                            val returnCode = if (jsonResult.has("return_code")) jsonResult.getInt("return_code") else -99
                            val returnMessage = if (jsonResult.has("return_message")) jsonResult.getString("return_message") else "Lỗi không rõ"
                            Log.e("ZaloPay API Response", "API returned error: Code $returnCode, Message: $returnMessage")
                            
                            // Nếu là lỗi timeout hoặc network, thử lại
                            if (returnCode == -99) {
                                lastException = Exception("Lỗi từ API backend: $returnMessage (Code: $returnCode)")
                                retryCount++
                                if (retryCount < maxRetries) {
                                    delay(1000L * retryCount) // Tăng thời gian chờ giữa các lần retry
                                    continue
                                }
                            }
                            throw Exception("Lỗi từ API backend: $returnMessage (Code: $returnCode)")
                        }
                        !jsonResult.has("zp_trans_token") -> {
                            Log.e("ZaloPay API Response", "Response does not contain zp_trans_token")
                            throw Exception("Phản hồi từ backend không chứa token thanh toán ZaloPay")
                        }
                        else -> {
                            val zpTransToken = jsonResult.getString("zp_trans_token")
                            if (zpTransToken.isNullOrEmpty()) {
                                Log.e("ZaloPay API Response", "zp_trans_token is null or empty")
                                throw Exception("Token thanh toán ZaloPay không hợp lệ từ backend")
                            }
                            Log.d("ZaloPay API Response", "Successfully created order with token: $zpTransToken")
                            return@withContext zpTransToken
                        }
                    }
                } catch (e: Exception) {
                    lastException = e
                    Log.e("ZaloPay API Response", "Exception calling backend API: ${e.message}", e)
                    
                    // Nếu là lỗi timeout hoặc network, thử lại
                    if (e is java.io.InterruptedIOException || e.cause is okhttp3.internal.http2.StreamResetException) {
                        retryCount++
                        if (retryCount < maxRetries) {
                            delay(1000L * retryCount)
                            continue
                        }
                    }
                    throw Exception("Lỗi gọi API backend: ${e.message ?: "Lỗi không xác định"}")
                }
            }
            
            // Nếu đã retry hết số lần cho phép
            throw lastException ?: Exception("Không thể tạo đơn hàng ZaloPay sau $maxRetries lần thử")
        }
    }

    fun increaseSoldForOrder(cartItems: List<CartItem>) {
        val db = FirebaseFirestore.getInstance()
        cartItems.forEach { item ->
            val productRef = db.collection("products").document(item.productId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(productRef)
                val currentSold = snapshot.getLong("sold") ?: 0
                transaction.update(productRef, "sold", currentSold + item.quantity)
                Log.d("CheckoutViewModel", "Updated sold for product ${item.productId}: ${currentSold + item.quantity}")
            }
        }
    }

}