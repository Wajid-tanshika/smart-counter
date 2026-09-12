package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "counter_state")
data class CounterState(
    @PrimaryKey val id: Int = 1,
    val countValue: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "counter_history")
data class CounterHistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val operation: String, // "INCREMENT", "DECREMENT", "RESET"
    val valueAfter: Int
)
