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

@Entity(tableName = "scanned_products")
data class ScannedProductEntity(
    @PrimaryKey val barcode: String,
    val name: String,
    val brand: String = "",
    val imageUrl: String = "",
    val category: String = "",
    val quantityInfo: String = "",
    val price: String = "",
    val description: String = "",
    val manufacturer: String = "",
    val countryOfOrigin: String = "",
    val ingredients: String = "",
    val format: String = "",
    val isUrl: Boolean = false,
    val url: String = "",
    val scannedAt: Long = System.currentTimeMillis()
)

data class ScannedProductInfo(
    val barcode: String,
    val name: String = "",
    val brand: String = "",
    val imageUrl: String = "",
    val category: String = "",
    val quantityInfo: String = "",
    val price: String = "",
    val description: String = "",
    val manufacturer: String = "",
    val countryOfOrigin: String = "",
    val ingredients: String = "",
    val format: String = "BARCODE",
    val isUrl: Boolean = false,
    val url: String = "",
    val isFound: Boolean = true,
    val isOffline: Boolean = false,
    val scannedAt: Long = System.currentTimeMillis()
) {
    val formattedDate: String
        get() = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(scannedAt))
}

