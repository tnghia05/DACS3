package com.eritlab.jexmon.presentation.screens.brands


import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
    val brandState by brandFilterViewModel.state.collectAsState()

    var isLoading by remember { mutableStateOf(false) }
    // Sử dụng remember để giữ lại selectedCategory khi recomposition

    var selectedCategory by remember { mutableStateOf(category) }


    LaunchedEffect(selectedCategory) {
        Log.d("BrandsHome", "Selected Category: $selectedCategory")
        isLoading = true
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("brands").get().addOnSuccessListener { result ->
            val brands = result.documents.mapNotNull { doc ->
                doc.toObject(BrandModel::class.java)?.copy(id = doc.id)
            }
            Log.d("BrandsHome", "Loaded brands size: ${brands.size}")
            brandFilterViewModel.filterBrandsByCategory(brands, selectedCategory)
            Log.d("BrandsHome", "Filtering brands for category: $selectedCategory")
            isLoading = false
        }.addOnFailureListener {
            isLoading = false
            Log.e("BrandsHome", "Error loading brands", it)
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
            .padding(start = 15.dp, end = 15.dp, top = 110.dp),
    ) {

        val categories = listOf("All", "Nam", "Nữ", "Dép", "Trẻ Em")
        val categoryImages = listOf(
            R.drawable.giay_lacoste_chaymon_120_2_nam_trang_navy_01_440_440,  // ảnh nền cho "Women"
            R.drawable._024_l6_pw1_26680062_41_1111,  // ảnh nền cho "Women"
            R.drawable.giay_lacoste_chaymon_120_2_nam_trang_navy_01_440_440,  // ảnh nền cho "Women"
            R.drawable._025_l2_pm1_86380187_64_111,  // ảnh nền cho "Women"
            R.drawable._024_l6_pw1_26680062_41_1111,  // ảnh nền cho "Women"
        )
        // selectedCategory đã được khai báo ở trên

        Row(modifier = Modifier.fillMaxWidth()) {
            categories.forEachIndexed { index, title ->
                val isSelected = selectedCategory.equals(title, ignoreCase = true)
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFFFFF3E0) else MaterialTheme.colors.PrimaryLightColor,
                    label = "BackgroundColorAnimation"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFFFF9800) else Color.LightGray,
                    label = "BorderColorAnimation"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .clickable {
                            selectedCategory = title
                            brandFilterViewModel.filterBrandsByCategory(brandState.brands, title)
                        },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(55.dp)
                            .background(backgroundColor, shape = RoundedCornerShape(12.dp))
                            .border(
                                width = 1.5.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = categoryImages[index]),
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)) // bo góc 12.dp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))


    }

    Spacer(modifier = Modifier.height(3.dp))

    Log.d("BrandsHome", "Brand list size: ${brandState.filteredBrands.size}")

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(700.dp)
                .padding(top = 200.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material.CircularProgressIndicator()
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .height(700.dp)
                .padding(top = 200.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(brandState.filteredBrands) { brand ->
                BrandItem(brand = brand)
            }
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
            .padding(bottom = 16.dp, start = 8.dp, end = 8.dp)
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
                .size(160.dp)
                .border(2.dp, Color.LightGray, shape = RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(brand.imageUrl ?: ""),
                contentDescription = brand.name,
                modifier = Modifier.size(140.dp)
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