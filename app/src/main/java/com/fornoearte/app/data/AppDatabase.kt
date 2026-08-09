package com.fornoearte.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

class Converters {
    @TypeConverter fun status(value: String) = OrderStatus.valueOf(value)
    @TypeConverter fun status(value: OrderStatus) = value.name
    @TypeConverter fun payment(value: String) = PaymentMethod.valueOf(value)
    @TypeConverter fun payment(value: PaymentMethod) = value.name
}

@Dao interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC") fun observeAll(): Flow<List<OrderEntity>>
    @Query("SELECT * FROM orders WHERE customerName LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR CAST(id AS TEXT) LIKE '%' || :query || '%' ORDER BY createdAt DESC") fun search(query: String): Flow<List<OrderEntity>>
    @Insert suspend fun insert(order: OrderEntity): Long
    @Update suspend fun update(order: OrderEntity)
    @Delete suspend fun delete(order: OrderEntity)
}

@Database(entities = [OrderEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    companion object { fun create(context: Context) = Room.databaseBuilder(context, AppDatabase::class.java, "forno-e-arte.db").fallbackToDestructiveMigration().build() }
}
