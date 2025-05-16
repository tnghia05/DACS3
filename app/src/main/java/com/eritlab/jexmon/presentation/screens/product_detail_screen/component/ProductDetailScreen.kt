package com.eritlab.jexmon.presentation.screens.product_detail_screen.component

// ... các imports khác từ Jetpack Compose, Material ...
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.eritlab.jexmon.R
import com.eritlab.jexmon.domain.model.CartItem
import com.eritlab.jexmon.domain.model.ProductModel
import com.eritlab.jexmon.domain.model.ReviewModel
import com.eritlab.jexmon.presentation.screens.cart_screen.CartViewModel
import com.eritlab.jexmon.presentation.screens.product_detail_screen.ProductDetailViewModel
import com.eritlab.jexmon.presentation.ui.theme.PrimaryColor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.floor

@Composable
fun ProductDetailScreen(
    productId: String,
    popBack: () -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToCheckout: (List<CartItem>) -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel(),
    onNavigateToProduct: (String) -> Unit
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser == null) {
        Toast.makeText(LocalContext.current, "Vui lòng đăng nhập để tiếp tục", Toast.LENGTH_SHORT).show()
        return
    }

    ProductDetailContent(
        viewModel = viewModel,
        cartViewModel = cartViewModel,
        productId = productId,
        popBack = popBack,
        onNavigateToCart = onNavigateToCart,
        onNavigateToCheckout = onNavigateToCheckout,
        userId = currentUser.uid,
        onNavigateToProduct = onNavigateToProduct
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailContent(
    viewModel: ProductDetailViewModel,
    cartViewModel: CartViewModel,
    productId: String,
    popBack: () -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToCheckout: (List<CartItem>) -> Unit,
    userId: String,
    onNavigateToProduct: (String) -> Unit
)
{
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val state = viewModel.state.value
    val context = LocalContext.current
    var isSheetOpen by rememberSaveable  { mutableStateOf(false) }
    var isBuyNowMode by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(productId) {
        if (!productId.isNullOrEmpty()) {
            viewModel.getProductDetail(productId)
        } else {
            Log.e("ProductDetailScreen", "Lỗi: productId rỗng hoặc null!")
        }
    }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = currentUser?.uid

    var fetchedNameFromCallback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUserId) {
        if (currentUserId != null && fetchedNameFromCallback == null) {
            Log.d("Composable", "Triggering getUserNameByIdCallback for UID: $currentUserId")
            viewModel.getUserNameByIdCallback(
                userId = currentUserId,
                onSuccess = { name ->
                    fetchedNameFromCallback = name
                    Log.d("Composable", "Callback success, fetched name: $name. State updated.")
                },
                onFailure = { e ->
                    Log.e("Composable", "Callback failed to fetch name", e)
                    fetchedNameFromCallback = null
                }
            )
        } else if (currentUserId == null) {
            fetchedNameFromCallback = null
        }
    }

    if (state.isLoading) {
        Log.d("ProductDetail", "Loading state: true")
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Text(text = "Đang tải.aaaa..", modifier = Modifier.padding(top = 8.dp))
        }

    } else if (state.productDetail != null)
    {
        val product = state.productDetail
        val availableSizes = product.stock.map { it.size }.distinct()

        var selectedPicture by rememberSaveable {
            mutableStateOf(
                if (product.images.isNotEmpty()) product.images[0] else null
            )
        }
        val scrollState = rememberScrollState()

        var selectedSize by rememberSaveable  {
            mutableStateOf(availableSizes.firstOrNull() ?: 0)
        }
        var availableColors by rememberSaveable  { mutableStateOf(listOf<String>()) }
        var selectedColor by rememberSaveable  { mutableStateOf<String?>(null) }
        Log.d("ProductDetail", "Available Sizes: $availableSizes")
        var isExpanded by rememberSaveable  { mutableStateOf(false) }
        var showReviewsSection by remember { mutableStateOf(false) }
        var showReviewInputForm by remember { mutableStateOf(false) }

        LaunchedEffect(product.id, product.brandId) {
            if (!product.brandId.isNullOrEmpty()) {
                viewModel.getRelatedProducts(product.brandId, product.id)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .height(700.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { popBack() },
                            modifier = Modifier
                                .background(Color.White, CircleShape)
                                .clip(CircleShape)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.back_icon),
                                contentDescription = null
                            )
                        }

                        Row(
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format("%.1f", product.rating),
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Image(
                                painter = painterResource(id = R.drawable.star_icon),
                                contentDescription = null
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(Color(0xFFF5F5F5))
                    ) {
                        selectedPicture?.let { image ->
                            AsyncImage(
                                model = image,
                                contentDescription = "Product Image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            )
                        } ?: Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.error),
                                    contentDescription = "No Image",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Không có ảnh sản phẩm",
                                    color = Color.Gray,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    if (product.images.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(30.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            product.images.forEach { image ->
                                IconButton(
                                    onClick = { selectedPicture = image },
                                    modifier = Modifier
                                        .size(18.dp)
                                        .border(
                                            width = 1.dp,
                                            color = if (selectedPicture == image) MaterialTheme.colors.PrimaryColor else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .background(Color.White, shape = RoundedCornerShape(10.dp))
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(image),
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    } else {
                        Text("Không có ảnh nào", color = Color.Gray)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, shape = RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp))
                            .padding(15.dp)
                    ) {
                        val discountText = "${product.discount.toInt()}%"

                        val discountedPrice = product.price * (1 - product.discount.toFloat() / 100)
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${String.format("%,d", discountedPrice.toLong())}đ",
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD0011B)
                            )

                            Text(
                                text = "${String.format("%,d", product.price.toLong())}đ",
                                fontSize = 15.sp,
                                color = Color.Gray,
                                textDecoration = TextDecoration.LineThrough
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = "Đã bán ${product.sold}",
                                fontSize = 14.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )

                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = product.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Divider(
                            color = Color.LightGray,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isExpanded) product.description else "${product.description.take(100)}...",
                                fontSize = 15.5.sp,
                                color = MaterialTheme.colors.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { isExpanded = !isExpanded }
                            ) {
                                Text(
                                    text = if (isExpanded) "Thu gọn" else "Xem thêm",
                                    color = MaterialTheme.colors.PrimaryColor,
                                    fontSize = 16.sp,
                                    textDecoration = TextDecoration.Underline
                                )
                                Icon(
                                    painter = painterResource(id = R.drawable.arrow_right),
                                    contentDescription = "",
                                    tint = MaterialTheme.colors.PrimaryColor,
                                    modifier = Modifier.rotate(if (isExpanded) 90f else 0f)
                                )
                            }
                        }
                        Divider(
                            color = Color.LightGray,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        Column {
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ship),
                                    contentDescription = "Shipping Icon",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(top = 2.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Column {
                                    Text(
                                        text = "Nhận từ 25 Th03 - 25 Th03",
                                        fontSize = 16.sp,
                                        color = Color.Black
                                    )

                                    Text(
                                        text = "Miễn phí vận chuyển",
                                        fontSize = 16.sp,
                                        color = Color.Black
                                    )

                                    Text(
                                        text = "Tặng Voucher đ15.000 nếu đơn giao sau thời gian trên.",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        Divider(
                            color = Color.LightGray,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.icon_doi_tra_hang),
                                contentDescription = "Return Package Icon",
                                modifier = Modifier
                                    .size(21.dp)
                                    .padding(top = 2.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "Trả hàng miễn phí 15 ngày",
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                        }

                        Divider(
                            color = Color.LightGray,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = String.format("%.1f", product.rating),
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontSize = 20.sp,
                            )

                            Spacer(modifier = Modifier.width(6.dp))
                            Image(
                                painter = painterResource(id = R.drawable.star_icon),
                                contentDescription = null
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = "Đánh Giá Sản Phẩm",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            if (!state.isLoadingReviews) {
                                Text(
                                    text = "(${state.reviews.size})",
                                    fontSize = 14.5.sp,
                                    color = Color.Black
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = "Tất cả >",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray,
                                modifier = Modifier.clickable {
                                    showReviewsSection = !showReviewsSection
                                }
                            )
                        }

                        if (!state.isLoadingReviews) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                var showAllReviews by remember { mutableStateOf(false) }
                                var showReviewInputForm by remember { mutableStateOf(false) }
                                val reviewsToShow = if (showAllReviews) state.reviews else state.reviews.take(3)

                                Button(
                                    onClick = { showReviewInputForm = !showReviewInputForm },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color.White,
                                        contentColor = MaterialTheme.colors.PrimaryColor
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colors.PrimaryColor),
                                    elevation = ButtonDefaults.elevation(
                                        defaultElevation = 0.dp,
                                        pressedElevation = 0.dp
                                    )
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.log_out),
                                            contentDescription = "Write Review",
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (showReviewInputForm) "Đóng form đánh giá" else "Viết đánh giá",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                if (showReviewInputForm) {
                                    val currentUser = FirebaseAuth.getInstance().currentUser
                                    if (currentUser != null) {
                                        ReviewInputForm(
                                            viewModel = viewModel,
                                            productId = product.id,
                                            userId = currentUser.uid,
                                            authorName = currentUser.displayName ?: "Người dùng"
                                        )
                                    } else {
                                        Text(
                                            text = "Vui lòng đăng nhập để viết đánh giá",
                                            color = Color.Gray,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                reviewsToShow.forEach { review ->
                                    ReviewItem(review = review)
                                }

                                if (state.reviews.size > 3 && !showAllReviews) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { showAllReviews = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = Color.White,
                                            contentColor = MaterialTheme.colors.PrimaryColor
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colors.PrimaryColor),
                                        elevation = ButtonDefaults.elevation(
                                            defaultElevation = 0.dp,
                                            pressedElevation = 0.dp
                                        )
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Xem thêm ${state.reviews.size - 3} đánh giá",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                painter = painterResource(id = R.drawable.arrow_right),
                                                contentDescription = "Show More",
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .rotate(90f)
                                            )
                                        }
                                    }
                                }

                                if (showAllReviews && state.reviews.size > 3) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { showAllReviews = false },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = Color.White,
                                            contentColor = MaterialTheme.colors.PrimaryColor
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colors.PrimaryColor),
                                        elevation = ButtonDefaults.elevation(
                                            defaultElevation = 0.dp,
                                            pressedElevation = 0.dp
                                        )
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Thu gọn",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                painter = painterResource(id = R.drawable.arrow_right),
                                                contentDescription = "Show Less",
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .rotate(-90f)
                                            )
                                        }
                                    }
                                }

                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (showReviewInputForm) {
                            val currentProductId = state.productDetail?.id ?: ""

                            val authorNameToShow = when {
                                !fetchedNameFromCallback.isNullOrBlank() -> fetchedNameFromCallback!!
                                !currentUser?.displayName.isNullOrBlank() -> currentUser!!.displayName!!
                                else -> "Người dùng ẩn danh"
                            }

                            if (currentProductId.isNotBlank() && currentUserId != null) {
                                Log.d("ProductDetail", "Showing Review Input Form for Product: $currentProductId, User ID: $currentUserId")
                                Log.d("ProductDetail", "Using Author Name: $authorNameToShow")

                                ReviewInputForm(
                                    viewModel = viewModel,
                                    productId = currentProductId,
                                    userId = currentUserId,
                                    authorName = authorNameToShow
                                )
                            } else {
                                Log.d("ProductDetail", "Conditions NOT met to show Review Input Form. Product ID Blank: ${currentProductId.isBlank()}, User ID Null: ${currentUserId == null}")

                                Text(
                                    text = when {
                                        currentProductId.isBlank() -> "Không thể tải thông tin sản phẩm để viết đánh giá."
                                        currentUserId == null -> "Vui lòng đăng nhập để viết đánh giá."
                                        else -> "Không thể hiển thị form đánh giá."
                                    },
                                    color = Color.Gray,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Divider(
                            color = Color.LightGray,
                            thickness = 8.dp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        val relatedProducts = viewModel.relatedProducts.value
                        Log.d("ProductDetail", "Related Products: $relatedProducts")
                        if (relatedProducts.isNotEmpty()) {
                            RelatedProductsSection(
                                products = relatedProducts,
                                onProductClick = onNavigateToProduct
                            )
                        }

                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .height(56.dp)
            ) {
                Button(
                    onClick = {
                        isBuyNowMode = false
                        isSheetOpen = true
                        coroutineScope.launch { sheetState.show() }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF26A69A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(topStart = 15.dp, bottomStart = 15.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.cart),
                        contentDescription = "Giỏ hàng",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Button(
                    onClick = {
                        isBuyNowMode = true
                        isSheetOpen = true
                        coroutineScope.launch { sheetState.show() }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFE53935),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(topEnd = 15.dp, bottomEnd = 15.dp),
                    modifier = Modifier
                        .weight(3f)
                        .fillMaxHeight()
                ) {
                    val discountedPrice = product.price * (1 - product.discount.toFloat() / 100)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Mua với voucher", fontSize = 14.sp)
                        Text("${String.format("%,d", discountedPrice.toLong())}đ", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

        }

        if (isSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { isSheetOpen = false },
                sheetState = sheetState
            ) {
                BottomSheetContent(
                    product = product,
                    selectedSize = selectedSize,
                    selectedColor = selectedColor,
                    availableSizes = availableSizes,
                    availableColors = availableColors,
                    onSizeSelected = { selectedSize = it },
                    onColorSelected = { selectedColor = it },
                    viewModel = viewModel,
                    cartViewModel = cartViewModel,
                    isBuyNowMode = isBuyNowMode,
                    onNavigateToCheckout = { cartItem -> 
                        isSheetOpen = false
                        onNavigateToCheckout(listOf(cartItem))
                    }
                )
            }
        }

        LaunchedEffect(selectedSize) {
            availableColors = product.stock
                .filter { it.size == selectedSize && it.quantity > 0 }
                .mapNotNull { convertToHex(it.color) }
                .distinct()
            Log.d("ProductDetail", "Available Colors: $availableColors")

            selectedColor = availableColors.firstOrNull()
        }

    }

    else {
        Toast.makeText(context, state.errorMessage, Toast.LENGTH_SHORT).show()
    }
}

fun convertToHex(color: String): String {
    val colorMap = mapOf(
        "red" to "#FF0000",
        "blue" to "#0000FF",
        "green" to "#008000",
        "yellow" to "#FFFF00",
        "black" to "#000000",
        "white" to "#FFFFFF",
        "gray" to "#808080",
        "purple" to "#800080",
        "orange" to "#FFA500",
        "pink" to "#FFC0CB"
    )

    return colorMap[color.lowercase()] ?: color
}

fun convertToColorName(hex: String): String {
    val colorMap = mapOf(
        "#FF0000" to "Red",
        "#0000FF" to "Blue",
        "#008000" to "Green",
        "#FFFF00" to "Yellow",
        "#000000" to "Black",
        "#FFFFFF" to "White",
        "#808080" to "Gray",
        "#800080" to "Purple",
        "#FFA500" to "Orange",
        "#FFC0CB" to "Pink"
    )

    return colorMap[hex.uppercase()] ?: hex
}

@Composable
fun BottomSheetContent(
    product: ProductModel,
    selectedSize: Int,
    selectedColor: String?,
    availableSizes: List<Int>,
    availableColors: List<String>,
    onSizeSelected: (Int) -> Unit,
    onColorSelected: (String) -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel(),
    isBuyNowMode: Boolean,
    onNavigateToCheckout: (CartItem) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var stockQuantity by rememberSaveable { mutableStateOf<Int?>(null) }
    var quantity by rememberSaveable { mutableStateOf(1) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(selectedSize, selectedColor) {
        if (selectedSize != null && selectedColor != null && product.id != null) {
            val nameColor = convertToColorName(selectedColor).replace(" ", "_")
            val documentId = "size_${selectedSize}_${nameColor}"

            Log.d("BottomSheet", "Fetching from: products/${product.id}/stock/$documentId")

            db.collection("products").document(product.id)
                .collection("stock").document(documentId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        stockQuantity = document.getLong("quantity")?.toInt() ?: 0
                    } else {
                        stockQuantity = 0
                    }
                }
                .addOnFailureListener {
                    stockQuantity = null
                }
        }
    }

    Log.d("BottomSheet", "BottomSheetContent hiển thị")

    Column( modifier = Modifier
        .fillMaxHeight(0.7f)
        .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(product.images[0]),
                contentDescription = "Product Image",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Giá: ${product.price}đ",
                        fontWeight = FontWeight.Bold,
                        color = Color.Red,
                        fontSize = 20.sp
                    )

                }

                Text(
                    text = "Kho: ${stockQuantity ?: "..."} sản phẩm",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }
        Divider(
            color = Color.LightGray,
            thickness = 0.5.dp,
            modifier = Modifier.padding(vertical = 10.dp)
        )
        Text("Chọn Size", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(availableSizes) { size ->
                Box(
                    modifier = Modifier
                        .border(
                            2.dp,
                            if (selectedSize == size) MaterialTheme.colors.primary else Color.Gray,
                            RoundedCornerShape(10.dp)
                        )
                        .background(Color.White, RoundedCornerShape(10.dp))
                        .clickable { onSizeSelected(size) }
                        .padding(15.dp)
                ) {
                    Log.d("BottomSheet", "Size: $size")
                    Text(text = size.toString(), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Divider(
            color = Color.LightGray,
            thickness = 0.9.dp,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Text("Chọn Màu", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(availableColors) { color ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(
                            1.dp,
                            if (selectedColor == color && stockQuantity != 0) MaterialTheme.colors.primary else Color.Transparent,
                            CircleShape
                        )
                        .background(
                            Color(android.graphics.Color.parseColor(color)).copy(alpha = if (stockQuantity == 0) 0.3f else 1f),
                            CircleShape
                        )
                        .clip(CircleShape)
                        .then(
                            if (stockQuantity != 0) {
                                Modifier.clickable { onColorSelected(color) }
                            } else {
                                Modifier
                            }
                        )
                ) {
                    if (stockQuantity == 0) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "×",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        Divider(
            color = Color.LightGray,
            thickness = 0.5.dp,
            modifier = Modifier.padding(vertical = 10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Chọn Số Lượng:", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Spacer(modifier = Modifier.width(100.dp))

            IconButton(onClick = { if (quantity > 1) quantity-- }) {
                Image(painter = painterResource(id = R.drawable.remove), contentDescription = null)
            }

            Text(
                quantity.toString(),
                textAlign = TextAlign.Center,
                modifier = Modifier.width(35.dp)
            )

            IconButton(onClick = { if (quantity < 5) quantity++ }) {
                Image(painter = painterResource(id = R.drawable.plus_icon), contentDescription = null)
            }
        }

        Button(
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.PrimaryColor,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 30.dp)
                .height(60.dp)
                .clip(RoundedCornerShape(15.dp)),
            onClick = {
                if (selectedColor == null) {
                    Toast.makeText(context, "Vui lòng chọn màu sắc", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val cartItem = CartItem(
                    userId = currentUser!!.uid,
                    productId = product.id,
                    name = product.name,
                    price = floor(product.price * (1 - product.discount.toFloat() / 100)),
                    priceBeforeDiscount = product.price,
                    size = selectedSize,
                    color = selectedColor,
                    quantity = quantity,
                    imageUrl = product.images.firstOrNull() ?: "",
                    discount = product.discount
                )

                Log.d("BottomSheet", "Created CartItem: $cartItem")

                if (isBuyNowMode) {
                    Log.d("BottomSheet", "Buy Now Mode: Navigating to checkout")
                    onNavigateToCheckout(cartItem)
                } else {
                    Log.d("BottomSheet", "Add to Cart Mode: Adding to cart")
                    coroutineScope.launch {
                        viewModel.addToCart(cartItem).collect { result ->
                            result.fold(
                                onSuccess = {
                                    Toast.makeText(context, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { e ->
                                    Toast.makeText(context, e.message ?: "Lỗi thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        ) {
            Text(text = if (isBuyNowMode) "Mua ngay" else "Thêm vào giỏ hàng")
        }

    }
}

@Composable
fun ReviewItem(review: ReviewModel) {
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var userName by remember { mutableStateOf<String?>(null) }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(review.userId) {
        if (!review.userId.isNullOrEmpty()) {
            val doc = db.collection("user").document(review.userId).get().await()
            avatarUrl = doc.getString("avatarUrl")
            userName = doc.getString("name")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(avatarUrl ?: R.drawable.user),
                contentDescription = "User Avatar",
                modifier = Modifier
                    .size(35.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = userName ?: review.userName,
                    fontSize = 14.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    repeat(5) { index ->
                        Icon(
                            painter = painterResource(
                                id = if (index < review.rating) R.drawable.star_icon
                                else R.drawable.star__1_
                            ),
                            contentDescription = "Star Rating",
                            tint = if (index < review.rating) Color(0xFFFFC107) else Color.Gray,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        if (review.comment.isNotEmpty()) {
            Text(
                text = review.comment,
                fontSize = 14.sp,
                color = Color.Black,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (!review.images.isNullOrEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                items(review.images) { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Review Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        Divider(
            color = Color.LightGray.copy(alpha = 0.5f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
fun ReviewInputForm(
    viewModel: ProductDetailViewModel,
    productId: String,
    userId: String,
    authorName: String
) {
    var reviewContent by remember { mutableStateOf("") }
    var selectedRating by remember { mutableStateOf(0) }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val reviewSubmissionState = viewModel.reviewSubmissionState.value
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        selectedImageUris = uris
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Viết đánh giá của bạn",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Chọn số sao:", fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            for (i in 1..5) {
                Icon(
                    painter = painterResource(
                        id = if (i <= selectedRating) R.drawable.star_icon
                        else R.drawable.star__1_
                    ),
                    contentDescription = "$i Star",
                    tint = if (i <= selectedRating) Color(0xFFFFD700) else Color.Gray,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { selectedRating = i }
                )
                if (i < 5) Spacer(modifier = Modifier.width(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = reviewContent,
            onValueChange = { reviewContent = it },
            label = { Text("Nội dung đánh giá") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.White,
                contentColor = MaterialTheme.colors.PrimaryColor
            ),
            border = BorderStroke(1.dp, MaterialTheme.colors.PrimaryColor)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.camera_icon),
                    contentDescription = "Upload Image",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedImageUris.isEmpty()) "Thêm ảnh"
                    else "Đã chọn ${selectedImageUris.size} ảnh"
                )
            }
        }

        if (selectedImageUris.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(selectedImageUris) { uri ->
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(4.dp))
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = {
                                selectedImageUris = selectedImageUris.filter { it != uri }
                            },
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.TopEnd)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.close),
                                contentDescription = "Remove Image",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.submitReview(
                    productId = productId,
                    userId = userId,
                    authorName = authorName,
                    rating = selectedRating,
                    content = reviewContent,
                    imageUris = selectedImageUris
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !reviewSubmissionState.isSubmitting && selectedRating > 0 && reviewContent.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.PrimaryColor,
                contentColor = Color.White
            )
        ) {
            if (reviewSubmissionState.isSubmitting) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Gửi đánh giá")
            }
        }

        reviewSubmissionState.submitError?.let { error ->
            Text(
                text = error,
                color = Color.Red,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        if (reviewSubmissionState.submitSuccess) {
            Text(
                text = "Gửi đánh giá thành công!",
                color = Color.Green,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.Center
            )

            LaunchedEffect(true) {
                reviewContent = ""
                selectedRating = 0
                selectedImageUris = emptyList()
                viewModel.resetReviewSubmissionState()
            }
        }
    }
}

@Composable
fun RelatedProductsSection(
    products: List<ProductModel>,
    onProductClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Các sản phẩm khác Cung Loại",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Xem tất cả >",
                fontSize = 14.sp,
                color = MaterialTheme.colors.PrimaryColor,
                modifier = Modifier.clickable { /* TODO: Navigate to shop */ }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(products) { product ->
                RelatedProductItem(product = product, onClick = { onProductClick(product.id) })
            }
        }
    }
}

@Composable
fun RelatedProductItem(
    product: ProductModel,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick)
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        AsyncImage(
            model = product.images.firstOrNull(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = product.name,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val discountedPrice = product.price * (1 - product.discount.toFloat() / 100)
            Text(
                text = "${String.format("%,d", discountedPrice.toLong())}đ",
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Đã bán ${product.sold}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}
