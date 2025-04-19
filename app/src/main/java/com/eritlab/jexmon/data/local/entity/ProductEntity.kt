package com.eritlab.jexmon.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.eritlab.jexmon.domain.model.ProductModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val isFavourite: Boolean,
    val categoryId: String,
    val brandId: String,
    val slug: String,
    val rating: Double,
    val images: List<String>, // Đổi từ String -> List<String>
    val createdAt: Long,
    val discount: Double,
    val sold: Int
) {
    fun toDomainModel(): ProductModel {
        val imageListType = object : TypeToken<List<String>>() {}.type
        return ProductModel(
            id = id,
            name = name,
            description = description,
            price = price,
            isFavourite = isFavourite,
            categoryId = categoryId,
            brandId = brandId,
            slug = slug,
            rating = rating,
            discount = discount,
            sold = sold,
            images = Gson().fromJson(images.toString(), imageListType) ?: emptyList(),
            createdAt = Timestamp(createdAt, 0)
        )
    }
    @Exclude // Để Firebase không lưu phương thức này
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "description" to description,
            "price" to price,
            "isFavourite" to isFavourite,
            "categoryId" to categoryId,
            "brandId" to brandId,
            "discount" to discount,
            "sold" to sold,
            "slug" to slug,
            "rating" to rating,
            "images" to images, // Firestore hỗ trợ List<String>
            "createdAt" to createdAt
        )
    }


}
