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
    viewModel: ProductDetailViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel(),
    onNavigateToProduct: (String) -> Unit
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser == null) {
        // Hiển thị thông báo yêu cầu đăng nhập
        Toast.makeText(LocalContext.current, "Vui lòng đăng nhập để tiếp tục", Toast.LENGTH_SHORT).show()
        return
    }

    ProductDetailContent(
        viewModel = viewModel,
        cartViewModel = cartViewModel,
        productId = productId,
        popBack = popBack,
        onNavigateToCart = onNavigateToCart,
        userId = currentUser.uid,
        onNavigateToProduct = onNavigateToProduct
    )
    //gọi thêm hàm bottom sheet

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailContent(
    viewModel: ProductDetailViewModel,
    cartViewModel: CartViewModel,
    productId: String,
    popBack: () -> Unit,
    onNavigateToCart: () -> Unit,
    userId: String,
    onNavigateToProduct: (String) -> Unit
)
{
    val sheetState = rememberModalBottomSheetState(

        skipPartiallyExpanded = true)  // ✅ Sửa lỗi
    val coroutineScope = rememberCoroutineScope()
    val state = viewModel.state.value
    val context = LocalContext.current
    var isSheetOpen by rememberSaveable  { mutableStateOf(false) }  // Dùng để kiểm soát trạng thái mở sheet
    LaunchedEffect(productId) {
        if (!productId.isNullOrEmpty()) {
            viewModel.getProductDetail(productId)
        } else {
            Log.e("ProductDetailScreen", "Lỗi: productId rỗng hoặc null!")
        }
    }

    val currentUser = FirebaseAuth.getInstance().currentUser // Lấy User Auth hiện tại
    val currentUserId = currentUser?.uid // UID người dùng (String?)

    // --- 1. Khai báo biến State để giữ tên lấy từ Callback ---
    // Tên này sẽ là kết quả fetch từ Firestore thông qua callback
    var fetchedNameFromCallback by remember { mutableStateOf<String?>(null) }
    // Bạn có thể thêm state cho loading/error nếu muốn hiển thị trạng thái fetch tên này riêng

    // --- 2. Sử dụng LaunchedEffect để gọi Callback function ---
    // Key = currentUserId: effect sẽ chạy (hoặc chạy lại) khi currentUserId thay đổi
    LaunchedEffect(currentUserId) {
        // Chỉ gọi khi có User ID và tên chưa được fetch (hoặc cần refresh)
        if (currentUserId != null && fetchedNameFromCallback == null) { // Thêm điều kiện kiểm tra fetchedNameFromCallback
            Log.d("Composable", "Triggering getUserNameByIdCallback for UID: $currentUserId")
            viewModel.getUserNameByIdCallback( // Gọi hàm callback
                userId = currentUserId,
                onSuccess = { name ->
                    // --- 3. Cập nhật State khi Callback thành công ---
                    fetchedNameFromCallback = name // Lưu tên lấy được (có thể là null) vào state cục bộ
                    Log.d("Composable", "Callback success, fetched name: $name. State updated.")
                    // Việc cập nhật state này sẽ kích hoạt recompose Composable
                },
                onFailure = { e ->
                    // --- Xử lý lỗi khi Callback thất bại ---
                    Log.e("Composable", "Callback failed to fetch name", e)
                    fetchedNameFromCallback = null // Có thể set null hoặc một giá trị báo lỗi đặc biệt
                    // Bạn cũng có thể hiển thị Toast/SnackBar lỗi ở đây
                }
            )
        } else if (currentUserId == null) {
            // Reset state nếu người dùng logout khi màn hình đang hiển thị
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
        var isExpanded by rememberSaveable  { mutableStateOf(false) }  // ✅ Thêm nhớ trạng thái mở rộng / thu gọn
        // Lấy state từ ViewModel
        val state = viewModel.state.value

        // Biến trạng thái UI để theo dõi xem có hiển thị phần đánh giá không
        var showReviewsSection by remember { mutableStateOf(false) }
        var showReviewInputForm by remember { mutableStateOf(false) }

        // Gọi lấy sản phẩm liên quan khi có thông tin sản phẩm
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
                        .verticalScroll(scrollState),  // Cuộn mượt mà không bị lỗi                    ,

                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 🔹 Thanh tiêu đề
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
                                text = String.format("%.1f", product.rating), //định dạng hiển thị 1 số thập phân sau dấu phẩy
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Image(
                                painter = painterResource(id = R.drawable.star_icon),
                                contentDescription = null
                            )
                        }
                    }

                    // 🔹 Hình ảnh sản phẩm
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


                    // 🔹 Thông tin sản phẩm
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, shape = RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp))
                            .padding(15.dp)
                    ) {
                        val discountText = "${product.discount.toInt()}%" // Luôn bỏ phần thập phân

                        val discountedPrice = product.price * (1 - product.discount.toFloat() / 100)
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp) // Điều chỉnh khoảng cách hợp lý
                        ) {
                            // Giá giảm
                            Text(
                                text = "${String.format("%,d", discountedPrice.toLong())}đ",
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD0011B) // Màu đỏ
                            )

                            // Giá gốc (gạch ngang)
                            Text(
                                text = "${String.format("%,d", product.price.toLong())}đ",
                                fontSize = 15.sp,
                                color = Color.Gray,
                                textDecoration = TextDecoration.LineThrough // Gạch ngang giá gốc
                            )

                            // Phần trăm giảm giá
