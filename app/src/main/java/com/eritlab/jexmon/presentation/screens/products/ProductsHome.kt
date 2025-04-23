package com.eritlab.jexmon.presentation.screens.products

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.eritlab.jexmon.presentation.dashboard_screen.component.AppBar
import com.eritlab.jexmon.presentation.graphs.detail_graph.DetailScreen
import kotlinx.coroutines.launch

@Composable
fun ProductsHome(
    brandId: String,

    navController: NavHostController = rememberNavController(),
    viewModel: ProductFilterViewModel = hiltViewModel(),
    onItemClick: (String) -> Unit, // Chỉ cần nhận vào onItemClick mà không cần định nghĩa lại nữa
    sortOption: String


) {
    var isLoading by remember { mutableStateOf(false) }

    // Lấy state từ viewModel
    val state = viewModel.state.collectAsState().value

    // Top Bar visibility
    val topBarVisibilityState = remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope() // Lấy scope cho coroutine

    LaunchedEffect(brandId, sortOption) {
        Log.d("ProductsHome", "Loading products for brand: $brandId with sort: $sortOption")
        isLoading = true
        viewModel.filterProductsByBrand(brandId, sortOption) // Gọi hàm lọc với cả brandId và sortOption
        isLoading = false
    }

    // AppBar
    AppBar(
        navController = navController,
        isVisible = topBarVisibilityState.value,
        searchCharSequence = { query ->
            viewModel.searchProducts(query) // Gọi hàm tìm kiếm trong ViewModel
        },
        onCartIconClick = { navController.navigate(DetailScreen.CartScreen.route) },
        onNotificationIconClick = { navController.navigate(DetailScreen.NotificationScreen.route) },
        onFilterApply = { brand, sort ->
            // Sử dụng coroutine scope để gọi hàm suspend
            scope.launch {
                viewModel.filterProductsByBrand(brand, sort) // Gọi hàm suspend trong scope
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 110.dp, start = 16.dp, end = 16.dp)
    ) {
        // Hiển thị CircularProgressIndicator nếu đang tải
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Hiển thị sản phẩm nếu không còn trạng thái loading
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
                    .height(600.dp)  // Cố định chiều cao cho LazyVerticalGrid
            ) {
                val productsToShow = if (state.searchQuery.isNotBlank()) {
                    state.searchResults
                } else {
                    state.filteredProducts
                }
                itemsIndexed(productsToShow) { index, product ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + scaleIn(initialScale = 0.8f)
                    ) {
                        ProductCard(
                            product = product,
                            onClick = {

                                product.id?.let {
                                    id ->
                                    Log.d("ProductsHome", "Navigating to product with id: $id")
                                    onItemClick(id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: com.eritlab.jexmon.domain.model.ProductModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clickable(onClick = onClick),
        elevation = 4.dp
    ) {
        Column {
            AsyncImage(
                model = product.images.firstOrNull(),
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$${product.price}",
                    style = MaterialTheme.typography.body2,
                    fontSize = 16.sp,
                    color = MaterialTheme.colors.primary
                )
            }
        }
    }
}
