package com.eritlab.jexmon.presentation.screens.admin.user

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

@Composable
fun EditUserScreen(
    navController: NavController,
    userId: String
) {
    var userData by remember { mutableStateOf<UserData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedRole by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri = it }
    }
    
    LaunchedEffect(key1 = userId) {
        // Fetch user data
        FirebaseFirestore.getInstance()
            .collection("user")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    userData = UserData(
                        id = document.id,
                        email = document.getString("gmail") ?: "",
                        name = document.getString("name") ?: "",
                        role = document.getString("role") ?: "user",
                        avatarUrl = document.getString("avatarUrl") ?: ""
                    )
                    selectedRole = userData?.role ?: "user"
                    isLoading = false
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Custom TopBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.navigateUp() }
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFFB71C1C)
                )
            }
            Text(
                text = "Edit User",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB71C1C)
            )
            // Empty box for alignment
            Box(modifier = Modifier.size(48.dp))
        }

        if (isLoading || isUploading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFB71C1C))
            }
        } else {
            userData?.let { user ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0xFFB71C1C), CircleShape)
                            .clickable { launcher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(selectedImageUri ?: user.avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "User avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change avatar",
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp),
                            tint = Color(0xFFB71C1C)
                        )
                    }
                    
                    Text(
                        text = user.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = user.email,
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                    
                    // Role selection
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Select Role:",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedRole == "user",
                                    onClick = { selectedRole = "user" },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFFB71C1C)
                                    )
                                )
                                Text("User")
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedRole == "admin",
                                    onClick = { selectedRole = "admin" },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFFB71C1C)
                                    )
                                )
                                Text("Admin")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Button(
                        onClick = {
                            isUploading = true
                            
                            // If there's a new image selected, upload it first
                            if (selectedImageUri != null) {
                                val storageRef = FirebaseStorage.getInstance().reference
                                val imageRef = storageRef.child("avatars/${UUID.randomUUID()}")
                                
                                imageRef.putFile(selectedImageUri!!)
                                    .addOnSuccessListener {
                                        imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                                            // Update user data with new avatar URL
                                            FirebaseFirestore.getInstance()
                                                .collection("user")
                                                .document(userId)
                                                .update(
                                                    mapOf(
                                                        "role" to selectedRole,
                                                        "avatarUrl" to downloadUrl.toString()
                                                    )
                                                )
                                                .addOnSuccessListener {
                                                    isUploading = false
                                                    navController.navigateUp()
                                                }
                                        }
                                    }
                            } else {
                                // Just update the role
                                FirebaseFirestore.getInstance()
                                    .collection("user")
                                    .document(userId)
                                    .update("role", selectedRole)
                                    .addOnSuccessListener {
                                        isUploading = false
                                        navController.navigateUp()
                                    }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFB71C1C),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Update User",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
} 