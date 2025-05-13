package com.eritlab.jexmon.presentation.screens.checkout_screen

import android.app.Activity
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.eritlab.jexmon.domain.model.CartItem
import com.eritlab.jexmon.presentation.common.component.DefaultBackArrow
import com.eritlab.jexmon.presentation.graphs.detail_graph.DetailScreen
import com.eritlab.jexmon.presentation.ui.theme.TextColor
import vn.zalopay.sdk.ZaloPayError
import vn.zalopay.sdk.ZaloPaySDK
import vn.zalopay.sdk.listeners.PayOrderListener
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CheckoutScreen(
    navController: NavController,
    cartItems: List<CartItem>,
    onBackClick: () -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var zpTransToken by remember { mutableStateOf("") } // Token cho ZaloPay
    LaunchedEffect(Unit) {
        viewModel.fetchCartItems()
        viewModel.fetchDefaultShippingAddress() // <<< GỌI THÊM DÒNG NÀY
        viewModel.fetchSelectedVoucher() // <<< Thêm dòng này


    }

    var selectedPaymentMethod by remember { mutableStateOf("COD") }

    val cartItems by viewModel.cartItems // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< thêm dòng này nè
    val shippingAddress by viewModel.shippingAddress
    val paymentMethod by viewModel.paymentMethod

    val subtotal by viewModel.subtotal
    val shippingFee by viewModel.shippingFee
    val discount by viewModel.discount
    val total by viewModel.total
    val isLoading by viewModel.isLoading
    val zaloPayToken by viewModel.zaloPayToken // Quan sát ZaloPay token từ ViewModel

    val selectedVoucher by viewModel.selectedVoucher.collectAsState()
    val scaffoldState = rememberScaffoldState()
    val aftertotal by viewModel.aftertotal
    val discountproduct by viewModel.discountproduct
    val fianltotal = discountproduct * (1 - discount / 100)*discountproduct
    val voucherDiscount = if (selectedVoucher != null) {
        selectedVoucher!!.discount
    } else {
        0.0
    }
    // *** Effect để kích hoạt thanh toán ZaloPay khi token có giá trị ***
    LaunchedEffect(zaloPayToken) {
        zaloPayToken?.let { token ->
            Log.d ("CheckoutScreen", "ZaloPay token received: $zaloPayToken. Initiating payment...")
            Log.d("CheckoutScreen", "ZaloPay token received: $token. Initiating payment...")
            // Lấy Activity context
            val activity = context as? Activity
            Log.d("CheckoutScreen", "Activity context: $activity")
            val merchantAppDeeplink = "demozpdk://app" // **DEEPLINK THỰC TẾ CỦA BẠN**

            if (activity != null) {
                ZaloPaySDK.getInstance().payOrder(
                    activity, // Truyền Activity
                    token,
                    merchantAppDeeplink,
                    object : PayOrderListener {
                        override fun onPaymentSucceeded(
                            transactionId: String,
                            transToken: String,
                            appTransID: String
                        ) {
                            Log.d("CheckoutScreen", "ZaloPay SUCCESS: $transactionId, $transToken, $appTransID")
                            // Báo cho ViewModel kết quả thành công
                         viewModel.handleZaloPayResult(null, transToken, appTransID)
                            viewModel.increaseSoldForOrder(cartItems)
                            navController.navigate(DetailScreen.PaymentSuccessScreen.route)
                            // ViewModel sẽ xử lý logic sa thành công (xóa cart, update order, navigate)
                        }

                        override fun onPaymentCanceled(
                            zpTransToken: String,
                            appTransID: String
                        ) {
                            Log.d("CheckoutScreen", "ZaloPay CANCELED: $zpTransToken, $appTransID")
                            // Báo cho ViewModel kết quả hủy
                           viewModel.handleZaloPayResult(ZaloPayError.EMPTY_RESULT, zpTransToken, appTransID)
                            // ViewModel sẽ xử lý logic sau hủy (update order status, show message)
                        }

                        override fun onPaymentError(
                            zaloPayError: ZaloPayError,
                            zpTransToken: String,
                            appTransID: String
                        ) {
                            Log.e("CheckoutScreen", "ZaloPay ERROR: $zaloPayError, $zpTransToken, $appTransID")
                            // Báo cho ViewModel kết quả lỗi
//                            viewModel.handleZaloPayResult(zaloPayError, zpTransToken, appTransID)
                            // ViewModel sẽ xử lý logic sau lỗi (update order status, show message)
                        }
                    }
                )
            } else {
                Log.e("CheckoutScreen", "Context is not an Activity, cannot initiate ZaloPay payment.")
                // Xử lý trường hợp không lấy được Activity context nếu cần
//                viewModel.handleZaloPayResult(ZaloPayError.PAYMENT_ERROR, null, null) // Báo lỗi chung
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Phần header
        Row(
            modifier = Modifier
                .padding(top = 15.dp, start = 15.dp, end = 15.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(0.5f)) {
                DefaultBackArrow {
                    onBackClick()
                }
            }
            Box(modifier = Modifier.weight(0.7f)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Thanh Toán",
                        color = MaterialTheme.colors.TextColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Log.d("CheckoutScreen", "Shipping Address: ${shippingAddress?.name}")

        // Phần nội dung
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(725.dp)
                .background(color = Color(0xFFF5F5F5)) // nền xám

                .padding(horizontal = 15.dp) // thêm padding cho đẹp
        ) {
            item {


                ShippingAddressSection(
                    name = shippingAddress?.name ?: "",
                    phone = shippingAddress?.sdt ?: "",
                    address = shippingAddress?.diachi ?: "",
                    onClick = {
                        navController.navigate("shipping_address_screen")
                    }
                )
                Spacer(modifier = Modifier.height(8.dp)) // Thêm khoảng cách giữa các sản phẩm

            }

            items(cartItems.size) { index ->
                val cartItem = cartItems[index]
                ProductItem(
                    storeName = "EritLab Store", // hoặc nếu bạn có storeName thì lấy ra
                    productName = cartItem.name,
                    productImageUrl = cartItem.imageUrl,
                    productPrice = cartItem.price,
                    quantity = cartItem.quantity,
                    color = cartItem.color,
                    size = cartItem.size,
                    discount = cartItem.discount
                )
                Spacer(modifier = Modifier.height(8.dp)) // Thêm khoảng cách giữa các sản phẩm
            }
            // TODO: Thêm các item cart, phương thức thanh toán, tổng tiền, etc ở đây
            item {
                VoucherItem(
                    voucherCode = selectedVoucher?.code ?: "Chưa chọn voucher",
                    voucherDescription = selectedVoucher?.detail ?: "Chưa có mô tả",
                    onClick = {
                        val code = selectedVoucher?.code ?: ""
                        navController.navigate("voucher_screen")
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
            //  Phương thức thanh toán
            item {
                payment(
                    selectedMethod = selectedPaymentMethod,
                    onSelectMethod = { selectedPaymentMethod = it
                        viewModel.setPaymentMethod(it) // Cập nhật phương thức thanh toán trong ViewModel
                    }
                )
            }
            item {
                chiTietThanhToan(
                    total = aftertotal,
                    discount = voucherDiscount,
                    shippingFee = shippingFee,
                )
            }


        }
        // phần footer
            ShopeeFooter(
                onClick = {
                    Log.d("CheckoutScreen", "Selected payment method: $selectedPaymentMethod")
                    Log.d ("CheckoutScreen", "Selected voucher: $paymentMethod ")
                    when (selectedPaymentMethod) {
                        "ZaloPay" -> {
                            viewModel.processPayment(
                                onSuccess = {
                                    // Callback này sẽ được gọi từ ViewModel nếu KHÔNG phải ZaloPay
                                    // hoặc từ ZaloPayListener -> ViewModel -> Composable nếu là ZaloPay THÀNH CÔNG.
                                            viewModel.increaseSoldForOrder(cartItems)

                                    Log.d("CheckoutScreen", "Payment process successful (Callback). Navigating...")
                                    navController.navigate("order_success_screen") // Điều hướng đến màn hình thành công
                                },
                                onError = { errorMessage ->
                                    // Callback này sẽ được gọi từ ViewModel nếu có lỗi (backend, ZaloPay fail/cancel)
                                    Log.e("CheckoutScreen", "Payment process failed (Callback): $errorMessage")
                                    // ViewModel sẽ cập nhật state error, hiển thị Snackbar thông qua Effect bên trên
                                    // hoặc bạn xử lý hiển thị thông báo ở đây nếu không dùng state error chung
                                }
                            )
                        }
                        else -> {
                            // Xử lý các phương thức thanh toán khác
                        }
                    }
                },
                total = discountproduct,
                dis = aftertotal,
                discount = voucherDiscount
            )

    }

}@Composable
fun ShopeeFooter(
    onClick: () -> Unit,
    total: Double,
    dis: Double,
    discount: Double
) {
    val finalAmount = total - (total / 100 * discount)


    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Tổng cộng",
                fontSize = 14.sp,
                color = Color.Black
            )
            Text(
                text = formatCurrency(finalAmount),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )
            Text(
                text = "Tiết kiệm ${formatCurrency(dis * discount / 100)}",
                fontSize = 12.sp,
                color = Color.Red
            )
        }

        Button(
            onClick = onClick, // <-- Sửa ở đây! Gắn lambda nhận được vào Button

            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .height(48.dp)
                .width(120.dp)
        ) {
            Text(
                text = "Đặt hàng",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun chiTietThanhToan(
    total: Double,
    discount: Double,
    shippingFee: Double,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape = RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Chi Tiết Thanh Toán",
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Tổng", fontSize = 14.sp)
            Text(text = formatCurrency(total ), fontSize = 14.sp)
        }

        // Phí vận chuyển
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Phí vận chuyển", fontSize = 14.sp)
            Text(text = formatCurrency(shippingFee), fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))
        // Phí vận chuyển
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = " Giảm Giá  Phí vận chuyển", fontSize = 14.sp)
            Text(text = "-" + formatCurrency(shippingFee), fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tổng cộng
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Tổng cộng Voucher Đã Giảm Giá ", fontSize = 14.sp)
            Text(text =  "-" +  formatCurrency((total / 100 * discount)), fontSize = 14.sp)

        }

        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            color = Color.LightGray,
            thickness = 1.dp
        )

        // Số tiền phải trả
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Số tiền phải trả",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = formatCurrency(total - (total / 100 * discount)),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )
        }
    }
}

// Hàm định dạng tiền tệ
fun formatCurrency(amount: Double): String {
    return "₫" + "%,.0f".format(amount)
}

@Composable
fun payment(
    selectedMethod: String = "COD",
    onSelectMethod: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape = RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Phương Thức Thanh Toán",
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        val methods = listOf(
            Triple("Thanh toán khi nhận hàng", "COD", ""),
            Triple("MoMo", "MoMo", "Giảm ₫10.000"),
            Triple("ZaloPay", "ZaloPay", "Thanh Toán Ngay"),
            Triple("SPayLater - Hạn mức 8.000.000₫", "SPayLater", "Trả góp linh hoạt")
        )

        methods.forEach { (label, key, promo) ->
            val isSelected = selectedMethod == key

            // Animate color for radio circle
            val circleColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFFFF6F00) else Color.Transparent,
                label = "radioColor"
            )

            val borderColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFFFF6F00) else Color.Gray,
                label = "borderColor"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectMethod(key) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = label,
                        fontSize = 16.sp,
                        color = if (isSelected) Color(0xFFFF6F00) else Color.Black,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    if (promo.isNotEmpty()) {
                        Text(
                            text = promo,
                            fontSize = 12.sp,
                            color = Color.Red
                        )
                    }
                }

                // Animated radio circle
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .border(
                            width = 1.5.dp,
                            color = borderColor,
                            shape = CircleShape
                        )
                        .background(
                            color = circleColor,
                            shape = CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}


