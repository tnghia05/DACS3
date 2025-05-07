package com.eritlab.jexmon.presentation.screens.product_detail_screen.component

// ... các imports khác từ Jetpack Compose, Material ...
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ProductDetailScreen(
    productId: String,
    popBack: () -> Unit,
    onNavigateToCart: () -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel()
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
        userId = currentUser.uid
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
    userId: String
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
                    selectedPicture?.let { image ->
                        Image(
                            painter = rememberAsyncImagePainter(image),
                            contentDescription = null,
                            modifier = Modifier.size(250.dp)
                        )
                    } ?: Text("Không có ảnh sản phẩm", color = Color.Gray)


                    if (product.images.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(50.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            product.images.forEach { image ->
                                IconButton(
                                    onClick = { selectedPicture = image },
                                    modifier = Modifier
                                        .size(18.dp)
                                        .border(
                                            width = 1.dp,
                                            color = if (selectedPicture == image) MaterialTheme.colors.primary else Color.Transparent,
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

                            Text(
                                // *Cải tiến:* Sử dụng kích thước thực tế của danh sách đánh giá từ state
                                // Thay vì số cứng "(206)"
                                text = "(${state.reviews.size})",
                                fontSize = 14.5.sp,
                                color = Color.Black
                            )

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
                        if (showReviewsSection) {
                            // Logic hiển thị danh sách bình luận dựa vào state.reviews
                            // Bao gồm xử lý loading, error, empty list
                            when {
                                state.isLoadingReviews -> {
                                    CircularProgressIndicator() // Hoặc Text("Đang tải bình luận...")
                                }
                                state.errorLoadingReviews != null -> {
                                    Text("Lỗi tải bình luận: ${state.errorLoadingReviews}")
                                }
                                state.reviews.isEmpty() -> {
                                    // Chỉ hiển thị nếu không trong trạng thái loading hoặc error
                                    if (!state.isLoadingReviews && state.errorLoadingReviews == null) {
                                        Text("Chưa có bình luận nào cho sản phẩm này.")
                                        Button(
                                            onClick = { showReviewInputForm = !showReviewInputForm }, // Bấm để đóng/mở form nhập liệu
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp) // Thêm padding ngang
                                        ) {
                                            Text(if (showReviewInputForm) "Ẩn Form Viết Đánh Giá" else "Viết đánh giá")
                                        }
                                    }
                                }
                                else -> {
                                    // Hiển thị danh sách bình luận thực tế
                                    Column { // Cân nhắc LazyColumn nếu danh sách dài
                                        state.reviews.forEach { review ->
                                            ReviewItem(review = review) // Composable hiển thị 1 bình luận
                                            Divider()
                                        }
                                        Button(
                                            onClick = { showReviewInputForm = !showReviewInputForm }, // Bấm để đóng/mở form nhập liệu
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp) // Thêm padding ngang
                                        ) {
                                            Text(if (showReviewInputForm) "Ẩn Form Viết Đánh Giá" else "Viết đánh giá")
                                        }
                                    }
                                }
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Mua với voucher", fontSize = 14.sp)
                        Text("₫363.636", fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                    onColorSelected = { selectedColor = it }
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
                            if (selectedColor == color) MaterialTheme.colors.primary else Color.Transparent,
                            CircleShape
                        )
                        .background(
                            Color(android.graphics.Color.parseColor(color)), CircleShape
                        )
                        .clip(CircleShape)
                        .clickable { onColorSelected(color) }
                )
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
                    price = product.price,
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
fun ReviewItem(review: ReviewModel) { // Nhận ReviewModel
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Ảnh đại diện người dùng (sử dụng ImageUrl)
            Image(
                painter = rememberAsyncImagePainter( R.drawable.user), // Thay R.drawable.default_avatar bằng ảnh default của bạn
                contentDescription = "User Avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = review.userName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Hiển thị số sao đánh giá
                    repeat(5) { index ->
                        Icon(
                            painter = painterResource(id = if (index < review.rating) R.drawable.star_icon else R.drawable.star__1_),
                            contentDescription = "Star Rating",
                            tint = if (index < review.rating) Color(0xFFFFC107) else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Hiển thị thời gian bình luận (chuyển Timestamp sang Date)
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    Text(
                        text = dateFormat.format(review.createdAt.toDate()), // Chuyển Timestamp sang Date để format
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Nội dung bình luận
        Text(
            text = review.comment,
            fontSize = 15.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = Color.LightGray, thickness = 0.5.dp)
    }
}@Composable
fun ReviewInputForm(
    viewModel: ProductDetailViewModel, // Truyền ViewModel vào
    productId: String, // ID sản phẩm hiện tại
    userId: String, // ID người dùng hiện tại (lấy từ Firebase Auth)
    authorName: String // Tên người dùng hiện tại
) {
    // --- Trạng thái UI cho Form nhập liệu ---
    var reviewContent by remember { mutableStateOf("") }
    var selectedRating by remember { mutableStateOf(0) } // 0 sao ban đầu
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val reviewSubmissionState = viewModel.reviewSubmissionState.value // Lấy trạng thái gửi từ ViewModel

    val context = LocalContext.current

    // Launcher để chọn nhiều ảnh
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        selectedImageUris = uris // Cập nhật danh sách Uri ảnh đã chọn
    }

    // Launcher để yêu cầu quyền đọc bộ nhớ (nếu cần thiết cho API level cũ hơn)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            imagePickerLauncher.launch("image/*") // Nếu được cấp quyền, mở trình chọn ảnh
        } else {
            // Xử lý khi người dùng từ chối quyền
            // Có thể hiển thị SnackBar hoặc Dialog thông báo
        }
    }

    // --- Giao diện Form ---
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)) // Thêm viền cho dễ nhìn
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

        // Chọn số sao
        Text("Chọn số sao:", fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center // Căn giữa các ngôi sao
        ) {
            for (i in 1..5) {
                Icon(
                    painter = painterResource(id = if (i < selectedRating) R.drawable.star_icon else R.drawable.star__1_),

                    contentDescription = "$i Star",
                    tint = if (i <= selectedRating) Color(0xFFFFD700) else Color.Gray, // Màu vàng nếu được chọn
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { selectedRating = i } // Bấm để chọn sao
                )
                if (i < 5) Spacer(modifier = Modifier.width(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nhập nội dung đánh giá
        OutlinedTextField(
            value = reviewContent,
            onValueChange = { reviewContent = it },
            label = { Text("Nội dung đánh giá") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp) // Chiều cao cho nội dung
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Nút chọn ảnh
        Button(
            onClick = {
                // Kiểm tra quyền đọc bộ nhớ trước khi mở trình chọn ảnh
                when {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE // Quyền đọc bộ nhớ cũ
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        // Quyền đã được cấp, mở trình chọn ảnh
                        imagePickerLauncher.launch("image/*")
                    }
                    // ShouldShowRequestPermissionRationale giải thích lý do cần quyền (tùy chọn)
                    // ActivityCompat.shouldShowRequestPermissionRationale(...) -> {
                    //    // Hiển thị dialog giải thích lý do
                    // }
                    else -> {
                        // Yêu cầu quyền
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
                // *Lưu ý:* Với Android 10+ (API 29+), quyền READ_EXTERNAL_STORAGE có thể không cần thiết
                // nếu bạn chỉ dùng Storage Access Framework (như GetMultipleContents).
                // Tuy nhiên, kiểm tra quyền vẫn là practice tốt hoặc nếu hỗ trợ API cũ hơn.
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Select Images")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Chọn ảnh đính kèm (${selectedImageUris.size})")
        }

        // Hiển thị ảnh đã chọn
        if (selectedImageUris.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedImageUris) { uri ->
                    AsyncImage(
                        model = uri, // Uri của ảnh đã chọn
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .aspectRatio(1f),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nút gửi đánh giá
        Button(
            onClick = {
                // Gọi hàm submitReview trong ViewModel
                viewModel.submitReview(
                    productId = productId,
                    userId = userId, // Đảm bảo bạn lấy được userId của người dùng hiện tại
                    authorName = authorName, // Đảm bảo bạn lấy được tên người dùng
                    rating = selectedRating, // Chuyển Int rating sang Float
                    content = reviewContent,
                    imageUris = selectedImageUris
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !reviewSubmissionState.isSubmitting && selectedRating > 0 && reviewContent.isNotBlank()
            // Nút chỉ bật khi không đang gửi, đã chọn sao và đã nhập nội dung
        ) {
            if (reviewSubmissionState.isSubmitting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Gửi đánh giá")
            }
        }

        // Hiển thị trạng thái gửi (thành công, lỗi)
        reviewSubmissionState.submitError?.let { error ->
            Text(
                text = error,
                color = Color.Red,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
            // Reset trạng thái lỗi sau khi hiển thị
            LaunchedEffect(error) {
                // Bạn có thể dùng SnackBar thay cho Text trực tiếp
                // Sau một khoảng thời gian, reset state
                // delay(3000) // Delay 3 giây
                // viewModel.resetReviewSubmissionState()
            }
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
            // Reset form và trạng thái thành công sau khi hiển thị
            LaunchedEffect(true) { // Dùng key true để chạy 1 lần khi success là true
                // delay(3000) // Delay 3 giây
                reviewContent = ""
                selectedRating = 0
                selectedImageUris = emptyList()
                viewModel.resetReviewSubmissionState() // Reset trạng thái trong ViewModel
            }
        }
    }
}