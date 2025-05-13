package com.eritlab.jexmon.presentation.screens.products

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eritlab.jexmon.domain.model.ProductFilter
import com.eritlab.jexmon.domain.model.ProductModel
import com.eritlab.jexmon.domain.model.SearchHistoryModel
import com.eritlab.jexmon.domain.model.SortOption
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
    val searchResults: List<ProductModel> = emptyList(),
    val searchHistory: List<SearchHistoryModel> = emptyList(),
    val searchSuggestions: List<String> = emptyList(),
    val currentFilter: ProductFilter = ProductFilter(),
    val isLoading: Boolean = false
)

class ProductFilterViewModel : ViewModel() {
    private var searchJob: kotlinx.coroutines.Job? = null
    private val searchDelay = 300L
    private val _state = MutableStateFlow(ProductFilterState())
    val state: StateFlow<ProductFilterState> = _state
    private val db = FirebaseFirestore.getInstance()

    // Thêm lại hàm filterProductsByBrand
    suspend fun filterProductsByBrand(brandIds: String, sortOption: String = "") {
        try {
            _state.value = _state.value.copy(isLoading = true)
            
            val brandIdList = brandIds.split(",").map { it.trim() }
            
            // Lấy dữ liệu từ Firebase Firestore theo danh sách brandId
            val productsSnapshot = if (brandIds == "all") {
                db.collection("products").get().await()
            } else {
                db.collection("products")
                    .whereIn("brandId", brandIdList)
                    .get()
                    .await()
            }

            var products = productsSnapshot.documents.mapNotNull { document ->
                document.toObject(ProductModel::class.java)?.copy(id = document.id)
            }

            // Sắp xếp theo sortOption
            products = when (sortOption.trim()) {
                "Giá thấp đến cao" -> products.sortedBy { it.price }
                "Giá cao đến thấp" -> products.sortedByDescending { it.price }
                "Mới nhất" -> products.sortedByDescending { it.createdAt }
                "Đánh giá cao nhất" -> products.sortedByDescending { it.rating }
                else -> products
            }

            _state.value = _state.value.copy(
                products = products,
                filteredProducts = products,
                selectedBrand = brandIds,
                isLoading = false
            )
        } catch (e: Exception) {
            Log.e("ProductFilter", "Lỗi khi lọc sản phẩm theo thương hiệu", e)
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    // Lưu lịch sử tìm kiếm
    private fun saveSearchHistory(query: String) {
        viewModelScope.launch {
            try {
                val searchHistory = SearchHistoryModel(query = query)
                // Lưu vào Firestore
                db.collection("search_history")
                    .add(searchHistory)
                    .await()
                
                // Cập nhật state với lịch sử mới
                val updatedHistory = _state.value.searchHistory + searchHistory
                _state.value = _state.value.copy(
                    searchHistory = updatedHistory.takeLast(10) // Giữ 10 lịch sử gần nhất
                )
            } catch (e: Exception) {
                Log.e("SearchHistory", "Lỗi khi lưu lịch sử tìm kiếm", e)
            }
        }
    }

    // Lấy gợi ý tìm kiếm
    private suspend fun fetchSearchSuggestions(query: String) {
        try {
            if (query.isBlank()) {
                _state.value = _state.value.copy(searchSuggestions = emptyList())
                return
            }

            // Lấy gợi ý từ tên sản phẩm trong Firestore
            val suggestionsSnapshot = db.collection("products")
                .orderBy("name")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(5)
                .get()
                .await()

            val suggestions = suggestionsSnapshot.documents
                .mapNotNull { it.getString("name") }
                .filter { it.lowercase().startsWith(query.lowercase()) }
                .distinct()
                .take(5)

            _state.value = _state.value.copy(searchSuggestions = suggestions)
        } catch (e: Exception) {
            Log.e("SearchSuggestions", "Lỗi khi lấy gợi ý tìm kiếm", e)
        }
    }

    // Áp dụng bộ lọc nâng cao
    fun applyFilter(filter: ProductFilter) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true)
                
                var filteredProducts = _state.value.products

                // Áp dụng các bộ lọc
                filteredProducts = filteredProducts.filter { product ->
                    val priceInRange = product.price.toFloat() in filter.priceRange
                    val meetsRating = product.rating >= filter.rating
                    val inCategory = filter.categories.isEmpty() || 
                        filter.categories.contains(product.categoryId)
                    
                    priceInRange && meetsRating && inCategory
                }

                // Áp dụng sắp xếp
                filteredProducts = when (filter.sortBy) {
                    SortOption.PRICE_LOW_TO_HIGH -> filteredProducts.sortedBy { it.price }
                    SortOption.PRICE_HIGH_TO_LOW -> filteredProducts.sortedByDescending { it.price }
                    SortOption.RATING -> filteredProducts.sortedByDescending { it.rating }
                    SortOption.NEWEST -> filteredProducts.sortedByDescending { it.createdAt }
                    else -> filteredProducts
                }

                _state.value = _state.value.copy(
                    filteredProducts = filteredProducts,
                    currentFilter = filter,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("ProductFilter", "Lỗi khi áp dụng bộ lọc", e)
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun searchProducts(query: String) {
        searchJob?.cancel()
        
        _state.value = _state.value.copy(searchQuery = query)
        
        if (query.isBlank()) {
            _state.value = _state.value.copy(
                searchResults = emptyList(),
                searchSuggestions = emptyList()
            )
            return
        }

        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(searchDelay)
            
            try {
                _state.value = _state.value.copy(isLoading = true)
                
                // Lấy gợi ý tìm kiếm
                fetchSearchSuggestions(query)
                
                val lowercaseQuery = query.lowercase().trim()
                
                // Thực hiện tìm kiếm
                val searchSnapshot = db.collection("products")
                    .get()
                    .await()
                
                val searchResults = searchSnapshot.documents.mapNotNull { document ->
                    val product = document.toObject(ProductModel::class.java)?.copy(id = document.id)
                    if (product?.name?.lowercase()?.contains(lowercaseQuery) == true) {
                        product
                    } else {
                        null
                    }
                }
                
                // Sắp xếp kết quả
                val sortedResults = searchResults.sortedWith(compareBy(
                    { !it.name.lowercase().startsWith(lowercaseQuery) },
                    { it.name.lowercase() }
                ))
                
                // Lưu lịch sử tìm kiếm
                saveSearchHistory(query)
                
                _state.value = _state.value.copy(
                    searchResults = sortedResults,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("ProductSearch", "Lỗi khi tìm kiếm sản phẩm", e)
                _state.value = _state.value.copy(
                    searchResults = emptyList(),
                    isLoading = false
                )
            }
        }
    }

    // Add function to delete search history
    fun deleteSearchHistory(query: String) {
        viewModelScope.launch {
            try {
                // Find and delete the search history document with matching query
                val querySnapshot = db.collection("search_history")
                    .whereEqualTo("query", query)
                    .get()
                    .await()

                // Delete from Firestore
                for (document in querySnapshot.documents) {
                    db.collection("search_history")
                        .document(document.id)
                        .delete()
                        .await()
                }

                // Update local state by removing the deleted query
                val updatedHistory = _state.value.searchHistory.filter { it.query != query }
                _state.value = _state.value.copy(searchHistory = updatedHistory)
            } catch (e: Exception) {
                Log.e("SearchHistory", "Error deleting search history", e)
            }
        }
    }
}
