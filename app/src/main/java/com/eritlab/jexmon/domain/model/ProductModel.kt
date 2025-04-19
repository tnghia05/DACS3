package com.eritlab.jexmon.domain.model

import com.eritlab.jexmon.data.local.entity.ProductEntity
import com.google.firebase.Timestamp

data class ProductModel(
    @JvmField val id: String = "",
    @JvmField val name: String = "",
    @JvmField val description: String = "",
    @JvmField val price: Double = 0.0,
    @JvmField var isFavourite: Boolean = false,
    @JvmField val categoryId: String = "",
    @JvmField val brandId: String = "",
    @JvmField val discount: Double = 0.0,
    @JvmField val slug: String = "",
    @JvmField val rating: Double = 0.0,
    @JvmField val sold: Int = 0,
    @JvmField val images: List<String> = emptyList(),
    @JvmField val createdAt: Timestamp = Timestamp.now(),
    @JvmField val stock: List<ProductStock> = listOf()
)
{
    fun toEntity(): ProductEntity {
        return ProductEntity(
            id = this.id,
            name = this.name,
            description = this.description,
            price = this.price,
            isFavourite = this.isFavourite,
            categoryId = this.categoryId,
            discount = this.discount,
            brandId = this.brandId,
            sold = this.sold,
            slug = this.slug,
            rating = this.rating,
            images = this.images, // Đã là List<String>, không cần chuyển đổi
            createdAt = this.createdAt.seconds // Chuyển Timestamp → Long
        )
    }



}
