package com.eritlab.jexmon.presentation.screens.products

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eritlab.jexmon.domain.model.ProductModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ProductFilterState(
    val products: List<ProductModel> = emptyList(),
    val filteredProducts: List<ProductModel> = emptyList(),
    val selectedBrand: String = "all",
    val searchQuery: String = "",
    val searchResults: List<ProductModel> = emptyList()
)



class ProductFilterViewModel : ViewModel() {
    private var searchJob: kotlinx.coroutines.Job? = null
    private val searchDelay = 300L // Độ trễ debounce 300ms
    private val _state = MutableStateFlow(ProductFilterState())
    val state: StateFlow<ProductFilterState> = _state

    private val db = FirebaseFirestore.getInstance()
    // Lọc sản phẩm theo thương hiệu (brand) và sắp xếp theo giá
    suspend fun filterProductsByBrand(
        brandIds: String,
        sortOption: String = ""
    ) {
        Log.d("ProductFilter", "Filtering for brands: $brandIds with sort option: $sortOption")

        try {
            val brandIdList = brandIds.split(",").map { it.trim() }
            
            // Lấy dữ liệu từ Firebase Firestore theo danh sách brandId
            val productsSnapshot = db.collection("products")
                .whereIn("brandId", brandIdList)
                .get()
                .await()

            var products = productsSnapshot.documents.mapNotNull { document ->
                document.toObject(ProductModel::class.java)?.copy(id = document.id)
            }
            Log.d ("ProductFilter", "Products count: ${products.size}")

            // Sắp xếp theo sortOption
            products = when (sortOption.trim()) {
                "Giá thấp đến cao" -> products.sortedBy { it.price }
                "Giá cao đến thấp" -> products.sortedByDescending { it.price }
                else -> products
            }

            Log.d("ProductFilter", "Filtered products count: ${products.size}")
            Log.d("ProductFilter", "Filtered products: ${products.map { it.name }}")
            Log.d("ProductFilter", "Sort option applied: $sortOption")
            Log.d("ProductFilter", "Sorted prices: ${products.map { it.price }}")

            // Cập nhật state
            _state.value = _state.value.copy(
                products = products,
                filteredProducts = products,
                selectedBrand = brandIds
            )
        } catch (e: Exception) {
            Log.e("ProductFilter", "Error fetching products from Firestore", e)
            e.printStackTrace() // In ra stack trace để debug
        }
    }

    fun searchProducts(query: String) {
        // Hủy job tìm kiếm cũ nếu có
        searchJob?.cancel()
        
        // Cập nhật searchQuery trong state
        _state.value = _state.value.copy(searchQuery = query)
        
        // Nếu query rỗng, xóa kết quả tìm kiếm
        if (query.isBlank()) {
            _state.value = _state.value.copy(searchResults = emptyList())
            return
        }

        Log.d("ProductSearch", "Bắt đầu tìm kiếm với query: $query")
        
        // Tạo job tìm kiếm mới với debounce
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(searchDelay)
            
            try {
                // Chuyển query về chữ thường để tìm kiếm
                val lowercaseQuery = query.trim()
                Log.d("ProductSearch", "Query sau khi xử lý: $lowercaseQuery")

                // Thực hiện tìm kiếm trên Firestore
                val searchSnapshot = db.collection("products")
                    .orderBy("name")
                    .whereGreaterThanOrEqualTo("name", query)
                    .whereLessThanOrEqualTo("name", query + "\uf8ff")
                    .get()
                    .await()
                
                Log.d("ProductSearch", "Số lượng documents tìm thấy: ${searchSnapshot.size()}")
                
                val searchResults = searchSnapshot.documents.mapNotNull { document ->
                    val product = document.toObject(ProductModel::class.java)?.copy(id = document.id)
                    Log.d("ProductSearch", "Tìm thấy sản phẩm: ${product?.name} (${product?.id})")
                    product
                }
                
                Log.d("ProductSearch", "Kết quả tìm kiếm: ${searchResults.map { it.name }}")
                Log.d("ProductSearch", "Tổng số kết quả: ${searchResults.size}")
                
                // Cập nhật kết quả tìm kiếm trong state
                _state.value = _state.value.copy(searchResults = searchResults)
            } catch (e: Exception) {
                Log.e("ProductSearch", "Lỗi khi tìm kiếm sản phẩm", e)
                Log.e("ProductSearch", "Chi tiết lỗi: ${e.message}")
                _state.value = _state.value.copy(searchResults = emptyList())
            }
        }
    }
}
