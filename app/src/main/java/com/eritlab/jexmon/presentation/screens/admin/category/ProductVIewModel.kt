package com.eritlab.jexmon.presentation.screens.admin.category

// Import cần thiết cho ViewModel

// Import các Data Class của bạn

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eritlab.jexmon.domain.model.BrandModel
import com.eritlab.jexmon.domain.model.ProductStock
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

// TODO: Đảm bảo bạn có Data class ProductModel, ProductStock, BrandModel


class ProductViewModel : ViewModel() { // <-- Sửa tên ViewModel
    // Instance của Firestore và Storage được giữ trong ViewModel
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance() // <-- Thêm Storage instance

    // StateFlow để expose danh sách Brand
    private val _brandList = MutableStateFlow<List<BrandModel>>(emptyList())
    val brandList: StateFlow<List<BrandModel>> = _brandList

    // TODO: Thêm StateFlow hoặc Channel/SharedFlow để expose trạng thái lưu (loading, success, error)
    // Đây là cách hiện đại hơn so với truyền callbacks vào hàm lưu
    // Ví dụ:
    // private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    // val saveStatus: StateFlow<SaveStatus> = _saveStatus
    // sealed class SaveStatus { object Idle : SaveStatus(); object Loading : SaveStatus(); data class Success(val message: String) : SaveStatus(); data class Error(val message: String) : SaveStatus() }


    // Init block để fetch dữ liệu ban đầu (ví dụ: danh sách Brands)
    init {
        fetchBrands()
    }

    // Hàm để fetch danh sách Brands (sử dụng Coroutines trong ViewModel)
    private fun fetchBrands() {
        viewModelScope.launch { // Sử dụng viewModelScope để chạy coroutine theo vòng đời ViewModel
            try {
                val result = firestore.collection("brands").get().await() // Sử dụng await
                val brands = result.documents.mapNotNull { it.toObject(BrandModel::class.java) }
                _brandList.value = brands // Cập nhật StateFlow
                Log.d("ProductViewModel", "Fetched brands: ${brands.map { it.name }}")
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error fetching brands", e)
                // TODO: Xử lý lỗi fetch brands (ví dụ: cập nhật một StateFlow lỗi)
            }
        }
    }

