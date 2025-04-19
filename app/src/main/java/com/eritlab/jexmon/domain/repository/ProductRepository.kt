package com.eritlab.jexmon.domain.repository

import com.eritlab.jexmon.domain.model.ProductModel
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getProduct(): Flow<List<ProductModel>>
    fun getProductDetail(productId: String): Flow<ProductModel?> // Thêm phương thức này

}
