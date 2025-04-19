package com.eritlab.jexmon.domain.use_case.get_product_detail

import com.eritlab.jexmon.domain.model.ProductModel
import com.eritlab.jexmon.domain.repository.ProductRepository
import com.eritlab.jexmon.common.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect

class GetProductDetailUseCase @Inject constructor(private val repository: ProductRepository) {
    operator fun invoke(productId: String): Flow<Resource<ProductModel>> = flow {
        try {
            emit(Resource.Loading())

            repository.getProductDetail(productId).collect { data ->
                if (data != null) {
                    emit(Resource.Success(data = data))
                } else {
                    emit(Resource.Error("Product not found"))
                }
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
        }
    }
}
