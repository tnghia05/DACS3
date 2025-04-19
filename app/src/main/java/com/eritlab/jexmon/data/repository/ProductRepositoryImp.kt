package com.eritlab.jexmon.data.repository

import com.eritlab.jexmon.data.local.dao.ProductDao
import com.eritlab.jexmon.data.local.entity.ProductEntity
import com.eritlab.jexmon.domain.model.ProductModel
import com.eritlab.jexmon.domain.model.ProductStock
import com.eritlab.jexmon.domain.repository.ProductRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class ProductRepositoryImp @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val productDao: ProductDao
) : ProductRepository {

    override fun getProduct(): Flow<List<ProductModel>> = callbackFlow {
        val collectionRef = firestore.collection("products")

        val listener = collectionRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val productList = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(ProductModel::class.java)?.copy(id = doc.id)
            } ?: emptyList()

            trySend(productList)

            // 🔹 Chuyển sang Entity và lưu vào Room
            CoroutineScope(Dispatchers.IO).launch {
                saveProductsToLocal(productList.map { it.toEntity() })
            }
        }

        awaitClose { listener.remove() }
    }



    override fun getProductDetail(productId: String): Flow<ProductModel?> = callbackFlow {
        val docRef = firestore.collection("products").document(productId)

        val listener = docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(null)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val product = snapshot.toObject(ProductModel::class.java)?.copy(id = snapshot.id)

                firestore.collection("products").document(productId)
                    .collection("stock")
                    .get()
                    .addOnSuccessListener { stockSnapshot ->
                        val stockList = stockSnapshot.documents.mapNotNull { doc ->
                            doc.toObject(ProductStock::class.java)?.copy(id = doc.id)
                        }
                        val updatedProduct = product?.copy(stock = stockList)
                        // 🔹 Cập nhật vào Room
                        updatedProduct?.let {
                            CoroutineScope(Dispatchers.IO).launch {
                                productDao.insertProduct(it.toEntity())
                            }
                        }


                        trySend(updatedProduct)
                    }
                    .addOnFailureListener {
                        trySend(product)
                    }
            } else {
                trySend(null)
            }
        }

        awaitClose { listener.remove() }
    }

    // 🔹 Lấy dữ liệu từ Room khi offline
    fun getLocalProducts(): Flow<List<ProductModel>> {
        return productDao.getAllProducts().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    // 🔹 Đồng bộ dữ liệu từ Room lên Firebase
    fun syncRoomToFirebase() {
        CoroutineScope(Dispatchers.IO).launch {
            productDao.getAllProducts().collect { products ->
                products.forEach { product ->
                    firestore.collection("products").document(product.id)
                        .set(product.toMap())
                }
            }
        }
    }

    // 🔹 Đồng bộ dữ liệu từ Firebase xuống Room
    fun syncFirebaseToRoom() {
        firestore.collection("products").get()
            .addOnSuccessListener { snapshot ->
                val productList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ProductModel::class.java)?.copy(id = doc.id)
                }

                CoroutineScope(Dispatchers.IO).launch {
                    productDao.insertProducts(productList.map { it.toEntity() })
                }
            }
    }
    suspend fun saveProductsToLocal(products: List<ProductEntity>) {
        productDao.insertProducts(products)
    }


}
