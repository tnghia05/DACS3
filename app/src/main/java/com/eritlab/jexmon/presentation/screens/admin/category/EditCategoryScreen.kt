package com.eritlab.jexmon.presentation.screens.admin.category

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

@Composable
fun EditCategoryScreen(navController: NavController, categoryId: String) {
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }
    var imageUrl by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
        }
    }

    // Fetch the existing category data from Firestore on screen load
    LaunchedEffect(categoryId) {
        firestore.collection("categories").document(categoryId).get()
            .addOnSuccessListener { document ->
                document?.let {
                    name = TextFieldValue(it.getString("name") ?: "")
                    description = TextFieldValue(it.getString("description") ?: "")
                    imageUrl = it.getString("imageUrl") ?: ""
                }
            }
            .addOnFailureListener {
                Log.e("EditCategoryScreen", "Error fetching category data", it)
            }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text("Chỉnh Sửa Danh Mục", fontSize = 24.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Tên danh mục") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Mô tả") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { imagePickerLauncher.launch("image/*") }) {
            Text("Chọn ảnh")
        }

        imageUri?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Image(
                painter = rememberImagePainter(it),
                contentDescription = "Selected Image",
                modifier = Modifier.size(100.dp)
            )
        } ?: imageUrl.takeIf { it.isNotEmpty() }?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Image(
                painter = rememberImagePainter(it),
                contentDescription = "Existing Image",
                modifier = Modifier.size(100.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                imageUri?.let { uri ->
                    val storageRef = storage.reference.child("categories/${UUID.randomUUID()}")
                    storageRef.putFile(uri)
                        .addOnSuccessListener {
                            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                saveCategoryToFirestore(name.text, description.text, downloadUri.toString(), firestore, navController, categoryId)
                            }
                        }
                        .addOnFailureListener {
                            Log.e("EditCategoryScreen", "Error uploading image", it)
                        }
                } ?: saveCategoryToFirestore(name.text, description.text, imageUrl, firestore, navController, categoryId)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Lưu Thay Đổi", color = Color.White)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { deleteCategoryFromFirestore(categoryId, firestore, navController) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
        ) {
            Text("Xóa Danh Mục", color = Color.White)
        }
    }
}

fun saveCategoryToFirestore(name: String, description: String, imageUrl: String, firestore: FirebaseFirestore, navController: NavController, categoryId: String) {
    val categoryData = mapOf(
        "name" to name,
        "description" to description,
        "imageUrl" to imageUrl
    )

    firestore.collection("categories").document(categoryId).update(categoryData)
        .addOnSuccessListener {
            Log.d("EditCategoryScreen", "Category updated successfully")
            navController.popBackStack()
        }
        .addOnFailureListener {
            Log.e("EditCategoryScreen", "Error updating category", it)
        }
}
