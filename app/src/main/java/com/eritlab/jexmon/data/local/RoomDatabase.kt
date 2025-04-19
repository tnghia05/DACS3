package com.eritlab.jexmon.data.local

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import com.eritlab.jexmon.data.local.dao.ProductDao
import com.eritlab.jexmon.data.local.dao.ProductStockDao
import com.eritlab.jexmon.data.local.entity.ProductEntity
import com.eritlab.jexmon.data.local.entity.ProductStockEntity
import androidx.sqlite.db.SupportSQLiteDatabase

@TypeConverters(Converters::class)
@Database(entities = [ProductEntity::class, ProductStockEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun productStockDao(): ProductStockDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null


        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "database-name"
                )
                    .fallbackToDestructiveMigration() // Thêm dòng này

                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
