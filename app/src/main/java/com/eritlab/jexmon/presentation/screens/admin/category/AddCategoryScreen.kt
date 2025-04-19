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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

@Composable
fun AddCategoryScreen(navController: NavController, categoryId: String? = null, existingName: String = "", existingDescription: String = "", existingImageUrl: String = "") {
    var name by remember { mutableStateOf(TextFieldValue(existingName)) }
    var id by remember { mutableStateOf(TextFieldValue(existingName)) }
    var description by remember { mutableStateOf(TextFieldValue(existingDescription)) }
    var imageUrl by remember { mutableStateOf(existingImageUrl) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(if (categoryId == null) "Thêm Danh Mục" else "Chỉnh Sửa Danh Mục", fontSize = 24.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = id,
            onValueChange = { id = it },
            label = { Text("ID danh mục") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                imageUri?.let { uri ->
                    val storageRef = storage.reference.child("categories/${UUID.randomUUID()}")
                    storageRef.putFile(uri)
                        .addOnSuccessListener {
                            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                saveCategoryToFirestore(id.text,name.text, description.text, downloadUri.toString(), firestore, navController, categoryId)
                            }
                        }
                        .addOnFailureListener {
                            Log.e("AddCategoryScreen", "Error uploading image", it)
                        }
                } ?: saveCategoryToFirestore(id.text,name.text, description.text, imageUrl, firestore, navController, categoryId)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (categoryId == null) "Thêm Danh Mục" else "Lưu Thay Đổi", color = Color.White)
        }

        if (categoryId != null) {
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
}

fun saveCategoryToFirestore(id : String, name: String, description: String, imageUrl: String, firestore: FirebaseFirestore, navController: NavController, categoryId: String?) {
    val categoryData = mapOf(
        "id" to id,
        "name" to name,
        "description" to description,
        "imageUrl" to imageUrl
    )

    if (categoryId == null) {
        val formattedId = id.trim().replace(" ", "_") // Chuẩn hóa ID từ tên nhãn hàng

        firestore.collection("categories").document(formattedId).set(categoryData)
            .addOnSuccessListener {
                Log.d("AddCategoryScreen", "Category added successfully")
                navController.popBackStack()
            }
            .addOnFailureListener {
                Log.e("AddCategoryScreen", "Error adding category", it)
            }
    } else {
        firestore.collection("categories").document(categoryId).update(categoryData)
            .addOnSuccessListener {
                Log.d("AddCategoryScreen", "Category updated successfully")
                navController.popBackStack()
            }
            .addOnFailureListener {
                Log.e("AddCategoryScreen", "Error updating category", it)
            }
    }
}

fun deleteCategoryFromFirestore(categoryId: String, firestore: FirebaseFirestore, navController: NavController) {
    firestore.collection("categories").document(categoryId).delete()
        .addOnSuccessListener {
            Log.d("AddCategoryScreen", "Category deleted successfully")
            navController.popBackStack()
        }
        .addOnFailureListener {
            Log.e("AddCategoryScreen", "Error deleting category", it)
        }
}
