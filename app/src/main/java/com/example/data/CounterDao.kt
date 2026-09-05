package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CounterDao {
    @Query("SELECT * FROM counter_state WHERE id = 1 LIMIT 1")
    fun getCounterState(): Flow<CounterState?>

    @Query("SELECT * FROM counter_state WHERE id = 1 LIMIT 1")
    suspend fun getCounterStateSync(): CounterState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateState(state: CounterState)

    @Query("SELECT * FROM counter_history ORDER BY timestamp DESC LIMIT 50")
    fun getHistory(): Flow<List<CounterHistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(entry: CounterHistoryEntry)

    @Query("DELETE FROM counter_history")
    suspend fun clearHistory()

    @Transaction
    suspend fun increment(byValue: Int = 1): Int {
        val currentState = getCounterStateSync() ?: CounterState()
        val newValue = currentState.countValue + byValue
        val updatedState = currentState.copy(countValue = newValue, updatedAt = System.currentTimeMillis())
        insertOrUpdateState(updatedState)
        insertHistoryEntry(
            CounterHistoryEntry(
                operation = "INCREMENT",
                valueAfter = newValue,
                timestamp = System.currentTimeMillis()
            )
        )
        return newValue
    }

    @Transaction
    suspend fun decrement(byValue: Int = 1): Int {
        val currentState = getCounterStateSync() ?: CounterState()
        val newValue = (currentState.countValue - byValue).coerceAtLeast(0)
        // If the current value was already 0, and we attempt to decrement, we can ignore or log it.
        if (currentState.countValue == 0) return 0
        val updatedState = currentState.copy(countValue = newValue, updatedAt = System.currentTimeMillis())
        insertOrUpdateState(updatedState)
        insertHistoryEntry(
            CounterHistoryEntry(
                operation = "DECREMENT",
                valueAfter = newValue,
                timestamp = System.currentTimeMillis()
            )
        )
        return newValue
    }

    @Transaction
    suspend fun reset(): Int {
        val updatedState = CounterState(countValue = 0, updatedAt = System.currentTimeMillis())
        insertOrUpdateState(updatedState)
        insertHistoryEntry(
            CounterHistoryEntry(
                operation = "RESET",
                valueAfter = 0,
                timestamp = System.currentTimeMillis()
            )
        )
        return 0
    }
}
