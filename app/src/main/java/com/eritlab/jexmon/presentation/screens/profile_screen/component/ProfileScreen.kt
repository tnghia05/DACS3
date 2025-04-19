package com.eritlab.jexmon.presentation.screens.profile_screen.component

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.HorizontalAnchorable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.eritlab.jexmon.presentation.common.component.DefaultBackArrow
import com.eritlab.jexmon.presentation.ui.theme.TextColor
import com.eritlab.jexmon.R
import com.eritlab.jexmon.presentation.graphs.Graph
import com.eritlab.jexmon.presentation.graphs.admin_graph.AdminScreen
import com.eritlab.jexmon.presentation.ui.theme.PrimaryColor
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ProfileScreen(
    navController: NavHostController,
    onBackBtnClick: () -> Unit,

) {
    Log.d("Navigation", "ProfileScreen NavController Hash: ${navController.hashCode()}")

    val currentUser = FirebaseAuth.getInstance().currentUser
    val isAdmin = remember { mutableStateOf(false) }

    // ✅ Kiểm tra quyền admin khi màn hình được khởi tạo
    LaunchedEffect(Unit) {
        currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            isAdmin.value = result.claims["admin"] as? Boolean ?: false
        }
        Log.d("ProfileScreen", "isAdmin: ${isAdmin.value}")
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
            val (image, cameraIcon) = createRefs()
            Image(
                painter = painterResource(id = R.drawable.profile_image),
                contentDescription = "Profile Image",
                modifier = Modifier
                    .clip(CircleShape)
                    .constrainAs(image) {
                        linkTo(start = parent.start, end = parent.end)
                    }
            )
            IconButton(
                onClick = { /* Thay đổi ảnh đại diện */ },
                modifier = Modifier.constrainAs(cameraIcon) {
                    bottom.linkTo(image.bottom)
                    end.linkTo(image.end)
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.camera_icon),
                    contentDescription = "Change Picture",
                    tint = MaterialTheme.colors.PrimaryColor
                )
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
        LaunchedEffect(Unit) {
            Log.d("Navigation", "🟢 Current Destination: ${navController.currentDestination?.route}")
        }

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


        Spacer(modifier = Modifier.height(15.dp))


        // Các mục khác
        ProfileOption(icon = R.drawable.user_icon, label = "Profile Picture") { }
        Spacer(modifier = Modifier.height(15.dp))
        ProfileOption(icon = R.drawable.bell, label = "Notification") { }
        Spacer(modifier = Modifier.height(15.dp))
        ProfileOption(icon = R.drawable.settings, label = "Settings") { }
        Spacer(modifier = Modifier.height(15.dp))
        ProfileOption(icon = R.drawable.question_mark, label = "Help Center") { }
        Spacer(modifier = Modifier.height(15.dp))
        ProfileOption(icon = R.drawable.log_out, label = "Logout") { }
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
