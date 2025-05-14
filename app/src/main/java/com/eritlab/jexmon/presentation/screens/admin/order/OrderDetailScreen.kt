package com.eritlab.jexmon.presentation.screens.admin.order

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.eritlab.jexmon.domain.model.OrderModel
import com.eritlab.jexmon.presentation.common.component.DefaultBackArrow
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun OrderDetailScreen(
    navController: NavController,
    orderId: String,
    onBackClick: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    var order by remember { mutableStateOf<OrderModel?>(null) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(orderId) {
        firestore.collection("orders").document(orderId)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.toObject(OrderModel::class.java)?.let {
                    order = it.copy(id = snapshot.id)
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        // Custom TopBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colors.background)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DefaultBackArrow(onClick = onBackClick)
            Text(
                text = "Chi tiết đơn hàng",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        // Content
        order?.let { orderData ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
            ) {
                // Thông tin khách hàng
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Log.d("OrderDetailScreen", "Shipping Address: ${orderData.items}")
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Thông tin khách hàng",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6200EE)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Họ tên: ${orderData.shippingAddress.name}")
                            Log.d("OrderDetailScreen", "Shipping Address: ${orderData.shippingAddress}")
                            Text("Số điện thoại: ${orderData.shippingAddress.sdt}")
                            Text("Địa chỉ: ${orderData.shippingAddress.diachi}")
                        }
                    }
                }

                // Trạng thái đơn hàng
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Trạng thái đơn hàng",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = orderData.status,
                                        color = when(orderData.status) {
                                            "Đã giao" -> Color.Green
                                            "Đã hủy" -> Color.Red
                                            else -> Color.Blue
                                        }
                                    )
                                }
                                IconButton(onClick = { showStatusDialog = true }) {
                                    Icon(Icons.Default.Edit, "Cập nhật trạng thái")
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Trạng thái thanh toán",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = orderData.paymentStatus,
                                        color = when(orderData.paymentStatus) {
                                            "PAID" -> Color.Green
                                            "CANCELED" -> Color.Red
                                            else -> Color.Blue
                                        }
                                    )
                                }
                                IconButton(onClick = { showPaymentDialog = true }) {
                                    Icon(Icons.Default.Edit, "Cập nhật thanh toán")
                                }
                            }
                        }
                    }
                }

                // Danh sách sản phẩm
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Danh sách sản phẩm",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6200EE)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            orderData.items.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = item.name,
                                        modifier = Modifier
                                            .size(60.dp)
                                            .background(Color.LightGray, RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = item.name, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = "Size: ${item.size}, Màu: ${item.color}",
                                            color = Color.Gray,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${item.quantity}x ${NumberFormat.getNumberInstance(Locale("vi", "VN")).format(item.price)}đ",
                                            color = Color(0xFF6200EE),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                if (index < orderData.items.size - 1) {
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                }
                            }
                        }
                    }
                }

                // Thông tin thanh toán
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Thông tin thanh toán",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6200EE)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tạm tính:")
                                Text(NumberFormat.getNumberInstance(Locale("vi", "VN")).format(orderData.subtotal) + "đ")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Phí vận chuyển:")
                                Text(NumberFormat.getNumberInstance(Locale("vi", "VN")).format(orderData.shippingFee) + "đ")
                            }

                            if (orderData.discount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Giảm giá:")
                                    Text("-" + NumberFormat.getNumberInstance(Locale("vi", "VN")).format(orderData.subtotal - (1 -(orderData.discount/100)) * orderData.subtotal) +  "đ")
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tổng cộng:", fontWeight = FontWeight.Bold)
                                Text(
                                    NumberFormat.getNumberInstance(Locale("vi", "VN")).format(orderData.total) + "đ",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6200EE)
                                )
                            }

                            Text(
                                text = "Thời gian đặt hàng: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi")).format(orderData.createdAt.toDate())}",
                                modifier = Modifier.padding(top = 8.dp),
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }

    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("Cập nhật trạng thái") },
            text = {
                Column {
                    listOf("Chờ xác nhận", "Đã xác nhận", "Đang giao", "Đã giao", "Đã hủy").forEach { status ->
                        TextButton(
                            onClick = {
                                firestore.collection("orders").document(orderId)
                                    .update("status", status)
                                showStatusDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(status)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStatusDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Cập nhật trạng thái thanh toán") },
            text = {
                Column {
                    listOf("Chưa thanh toán", "Đã thanh toán", "Đã hủy").forEach { status ->
                        TextButton(
                            onClick = {
                                firestore.collection("orders").document(orderId)
                                    .update("paymentStatus", status)
                                showPaymentDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(status)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
} 