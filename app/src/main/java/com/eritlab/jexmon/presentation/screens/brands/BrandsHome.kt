package com.eritlab.jexmon.presentation.screens.brands


import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.eritlab.jexmon.presentation.dashboard_screen.component.AppBar
import com.eritlab.jexmon.presentation.graphs.detail_graph.DetailScreen
import com.eritlab.jexmon.presentation.screens.dashboard_screen.DashboardViewModel
import com.eritlab.jexmon.presentation.ui.theme.PrimaryLightColor
import com.google.firebase.firestore.FirebaseFirestore

@Composable

fun BrandsHome(
    category: String, // ← thêm dòng này
    navController: NavHostController = rememberNavController(),

    popularProductState: LazyListState = rememberLazyListState(),
    suggestionProductState: LazyListState = rememberLazyListState(),
    productViewModel: DashboardViewModel = hiltViewModel(),
    onItemClick: (String) -> Unit
) {


    val state by productViewModel.state.collectAsState()
    val brandFilterViewModel: BrandFilterViewModel = viewModel()

    LaunchedEffect(category) {
        // Fetch brands from Firestore and filter by category
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("brands").get().addOnSuccessListener { result ->
            val brands = result.documents.mapNotNull { doc ->
                doc.toObject(BrandModel::class.java)?.copy(id = doc.id)
            }
            brandFilterViewModel.filterBrandsByCategory(brands, category)
        }
    }
    //topBar visibility state
    val topBarVisibilityState = remember {
        mutableStateOf(true)
    }
    AppBar(
        navController = navController,
        isVisible = topBarVisibilityState.value,
        searchCharSequence = {

        },
        onCartIconClick = {
            navController.navigate(DetailScreen.CartScreen.route)
        },
        onNotificationIconClick = {
            navController.navigate(DetailScreen.NotificationScreen.route)
        })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 15.dp, end = 15.dp, top = 150.dp),
    ) {


        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.user),
                    contentDescription = "All",
                    modifier = Modifier
                        .background(
                            MaterialTheme.colors.PrimaryLightColor,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .size(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onItemClick("all") }
                        .padding(10.dp)
                )
                Text(text = "All", fontSize = 14.sp, textAlign = TextAlign.Center)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.user),
                    contentDescription = "Women",
                    modifier = Modifier
                        .background(
                            MaterialTheme.colors.PrimaryLightColor,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .size(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onItemClick("women") }
                        .padding(10.dp)
                )
                Text(text = "Women", fontSize = 14.sp, textAlign = TextAlign.Center)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.user),
                    contentDescription = "Men",
                    modifier = Modifier
                        .background(
                            MaterialTheme.colors.PrimaryLightColor,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .size(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onItemClick("men") }
                        .padding(10.dp)
                )
                Text(text = "Men", fontSize = 14.sp, textAlign = TextAlign.Center)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.user),
                    contentDescription = "Shoes",
                    modifier = Modifier
                        .background(
                            MaterialTheme.colors.PrimaryLightColor,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .size(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onItemClick("shoes") }
                        .padding(10.dp)
                )
                Text(text = "Shoes", fontSize = 14.sp, textAlign = TextAlign.Center)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.user),
                    contentDescription = "Kids",
                    modifier = Modifier
                        .background(
                            MaterialTheme.colors.PrimaryLightColor,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .size(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onItemClick("kids") }
                        .padding(10.dp)
                )
                Text(text = "Kids", fontSize = 14.sp, textAlign = TextAlign.Center)
            }
            }
        Spacer(modifier = Modifier.height(30.dp))


        }

        Spacer(modifier = Modifier.height(30.dp))

    val brandState by brandFilterViewModel.state.collectAsState()
    Log.d("BrandsHome", "Brand list size: ${brandState.filteredBrands.size}")

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .padding(top = 250.dp),
    ) {
        items(brandState.filteredBrands) { brand ->
            BrandItem(brand = brand)
        }
    }

    }









@Composable
fun BrandItem(
    brand: BrandModel,
    onItemClick: (String) -> Unit = {}
) {

    Column(
        modifier = Modifier
            .padding(bottom = 16.dp)
            .clickable {
                brand.id?.let {
                    Log.d("BrandItem", "Clicked brand: $it")
                    onItemClick(it)
                } ?: Log.e("BrandItem", "Brand ID is null")
            }
    ) {
        //he he h
        Box(
            modifier = Modifier
                .size(150.dp)
                .background(Color.LightGray, shape = RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(brand.imageUrl ?: ""),
                contentDescription = brand.name,
                modifier = Modifier.size(100.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = brand.name ?: "Unknown Brand",
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.width(150.dp)
        )



    }
}
