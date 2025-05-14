package com.eritlab.jexmon.presentation.screens.don_da_mua

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.eritlab.jexmon.domain.model.OrderModel
import com.eritlab.jexmon.presentation.common.component.DefaultBackArrow
import com.eritlab.jexmon.presentation.ui.theme.TextColor
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DonMua(
    navController: NavController,
    viewModel: OrderViewModel = hiltViewModel(),
    onBackBtnClick: () -> Unit,
//
    ) {
    val orders = viewModel.filteredOrders.value
    val isLoading = viewModel.isLoading.value
    val error = viewModel.error.value

    var selectedStatus by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar

        Row(
            modifier = Modifier
                .padding(top = 15.dp, start = 15.dp, end = 15.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(0.5f)) {
                DefaultBackArrow {
                    onBackBtnClick()

                }
            }
            Box(modifier = Modifier.weight(0.7f)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Đơn hàng",
                        color = MaterialTheme.colors.TextColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )


                }
            }

        }

        // Status Filter
        val tabs = listOf("Tất cả", "Chờ xác nhận", "Đang giao", "Đã giao", "Đã hủy")
        var selectedTabIndex by remember { mutableStateOf(0) }
        Log.d("Tab", "Selected Tab Index: $selectedTabIndex")

        ScrollableTabRow(

            selectedTabIndex = selectedTabIndex,
            backgroundColor = Color.White,
            contentColor = Color(0xFFFF5722),
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = Color(0xFFFF5722)
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = {
                        selectedTabIndex = index
                        selectedStatus = if (index == 0) null else title
                        viewModel.filterOrdersByStatus(selectedStatus)
                    },
                    text = {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            color = if (selectedTabIndex == index) Color(0xFFFF5722) else Color.Gray
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error, color = Color.Red)
                }
            }
            orders.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không có đơn hàng nào")
                }
            }
            else -> {
                LazyColumn(

                    modifier = Modifier.fillMaxSize()
                        .height(630.dp)
                    ,
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(orders) { order ->
                        OrderItem(order = order, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
        contentColor = if (selected) Color.White else MaterialTheme.colors.onSurface
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            label()
        }
    }
}

@Composable
fun OrderItem(
    order: OrderModel,
    viewModel: OrderViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Order ID and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Đơn hàng: ${if (order.id.length > 6) order.id.take(6) + "..." else order.id}",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = order.status ?: "",
                    color = when(order.status) {
                        "Đã giao" -> Color.Green
                        "Đã hủy" -> Color.Red
                        else -> Color.Blue
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Order Items
            order.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Product Image
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )

                    // Product Details
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.name, fontWeight = FontWeight.Medium)
                        Text(
                            text = "Size: ${item.size}, Màu: ${item.color}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "${item.quantity}x ${item.price}đ",
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Total Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tổng tiền:", fontWeight = FontWeight.Medium)
                Text(
                    text = "${formatPrice(order.total)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
            }

            // Buttons Row
            if (order.status != "Đã giao" && order.status != "Đã hủy") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cancel Button - Don't show for "Dang giao" status
                    if (order.status != "Đang giao") {
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.cancelOrder(
                                        orderId = order.id,
                                        onSuccess = {
                                            scope.launch {
                                                scaffoldState.snackbarHostState.showSnackbar("Đã hủy đơn hàng thành công")
                                            }
                                        },
                                        onError = { error ->
                                            scope.launch {
                                                scaffoldState.snackbarHostState.showSnackbar(error)
                                            }
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color.Red,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Hủy đơn hàng")
                        }
                    }

                    // Received Button - Only show for "Dang giao" status
                    if (order.status == "Đang giao") {
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.markOrderAsReceived(
                                        orderId = order.id,
                                        onSuccess = {
                                            scope.launch {
                                                scaffoldState.snackbarHostState.showSnackbar("Đã xác nhận nhận hàng thành công")
                                            }
                                        },
                                        onError = { error ->
                                            scope.launch {
                                                scaffoldState.snackbarHostState.showSnackbar(error)
                                            }
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF4CAF50),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Đã nhận")
                        }
                    }
                }
            }
        }
    }

    SnackbarHost(
        hostState = scaffoldState.snackbarHostState,
        modifier = Modifier.padding(16.dp)
    )
}

// Đảm bảo hàm formatPrice không thay đổi
private fun formatPrice(price: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    return format.format(price)
}