package com.eritlab.jexmon.presentation.screens.product_detail_screen.component

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.eritlab.jexmon.R
import com.eritlab.jexmon.presentation.common.CustomDefaultBtn
import com.eritlab.jexmon.presentation.screens.product_detail_screen.ProductDetailViewModel
import com.eritlab.jexmon.presentation.ui.theme.PrimaryColor
import com.eritlab.jexmon.presentation.ui.theme.PrimaryLightColor
import com.eritlab.jexmon.presentation.ui.theme.TextColor
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.rememberCoroutineScope
import com.eritlab.jexmon.domain.model.ProductModel
import com.eritlab.jexmon.domain.model.ProductStock
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextDecoration
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProductDetailScreen(
    productId: String,
    popBack: () -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel() // ✅ Truyền ViewModel mặc định
) {
    ProductDetailContent(viewModel = viewModel, productId = productId, popBack = popBack)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailContent(
    viewModel: ProductDetailViewModel,
    productId: String,
    popBack: () -> Unit
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
                        .height(800.dp)
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
                                text = product.rating.toString(),
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
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD0011B) // Màu đỏ
                            )

                            // Giá gốc (gạch ngang)
                            Text(
                                text = "${String.format("%,d", product.price.toLong())}đdd",
                                fontSize = 18.sp,
                                color = Color.Gray,
                                textDecoration = TextDecoration.LineThrough // Gạch ngang giá gốc
                            )

                            // Phần trăm giảm giá
                            Text(
                                text = "-${discountText}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF8800) // Màu cam
                            )


                            Spacer(modifier = Modifier.weight(1f)) // Đẩy "Đã bán" sang bên phải

                            // Đã bán
                            Text(
                                text = "Đã bbán ${product.sold}",
                                fontSize = 16.sp,
                                color = Color.Gray
                            )

                        }
                        Spacer(modifier = Modifier.height(10.dp))



                        Text(
                            text = product.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 25.sp
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
                                fontSize = 19.sp,
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

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ship), // Icon giao hàng
                                    contentDescription = "Shipping Icon",
                                    tint = Color(0xFF4CAF50), // Màu xanh lá
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(
                                    text = "Nhận từ 25 Th03 - 25 Th03, phí giao đ0",
                                    fontSize = 19.sp,
                                    color = Color.Black
                                )
                            }

                            Text(
                                text = "Tặng Voucher đ15.000 nếu đơn giao sau thời gian trên.",
                                fontSize = 16.sp,
                                color = Color.Gray
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
                                text = product.rating.toString(),
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Image(
                                painter = painterResource(id = R.drawable.star_icon),
                                contentDescription = null
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(
                                text = "Đánh Giá Sản Phẩm",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(
                                text = "(206)",
                                fontSize = 17.sp,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = "Tất cảaa >",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                        }



                    }
                }
            }

            // 🔹 Nút thêm vào giỏ hàng
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        isSheetOpen = true
                        coroutineScope.launch {
                            sheetState.show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.PrimaryColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(15.dp))
                ) {
                    Text(
                        text = "Thêm vào giỏ hàng - ${String.format("%,d", product.price.toLong())}đ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
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
    onColorSelected: (String) -> Unit
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
        .fillMaxHeight(0.5f)
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
        Text("Chọn Sizee", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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

            Spacer(modifier = Modifier.width(150.dp)) // Tạo khoảng cách nhỏ giữa text và nút

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


        val context = LocalContext.current
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
                Toast.makeText(context, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
            },
        ) {
            Text("Xác nhận")
        }

    }

}

