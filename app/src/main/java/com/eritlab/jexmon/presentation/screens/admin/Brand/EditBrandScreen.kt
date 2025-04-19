package com.eritlab.jexmon.presentation.screens.admin.Brand

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
fun EditBrandScreen(navController: NavController, brandId: String) {
    var id by remember { mutableStateOf(TextFieldValue("")) }
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }
    var categoryId by remember { mutableStateOf(TextFieldValue("")) }
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

    // Fetch the existing brand data from Firestore on screen load
    LaunchedEffect(brandId) {
        firestore.collection("brands").document(brandId).get()
            .addOnSuccessListener { document ->
                document?.let {
                    name = TextFieldValue(it.getString("name") ?: "")
                    description = TextFieldValue(it.getString("description") ?: "")
                    categoryId = TextFieldValue(it.getString("categoryId") ?: "")
                    imageUrl = it.getString("imageUrl") ?: ""
                }
            }
            .addOnFailureListener {
                Log.e("EditBrandScreen", "Error fetching brand data", it)
            }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text("Chỉnh Sửa Nhãn Hàng", fontSize = 24.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(16.dp))


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

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = categoryId,
            onValueChange = { categoryId = it },
            label = { Text("ID Danh mục") },
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
                    val storageRef = storage.reference.child("brands/${UUID.randomUUID()}")
                    storageRef.putFile(uri)
                        .addOnSuccessListener {
                            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                saveBrandToFirestore(name.text, description.text, categoryId.text, downloadUri.toString(), firestore, navController, brandId)
                            }
                        }
                        .addOnFailureListener {
                            Log.e("EditBrandScreen", "Error uploading image", it)
                        }
                } ?: saveBrandToFirestore(name.text, description.text, categoryId.text, imageUrl, firestore, navController, brandId)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Lưu Thay Đổi", color = Color.White)
        }

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

fun saveBrandToFirestore(name: String, description: String, categoryId: String, imageUrl: String, firestore: FirebaseFirestore, navController: NavController, brandId: String) {
    val brandData = mapOf(
        "name" to name,
        "description" to description,
        "categoryId" to categoryId,
        "imageUrl" to imageUrl
    )

    firestore.collection("brands").document(brandId).update(brandData)
        .addOnSuccessListener {
            Log.d("EditBrandScreen", "Brand updated successfully")
            navController.popBackStack()
        }
        .addOnFailureListener {
            Log.e("EditBrandScreen", "Error updating brand", it)
        }
}

fun deleteBrandFromFirestore(brandId: String, firestore: FirebaseFirestore, navController: NavController) {
    firestore.collection("brands").document(brandId).delete()
        .addOnSuccessListener {
            Log.d("EditBrandScreen", "Brand deleted successfully")
            navController.popBackStack()
        }
        .addOnFailureListener {
            Log.e("EditBrandScreen", "Error deleting brand", it)
        }
}
