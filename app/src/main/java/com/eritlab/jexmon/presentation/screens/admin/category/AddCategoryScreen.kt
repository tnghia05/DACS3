package com.eritlab.jexmon.presentation.screens.admin.category

// --- Import cần thiết ---
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.eritlab.jexmon.domain.model.BrandModel
import com.eritlab.jexmon.domain.model.ProductModel
import com.eritlab.jexmon.domain.model.ProductStock
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID


// --- TODO: Đảm bảo bạn có các định nghĩa sau trong các file phù hợp ---
// Data class ProductStock (từ package com.eritlab.jexmon.domain.model)
// Data class BrandModel (từ package com.eritlab.jexmon.domain.model)
// Data class ProductModel (từ package com.eritlab.jexmon.domain.model) - Đảm bảo khớp với các trường dưới đây


// Hàm lưu sản phẩm chính và stock (Sử dụng await)

@Composable
fun AddProductScreen(navController: NavController, productId: String? = null) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val storage = remember { FirebaseStorage.getInstance() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- State variables cho thông tin sản phẩm ---
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }
    var price by remember { mutableStateOf(TextFieldValue("")) }
    var discount by remember { mutableStateOf(TextFieldValue("")) }
    var rating by remember { mutableStateOf(TextFieldValue("")) }
    var imageUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var newImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // --- State cho Brand ---
    val brandList = remember { mutableStateOf<List<BrandModel>>(emptyList()) }
    val selectedBrand = remember { mutableStateOf<BrandModel?>(null) }

    // --- State cho danh sách stock ---
    val stockList = remember { mutableStateListOf<ProductStock>() }

    // --- LaunchedEffect để Load dữ liệu khi ở chế độ chỉnh sửa ---
    LaunchedEffect(productId) {
        if (productId != null) {
            try {
                // Load dữ liệu sản phẩm chính
                val productDoc = firestore.collection("products").document(productId).get().await()
                val product = productDoc.toObject(ProductModel::class.java)
                
                if (product != null) {
                    // Cập nhật các state với dữ liệu sản phẩm
                    name = TextFieldValue(product.name)
                    description = TextFieldValue(product.description)
                    price = TextFieldValue(product.price.toString())
                    discount = TextFieldValue(product.discount.toString())
                    rating = TextFieldValue(product.rating.toString())
                    imageUrls = product.images

                    // Load brands và tìm brand của sản phẩm
                    val brandsSnapshot = firestore.collection("brands").get().await()
                    val brands = brandsSnapshot.documents.mapNotNull { it.toObject(BrandModel::class.java) }
                    brandList.value = brands
                    selectedBrand.value = brands.find { it.id == product.brandId }

                    // Load stock
                    val stockSnapshot = firestore.collection("products")
                        .document(productId)
                        .collection("stock")
                        .get()
                        .await()
                    
                    stockList.clear()
                    stockSnapshot.documents.mapNotNull { 
                        it.toObject(ProductStock::class.java)
                    }.let { stocks ->
                        stockList.addAll(stocks)
                    }
                    
                    // Nếu không có stock nào, thêm một dòng trống
                    if (stockList.isEmpty()) {
                        stockList.add(ProductStock())
                    }
                }
            } catch (e: Exception) {
                Log.e("AddProductScreen", "Error loading product data", e)
                Toast.makeText(context, "Lỗi khi tải dữ liệu sản phẩm", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Nếu là thêm mới, khởi tạo một stock trống
            stockList.clear()
            stockList.add(ProductStock())
            
            // Load danh sách brands
            try {
                val brandsSnapshot = firestore.collection("brands").get().await()
                val brands = brandsSnapshot.documents.mapNotNull { it.toObject(BrandModel::class.java) }
                brandList.value = brands
                if (brands.isNotEmpty()) {
                    selectedBrand.value = brands[0]
                }
            } catch (e: Exception) {
                Log.e("AddProductScreen", "Error loading brands", e)
            }
        }
    }

    // --- Image Picker Launcher (Cho phép chọn nhiều ảnh) ---
    val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        newImageUris = uris // Cập nhật state newImageUris
    }

    // --- Layout chính của màn hình ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .height(800.dp) // Thay đổi chiều cao của Column
            .verticalScroll(rememberScrollState()), // <-- Thêm Modifier cuộn dọc
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(if (productId == null) "Thêm Sản Phẩm " else "Chỉnh Sửa Sản Phẩm", fontSize = 24.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(16.dp))

        // --- Các trường nhập thông tin sản phẩm ---
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Tên Sản Phẩm") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Brand Dropdown Menu
        BrandDropdownMenu(
            brandList = brandList.value,
            selectedBrand = selectedBrand.value,
            onBrandSelected = { selectedBrand.value = it }
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Mô tả") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = price,
            onValueChange = { price = it },
            label = { Text("Giá Sản Phẩm") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = discount,
            onValueChange = { discount = it },
            label = { Text("Giảm Giá (%) ") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = rating,
            onValueChange = { rating = it },
            label = { Text("Đánh Giá") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(16.dp)) // Thêm khoảng trống trước phần stock

        // --- Tích hợp StockInputSection TẠI ĐÂY ---
        StockInputSection(
            stockList = stockList,
            onRemoveItem = { itemToRemove -> stockList.remove(itemToRemove) },
            onAddItem = { stockList.add(ProductStock()) }
        )
        // --- KẾT THÚC TÍCH HỢP StockInputSection ---

        Spacer(modifier = Modifier.height(16.dp)) // Khoảng trống sau phần stock

        // --- Chọn ảnh --- // Chỉ MỘT LẦN
        Button(onClick = { imagePickerLauncher.launch("image/*") }) { // "image/*" cho phép chọn bất kỳ loại ảnh
            Text("Chọn ảnh")
        }

        // --- Hiển thị các ảnh đã chọn mới và ảnh cũ --- // Chỉ MỘT LẦN
        if (newImageUris.isNotEmpty() || imageUrls.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row( // Sử dụng Row với cuộn ngang
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp)
            ) {
                imageUrls.forEach { url -> // Hiển thị ảnh cũ
                    Image(
                        painter = rememberAsyncImagePainter(url),
                        contentDescription = "Product Image",
                        modifier = Modifier
                            .size(100.dp)
                            .padding(horizontal = 4.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                newImageUris.forEach { uri -> // Hiển thị ảnh mới chọn
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .size(100.dp)
                            .padding(horizontal = 4.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp)) // Khoảng trống trước nút Lưu

        // --- Nút Lưu Sản phẩm (Gọi hàm await) ---
        Button(
            onClick = {
                if (!isLoading) {
                    coroutineScope.launch {
                        isLoading = true
                        try {
                            // 1. Validate dữ liệu nhập liệu cơ bản
                            if (name.text.isBlank()) throw Exception("Tên sản phẩm không được trống")
                            if (description.text.isBlank()) throw Exception("Mô tả không được trống")
                            val parsedPrice = price.text.toDoubleOrNull() ?: throw Exception("Giá sản phẩm không hợp lệ")
                            if (parsedPrice < 0) throw Exception("Giá sản phẩm phải lớn hơn 0")
                            val parsedDiscount = discount.text.toIntOrNull() ?: throw Exception("Giảm giá không hợp lệ")
                            if (parsedDiscount < 0 || parsedDiscount > 100) throw Exception("Giảm giá phải từ 0-100%")
                            val parsedRating = rating.text.toDoubleOrNull() ?: throw Exception("Đánh giá không hợp lệ")
                            if (parsedRating < 0 || parsedRating > 5) throw Exception("Đánh giá phải từ 0-5 sao")
                            if (selectedBrand.value?.id == null) throw Exception("Vui lòng chọn nhãn hàng")

                            // 2. Upload ảnh mới nếu có
                            val uploadedImageUrls = mutableListOf<String>()
                            uploadedImageUrls.addAll(imageUrls)

                            newImageUris.forEach { uri ->
                                val storageRef = storage.reference.child("product_images/${UUID.randomUUID()}")
                                storageRef.putFile(uri).await()
                                val downloadUrl = storageRef.downloadUrl.await().toString()
                                uploadedImageUrls.add(downloadUrl)
                            }

                            // 3. Lấy hoặc tạo document reference
                            val productDocRef = if (productId != null) {
                                firestore.collection("products").document(productId)
                            } else {
                                firestore.collection("products").document()
                            }

                            // 4. Lấy dữ liệu sản phẩm hiện tại nếu đang cập nhật
                            val existingProduct = if (productId != null) {
                                firestore.collection("products").document(productId).get().await()
                                    .toObject(ProductModel::class.java)
                            } else null

                            // 5. Tạo dữ liệu sản phẩm mới
                            val productData = hashMapOf(
                                "id" to productDocRef.id,
                                "brandId" to selectedBrand.value?.id,
                                "categoryId" to (existingProduct?.categoryId ?: "default"),
                                "createdAt" to (existingProduct?.createdAt ?: Timestamp.now()),
                                "description" to description.text,
                                "discount" to parsedDiscount.toDouble(),
                                "images" to uploadedImageUrls,
                                "isFavourite" to (existingProduct?.isFavourite ?: false),
                                "name" to name.text,
                                "price" to parsedPrice,
                                "rating" to parsedRating,
                                "slug" to name.text.trim().replace(" ", "-").toLowerCase(),
                                "sold" to (existingProduct?.sold ?: 0)
                            )

                            // 6. Lưu sản phẩm
                            productDocRef.set(productData).await()

                            // 7. Cập nhật stock
                            val stockCollectionRef = productDocRef.collection("stock")
                            
                            // Xóa stock cũ
                            val oldStockSnapshot = stockCollectionRef.get().await()
                            val deleteBatch = firestore.batch()
                            oldStockSnapshot.documents.forEach { doc ->
                                deleteBatch.delete(doc.reference)
                            }
                            deleteBatch.commit().await()

                            // Thêm stock mới
                            val stockBatch = firestore.batch()
                            stockList.forEach { stock ->
                                if (stock.size > 0 && stock.color.isNotBlank() && stock.quantity >= 0) {
                                    val stockDocumentId = "size_${stock.size}_${stock.color.replace(Regex("[.#\\[\\]*~/ ]"), "_")}"
                                    val stockDocRef = stockCollectionRef.document(stockDocumentId)
                                    val stockDataMap = mapOf(
                                        "id" to (stock.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()),
                                        "size" to stock.size,
                                        "color" to stock.color,
                                        "quantity" to stock.quantity
                                    )
                                    stockBatch.set(stockDocRef, stockDataMap)
                                }
                            }
                            stockBatch.commit().await()

                            // Thành công
                            Toast.makeText(
                                context,
                                if (productId == null) "Thêm sản phẩm thành công" else "Cập nhật sản phẩm thành công",
                                Toast.LENGTH_SHORT
                            ).show()
                            navController.popBackStack()

                        } catch (e: Exception) {
                            Log.e("AddProductScreen", "Error saving product", e)
                            Toast.makeText(context, e.message ?: "Lỗi khi lưu sản phẩm", Toast.LENGTH_SHORT).show()
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text(if (productId == null) "Thêm Sản Phẩm" else "Lưu Thay Đổi", color = Color.White)
            }
        }

        // --- Nút Xóa Sản phẩm (Chỉ hiện khi chỉnh sửa) ---
        if (productId != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (!isLoading) { // Ngăn double click khi đang xóa
                        coroutineScope.launch {
                            isLoading = true // Cập nhật trạng thái loading
                            deleteProductFromFirestore( // <-- Gọi hàm xóa sản phẩm đúng
                                productId = productId,
                                firestore = firestore,
                                onSuccess = {
                                    isLoading = false
                                    Toast.makeText(context, "Xóa sản phẩm thành công", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                },
                                onFailure = { errorMessage ->
                                    isLoading = false
                                    Toast.makeText(context, "Lỗi xóa sản phẩm: $errorMessage", Toast.LENGTH_SHORT).show()
                                    Log.e("AddProductScreen", "Delete failed: $errorMessage")
                                }
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red),
                enabled = !isLoading // Vô hiệu hóa nút khi đang loading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Xóa Sản Phẩm", color = Color.White) // Đổi tên nút
                }
            }
        }
    } // <-- Kết thúc của Column
} // <-- Kết thúc của Composable AddProductScreen


// --- Định nghĩa các hàm và Composable cần thiết khác ---

// Hàm lưu sản phẩm chính và stock (Sử dụng await) - Đã hoàn thiện ở các câu trả lời trước
suspend fun saveProductAndStock(
    productId: String?, // null nếu thêm mới
    name: String,
    description: String,
    price: String, // Giá trị String từ UI
    discount: String, // Giá trị String từ UI
    rating: String, // Giá trị String từ UI
    newImageUris: List<Uri>, // Danh sách Uri ảnh mới
    currentImageUrls: List<String>, // Danh sách Url ảnh cũ
    brandId: String?, // ID brand từ selectedBrand
    stockList: List<ProductStock>, // Danh sách stock
    firestore: FirebaseFirestore,
    storage: FirebaseStorage,
    context: Context,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    try {
        // 1. Validate dữ liệu nhập liệu cơ bản
        if (name.isBlank()) { onFailure("Tên sản phẩm không được trống"); return }
        if (description.isBlank()) { onFailure("Mô tả không được trống"); return }
        val parsedPrice = price.toDoubleOrNull()
        if (parsedPrice == null || parsedPrice < 0) { onFailure("Giá sản phẩm không hợp lệ"); return }
        val parsedDiscount = discount.toIntOrNull()
        if (parsedDiscount == null || parsedDiscount < 0 || parsedDiscount > 100) { onFailure("Giảm giá không hợp lệ"); return }
        val parsedRating = rating.toDoubleOrNull()
        if (parsedRating == null || parsedRating < 0 || parsedRating > 5) { onFailure("Đánh giá không hợp lệ (0-5)"); return }
        if (brandId == null || brandId.isBlank()) { onFailure("Vui lòng chọn nhãn hàng"); return }
        Log.d("SaveProduct", "Parsed values: price=$parsedPrice, discount=$parsedDiscount, rating=$parsedRating")

        // 2. Upload ảnh mới nếu có và gom URL với ảnh cũ
        val uploadedImageUrls = mutableListOf<String>()
        uploadedImageUrls.addAll(currentImageUrls) // Thêm các URL ảnh cũ
        Log.d("SaveProduct", "Current image URLs: $currentImageUrls")
        // Upload ảnh mới và lấy URL
        newImageUris.forEach { uri ->
            val storageRef = storage.reference.child("product_images/${UUID.randomUUID()}")
            try {
                storageRef.putFile(uri).await() // Tải ảnh lên
                val downloadUrl = storageRef.downloadUrl.await().toString() // Lấy URL
                uploadedImageUrls.add(downloadUrl) // Thêm vào danh sách
            } catch (e: Exception) {
                Log.e("SaveProduct", "Error uploading image $uri", e)
                // Bỏ qua ảnh lỗi và tiếp tục
            }
        }


        // 3. Tạo hoặc cập nhật tài liệu sản phẩm chính trong collection "products"
        val productDocRef = if (productId == null) {
            Log.d("SaveProduct", "Creating new product document")
            firestore.collection("products").document() // ID tự động nếu thêm mới


        } else {
            Log.d("SaveProduct", "Updating existing product document with ID: $productId")
            firestore.collection("products").document(productId) // ID hiện có nếu chỉnh sửa
        }

        // Tạo đối tượng ProductModel với dữ liệu đã xử lý
        val productData = (if (productId == null) Timestamp.now() else null)?.let {
            ProductModel( // Sử dụng ProductModel của bạn
                id = productDocRef.id,
                brandId = brandId,
                categoryId = "...", // TODO: Lấy categoryId thực tế
                createdAt = it,
                description = description,
                discount = (parsedDiscount ?: 0).toDouble(), // Int
                images = uploadedImageUrls, // Danh sách URL cuối cùng
                isFavourite = false, // TODO: Lấy giá trị thực tế
                name = name,
                price = parsedPrice ?: 0.0, // Double
                rating = parsedRating ?: 0.0, // Double
                slug = name.trim().replace(" ", "-").toLowerCase(), // TODO: Tạo slug (xử lý ký tự đặc biệt)
                sold = 0, // TODO: Lấy giá trị thực tế
            )
        }
        Log.d("SaveProduct", "Product data: $productData")

        if (productData != null) {
            productDocRef.set(productData).await()
        } // Lưu/Cập nhật tài liệu chính
        else    {
            Log.e("SaveProduct", "Failed to create product data")
            onFailure("Lỗi khi tạo dữ liệu sản phẩm")
            return
        }



        // 4. Lưu danh sách Stock vào subcollection "stock"
        val stockCollectionRef = productDocRef.collection("stock")
        Log.d("SaveProduct", "Stock collection reference: $stockCollectionRef")

        // TODO: Xóa stock cũ trước khi lưu stock mới (nếu cần)
        if (productId != null) { // Chỉ xóa khi chỉnh sửa
            val oldStockSnapshot = stockCollectionRef.get().await()
            if (!oldStockSnapshot.isEmpty) {
                val deleteBatch = firestore.batch()
                for (doc in oldStockSnapshot.documents) {
                    deleteBatch.delete(doc.reference)
                }
                deleteBatch.commit().await()
            }
        }

        // Sử dụng Batch Write để ghi nhiều tài liệu stock
        val stockBatch = firestore.batch()

        stockList.forEach { stock ->
            if (stock.size > 0 && stock.color.isNotBlank() && stock.quantity >= 0) {
                // Tạo document ID dựa trên size và màu (đảm bảo hợp lệ)
                val stockDocumentId = "size_${stock.size}_${stock.color.replace(Regex("[.#\\[\\]*~/ ]"), "_")}"

                val stockDocRef = stockCollectionRef.document(stockDocumentId)

                val stockDataMap = mapOf( // Map từ ProductStock data class của bạn
                    "id" to if (stock.id.isBlank()) UUID.randomUUID().toString() else stock.id,
                    "size" to stock.size,
                    "color" to stock.color,
                    "quantity" to stock.quantity
                )
                stockBatch.set(stockDocRef, stockDataMap)
            }
        }

            stockBatch.commit().await() // Commit batch nếu có stock hợp lệ
        

        onSuccess() // Thông báo thành công

    } catch (e: Exception) {
        Log.e("SaveProduct", "Error saving product and stock", e)
        onFailure("Lỗi khi lưu dữ liệu: ${e.message}")
    }
}


// Hàm xóa sản phẩm và stock (Sử dụng await) - Đã hoàn thiện ở các câu trả lời trước
suspend fun deleteProductFromFirestore(
    productId: String,
    firestore: FirebaseFirestore,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    try {
        val batch = firestore.batch()

        val stockCollectionRef = firestore.collection("products").document(productId).collection("stock")
        val stockSnapshot = stockCollectionRef.get().await()
        for (doc in stockSnapshot.documents) {
            batch.delete(doc.reference)
        }

        val productDocRef = firestore.collection("products").document(productId)
        batch.delete(productDocRef)

        batch.commit().await()

        onSuccess()

    } catch (e: Exception) {
        Log.e("DeleteProduct", "Error deleting product and stock", e)
        onFailure("Lỗi khi xóa sản phẩm: ${e.message}")
    }
}


@Composable
fun StockInputSection(
    stockList: MutableList<ProductStock>,
    onRemoveItem: (ProductStock) -> Unit,
    onAddItem: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Thêm các size & màu", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        stockList.forEachIndexed { index, stock ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = if (stock.size == 0 && stock.size.toString() != "0") "" else stock.size.toString(),
                    onValueChange = { newValue ->
                        val intValue = newValue.toIntOrNull() ?: 0
                        stockList[index] = stock.copy(size = intValue)
                    },
                    label = { Text("Size") },
                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = stock.color,
                    onValueChange = { newValue ->
                        stockList[index] = stock.copy(color = newValue)
                    },
                    label = { Text("Màu") },
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                )
                OutlinedTextField(
                    value = if (stock.quantity == 0 && stock.quantity.toString() != "0") "" else stock.quantity.toString(),
                    onValueChange = { newValue ->
                        val intValue = newValue.toIntOrNull() ?: 0
                        stockList[index] = stock.copy(quantity = intValue)
                    },
                    label = { Text("SL") },
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                IconButton(onClick = { onRemoveItem(stock) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa mục stock này")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(onClick = { stockList.add(ProductStock()) }) {
            Text("Thêm dòng mới")
        }
    }
}

@Composable
fun BrandDropdownMenu(
    brandList: List<BrandModel>,
    selectedBrand: BrandModel?,
    onBrandSelected: (BrandModel) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = selectedBrand?.name ?: "",
            onValueChange = {},
            label = { Text("Tên nhãn hàng") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null
                )
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            brandList.forEach { brand ->
                DropdownMenuItem(onClick = {
                    onBrandSelected(brand)
                    expanded = false
                }) {
                    Text(brand.name)
                }
            }
        }
    }
}