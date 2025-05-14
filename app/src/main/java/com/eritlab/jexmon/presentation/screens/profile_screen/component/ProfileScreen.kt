package com.eritlab.jexmon.presentation.screens.profile_screen.component

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.eritlab.jexmon.R
import com.eritlab.jexmon.presentation.common.component.DefaultBackArrow
import com.eritlab.jexmon.presentation.graphs.admin_graph.AdminScreen
import com.eritlab.jexmon.presentation.graphs.home_graph.ShopHomeScreen
import com.eritlab.jexmon.presentation.ui.theme.PrimaryColor
import com.eritlab.jexmon.presentation.ui.theme.TextColor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun ProfileScreen(
    navController: NavHostController,
    onBackBtnClick: () -> Unit,
) {
    Log.d("Navigation", "ProfileScreen NavController Hash: ${navController.hashCode()}")

    // Get the current Activity context
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    val isAdmin = remember { mutableStateOf(false) }
    val uid = currentUser?.uid ?: ""
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    val db = FirebaseFirestore.getInstance()

    // ✅ Kiểm tra quyền admin khi màn hình được khởi tạo
    LaunchedEffect(Unit) {
        if (uid.isNotEmpty()) {
            val doc = db.collection("user").document(uid).get().await()
            isAdmin.value = doc.getString("role") == "admin"
            Log.d("UserRole", "User ID: ${isAdmin.value}")
            Log.d("UserRole", "User role: ${doc.getString("role")}")
        }

    }

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            val doc = db.collection("user").document(uid).get().await()
            avatarUrl = doc.getString("avatarUrl")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(15.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
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
                Text(
                    text = "Profile",
                    color = MaterialTheme.colors.TextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Avatar
        ConstraintLayout(
            modifier = Modifier.fillMaxWidth()
        ) {
            val (image) = createRefs()
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .constrainAs(image) {
                        linkTo(start = parent.start, end = parent.end)
                    }
                    .border(
                        width = 4.dp,
                        color = Color(0xFFFF6600),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(avatarUrl ?: R.drawable.user),
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
        LaunchedEffect(Unit) {
            Log.d("Navigation", "🟢 Current Destination: ${navController.currentDestination?.route}")
        }


        if (isAdmin.value) {
            Log.d("Navigation", "🟢 User is Admin")
            ProfileOption(
                icon = R.drawable.user_icon, // Icon cho Admin Panel
                label = "Admin Panel",
                onClick = {
                    Log.d("Navigation", "🟢 Clicked on Admin Panel")
                    Log.d("Navigation", "Navigating to: ${AdminScreen.Dashboard.route}")

                    navController.navigate(AdminScreen.Dashboard.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        } else {
            Log.d("Navigation", "🟢 User is not Admin")
        }



        Spacer(modifier = Modifier.height(15.dp))


        // Các mục khác
        ProfileOption(icon = R.drawable.user_icon, label = "Profile") {
            navController.navigate("profile_detail/$uid")
        }
        Spacer(modifier = Modifier.height(15.dp))
        ProfileOption(icon = R.drawable.bell, label = "Đơn Mua",
            onClick ={
                Log.d("Navigation", "🟢 Clicked on Đơn Mua")
                navController.navigate(ShopHomeScreen.DonMuaScreen.route) {
                }

            }
        )
        Spacer(modifier = Modifier.height(15.dp))
        ProfileOption(icon = R.drawable.settings, label = "Settings") { }
        Spacer(modifier = Modifier.height(15.dp))
        ProfileOption(icon = R.drawable.question_mark, label = "ChatBot") { }
        Spacer(modifier = Modifier.height(15.dp))
        ProfileOption(icon = R.drawable.log_out, label = "Logout") {
            FirebaseAuth.getInstance().signOut()
            // Restart the activity
            val intent = Intent(context, context.javaClass)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
            (context as? Activity)?.finish()
        }
    }
}

// ✅ Tách phần UI của mỗi dòng thành một hàm tái sử dụng
@Composable
fun ProfileOption(icon: Int, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(Color(0x8DB3B0B0), shape = RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.weight(0.05f),
            tint = MaterialTheme.colors.PrimaryColor
        )
        Text(label, modifier = Modifier.weight(0.2f))
        Icon(
            painter = painterResource(id = R.drawable.arrow_right),
            contentDescription = null,
            modifier = Modifier.weight(0.05f),
            tint = MaterialTheme.colors.TextColor
        )
    }
}
