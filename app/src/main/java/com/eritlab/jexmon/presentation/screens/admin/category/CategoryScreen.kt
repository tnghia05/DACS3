package com.eritlab.jexmon.presentation.screens.admin.category


import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.eritlab.jexmon.domain.model.CagerotyModel
import com.eritlab.jexmon.presentation.graphs.admin_graph.AdminScreen
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun CategoryScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val categoryList = remember { mutableStateListOf<CagerotyModel>() }

    LaunchedEffect(Unit) {
        firestore.collection("categories").addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                val categories = it.documents.mapNotNull { doc ->
                    doc.toObject(CagerotyModel::class.java)?.copy(id = doc.id)
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
            text = "Danh sách Danh mục",
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
            Text(text = "Thêm Danh mục", color = Color.White)
        }
    }

    Column(
        modifier = Modifier
            .padding(top = 125.dp, start = 12.dp, end = 12.dp)
            .size(400.dp)
            .verticalScroll(rememberScrollState())
    ) {
        categoryList.forEach { category ->
            CategoryItem(category = category, onEditClick = {
                navController.navigate("admin_edit_category_screen/${category.id}")
            }, onDeleteClick = {
                deleteCategoryFromFirestore(category.id, firestore)
            })
        }
    }
}

@Composable
fun CategoryItem(category: CagerotyModel, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
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
                painter = rememberImagePainter(category.imageUrl),
                contentDescription = category.name,
                modifier = Modifier.size(64.dp).background(Color.Gray, RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(category.description, fontSize = 14.sp, color = Color.Gray)
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
    firestore.collection("categories").document(categoryId).delete()
        .addOnSuccessListener {
            Log.d("CategoryScreen", "Category deleted successfully")
        }
        .addOnFailureListener {
            Log.e("CategoryScreen", "Error deleting category", it)
        }
}
