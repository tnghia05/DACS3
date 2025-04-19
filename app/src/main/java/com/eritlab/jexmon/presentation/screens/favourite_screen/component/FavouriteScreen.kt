package com.eritlab.jexmon.presentation.screens.favourite_screen.component

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.eritlab.jexmon.R
import com.eritlab.jexmon.domain.model.ProductModel
import com.eritlab.jexmon.presentation.screens.dashboard_screen.DashboardViewModel

@Composable
fun FavouriteScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onProductClick: (String) -> Unit = {},
    onCartClick: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(top = 70.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            state.product?.filter { it.isFavourite }?.let { favoriteProducts ->
                items(favoriteProducts.size) { index ->
                    val product = favoriteProducts[index]
                    Log.d("FavouriteScreen", "Product: ${product.name}, Is Favourite: ${product.isFavourite}")
                    ProductCard(
                        product = product,
                        onProductClick = onProductClick,
                        onFavoriteClick = {
                            product.id?.let { id ->
                                product.isFavourite = !product.isFavourite
                                viewModel.fetchProducts() // Refresh the products list
                            }
                        },
                        onCartClick = { product.id?.let { onCartClick(it) } }
                    )
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: ProductModel,
    onProductClick: (String) -> Unit,
    onFavoriteClick: () -> Unit,
    onCartClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { product.id?.let { onProductClick(it) } },
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(product.images.firstOrNull()),
                    contentDescription = product.description,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = if (product.isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colors.primary
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$${product.price}",
                    color = MaterialTheme.colors.primary
                )
                }
                IconButton(
                    onClick = onCartClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.cart_icon),
                        contentDescription = "Add to Cart",
                        tint = MaterialTheme.colors.primary
                    )
                }
            }
        }
    }
}