@Composable
fun VoucherItem(
    voucherCode: String ,
    voucherDescription: String ,
    onClick: () -> Unit = {} // có thể click để chọn/đổi voucher
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape = RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon hoặc ký hiệu voucher
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFE0F7FA), shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎁",
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = voucherCode,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = voucherDescription,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Chọn",
                fontSize = 14.sp,
                color = Color(0xFF008EFF),
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@Composable
fun ProductItem(
    storeName: String,
    productName: String,
    productImageUrl: String,
    productPrice: Double,
    oldPrice: Double? = null, // Có thể null nếu không có giá cũ
    quantity: Int = 1,
    color: String ,
    size: Int,
    shippingMethod: String = "Nhanh",
    originalShippingFee: Double = 30000.0,
    shippingFee: Double = 0.0,
    deliveryDateRange: String = "29 Tháng 4 - 1 Tháng 5",
    voucherCondition: String = "Nhận Voucher trị giá ₫15.000 nếu đơn hàng được giao đến bạn sau ngày 1 Tháng 5 2025." ,
    discount: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape = RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        // Tên cửa hàng
        Text(
            text = storeName,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Thông tin sản phẩm
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ảnh sản phẩm
            androidx.compose.foundation.Image(
                painter = rememberAsyncImagePainter(model = productImageUrl),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Tên + giá
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = productName,
                    color = Color.Black,
                    fontSize = 14.sp,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(

                ) {
                    Text(
                        text = "Màu : $color ",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Size : $size",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatPrice(productPrice * (1 - discount / 100)),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    if (discount != 0.0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatPrice(productPrice),
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 2.dp),
                            fontWeight = FontWeight.Normal,
                            style = MaterialTheme.typography.body2.copy(
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            )
                        )
                    }
                }

            }

            Spacer(modifier = Modifier.width(8.dp))

            // Số lượng
            Text(
                text = "x$quantity",
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.height(36.dp))


        // Phương thức vận chuyển
        Column {
            Row {
                Text(
                    text = "Phương Thức Vận Chuyển",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))

            }

        }
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color(0xFF008EFF), // Xanh đậm
                    shape = RoundedCornerShape(8.dp)
                )
                .background(Color(0xFFE0F7FA), shape = RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = shippingMethod,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (originalShippingFee > 0) {
                    Text(
                        text = formatPrice(originalShippingFee),
                        fontSize = 13.sp,
                        color = Color.Gray,
                        style = MaterialTheme.typography.body2.copy(
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (shippingFee == 0.0) "Miễn phí" else formatPrice(shippingFee),
                    fontSize = 14.sp,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Đảm bảo nhận hàng từ $deliveryDateRange",
                fontSize = 14.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = voucherCondition,
                fontSize = 13.sp,
                color = Color(0xFF008EFF)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(), // cho phép chiếm hết chiều ngang
                horizontalArrangement = Arrangement.SpaceBetween // tự động đẩy 2 Text về hai phía
            ) {
                Text(
                    text = "Tổng số tiền ($quantity sản phẩm)",
                    fontSize = 14.sp,
                    color = Color.Black
                )

                Text(
                    text = formatPrice(productPrice * (1 - discount / 100) * quantity),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

    }
}


@Composable
fun ShippingAddressSection(
    name: String,
    phone: String,
    address: String,
    onClick: () -> Unit // <-- thêm cái onClick callback
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .background(color = Color.White, shape = RoundedCornerShape(8.dp)) // nền trắng bo góc
            .clickable { onClick() } // <-- thêm clickable nè
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location Icon",
                tint = Color.Red,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$name ",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                maxLines = 1
            )
            Text(

                text = "$phone",
                fontSize = 15.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = address,
            fontSize = 15.sp,
            color = Color.DarkGray,
            modifier = Modifier.padding(start = 30.dp)

        )
    }
}



// Đảm bảo hàm formatPrice không thay đổi
private fun formatPrice(price: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    return format.format(price)
}