//                            Text(
//                                text = "-${discountText}",
//                                fontSize = 18.sp,
//                                fontWeight = FontWeight.Bold,
//                                color = Color(0xFFFF8800) // Màu cam
//                            )


                            Spacer(modifier = Modifier.weight(1f)) // Đẩy "Đã bán" sang bên phải

                            // Đã bán
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
                            color = Color.LightGray, // Màu viền
                            thickness = 0.5.dp, // Độ dày viền
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
                                modifier = Modifier.weight(1f) // Cho phép chiếm phần lớn diện tích
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
                            color = Color.LightGray, // Màu viền
                            thickness = 0.5.dp, // Độ dày viền
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        Column {
                            Spacer(modifier = Modifier.height(4.dp)) // Khoảng cách nhỏ

                            Row(
                                verticalAlignment = Alignment.Top, // Cho icon và chữ căn hàng trên cùng
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Icon bên trái
                                Icon(
                                    painter = painterResource(id = R.drawable.ship),
                                    contentDescription = "Shipping Icon",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(top = 2.dp) // Căn chỉnh nhỏ nếu cần
                                )

                                Spacer(modifier = Modifier.width(8.dp)) // Khoảng cách giữa icon và nội dung

                                // Nội dung chữ bên phải
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
                            color = Color.LightGray, // Màu viền
                            thickness = 0.5.dp, // Độ dày viền
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.Top, // Cho icon và chữ căn hàng trên cùng
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Icon bên trái
                            Image(
                                painter = painterResource(id = R.drawable.icon_doi_tra_hang),
                                contentDescription = "Return Package Icon",
                                modifier = Modifier
                                    .size(21.dp)
                                    .padding(top = 2.dp) // Căn chỉnh nhỏ nếu cần
                            )

                            Spacer(modifier = Modifier.width(8.dp)) // Khoảng cách giữa icon và nội dung

                            // Nội dung chữ bên phải
                            Text(
                                text = "Trả hàng miễn phí 15 ngày",
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                        }

                        Divider(
                            color = Color.LightGray, // Màu viền
                            thickness = 0.5.dp, // Độ dày viền
                            modifier = Modifier.padding(vertical = 10.dp)
                        )


                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = String.format("%.1f", product.rating), //định dạng hiển thị 1 số thập phân sau dấu phẩy
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

                            Spacer(modifier = Modifier.weight(1f)) // Đẩy Text "Tất cảaa >" sang phải

                            // Gắn clickable modifier vào Text "Tất cảaa >"
                            Text(
                                text = "Tất cả >",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray,
                                modifier = Modifier.clickable {
                                    // Khi bấm vào Text này, đảo ngược trạng thái hiển thị phần đánh giá
                                    showReviewsSection = !showReviewsSection
                                    // *Lưu ý:* Nếu bạn chọn chỉ tải bình luận khi bấm lần đầu,
                                    // thì gọi viewModel.getReviews(productId) ở đây nếu showReviewsSection vừa chuyển sang true.
                                    // Tuy nhiên, tải ngay khi load màn hình thường cho trải nghiệm tốt hơn.
                                }
                            )
                        }

                        // Reviews section
                        if (!state.isLoadingReviews) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                var showAllReviews by remember { mutableStateOf(false) }
                                var showReviewInputForm by remember { mutableStateOf(false) }
                                val reviewsToShow = if (showAllReviews) state.reviews else state.reviews.take(3)

                                // Write Review Button at the top
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

                                // Review Input Form
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

                                // Existing reviews
                                reviewsToShow.forEach { review ->
                                    ReviewItem(review = review)
                                }

                                // Show More/Less buttons (existing code)
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



                        Spacer(modifier = Modifier.height(8.dp)) // Khoảng cách

                        // --- GỌI COMposable ReviewInputForm DƯỚI ĐÂY (điều kiện) ---
                        if (showReviewInputForm) {
                            val currentProductId = state.productDetail?.id ?: ""

                            // --- XÁC ĐỊNH TÊN TÁC GIẢ ĐỂ TRUYỀN VÀO FORM ---
                            // Logic này chạy mỗi khi Composable recompose, sử dụng giá trị MỚI NHẤT của các nguồn tên.
                            val authorNameToShow = when {
                                // 1. Ưu tiên tên lấy từ Callback và lưu trong State CỤC BỘ này (nếu có và không rỗng)
                                // Giá trị này được cập nhật bởi LaunchedEffect.
                                !fetchedNameFromCallback.isNullOrBlank() -> fetchedNameFromCallback!! // !! an toàn sau isNullOrBlank()
                                // 2. Nếu tên từ callback là null/rỗng, thử dùng tên hiển thị từ Firebase Auth
                                !currentUser?.displayName.isNullOrBlank() -> currentUser!!.displayName!! // !! an toàn sau isNullOrBlank
                                // 3. Cuối cùng, nếu cả hai cách trên đều không có tên, dùng tên mặc định
                                else -> "Người dùng ẩn danh"
                            }

                            // Kiểm tra điều kiện CHÍNH để hiển thị Form
                            if (currentProductId.isNotBlank() && currentUserId != null) {
                                Log.d("ProductDetail", "Showing Review Input Form for Product: $currentProductId, User ID: $currentUserId")
                                Log.d("ProductDetail", "Using Author Name: $authorNameToShow") // Log tên đã xác định

                                ReviewInputForm(
                                    viewModel = viewModel,
                                    productId = currentProductId,
                                    userId = currentUserId,
                                    authorName = authorNameToShow // Truyền tên đã xác định (đã xử lý null)
                                )
                            } else {
                                // Nếu một trong hai (hoặc cả hai) điều kiện trên KHÔNG đúng, HIỂN THỊ THÔNG BÁO
                                Log.d("ProductDetail", "Conditions NOT met to show Review Input Form. Product ID Blank: ${currentProductId.isBlank()}, User ID Null: ${currentUserId == null}")

                                Text(
                                    // Hiển thị thông báo phù hợp dựa trên lý do tại sao không hiển thị form
                                    text = when {
                                        currentProductId.isBlank() -> "Không thể tải thông tin sản phẩm để viết đánh giá." // Thiếu Product ID
                                        currentUserId == null -> "Vui lòng đăng nhập để viết đánh giá." // Thiếu User ID
                                        else -> "Không thể hiển thị form đánh giá." // Trường hợp khác (ít xảy ra với logic trên)
                                    },
                                    color = Color.Gray,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        // --- KẾT THÚC phần ReviewInputForm ---
                        // Thêm vào ProductDetailContent sau phần reviews
// ... trong ProductDetailContent ...
                            // ... phần reviews hiện tại ...

                            Divider(
                                color = Color.LightGray,
                                thickness = 8.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            // Thêm phần sản phẩm liên quan
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

            //Nút thêm vào giỏ hàng và mua ngay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .height(56.dp) // ✅ Tăng chiều cao nút lên
            ) {
                // 🔹 Nút "Thêm vào giỏ hàng" (chiếm 2 phần)
                Button(
                    onClick = {
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
                        .fillMaxHeight() // ✅ Đảm bảo chiếm hết chiều cao Row
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.cart),
                        contentDescription = "Giỏ hàng",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 🔹 Nút "Mua với voucher" (chiếm 3 phần)
                Button(
                    onClick = {
                        // Action mua hàng
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

        // day khong lien quan
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
                    cartViewModel = cartViewModel
                )
            }
        }




        LaunchedEffect(selectedSize) {
            availableColors = product.stock
                .filter { it.size == selectedSize && it.quantity > 0 }
                .mapNotNull { convertToHex(it.color) }
                .distinct()
            Log.d("ProductDetail", "Available Colors: $availableColors")

            selectedColor = availableColors.firstOrNull() // Đặt màu đầu tiên hợp lệ nếu có
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

    return colorMap[color.lowercase()] ?: color // Nếu không tìm thấy, giả định nó là mã hex
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

    return colorMap[hex.uppercase()] ?: hex // Nếu không tìm thấy, giữ nguyên mã hex
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
    cartViewModel: CartViewModel = hiltViewModel()
) {
    val db = FirebaseFirestore.getInstance()
    var stockQuantity by rememberSaveable  { mutableStateOf<Int?>(null) }

    // Khi selectedSize và selectedColor thay đổi, cập nhật số lượng từ Firestore
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
        // Hàng chứa ảnh sản phẩm và thông tin
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ảnh sản phẩm
            Image(
                painter = rememberAsyncImagePainter(product.images[0]),
                contentDescription = "Product Image",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            // Cột chứa thông tin sản phẩm
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
            color = Color.LightGray, // Màu viền
            thickness = 0.5.dp, // Độ dày viền
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
            color = Color.LightGray, // Màu viền
            thickness = 0.9.dp, // Độ dày viền
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
            color = Color.LightGray, // Màu viền
            thickness = 0.5.dp, // Độ dày viền
            modifier = Modifier.padding(vertical = 10.dp)
        )

        var quantity by rememberSaveable  { mutableStateOf(1) }
        Row(
            modifier = Modifier.fillMaxWidth(), // Đảm bảo hàng chiếm toàn bộ chiều rộng
            horizontalArrangement = Arrangement.Center, // Căn giữa các phần tử
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Chọn Số Lượng:", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Spacer(modifier = Modifier.width(100.dp)) // Tạo khoảng cách nhỏ giữa text và nút

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

        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid
        Log.d("BottomSheet", "Current User UID: $uid")

        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

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
                val cartItem = CartItem(
                    userId = currentUser!!.uid,
                    productId = product.id,
                    name = product.name,
                     price = floor(product.price * (1 - product.discount.toFloat() / 100)),
                    priceBeforeDiscount = product.price,
                    size = selectedSize,
                    color = selectedColor ?: "",
                    quantity = quantity,
                    imageUrl = product.images.firstOrNull() ?: "",
                    discount = product.discount

                )

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
        ) {
            Text("Xác nhận")
        }


    }

}

// 🔹 Composable để hiển thị một mục bình luận (Sử dụng ReviewModel)
@Composable
fun ReviewItem(review: ReviewModel) {
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var userName by remember { mutableStateOf<String?>(null) }
    val db = FirebaseFirestore.getInstance()

    // Lấy avatarUrl và name từ Firestore theo userId
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
        // User info row with avatar, name, and rating
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Image(
                painter = rememberAsyncImagePainter(avatarUrl ?: R.drawable.user),
                contentDescription = "User Avatar",
                modifier = Modifier
                    .size(35.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Name and rating in a column
            Column {
                Text(
                    text = userName ?: review.userName, // Ưu tiên tên lấy từ Firestore, fallback sang tên trong review
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

        // Review content with proper spacing
        if (review.comment.isNotEmpty()) {
            Text(
                text = review.comment,
                fontSize = 14.sp,
                color = Color.Black,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Images in a row (if any)
        // Images in a row (if any)
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

    // Image picker launcher
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

        // Star rating selection
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

        // Review content input
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

        // Image upload button
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

        // Image preview
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
                        // Delete button
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

        // Submit button
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

        // Error message
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

        // Success message
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

// Thêm composable cho sản phẩm liên quan
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
        // Tiêu đề
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

        // Grid sản phẩm
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
        // Ảnh sản phẩm
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

        // Tên sản phẩm
        Text(
            text = product.name,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Giá và đã bán
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
