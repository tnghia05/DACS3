package com.eritlab.jexmon.presentation.screens.dashboard_screen.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.eritlab.jexmon.R
import com.eritlab.jexmon.domain.model.ProductModel
import com.eritlab.jexmon.presentation.screens.dashboard_screen.DashboardViewModel
import com.eritlab.jexmon.presentation.ui.theme.PrimaryColor
import com.eritlab.jexmon.presentation.ui.theme.PrimaryLightColor
import com.eritlab.jexmon.presentation.ui.theme.TextColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardScreen(
    popularProductState: LazyListState = rememberLazyListState(),
    suggestionProductState: LazyListState = rememberLazyListState(),
    productViewModel: DashboardViewModel = hiltViewModel(),
    onItemClick: (String) -> Unit,
    onBannerClick: (String) -> Unit
) {
    val state by productViewModel.state.collectAsState()
    val bannerListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll effect for banner
    LaunchedEffect(key1 = Unit) {
        while(true) {
            delay(3000) // Delay 3 seconds between scrolls
            coroutineScope.launch {
                // Get the current first visible item
                val firstVisible = bannerListState.firstVisibleItemIndex
                // Scroll to next item (or back to start if at end)
                bannerListState.animateScrollToItem(if (firstVisible < 2) firstVisible + 1 else 0)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 15.dp, end = 15.dp)
    ) {
        LazyRow(
            state = bannerListState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) {
            items(3) { index ->
                when (index) {
                    0 -> {
                        Box(
                            modifier = Modifier
                                .width(320.dp)
                                .height(120.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color(0x99000000))
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.banner30_4),
                                contentDescription = "Banner 30/4",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x55000000))
                                    .padding(start = 180.dp, end = 20.dp,  bottom = 15.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    buildAnnotatedString {
                                        withStyle(style = SpanStyle(
                                            color = Color.White
                                        )) {
                                            append("Chào Mừng 30/4  ")
                                        }
                                        withStyle(style = SpanStyle(
                                            color = Color(0xFFFFD700)
                                        )) {
                                            append(" Giảm 50%")
                                        }
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            color = Color.Black,
                                            offset = Offset(2f, 2f),
                                            blurRadius = 3f
                                        )
                                    )
                                )
                            }
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .width(320.dp)
                                .height(120.dp)
                                .clip(RoundedCornerShape(15.dp))
                        ) {
                            Image(
                                painter = painterResource(
                                    id = when (index) {
                                        1 -> R.drawable.thieunhi
                                        else -> R.drawable.thieunhi
                                    }
                                ),
                                contentDescription = when (index) {
                                    1 -> ""
                                    else -> "Winter Banner"
                                },
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x55000000))
                                    .padding(start = 180.dp, end = 20.dp, bottom = 15.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    buildAnnotatedString {
                                        withStyle(style = SpanStyle(
                                            color = Color.White
                                        )) {
                                            append(when (index) {
                                                1 -> ""
                                                else -> "Winter Collection "
                                            })
                                        }
                                        withStyle(style = SpanStyle(
                                            color = Color(0xFFFFD700)
                                        )) {
                                            append(when (index) {
                                                1 -> ""
                                                else -> "Giảm 50%"
                                            })
                                        }
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            color = Color.Black,
                                            offset = Offset(2f, 2f),
                                            blurRadius = 3f
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(15.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.flash_icon),
                    contentDescription = "Flash Deal",
                    modifier = Modifier
                        .background(
                            MaterialTheme.colors.PrimaryLightColor,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .size(50.dp)

                        .clip(RoundedCornerShape(10.dp))
                        .clickable {

                        }
                        .padding(10.dp)
                )
                Text(text = "Flash\nDeal", fontSize = 14.sp, textAlign = TextAlign.Center)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bill_icon),
                    contentDescription = "Bill",
                    modifier = Modifier
                        .background(
                            MaterialTheme.colors.PrimaryLightColor,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .size(50.dp)

                        .clip(RoundedCornerShape(10.dp))
                        .clickable {

                        }
                        .padding(10.dp)
                )
                Text(text = "Bill", fontSize = 14.sp, textAlign = TextAlign.Center)
            }


            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.game_icon),
                    contentDescription = "Game",
                    modifier = Modifier
                        .background(
                            MaterialTheme.colors.PrimaryLightColor,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .size(50.dp)

                        .clip(RoundedCornerShape(10.dp))
                        .clickable {

                        }
                        .padding(10.dp)
                )
                Text(text = "Game", fontSize = 14.sp, textAlign = TextAlign.Center)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.gift_icon),
                    contentDescription = "Daily Gift",
                    modifier = Modifier
                        .background(
                            MaterialTheme.colors.PrimaryLightColor,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .size(50.dp)

                        .clip(RoundedCornerShape(10.dp))
                        .clickable {

                        }
                        .padding(10.dp)
                )
                Text(text = "Daily\nGift", fontSize = 14.sp, textAlign = TextAlign.Center)
            }


            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.discover),
                    contentDescription = "More",
                    modifier = Modifier
                        .background(
                            MaterialTheme.colors.PrimaryLightColor,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .size(50.dp)

                        .clip(RoundedCornerShape(10.dp))
                        .clickable {

                        }
                        .padding(10.dp)
                )
                Text(text = "More", fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Special for you", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = "See More", color = MaterialTheme.colors.TextColor)
        }

        Spacer(modifier = Modifier.height(15.dp))
