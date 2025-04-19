package com.eritlab.jexmon.di

import android.content.Context
import androidx.room.Room
import com.eritlab.jexmon.data.local.AppDatabase
import com.eritlab.jexmon.data.local.dao.ProductDao
import com.eritlab.jexmon.data.local.dao.ProductStockDao
import com.eritlab.jexmon.data.repository.ProductRepositoryImp
import com.eritlab.jexmon.domain.repository.ProductRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    // 🔹 Cung cấp instance của Firebase Firestore
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    fun provideProductStockDao(db: AppDatabase): ProductStockDao = db.productStockDao()

    // 🔹 Cung cấp instance của Room Database
    @Provides
    @Singleton
    fun provideDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "product_database"
        )            .fallbackToDestructiveMigration() // Thêm dòng này

            .build()
    }

    // 🔹 Cung cấp DAO của Room
    @Provides
    @Singleton
    fun provideProductDao(database: AppDatabase): ProductDao {
        return database.productDao()
    }

    // 🔹 Cung cấp Repository sử dụng cả Firebase và Room
    @Provides
    @Singleton
    fun provideProductRepository(
        firestore: FirebaseFirestore,
        productDao: ProductDao
    ): ProductRepository {
        return ProductRepositoryImp(firestore, productDao)
    }
    @Provides
    @Singleton
    fun provideContext(application: android.app.Application): Context {
        return application.applicationContext
    }
}
