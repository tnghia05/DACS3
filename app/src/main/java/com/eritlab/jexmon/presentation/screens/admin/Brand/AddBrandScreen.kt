package com.eritlab.jexmon.presentation.screens.admin

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
fun AddBrandScreen(navController: NavController, brandId: String? = null, existingName: String = "", existingDescription: String = "", existingImageUrl: String = "") {
    var name by remember { mutableStateOf(TextFieldValue(existingName)) }
    var id by remember { mutableStateOf(TextFieldValue(existingName)) }
    var description by remember { mutableStateOf(TextFieldValue(existingDescription)) }
    var categoryId by remember { mutableStateOf(TextFieldValue("")) }
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
        Text(if (brandId == null) "Thêm Nhãn Hàng" else "Chỉnh Sửa Nhãn Hàng", fontSize = 24.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(16.dp))


        OutlinedTextField(
            value = id,
            onValueChange = { id = it },
            label = { Text("ID nhãn hàng") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = categoryId,
            onValueChange = { categoryId = it },
            label = { Text("ID danh mục") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Tên nhãn hàng") },
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
                    val storageRef = storage.reference.child("brands/${UUID.randomUUID()}")
                    storageRef.putFile(uri)
                        .addOnSuccessListener {
                            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                saveBrandToFirestore(id.text, name.text, description.text, categoryId.text, downloadUri.toString(), firestore, navController, brandId)
                            }
                        }
                        .addOnFailureListener {
                            Log.e("AddBrandScreen", "Error uploading image", it)
                        }
                } ?: saveBrandToFirestore(id.text, name.text, description.text, categoryId.text, imageUrl, firestore, navController, brandId)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (brandId == null) "Thêm Nhãn Hàng" else "Lưu Thay Đổi", color = Color.White)
        }

        if (brandId != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { deleteBrandFromFirestore(brandId, firestore, navController) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
            ) {
                Text("Xóa Nhãn Hàng", color = Color.White)
            }
        }
    }
}

fun saveBrandToFirestore(id: String, name: String, description: String, categoryId: String, imageUrl: String, firestore: FirebaseFirestore, navController: NavController, brandId: String?) {
    val brandData = mapOf(
        "id" to id,
        "name" to name,
        "description" to description,
        "categoryId" to categoryId,
        "imageUrl" to imageUrl
    )
    val formattedId = id.trim().replace(" ", "_") // Chuẩn hóa ID từ tên nhãn hàng

    if (brandId == null) {
        firestore.collection("brands").document(formattedId).set(brandData)
            .addOnSuccessListener {
                Log.d("AddBrandScreen", "Brand added successfully")
                navController.popBackStack()
            }
            .addOnFailureListener {
                Log.e("AddBrandScreen", "Error adding brand", it)
            }
    } else {
        firestore.collection("brands").document(brandId).update(brandData)
            .addOnSuccessListener {
                Log.d("AddBrandScreen", "Brand updated successfully")
                navController.popBackStack()
            }
            .addOnFailureListener {
                Log.e("AddBrandScreen", "Error updating brand", it)
            }
    }
}

fun deleteBrandFromFirestore(brandId: String, firestore: FirebaseFirestore, navController: NavController) {
    firestore.collection("brands").document(brandId).delete()
        .addOnSuccessListener {
            Log.d("AddBrandScreen", "Brand deleted successfully")
            navController.popBackStack()
        }
        .addOnFailureListener {
            Log.e("AddBrandScreen", "Error deleting brand", it)
        }
}
