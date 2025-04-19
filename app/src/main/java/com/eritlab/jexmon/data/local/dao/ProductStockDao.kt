package com.eritlab.jexmon.data.local.dao

import androidx.room.*
import com.eritlab.jexmon.data.local.entity.ProductEntity
import com.eritlab.jexmon.data.local.entity.ProductStockEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface ProductStockDao {
    @Query("SELECT * FROM product_stock WHERE productId = :productId")
    fun getProductStock(productId: String): Flow<List<ProductStockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: List<ProductStockEntity>)

    @Query("DELETE FROM product_stock WHERE productId = :productId")
    suspend fun deleteStockByProduct(productId: String)
}