//special offer cart
        LazyRow(
            state = popularProductState,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) {
            item {
                ConstraintLayout(
                    modifier = Modifier
                        .width(280.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onBannerClick("men") }
                ) {
                    //constrains
                    val (bannerText, bannerImage) = createRefs()
                    Image(
                        painter = painterResource(id = R.drawable.image_banner_3),
                        contentDescription = "",
                        modifier = Modifier.constrainAs(bannerImage) {}
                    )
                    Column(
                        modifier = Modifier
                            .background(Color(0x8DB3B0B0))
                            .padding(15.dp)
                            .constrainAs(bannerText) {
                                top.linkTo(bannerImage.top)
                                bottom.linkTo(bannerImage.bottom)
                                start.linkTo(bannerImage.start)
                                end.linkTo(bannerImage.end)
                                height = Dimension.fillToConstraints
                                width = Dimension.fillToConstraints
                            }
                    ) {
                        Text(
                            text = "Giày Nam",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.heightIn(15.dp))
                        Text(text = "85 Brands", color = Color.White)
                    }


                }
            }
            item {
                //second item
                ConstraintLayout(
                    modifier = Modifier
                        .width(280.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onBannerClick("fashion") }
                ) {
                    //constrains
                    val (bannerText2, bannerImage2) = createRefs()
                    Image(
                        painter = painterResource(id = R.drawable.image_banner_2),
                        contentDescription = "",
                        modifier = Modifier.constrainAs(bannerImage2) {}
                    )
                    Column(
                        modifier = Modifier
                            .background(Color(0x8DB3B0B0))
                            .padding(15.dp)
                            .constrainAs(bannerText2) {
                                top.linkTo(bannerImage2.top)
                                bottom.linkTo(bannerImage2.bottom)
                                start.linkTo(bannerImage2.start)
                                end.linkTo(bannerImage2.end)
                                height = Dimension.fillToConstraints
                                width = Dimension.fillToConstraints
                            }
                    ) {
                        Text(
                            text = "Giày Nữ",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.heightIn(15.dp))
                        Text(text = "15 Brands", color = Color.White)
                    }
                }
            }
            item {
                //second item
                ConstraintLayout(
                    modifier = Modifier
                        .width(280.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onBannerClick("all") }
                ) {
                    //constrains
                    val (bannerText2, bannerImage2) = createRefs()
                    Image(
                        painter = painterResource(id = R.drawable.image_banner_2),
                        contentDescription = "",
                        modifier = Modifier.constrainAs(bannerImage2) {}
                    )
                    Column(
                        modifier = Modifier
                            .background(Color(0x8DB3B0B0))
                            .padding(15.dp)
                            .constrainAs(bannerText2) {
                                top.linkTo(bannerImage2.top)
                                bottom.linkTo(bannerImage2.bottom)
                                start.linkTo(bannerImage2.start)
                                end.linkTo(bannerImage2.end)
                                height = Dimension.fillToConstraints
                                width = Dimension.fillToConstraints
                            }
                    ) {
                        Text(
                            text = "Xem Thêm",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.heightIn(15.dp))
                        Text(text = "15 Brands", color = Color.White)
                    }
                }
            }

        }



        Spacer(modifier = Modifier.height(15.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Sản Phẩm Mới Nhất", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = "See More", color = MaterialTheme.colors.TextColor)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Latest Products Section
        LazyRow(
            state = suggestionProductState,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) {
            state.latestProducts.let { products ->
                items(products.size) { index ->
                    ProductItem(product = products[index], onItemClick = onItemClick)
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Best Selling Products Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Sản Phẩm Bán Chạy", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = "See More", color = MaterialTheme.colors.TextColor)
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) {
            state.bestSellingProducts.let { products ->
                items(products.size) { index ->
                    ProductItem(product = products[index], onItemClick = onItemClick)
                }
            }
        }
    }
}

@Composable
fun ProductItem(
    product: ProductModel,
    onItemClick: (String) -> Unit
) {
    var favouriteRemember by remember { mutableStateOf(product.isFavourite) }
    
    Column {
        Box(
            modifier = Modifier
                .size(150.dp)
                .background(Color.LightGray, shape = RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                    product.id.takeIf { it.isNotBlank() }?.let { id ->
                        onItemClick(id)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(product.images.firstOrNull()),
                contentDescription = product.description
            )
            if (product.sold > 5) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(Color(0xFFFF4444), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "Hot",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Text(
            text = product.name,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(151.dp)
        )

        Row(
            modifier = Modifier.width(150.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatPrice(product.price),
                fontWeight = FontWeight(600),
                color = MaterialTheme.colors.PrimaryColor
            )
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(MaterialTheme.colors.primaryVariant, shape = CircleShape)
                    .clip(CircleShape)
                    .clickable {
                        favouriteRemember = !favouriteRemember
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(
                        id = if (favouriteRemember)
                            R.drawable.heart_icon_2
                        else R.drawable.heart_icon
                    ),
                    contentDescription = "Favourite Icon",
                    modifier = Modifier.padding(3.dp),
                    colorFilter = if (favouriteRemember) ColorFilter.tint(Color.Red) else null
                )
            }
        }
    }
}

private fun formatPrice(price: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    return format.format(price)
}