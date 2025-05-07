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
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid // Lấy UID của user đang login

        Log.d("ProductDetailViewModel", "Received productId: $productId")
        if (productId.isNotBlank()) {
            getProductDetail(productId) // Tải thông tin sản phẩm
            getReviews(productId)       // Tải danh sách bình luận hiện có
            // Nếu có user đang login, thử lấy tên profile từ Firestore
            if (currentUserId != null) {
                viewModelScope.launch {
                    val fetchedUserName = getUserNameById(currentUserId)
                    // Lúc này bạn có thể sử dụng fetchedUserName
                    // Ví dụ: lưu nó vào ProductDetailState để Composable sử dụng
                    // _state.value = _state.value.copy(currentUserName = fetchedUserName) // Cần thêm currentUserName vào ProductDetailState
                    Log.d("ViewModel", "Fetched user name from profile: $fetchedUserName")

                    // Bạn có thể dùng tên này khi gọi submitReview từ Composable
                    // Ví dụ, thay vì truyền authorName từ UI, UI chỉ cần truyền userId
                    // và submitReview sẽ dùng userId để gọi getUserNameById
                }
            } else {
                // Xử lý trường hợp user chưa login (ví dụ: không cho viết review)
                Log.d("ViewModel", "User not logged in, cannot fetch profile name.")
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
                    _state.value = ProductDetailState(isLoading = true)
                }
                is Resource.Success -> {
                    _state.value = if (result.data != null) {
                        ProductDetailState(productDetail = result.data)
                    } else {
                        ProductDetailState(errorMessage = "Product not found")
                    }
                }
                is Resource.Error -> {
                    _state.value = ProductDetailState(errorMessage = result.message ?: "Unknown error")
                }
            }
        }.launchIn(viewModelScope)
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

    // Hàm tải danh sách bình luận (Đã đưa ra ngoài hàm getProductDetail)
    // Hàm tải danh sách bình luận (Chỉnh sửa truy vấn)
    fun getReviews(productId: String) {
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
            _state.value = _state.value.copy(isLoadingReviews = true, errorLoadingReviews = null)
            try {
                val db = FirebaseFirestore.getInstance()
                // --- CHỈNH SỬA TRUY VẤN ĐỂ ĐỌC TỪ TOP-LEVEL COLLECTION "reviews" ---
                val reviewsCollectionRef = db.collection("reviews")

                val querySnapshot = reviewsCollectionRef
                    .whereEqualTo("productId", productId) // Lọc bình luận theo productId
                    .orderBy("createdAt", Query.Direction.DESCENDING) // Sắp xếp
                    .get()
                    .await() // Đợi kết quả

                val reviewsList = querySnapshot.documents.mapNotNull { document ->
                    try {
                        // Mapping sang ReviewModel. Đảm bảo ReviewModel khớp với cấu trúc Firestore
                        document.toObject(ReviewModel::class.java)?.copy(id = document.id)
                    } catch (e: Exception) {
                        Log.e("ProductDetailViewModel", "Error converting review document: ${document.id}", e)
                        null // Bỏ qua document bị lỗi
                    }
                }

                _state.value = _state.value.copy(
                    reviews = reviewsList,
                    isLoadingReviews = false,
                    errorLoadingReviews = null
                )
            } catch (e: Exception) {
                Log.e("ProductDetailViewModel", "Error fetching reviews for product $productId", e)
                _state.value = _state.value.copy(
                    reviews = emptyList(),
                    isLoadingReviews = false,
                    errorLoadingReviews = "Không thể tải bình luận."
                )
            }
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
        imageUris: List<Uri>
    ) {
        if (productId.isBlank() || userId.isBlank() || authorName.isBlank() || rating < 1 || content.isBlank()) {
            _reviewSubmissionState.value = ReviewSubmissionState(submitError = "Vui lòng nhập đủ thông tin đánh giá (sao, nội dung).")
            return
        }

        viewModelScope.launch {
            _reviewSubmissionState.value = ReviewSubmissionState(isSubmitting = true, submitError = null, submitSuccess = false)
            val storage = FirebaseStorage.getInstance()
            val db = FirebaseFirestore.getInstance()
            val imageUrls = mutableListOf<String>()

            try {
                // 1. Tải ảnh lên Firebase Storage (logic giữ nguyên)
                for (uri in imageUris) {
                    val fileName = UUID.randomUUID().toString()
                    val ref = storage.reference.child("review_images/${productId}/${userId}/${fileName}")
                    val uploadTask = ref.putFile(uri)
                    val downloadUrl = uploadTask.await().storage.downloadUrl.await()
                    imageUrls.add(downloadUrl.toString())
                }

                // 2. Tạo đối tượng đánh giá mới (ReviewModel)
                // ReviewModel của bạn đã có productId, userId, author, rating, content, imageUrls, createdAt
                val newReview = ReviewModel(
                    // id sẽ được Firestore tự tạo khi dùng .add()
                    productId = productId, // <--- Đảm bảo productId được gán vào model
                    userId = userId,
                    userName = authorName, // Sử dụng userName thay vì author như trong ReviewModel của bạn
                    rating = rating.toInt(), // Chuyển Float sang Int nếu ReviewModel dùng Int
                    comment = content, // Sử dụng comment thay vì content như trong ReviewModel của bạn
                    imageUrls = if (imageUrls.isNotEmpty()) imageUrls else null, // Lưu danh sách URL ảnh
                    createdAt = Timestamp.now()
                )

                // 3. Lưu đánh giá vào Firestore (CHỈNH SỬA collection)
                // --- LƯU VÀO TOP-LEVEL COLLECTION "reviews" ---
                db.collection("reviews") // <-- Ghi vào collection "reviews" ở cấp cao nhất
                    .add(newReview) // Dùng add() để Firestore tự tạo document ID
                    .await() // Đợi task ghi vào Firestore hoàn thành

                // Gửi đánh giá thành công
                _reviewSubmissionState.value = ReviewSubmissionState(submitSuccess = true)

                // Tải lại danh sách bình luận (sẽ dùng hàm getReviews đã chỉnh sửa)
                getReviews(productId)

            } catch (e: Exception) {
                Log.e("ProductDetailViewModel", "Error submitting review", e)
                _reviewSubmissionState.value = ReviewSubmissionState(
                    isSubmitting = false,
                    submitError = e.message ?: "Lỗi khi gửi đánh giá. Vui lòng thử lại."
                )
            }
        }
    }

    // Hàm để reset trạng thái gửi đánh giá (gọi từ UI sau khi hiển thị thông báo thành công/lỗi)
    fun resetReviewSubmissionState() {
        _reviewSubmissionState.value = ReviewSubmissionState()
    }
}

