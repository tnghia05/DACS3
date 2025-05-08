package com.eritlab.jexmon.presentation.screens.product_detail_screen

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eritlab.jexmon.common.Constrains
import com.eritlab.jexmon.common.Resource
import com.eritlab.jexmon.domain.model.CartItem
import com.eritlab.jexmon.domain.model.ReviewModel
import com.eritlab.jexmon.domain.use_case.get_product_detail.GetProductDetailUseCase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject


@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val getProductDetailUseCase: GetProductDetailUseCase,
    stateHandle: SavedStateHandle
) : ViewModel() {
    // State cho chi tiết sản phẩm và danh sách bình luận hiện có
    private val _state = mutableStateOf(ProductDetailState())
    val state: State<ProductDetailState> = _state

    // State cho quá trình gửi bình luận mới
    private val _reviewSubmissionState = mutableStateOf(ReviewSubmissionState())
    val reviewSubmissionState: State<ReviewSubmissionState> = _reviewSubmissionState

    // Data class cho trạng thái gửi bình luận
    data class ReviewSubmissionState(
        val isSubmitting: Boolean = false,
        val submitError: String? = null,
        val submitSuccess: Boolean = false
    )

    init {
        val productId = stateHandle.get<String>(Constrains.PRODUCT_ID_PARAM) ?: ""
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        Log.d("ProductDetailViewModel", "Received productId: $productId")
        if (productId.isNotBlank()) {
            // Load both product details and reviews together
            viewModelScope.launch {
                _state.value = _state.value.copy(isLoading = true)
                getProductDetail(productId)
                getReviews(productId)
            }
        } else {
            _state.value = ProductDetailState(errorMessage = "Invalid product ID")
        }
    }

    // Hàm tải chi tiết sản phẩm
    fun getProductDetail(productId: String) {
        if (productId.isBlank()) {
            _state.value = ProductDetailState(errorMessage = "Product ID cannot be empty")
            return
        }
        getProductDetailUseCase(productId).onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isLoading = true)
                }
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        productDetail = result.data,
                        errorMessage = null.toString()
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = result.message ?: "Unknown error"
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    // Hàm tải danh sách bình luận
    private fun getReviews(productId: String) {
        if (productId.isBlank()) {
            Log.w("ProductDetailViewModel", "Product ID is blank, cannot fetch reviews.")
            _state.value = _state.value.copy(
                reviews = emptyList(),
                isLoadingReviews = false,
                errorLoadingReviews = "Product ID không hợp lệ để tải bình luận."
            )
            return
        }

        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val reviewsCollectionRef = db.collection("reviews")

                val querySnapshot = reviewsCollectionRef
                    .whereEqualTo("productId", productId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val reviewsList = querySnapshot.documents.mapNotNull { document ->
                    try {
                        document.toObject(ReviewModel::class.java)?.copy(id = document.id)
                    } catch (e: Exception) {
                        Log.e("ProductDetailViewModel", "Error converting review document: ${document.id}", e)
                        null
                    }
                }

                Log.d("ProductDetailViewModel", "Fetched ${reviewsList.size} reviews for product $productId")

                _state.value = _state.value.copy(
                    reviews = reviewsList,
                    isLoadingReviews = false,
                    errorLoadingReviews = null
                )
            } catch (e: Exception) {
                Log.e("ProductDetailViewModel", "Error fetching reviews for product $productId", e)
                _state.value = _state.value.copy(
                    isLoadingReviews = false,
                    errorLoadingReviews = "Không thể tải bình luận."
                )
            }
        }
    }

    // Hàm getUserNameById sử dụng Callback (KHÔNG dùng suspend)
    fun getUserNameByIdCallback(
        userId: String,
        // Callback khi thành công. Nhận String? (tên hoặc null nếu không tìm thấy)
        onSuccess: (String?) -> Unit,
        // Callback khi thất bại. Nhận Exception nếu có lỗi xảy ra
        onFailure: (Exception) -> Unit
    ) {
        // Kiểm tra userId không rỗng
        if (userId.isBlank()) {
            Log.w("ViewModel", "User ID is blank, cannot fetch user name via callback.")
            onFailure(IllegalArgumentException("User ID cannot be blank")) // Thông báo lỗi qua callback
            return
        }

        val db = FirebaseFirestore.getInstance()

        // --- THAY "users" BẰNG TÊN COLLECTION THỰC TẾ CHỨA PROFILE USER CỦA BẠN ---
        db.collection("user").document(userId).get()
            .addOnSuccessListener { documentSnapshot ->
                // Block này chạy khi Firestore get thành công (document có hoặc không tồn tại)
                if (documentSnapshot.exists()) {
                    // Document user tồn tại, lấy giá trị của trường "name"
                    val userName = documentSnapshot.getString("name")
                    onSuccess(userName) // Truyền tên (có thể null nếu trường name thiếu/sai kiểu) qua callback thành công
                } else {
                    // Document user không tồn tại
                    Log.w("ViewModel", "User document with ID $userId not found via callback.")
                    onSuccess(null) // Truyền null qua callback thành công để báo không tìm thấy
                }
            }
            .addOnFailureListener { e ->
                // Block này chạy khi có lỗi xảy ra trong quá trình get Firestore
                Log.e("ViewModel", "Error fetching user name for ID $userId via callback.", e)
                onFailure(e) // Truyền lỗi qua callback thất bại
            }
    }
    /// Hàm tải u
    // ser
    suspend fun getUserNameById(userId: String): String? {
        // Kiểm tra userId không rỗng
        if (userId.isBlank()) {
            Log.w("ViewModel", "User ID is blank, cannot fetch user name.")
            return null
        }

        return try {
            val db = FirebaseFirestore.getInstance()
            // --- THAY "users" BẰNG TÊN COLLECTION THỰC TẾ CHỨA PROFILE USER CỦA BẠN ---
            val userDocument = db.collection("user").document(userId).get().await()

            if (userDocument.exists()) {
                // Document user tồn tại, lấy giá trị của trường "name"
                // Sử dụng getString("name") để lấy giá trị String
                userDocument.getString("name")
            } else {
                // Document user không tồn tại với userId này
                Log.w("ViewModel", "User document with ID $userId not found.")
                null
            }
        } catch (e: Exception) {
            // Xảy ra lỗi khi truy vấn Firestore
            Log.e("ViewModel", "Error fetching user name for ID $userId", e)
            null
        }
    }

    // Hàm thêm sản phẩm vào giỏ hàng
    // Giữ nguyên cấu trúc Flow<Result<Unit>> như bạn đã có
    fun addToCart(cartItem: CartItem): Flow<Result<Unit>> = flow {
        try {
            // Cập nhật trạng thái thêm giỏ hàng nếu cần
            // _state.value = _state.value.copy(isAddingToCart = true, addToCartSuccess = false, addToCartError = "")

            val db = FirebaseFirestore.getInstance()
            val querySnapshot = db.collection("carts")
                .whereEqualTo("userId", cartItem.userId)
                .whereEqualTo("productId", cartItem.productId)
                .whereEqualTo("size", cartItem.size)
                .whereEqualTo("color", cartItem.color)
                .get()
                .await() // await trong flow builder là hợp lệ

            if (!querySnapshot.isEmpty) {
                // Đã có item tương tự -> cập nhật số lượng
                val doc = querySnapshot.documents.first()
                val existingItem = doc.toObject(CartItem::class.java)
                val updatedQuantity = (existingItem?.quantity ?: 0) + cartItem.quantity

                // Tạo CartItem mới với ID document Firestore để set
                val updatedItem = existingItem?.copy(quantity = updatedQuantity) ?: cartItem.copy(quantity = updatedQuantity)


                db.collection("carts")
                    .document(doc.id)
                    .set(updatedItem) // Sử dụng set để ghi đè hoặc tạo mới nếu id không tồn tại
                    .await()
            } else {
                // Chưa có -> thêm mới
                db.collection("carts")
                    .add(cartItem) // add sẽ tự tạo document và ID
                    .await()
            }

            // Cập nhật trạng thái thành công nếu cần
            // _state.value = _state.value.copy(isAddingToCart = false, addToCartSuccess = true)
            emit(Result.success(Unit))

        } catch (e: Exception) {
            // Cập nhật trạng thái lỗi nếu cần
            // _state.value = _state.value.copy(
            //     isAddingToCart = false,
            //     addToCartSuccess = false,
            //     addToCartError = e.message ?: "Không thể thêm vào giỏ hàng"
            // )
            emit(Result.failure(e))
        }
    }

    // --- HÀM GỬI BÌNH LUẬN MỚI ---
    fun submitReview(
        productId: String,
        userId: String,
        authorName: String,
        rating: Int,
        content: String,
        imageUris: List<Uri>,
        productVariant: String? = null
    ) {
        viewModelScope.launch {
            try {
                _reviewSubmissionState.value = ReviewSubmissionState(isSubmitting = true)

                // Upload images first if any
                val uploadedImageUrls = mutableListOf<String>()
                if (imageUris.isNotEmpty()) {
                    val storage = FirebaseStorage.getInstance()
                    imageUris.forEach { uri ->
                        val imageRef = storage.reference.child("reviews/${productId}/${System.currentTimeMillis()}_${uri.lastPathSegment}")
                        val uploadTask = imageRef.putFile(uri)
                        uploadTask.await()
                        val downloadUrl = imageRef.downloadUrl.await()
                        uploadedImageUrls.add(downloadUrl.toString())
                    }
                }

                // Create review document
                val review = ReviewModel(
                    id = UUID.randomUUID().toString(),
                    productId = productId,
                    userId = userId,
                    userName = authorName,
                    rating = rating,
                    comment = content,
                    createdAt = Timestamp.now(),
                    images = uploadedImageUrls.takeIf { it.isNotEmpty() },
                    productVariant = productVariant,
                    helpfulCount = 0
                )

                // Add review to Firestore
                val db = FirebaseFirestore.getInstance()
                db.collection("reviews")
                    .document(review.id)
                    .set(review)
                    .await()

                // Update product rating average and count
                val productRef = db.collection("products").document(productId)
                db.runTransaction { transaction ->
                    val product = transaction.get(productRef)
                    val currentRatingCount = product.getLong("ratingCount")?.toInt() ?: 0
                    val currentRatingSum = product.getDouble("ratingSum") ?: 0.0

                    val newRatingCount = currentRatingCount + 1
                    val newRatingSum = currentRatingSum + rating
                    val newRatingAverage = newRatingSum / newRatingCount

                    transaction.update(productRef, mapOf(
                        "ratingCount" to newRatingCount,
                        "ratingSum" to newRatingSum,
                        "rating" to newRatingAverage
                    ))
                }.await()

                // Update local state
                _state.value = _state.value.copy(
                    reviews = _state.value.reviews + review
                )

                _reviewSubmissionState.value = ReviewSubmissionState(submitSuccess = true)

                // Reset after success
                delay(2000)
                _reviewSubmissionState.value = ReviewSubmissionState()

            } catch (e: Exception) {
                _reviewSubmissionState.value = ReviewSubmissionState(
                    submitError = e.message ?: "Failed to submit review"
                )
            }
        }
    }

    // Hàm để reset trạng thái gửi đánh giá (gọi từ UI sau khi hiển thị thông báo thành công/lỗi)
    fun resetReviewSubmissionState() {
        _reviewSubmissionState.value = ReviewSubmissionState()
    }
}