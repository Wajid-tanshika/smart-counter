package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SmartCounterDao {

    // --- Counting Sessions ---
    @Query("SELECT * FROM counting_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<CountingSessionEntity>>

    @Query("SELECT * FROM counting_sessions ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSession(): CountingSessionEntity?

    @Query("SELECT * FROM counting_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): CountingSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CountingSessionEntity): Long

    @Update
    suspend fun updateSession(session: CountingSessionEntity)

    @Query("DELETE FROM counting_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("DELETE FROM counting_sessions")
    suspend fun deleteAllSessions()

    // --- Inventory Items ---
    @Query("SELECT * FROM inventory_items ORDER BY updatedAt DESC")
    fun getAllInventory(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE barcode = :barcode LIMIT 1")
    suspend fun findInventoryByBarcode(barcode: String): InventoryItemEntity?

    @Query("SELECT * FROM inventory_items WHERE id = :id LIMIT 1")
    suspend fun getInventoryById(id: Long): InventoryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(item: InventoryItemEntity): Long

    @Update
    suspend fun updateInventory(item: InventoryItemEntity)

    @Query("UPDATE inventory_items SET quantity = :quantity, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateInventoryQuantity(id: Long, quantity: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteInventory(id: Long)

    @Query("DELETE FROM inventory_items")
    suspend fun deleteAllInventory()

    // --- Activity History ---
    @Query("SELECT * FROM activity_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ActivityHistoryEntity>>

    @Query("SELECT * FROM activity_history WHERE type = :type ORDER BY timestamp DESC")
    fun getHistoryByType(type: String): Flow<List<ActivityHistoryEntity>>

    @Query("SELECT * FROM activity_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 10): Flow<List<ActivityHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: ActivityHistoryEntity): Long

    @Query("DELETE FROM activity_history WHERE id = :id")
    suspend fun deleteHistory(id: Long)

    @Query("DELETE FROM activity_history")
    suspend fun deleteAllHistory()

    // --- Stats Aggregations ---
    @Query("SELECT COUNT(*) FROM counting_sessions")
    fun getTotalSessionsCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(count), 0) FROM counting_sessions")
    fun getTotalItemsCounted(): Flow<Int>

    @Query("SELECT COUNT(*) FROM activity_history WHERE type IN ('BARCODE', 'QR', 'BULK_SCAN')")
    fun getTotalScansCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM inventory_items")
    fun getTotalInventoryCount(): Flow<Int>
}
