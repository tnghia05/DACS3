package com.eritlab.jexmon.data.local.dao

import androidx.room.*
import com.eritlab.jexmon.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long // ✅ Trả về Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>): List<Long> // ✅ Trả về List<Long>

    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<ProductEntity>> // ✅ Trả về Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    fun getProductById(productId: String): Flow<ProductEntity?> // ✅ Trả về Flow<ProductEntity?>

    @Query("DELETE FROM products")
    suspend fun clearProducts(): Int // ✅ Trả về Int (số dòng bị xóa)
}
