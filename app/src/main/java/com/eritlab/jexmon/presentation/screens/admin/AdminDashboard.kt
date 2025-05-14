package com.eritlab.jexmon.presentation.screens.admin

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.eritlab.jexmon.presentation.graphs.Graph
import com.eritlab.jexmon.presentation.graphs.admin_graph.AdminScreen
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AdminDashboard(navController: NavHostController) {
    var totalProducts by remember { mutableStateOf(0) }
    var totalOrders by remember { mutableStateOf(0) }
    var totalRevenue by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        
        // Fetch total products
        db.collection("products")
            .get()
            .addOnSuccessListener { products ->
                totalProducts = products.size()
            }
            
        // Fetch orders and calculate total
        db.collection("orders")
            .get()
            .addOnSuccessListener { orders ->
                totalOrders = orders.size()
                var revenue = 0.0
                orders.forEach { order ->
                    revenue += order.getDouble("total") ?: 0.0
                }
                totalRevenue = revenue
                isLoading = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF5F5))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Shoe Store Admin",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFB71C1C)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .background(Color(0xFFF5F5F5), shape = RoundedCornerShape(1.dp))
                .border(1.dp, Color.LightGray, shape = RoundedCornerShape(25.dp))
                .padding(13.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.Red,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.ThumbUp,
                        contentDescription = "Total Products",
                        tint = Color.Red,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "Total Products",
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                )
                Text(
                    text = if (isLoading) "..." else totalProducts.toString(),
                    style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color(0xFF4CAF50),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = "Total Orders",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "Total Orders",
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                )
                Text(
                    text = if (isLoading) "..." else totalOrders.toString(),
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFA500))
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Face,
                        contentDescription = "Total Revenue",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "Total\nRevenue",
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Red),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isLoading) "..." else formatPrice(totalRevenue),
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Row {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .weight(1f)
            ) {
                GridButton(label = "Manage Products") {
                    navController.navigate("manageProducts")
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .weight(1f)
            ) {
                GridButton(label = "Manage Orders") {
                    navController.navigate(AdminScreen.OrderManagementScreen.route)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .weight(1f)
            ) {
                GridButton(label = "Manage Brands") {
                    Log.d("AdminDashboard", "Navigating to: ${AdminScreen.BrandScreen.route}")
                    Log.d("AdminDashboard", "Current Destination: ${navController.currentDestination?.route}")

                    // Điều hướng chính xác qua admin_graph
                    navController.navigate(Graph.ADMIN) {
                        launchSingleTop = true
                    }

                    // Sau khi vào admin_graph, tiếp tục vào BrandScreen
                    navController.navigate(AdminScreen.BrandScreen.route) {
                        launchSingleTop = true
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .weight(1f)
            ) {
                GridButton(label = "Manage Categories") {
                    // Điều hướng chính xác qua admin_graph
                    navController.navigate(Graph.ADMIN) {
                        launchSingleTop = true
                    }

                    // Sau khi vào admin_graph, tiếp tục vào BrandScreen
                    navController.navigate(AdminScreen.CategoryScreen.route) {
                        launchSingleTop = true
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .weight(1f)
            ) {
                GridButton(label = "Manage Reviews") {
                    navController.navigate("manageReviews")
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .weight(1f)
            ) {
                GridButton(label = "Manage Users") {
                    navController.navigate(AdminScreen.UserManagementScreen.route)
                }
            }
        }
    }
}

@Composable
fun GridButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(56.dp)
            .shadow(4.dp, RoundedCornerShape(8.dp)),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color(0xFFB71C1C),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
// Đảm bảo hàm formatPrice không thay đổi
private fun formatPrice(price: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    return format.format(price)
}