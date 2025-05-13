package com.eritlab.jexmon.presentation.screens.admin

import android.util.Log
import androidx.collection.forEach
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.eritlab.jexmon.presentation.graphs.Graph
import com.eritlab.jexmon.presentation.graphs.admin_graph.AdminScreen

@Composable
fun AdminDashboard(navController: NavHostController) {
    LaunchedEffect(Unit) {
        Log.d("Navigation", "Current Graph: ${navController.graph}")
        Log.d("Navigation", "Current Destination: ${navController.currentDestination?.route}")
        Log.d("Navigation", "All destinations:")
        navController.graph.forEach { node ->
            Log.d("Destination", "Route: ${node.route}")
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
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Pending Order",
                    tint = Color.Red,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "Pending Order",
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                )
                Text(
                    text = "301",
                    style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Completed Order",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "Completed order",
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                )
                Text(
                    text = "10",
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFA500))
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.ThumbUp,
                    contentDescription = "Whole Time Earning",
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "Whole Time\nEarning",
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Red),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "100$",
                    style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
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
                // Empty column for symmetry
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
