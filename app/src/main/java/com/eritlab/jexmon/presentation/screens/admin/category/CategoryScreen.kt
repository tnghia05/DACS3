package com.eritlab.jexmon.presentation.screens.admin.category


import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.eritlab.jexmon.domain.model.ProductModel
import com.eritlab.jexmon.presentation.graphs.admin_graph.AdminScreen
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun CategoryScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val categoryList = remember { mutableStateListOf<ProductModel>() }

    LaunchedEffect(Unit) {
        firestore.collection("products").addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                val categories = it.documents.mapNotNull { doc ->
                    doc.toObject(ProductModel::class.java)?.copy(id = doc.id)
                }
                categoryList.clear()
                categoryList.addAll(categories)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Danh sách Sản Phẩm",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp),
            color = Color.Black
        )

        Button(
            onClick = { navController.navigate(AdminScreen.AddCategoryScreen.route) },
            modifier = Modifier.align(Alignment.End),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE))
        ) {
            Text(text = "Thêm Sản Phẩm ", color = Color.White)
        }
    }

    Column(
        modifier = Modifier
            .padding(top = 125.dp, start = 12.dp, end = 12.dp)
            .size(600.dp)
            .verticalScroll(rememberScrollState())
    ) {
        categoryList.forEach { category ->
            CategoryItem(category = category, onEditClick = {
                navController.navigate(AdminScreen.EditCategoryScreen.passCategoryId(category.id))
            }, onDeleteClick = {
                deleteCategoryFromFirestore(category.id, firestore)
            })
        }
    }
}

@Composable
fun CategoryItem(category: ProductModel, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {},
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.background(Color.White).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Image(
                painter = rememberImagePainter(category.images[0]),
                contentDescription = category.name,
                modifier = Modifier.size(64.dp).background(Color.Gray, RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Log.d("CategoryItem", "Category image URL: ${category.images[0]}")
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(category.name, fontSize = 14.sp, color = Color.Gray)
            }
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Category")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Category",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

fun deleteCategoryFromFirestore(categoryId: String, firestore: FirebaseFirestore) {
    Log.d("CategoryScreen", "Deleting category with ID: $categoryId")
    firestore.collection("products").document(categoryId).delete()
        .addOnSuccessListener {
            Log.d("CategoryScreen", "Category deleted successfully")
        }
        .addOnFailureListener {
            Log.e("CategoryScreen", "Error deleting category", it)
        }
}
