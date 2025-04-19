package com.eritlab.jexmon.presentation.screens.admin

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.eritlab.jexmon.R
import com.eritlab.jexmon.domain.model.BrandModel
import com.eritlab.jexmon.presentation.graphs.admin_graph.AdminScreen
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun BrandScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val brandList = remember { mutableStateListOf<BrandModel>() }

    LaunchedEffect(Unit) {
        firestore.collection("brands").addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                val brands = it.documents.mapNotNull { doc ->
                    doc.toObject(BrandModel::class.java)?.copy(id = doc.id)
                }
                brandList.clear()
                brandList.addAll(brands)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Tiêu đề
        Text(
            text = "Danh sách Thương hiệu",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(top = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            color = Color.Black
        )

        // Nút thêm
        Button(
            onClick = {
                // Handle click event to add a brand or navigate to add screen
                Log.d("BrandScreen", "Add new brand")
                // Ví dụ: navController.navigate("add_brand_screen")
                navController.navigate(AdminScreen.AddBrandScreen.route)

                },
            modifier = Modifier
                .padding(bottom = 16.dp)
                .align(Alignment.End)
            ,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE)) // Purple color
        ) {
            Text(text = "Thêm Thương hiệu", color = Color.White)
        }
    }

    Column(
        modifier = Modifier
            .padding(top = 125.dp, start = 12.dp, end = 12.dp)
            .size(800.dp)
            .verticalScroll(rememberScrollState()) 
    ) {
        brandList.forEach { brand ->
            BrandItem(brand = brand, onEditClick = {
                navController.navigate("admin_edit_brand_screen/${brand.id}")
            }, onDeleteClick = {
                // Handle delete action
                Log.d("BrandScreen", "Delete brand: ${brand.name}")
                deleteBrandFromFirestore(brand.id, firestore)
            })
        }
    }

}



@Composable
fun BrandItem(brand: BrandModel, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    Log.d("BrandItem", "Brand name: ${brand.name}")
    Log.d("BrandItem", "Brand description: ${brand.description}")
    Log.d("BrandItem", "Brand image URL: ${brand.imageUrl}")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { /* Navigate to brand detail if needed */ },
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .background(Color.White)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberImagePainter(brand.imageUrl),
                contentDescription = brand.name,
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.Gray, RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(brand.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(brand.description, fontSize = 14.sp, color = Color.Gray)
            }
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Brand")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Brand",
                        tint = Color.Red
                    )
                }
            }

        }
    }

}
// Function to delete a brand from Firestore
fun deleteBrandFromFirestore(brandId: String, firestore: FirebaseFirestore) {
    firestore.collection("brands").document(brandId).delete()
        .addOnSuccessListener {
            Log.d("BrandScreen", "Brand deleted successfully")
        }
        .addOnFailureListener {
            Log.e("BrandScreen", "Error deleting brand", it)
        }
}
