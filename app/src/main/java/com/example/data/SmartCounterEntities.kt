package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "counting_sessions")
data class CountingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val count: Int,
    val target: Int = 0,
    val category: String = "General",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val barcode: String = "",
    val quantity: Int = 0,
    val minStock: Int = 10,
    val category: String = "General",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean
        get() = quantity in 1..minStock

    val isOutOfStock: Boolean
        get() = quantity <= 0
}

@Entity(tableName = "activity_history")
data class ActivityHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "COUNT", "BARCODE", "QR", "INVENTORY", "BULK_SCAN"
    val title: String,
    val result: String,
    val quantity: Int = 1,
    val category: String = "General",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
