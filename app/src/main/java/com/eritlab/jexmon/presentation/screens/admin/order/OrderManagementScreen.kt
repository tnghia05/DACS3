package com.eritlab.jexmon.presentation.screens.admin.order

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.eritlab.jexmon.domain.model.OrderModel
import com.eritlab.jexmon.presentation.common.component.DefaultBackArrow
import com.eritlab.jexmon.presentation.graphs.admin_graph.AdminScreen
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

@Composable
fun OrderManagementScreen(
    navController: NavController,
    onBackBtnClick: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val orderList = remember { mutableStateListOf<OrderModel>() }

    LaunchedEffect(Unit) {
        firestore.collection("orders").addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                val orders = it.documents.mapNotNull { doc ->
                    doc.toObject(OrderModel::class.java)?.copy(id = doc.id)
                }
                orderList.clear()
                orderList.addAll(orders)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DefaultBackArrow(onClick = onBackBtnClick)
            Text(
                text = "Quản lý đơn hàng",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color.Black
            )
        }
    }

    Column(
        modifier = Modifier
            .padding(top = 80.dp, start = 12.dp, end = 12.dp)
            .size(600.dp)
            .verticalScroll(rememberScrollState())
    ) {
        orderList.forEach { order ->
            OrderItem(
                order = order,
                onStatusUpdate = { newStatus ->
                    updateOrderStatus(order.id, newStatus, firestore)
                },
                onPaymentStatusUpdate = { newPaymentStatus ->
                    updatePaymentStatus(order.id, newPaymentStatus, firestore)
                },
                onDelete = {
                    deleteOrderFromFirestore(order.id, firestore)
                },
                onItemClick = {
                    navController.navigate(AdminScreen.OrderDetailScreen.passOrderId(order.id))
                }
            )
        }
    }
}

@Composable
fun OrderItem(
    order: OrderModel,
    onStatusUpdate: (String) -> Unit,
    onPaymentStatusUpdate: (String) -> Unit,
    onDelete: () -> Unit,
    onItemClick: () -> Unit
) {
    var showStatusDialog = remember { mutableStateOf(false) }
    var showPaymentStatusDialog = remember { mutableStateOf(false) }
    var showDeleteConfirmation = remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onItemClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Đơn hàng: ${order.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Trạng thái: ${order.status}",
                        fontSize = 14.sp,
                        color = when(order.status) {
                            "Đã giao" -> Color.Green
                            "Đã hủy" -> Color.Red
                            else -> Color.Blue
                        }
                    )
                    Text(
                        text = "Thanh toán: ${order.paymentStatus}",
                        fontSize = 14.sp,
                        color = when(order.paymentStatus) {
                            "PAID" -> Color.Green
                            "CANCELED" -> Color.Red
                            else -> Color.Blue
                        }
                    )
                    Text(
                        text = "Tổng tiền: ${NumberFormat.getNumberInstance(Locale("vi", "VN")).format(order.total)}đ",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6200EE)
                    )
                }
                
                Row {
                    IconButton(onClick = { showStatusDialog.value = true }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Cập nhật trạng thái")
                    }
                    IconButton(onClick = { showDeleteConfirmation.value = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Xóa đơn hàng",
                            tint = Color.Red
                        )
                    }
                }
            }
        }
    }

    if (showStatusDialog.value) {
        androidx.compose.material.AlertDialog(
            onDismissRequest = { showStatusDialog.value = false },
            title = { Text("Cập nhật trạng thái") },
            text = {
                Column {
                    listOf("Chờ xác nhận", "Đã xác nhận", "Đang giao", "Đã giao", "Đã hủy").forEach { status ->
                        androidx.compose.material.TextButton(
                            onClick = {
                                onStatusUpdate(status)
                                showStatusDialog.value = false
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
                androidx.compose.material.TextButton(onClick = { showStatusDialog.value = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showDeleteConfirmation.value) {
        androidx.compose.material.AlertDialog(
            onDismissRequest = { showDeleteConfirmation.value = false },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc chắn muốn xóa đơn hàng này không?") },
            confirmButton = {
                androidx.compose.material.TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation.value = false
                    }
                ) {
                    Text("Xóa", color = Color.Red)
                }
            },
            dismissButton = {
                androidx.compose.material.TextButton(onClick = { showDeleteConfirmation.value = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

fun updateOrderStatus(orderId: String, newStatus: String, firestore: FirebaseFirestore) {
    firestore.collection("orders").document(orderId)
        .update("status", newStatus)
}

fun updatePaymentStatus(orderId: String, newStatus: String, firestore: FirebaseFirestore) {
    firestore.collection("orders").document(orderId)
        .update("paymentStatus", newStatus)
}

fun deleteOrderFromFirestore(orderId: String, firestore: FirebaseFirestore) {
    firestore.collection("orders").document(orderId).delete()
} 