package com.eritlab.jexmon.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude

@Entity(tableName = "product_stock")
data class ProductStockEntity(
    @PrimaryKey val id: String,
    val productId: String, // Foreign key liên kết với product
    val size: Int,
    val color: String,
    val quantity: Int
)
{
    @Exclude // Để Firebase không lưu phương thức này
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "productId" to productId,
            "size" to size,
            "color" to color,
            "quantity" to quantity
        )
    }
}