    // Hàm lưu sản phẩm và stock sử dụng Callbacks (như yêu cầu của bạn)
    // Bây giờ là một phương thức của ViewModel
    fun saveProductAndStockCallback(
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
        context: Context, // <-- Vẫn cần Context nếu dùng Toast trong callbacks onSuccess/onFailure
        onOverallSuccess: () -> Unit, // Callback khi toàn bộ quá trình thành công
        onOverallFailure: (String) -> Unit // Callback khi có lỗi xảy ra
    ) {
        // TODO: Cập nhật StateFlow trạng thái lưu thành Loading ở đây (_saveStatus.value = SaveStatus.Loading)

        // 1. Validate dữ liệu nhập liệu cơ bản (Giống như phiên bản await)
        val parsedPrice = price.toDoubleOrNull()
        val parsedDiscount = discount.toIntOrNull()
        val parsedRating = rating.toDoubleOrNull()

        if (name.isBlank()) { onOverallFailure("Tên sản phẩm không được trống"); return }
        if (description.isBlank()) { onOverallFailure("Mô tả không được trống"); return }
        if (parsedPrice == null || parsedPrice < 0) { onOverallFailure("Giá sản phẩm không hợp lệ"); return }
        if (parsedDiscount == null || parsedDiscount < 0 || parsedDiscount > 100) { onOverallFailure("Giảm giá không hợp lệ"); return }
        if (parsedRating == null || parsedRating < 0 || parsedRating > 5) { onOverallFailure("Đánh giá không hợp lệ (0-5)"); return }
        if (brandId == null || brandId.isBlank()) { onOverallFailure("Vui lòng chọn nhãn hàng"); return }

        // --- Callback Hell Bắt Đầu ---

        // 2. Upload ảnh mới nếu có (Logic Callbacks tải nhiều ảnh phức tạp)
        // Để tải NHIỀU ảnh bằng Callbacks và đảm bảo tất cả hoàn thành, bạn cần quản lý đếm số lượng ảnh
        // đã upload thành công và xử lý khi tất cả callbacks trả về.

        val finalImageUrls = mutableListOf<String>().apply { addAll(currentImageUrls) } // Bắt đầu với ảnh cũ
        val totalNewImages = newImageUris.size
        var uploadedCount = 0
        val uploadErrors = mutableListOf<Exception>()

        // Nếu không có ảnh mới để upload, tiến thẳng đến bước 3
        if (totalNewImages == 0) {
            saveProductDocumentCallback(
                productId = productId,
                name = name,
                description = description,
                price = parsedPrice,
                discount = parsedDiscount,
                rating = parsedRating,
                imageUrls = finalImageUrls, // finalImageUrls chỉ chứa ảnh cũ
                brandId = brandId,
                // Sử dụng firestore instance của ViewModel
                onProductSaveSuccess = { productDocRef ->
                    saveStockCollectionCallback(
                        productDocRef = productDocRef,
                        stockList = stockList,
                        // Sử dụng firestore instance của ViewModel
                        onStockSaveSuccess = { onOverallSuccess() },
                        onStockSaveFailure = { e -> onOverallFailure("Lỗi khi lưu stock: ${e.message}") }
                    )
                },
                onProductSaveFailure = { e -> onOverallFailure("Lỗi khi lưu thông tin sản phẩm: ${e.message}") }
            )
            return // Thoát khỏi hàm saveProductAndStockCallback
        }

        // Nếu có ảnh mới để upload, lặp qua danh sách newImageUris
        newImageUris.forEach { uri ->
            val storageRef = storage.reference.child("product_images/${UUID.randomUUID()}")
            storageRef.putFile(uri)
                .addOnSuccessListener { taskSnapshot ->
                    storageRef.downloadUrl
                        .addOnSuccessListener { downloadUri ->
                            finalImageUrls.add(downloadUri.toString())
                            uploadedCount++
                            if (uploadedCount == totalNewImages) {
                                // Tất cả ảnh mới đã upload (thành công hoặc thất bại)
                                if (uploadErrors.isEmpty()) {
                                    // Tất cả ảnh upload thành công, tiến hành bước 3
                                    saveProductDocumentCallback(
                                        productId = productId,
                                        name = name, description = description, price = parsedPrice, discount = parsedDiscount, rating = parsedRating,
                                        imageUrls = finalImageUrls, brandId = brandId,
                                        // Sử dụng firestore instance của ViewModel
                                        onProductSaveSuccess = { productDocRef ->
                                            saveStockCollectionCallback(
                                                productDocRef = productDocRef, stockList = stockList,
                                                // Sử dụng firestore instance của ViewModel
                                                onStockSaveSuccess = { onOverallSuccess() },
                                                onStockSaveFailure = { e -> onOverallFailure("Lỗi khi lưu stock: ${e.message}") }
                                            )
                                        },
                                        onProductSaveFailure = { e -> onOverallFailure("Lỗi khi lưu thông tin sản phẩm: ${e.message}") }
                                    )
                                } else {
                                    // Có lỗi khi upload một hoặc nhiều ảnh
                                    onOverallFailure("Lỗi khi tải ảnh lên: ${uploadErrors.first().message}")
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            uploadErrors.add(e)
                            uploadedCount++
                            if (uploadedCount == totalNewImages) {
                                // Đã xử lý tất cả ảnh upload, có lỗi
                                onOverallFailure("Lỗi khi tải ảnh lên: ${uploadErrors.first().message}")
                            }
                        }
                }
                .addOnFailureListener { e ->
                    uploadErrors.add(e)
                    uploadedCount++
                    if (uploadedCount == totalNewImages) {
                        // Đã xử lý tất cả ảnh upload, có lỗi
                        onOverallFailure("Lỗi khi tải ảnh lên: ${uploadErrors.first().message}")
                    }
                }
        }
        // --- Callback Hell Kết Thúc (Phần tải ảnh) ---
    }

    // Hàm trợ giúp để lưu document sản phẩm (sử dụng Callback) - Phương thức private của ViewModel
    private fun saveProductDocumentCallback( // <-- Sửa thành private fun
        productId: String?,
        name: String,
        description: String,
        price: Double, // Đã parse
        discount: Int, // Đã parse
        rating: Double, // Đã parse
        imageUrls: List<String>, // Đã có URLs cuối cùng
        brandId: String,
        // KHÔNG cần firestore ở đây, dùng instance của ViewModel
        onProductSaveSuccess: (DocumentReference) -> Unit, // Trả về DocumentReference
        onProductSaveFailure: (Exception) -> Unit
    ) {
        val productsCollection = firestore.collection("products") // Sử dụng instance của ViewModel
        val productDocRef = if (productId == null) {
            productsCollection.document() // Tạo ID mới nếu thêm mới
        } else {
            productsCollection.document(productId) // Sử dụng ID hiện có nếu chỉnh sửa
        }

        // Tạo ProductModel (hoặc Product) data class
        val productData = mapOf( // Sử dụng Map thay vì data class ProductModel để đơn giản hóa ví dụ callback
            "id" to productDocRef.id,
            "brandId" to brandId,
            "categoryId" to "...", // TODO: Lấy categoryId thực tế
            "createdAt" to if (productId == null) Timestamp.now() else null, // Gán thời gian tạo
            "description" to description,
            "discount" to discount,
            "imageUrls" to imageUrls,
            "isFavourite" to false, // TODO: Lấy giá trị thực tế
            "name" to name,
            "price" to price,
            "rating" to rating,
            "slug" to name.trim().replace(" ", "-").toLowerCase(), // TODO: Tạo slug
            "sold" to 0 // TODO: Lấy giá trị thực tế
        )

        productDocRef.set(productData)
            .addOnSuccessListener {
                onProductSaveSuccess(productDocRef) // Gọi callback thành công và truyền DocumentReference
            }
            .addOnFailureListener { e ->
                onProductSaveFailure(e) // Gọi callback thất bại
            }
    }

    // Hàm trợ giúp để lưu Stock (sử dụng Callback) - Phương thức private của ViewModel
    // Lưu ý: Triển khai Batch Write với Callbacks phức tạp hơn nhiều so với Batch Write với await
    // Dưới đây là ví dụ đơn giản từng item, KHÔNG dùng Batch Write.
    // Để dùng Batch Write với Callbacks, cần quản lý trạng thái Batch và Callbacks phức tạp hơn.
    private fun saveStockCollectionCallback( // <-- Sửa thành private fun
        productDocRef: DocumentReference,
        stockList: List<ProductStock>,
        // KHÔNG cần firestore ở đây, dùng instance của ViewModel
        onStockSaveSuccess: () -> Unit,
        onStockSaveFailure: (Exception) -> Unit
    ) {
        val stockCollectionRef = productDocRef.collection("stock") // Sử dụng instance của ViewModel

        // TODO: Xóa stock cũ trước khi lưu stock mới (tùy chọn) - Cần làm bằng Callbacks

        var stockSaveCount = 0
        val totalStocks = stockList.size
        val errors = mutableListOf<Exception>()

        if (totalStocks == 0) {
            onStockSaveSuccess() // Thành công nếu không có stock nào để lưu
            return
        }

        stockList.forEach { stock ->
            // Tạo document ID dựa trên size và màu (như kế hoạch)
            val stockDocumentId = "size_${stock.size}_${stock.color.replace(Regex("[.#\\[\\]*~/ ]"), "_")}"
            val stockDocRef = stockCollectionRef.document(stockDocumentId)

            val stockDataMap = mapOf(
                "id" to if (stock.id.isBlank()) UUID.randomUUID().toString() else stock.id,
                "size" to stock.size,
                "color" to stock.color,
                "quantity" to stock.quantity
            )

            stockDocRef.set(stockDataMap)
                .addOnSuccessListener {
                    stockSaveCount++
                    if (stockSaveCount == totalStocks) {
                        // Đã xử lý tất cả stock
                        if (errors.isEmpty()) {
                            onStockSaveSuccess() // Thành công nếu không có lỗi
                        } else {
                            onStockSaveFailure(errors.first()) // Thất bại nếu có lỗi
                        }
                    }
                }
                .addOnFailureListener { e ->
                    errors.add(e)
                    stockSaveCount++
                    if (stockSaveCount == totalStocks) {
                        onStockSaveFailure(errors.first()) // Thất bại nếu có lỗi
                    }
                }
        }
    }

    // TODO: Thêm hàm deleteProductFromFirestoreCallback nếu cần xóa bằng Callbacks
    /*
    fun deleteProductFromFirestoreCallback(
        productId: String,
        onOverallSuccess: () -> Unit,
        onOverallFailure: (String) -> Unit
    ) {
        // Triển khai logic xóa bằng Callbacks và Batch Write
        // Tương tự saveProductAndStockCallback, sẽ có Callbacks lồng nhau
    }
    */
}

// --- Cách sử dụng trong Composable (AddProductScreen) ---
/*
@Composable
fun AddProductScreen(navController: NavController, productId: String? = null) {
    // Lấy ViewModel instance
    val viewModel: ProductViewModel = viewModel() // Import androidx.lifecycle.viewmodel.compose.viewModel

    // Theo dõi StateFlow brandList từ ViewModel
    val brandList by viewModel.brandList.collectAsState() // Import androidx.compose.runtime.collectAsState

    // TODO: Theo dõi StateFlow trạng thái lưu (saveStatus) từ ViewModel
    // val saveStatus by viewModel.saveStatus.collectAsState()

    // ... (các biến state UI khác: name, description, price, stockList, newImageUris, imageUrls)

    val context = LocalContext.current

    // TODO: Sử dụng LaunchedEffect để quan sát saveStatus và hiển thị Toast/Navigate
    // LaunchedEffect(saveStatus) {
    //     when (saveStatus) {
    //          is SaveStatus.Success -> { Toast.makeText(context, saveStatus.message, Toast.LENGTH_SHORT).show(); navController.popBackStack() }
    //          is SaveStatus.Error -> Toast.makeText(context, saveStatus.message, Toast.LENGTH_SHORT).show()
    //          // Handle Loading state in UI (ví dụ: hiển thị ProgressBar)
    //     }
    // }


    // ... (các phần UI khác trong Column)

    // Nút Lưu Sản phẩm
    Button(
        onClick = {
            // Thu thập dữ liệu từ các state UI
            val name = name.text
            val description = description.text
            val price = price.text
            val discount = discount.text
            val rating = rating.text
            val selectedBrandId = selectedBrand.value?.id
            val stockListToSave = stockList.toList() // Truyền List<ProductStock>

            // Gọi phương thức lưu từ ViewModel
            // ViewModel sẽ xử lý logic lưu và thông báo kết quả qua callbacks (hoặc StateFlow)
            viewModel.saveProductAndStockCallback(
                productId = productId,
                name = name,
                description = description,
                price = price,
                discount = discount,
                rating = rating,
                newImageUris = newImageUris,
                currentImageUrls = imageUrls,
                brandId = selectedBrandId,
                stockList = stockListToSave,
                context = context, // Truyền Context (cần cẩn thận)
                onOverallSuccess = {
                    // Xử lý UI khi thành công (hiển thị Toast, navigate)
                    // Tốt nhất nên dùng StateFlow/Events từ ViewModel thay vì làm trực tiếp ở đây
                    Toast.makeText(context, if (productId == null) "Thêm sản phẩm thành công" else "Cập nhật sản phẩm thành công", Toast.LENGTH_SHORT).show()
                     navController.popBackStack()
                },
                onOverallFailure = { errorMessage ->
                     // Xử lý UI khi thất bại (hiển thị Toast)
                     // Tốt nhất nên dùng StateFlow/Events từ ViewModel
                     Toast.makeText(context, "Lỗi: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            )
        },
         // TODO: Điều khiển enabled dựa trên trạng thái lưu từ ViewModel (ví dụ: saveStatus == SaveStatus.Loading)
        enabled = true // Tạm thời enable
    ) {
         // TODO: Hiển thị CircularProgressIndicator dựa trên trạng thái lưu từ ViewModel
        Text("Lưu Sản phẩm (Callback)")
    }

    // TODO: Nút Xóa sản phẩm (gọi phương thức xóa từ ViewModel)
    // if (productId != null) {
    //     Button(onClick = { viewModel.deleteProductFromFirestoreCallback(...) }) { Text("Xóa Sản Phẩm") }
    // }
}
*/