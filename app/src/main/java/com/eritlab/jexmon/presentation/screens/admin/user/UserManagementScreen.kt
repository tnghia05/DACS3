package com.eritlab.jexmon.presentation.screens.admin.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.eritlab.jexmon.presentation.graphs.admin_graph.AdminScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun UserManagementScreen(
    navController: NavController
) {
    var users by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(key1 = true) {
        // Fetch users from Firestore
        FirebaseFirestore.getInstance()
            .collection("user")
            .get()
            .addOnSuccessListener { documents ->
                users = documents.map { doc ->
                    UserData(
                        id = doc.id,
                        email = doc.getString("gmail") ?: "",
                        name = doc.getString("name") ?: "",
                        role = doc.getString("role") ?: "user",
                        avatarUrl = doc.getString("avatarUrl") ?: ""
                    )
                }
                isLoading = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Custom TopBar
        Text(
            text = "User Management",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFB71C1C),
            modifier = Modifier.padding(vertical = 16.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .height(600.dp),
                contentAlignment = Alignment.Center

            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
                    .height(600.dp),

                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(users) { user ->
                    UserItem(
                        user = user,
                        onEditClick = {
                            navController.navigate(AdminScreen.EditUserScreen.passUserId(user.id))
                        },
                        onDeleteClick = {
                            // Delete user from Authentication
                            FirebaseAuth.getInstance().currentUser?.let { currentUser ->
                                if (currentUser.email != user.email) {
                                    // Delete from Firestore
                                    FirebaseFirestore.getInstance()
                                        .collection("user")
                                        .document(user.id)
                                        .delete()
                                        .addOnSuccessListener {
                                            // Remove from local list
                                            users = users.filter { it.id != user.id }
                                        }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun UserItem(
    user: UserData,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Avatar
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "User avatar",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                
                // User info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = user.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = user.email,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                    Text(
                        text = "Role: ${user.role}",
                        fontSize = 12.sp,
                        color = if (user.role == "admin") Color(0xFFB71C1C) else Color.Gray,
                        maxLines = 1
                    )
                }
            }
            
            // Action buttons
            Row(
                modifier = Modifier.padding(start = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit user",
                        tint = Color(0xFFB71C1C),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete user",
                        tint = Color(0xFFB71C1C),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

data class UserData(
    val id: String,
    val email: String,
    val name: String,
    val role: String,
    val avatarUrl: String = ""
) 