package com.eritlab.jexmon.presentation.screens.brands


import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.eritlab.jexmon.R
import com.eritlab.jexmon.domain.model.BrandModel
import com.eritlab.jexmon.presentation.screens.dashboard_screen.DashboardViewModel
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun BrandsHome(
    category: String,
    navController: NavHostController = rememberNavController(),
    popularProductState: LazyListState = rememberLazyListState(),
    suggestionProductState: LazyListState = rememberLazyListState(),
    productViewModel: DashboardViewModel = hiltViewModel(),
    onItemClick: (String) -> Unit
) {
    val state by productViewModel.state.collectAsState()
    val brandFilterViewModel: BrandFilterViewModel = viewModel()
    val brandState by brandFilterViewModel.state.collectAsState()
    val productCountMap = remember { mutableStateMapOf<String, Int>() }

    var isLoading by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(category) }

    LaunchedEffect(selectedCategory) {
        Log.d("BrandsHome", "Selected Category: $selectedCategory")
        isLoading = true
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("brands").get().addOnSuccessListener { result ->
            val brands = result.documents.mapNotNull { doc ->
                doc.toObject(BrandModel::class.java)?.copy(id = doc.id)
            }

            brands.forEach { brand ->
                firestore.collection("products")
                    .whereEqualTo("brandId", brand.id)
                    .get()
                    .addOnSuccessListener { productSnapshot ->
                        productCountMap[brand.id] = productSnapshot.size()
                    }
            }

            brandFilterViewModel.filterBrandsByCategory(brands, selectedCategory)
            isLoading = false
        }.addOnFailureListener {
            isLoading = false
            Log.e("BrandsHome", "Error loading brands", it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Categories Header
            Text(
                text = "Danh mục thương hiệu",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Categories ScrollRow
            val categories = listOf("All", "Nam", "Nữ", "Dép", "Trẻ Em")
            val categoryImages = listOf(
                R.drawable.giay_lacoste_chaymon_120_2_nam_trang_navy_01_440_440,
                R.drawable._024_l6_pw1_26680062_41_1111,
                R.drawable.giay_lacoste_chaymon_120_2_nam_trang_navy_01_440_440,
                R.drawable._025_l2_pm1_86380187_64_111,
                R.drawable._024_l6_pw1_26680062_41_1111,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                categories.forEachIndexed { index, title ->
                    val isSelected = selectedCategory.equals(title, ignoreCase = true)
                    val backgroundColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else Color.White,
                        label = "BackgroundColorAnimation"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colors.primary else Color.LightGray,
                        label = "BorderColorAnimation"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colors.primary else Color.Gray,
                        label = "TextColorAnimation"
                    )

                    Column(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedCategory = title
                                brandFilterViewModel.filterBrandsByCategory(brandState.brands, title)
                            }
                            .background(backgroundColor)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            Image(
                                painter = painterResource(id = categoryImages[index]),
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            color = textColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Brands Grid Section
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .height(600.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(brandState.filteredBrands) { brand ->
                    Log.d("BrandsHome", "Brand name: ${brand.name}")
                    BrandItem(
                        brand = brand,
                        onItemClick = onItemClick,
                        productCount = productCountMap[brand.id] ?: 0
                    )
                }
            }
        }
    }
}

@Composable
fun BrandItem(
    brand: BrandModel,
    onItemClick: (String) -> Unit,
    productCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                brand.id?.let { brandId ->
                    Log.d("BrandItem", "Clicked brand: $brandId")
                    onItemClick(brandId)
                }
            },
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp
    ) {
        Log.d("BrandItem", "Brand name: ${brand.name}")
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = brand.imageUrl,
                        error = rememberAsyncImagePainter(R.drawable.error)
                    ),
                    contentDescription = brand.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = brand.name ?: "Unknown Brand",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colors.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$productCount sản phẩm",
                            color = MaterialTheme.colors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}