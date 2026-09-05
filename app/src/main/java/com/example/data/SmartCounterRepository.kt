package com.example.data

import kotlinx.coroutines.flow.Flow

class SmartCounterRepository(private val dao: SmartCounterDao) {

    // Sessions
    val allSessions: Flow<List<CountingSessionEntity>> = dao.getAllSessions()
    suspend fun getLatestSession(): CountingSessionEntity? = dao.getLatestSession()
    suspend fun saveSession(title: String, count: Int, target: Int, category: String, notes: String): Long {
        val session = CountingSessionEntity(
            title = title.ifBlank { "Counting Session" },
            count = count,
            target = target,
            category = category.ifBlank { "General" },
            notes = notes,
            timestamp = System.currentTimeMillis()
        )
        val id = dao.insertSession(session)
        // Also record in activity history
        dao.insertHistory(
            ActivityHistoryEntity(
                type = "COUNT",
                title = session.title,
                result = "$count items",
                quantity = count,
                category = session.category,
                notes = notes,
                timestamp = session.timestamp
            )
        )
        return id
    }
    suspend fun updateSession(session: CountingSessionEntity) = dao.updateSession(session)
    suspend fun deleteSession(id: Long) = dao.deleteSession(id)
    suspend fun deleteAllSessions() = dao.deleteAllSessions()

    // Inventory
    val allInventory: Flow<List<InventoryItemEntity>> = dao.getAllInventory()
    suspend fun findInventoryByBarcode(barcode: String): InventoryItemEntity? = dao.findInventoryByBarcode(barcode.trim())
    suspend fun saveInventoryItem(
        id: Long = 0,
        name: String,
        barcode: String,
        quantity: Int,
        minStock: Int,
        category: String,
        notes: String
    ): Long {
        val item = InventoryItemEntity(
            id = id,
            name = name.ifBlank { "New Product" },
            barcode = barcode.trim(),
            quantity = quantity,
            minStock = minStock,
            category = category.ifBlank { "General" },
            notes = notes,
            updatedAt = System.currentTimeMillis()
        )
        val itemId = dao.insertInventory(item)
        dao.insertHistory(
            ActivityHistoryEntity(
                type = "INVENTORY",
                title = item.name,
                result = "Qty: ${item.quantity}",
                quantity = item.quantity,
                category = item.category,
                notes = if (barcode.isNotBlank()) "Barcode: $barcode" else notes,
                timestamp = item.updatedAt
            )
        )
        return itemId
    }
    suspend fun adjustInventoryQuantity(id: Long, newQuantity: Int, itemName: String = "") {
        dao.updateInventoryQuantity(id, newQuantity)
        dao.insertHistory(
            ActivityHistoryEntity(
                type = "INVENTORY",
                title = itemName.ifBlank { "Inventory Adjustment" },
                result = "Updated Qty: $newQuantity",
                quantity = newQuantity,
                timestamp = System.currentTimeMillis()
            )
        )
    }
    suspend fun deleteInventory(id: Long) = dao.deleteInventory(id)
    suspend fun deleteAllInventory() = dao.deleteAllInventory()

    // Activity History
    val allHistory: Flow<List<ActivityHistoryEntity>> = dao.getAllHistory()
    val recentHistory: Flow<List<ActivityHistoryEntity>> = dao.getRecentHistory(15)
    suspend fun recordScan(type: String, title: String, rawValue: String, quantity: Int = 1, notes: String = ""): Long {
        return dao.insertHistory(
            ActivityHistoryEntity(
                type = type, // "BARCODE", "QR", "BULK_SCAN"
                title = title,
                result = rawValue,
                quantity = quantity,
                notes = notes,
                timestamp = System.currentTimeMillis()
            )
        )
    }
    suspend fun deleteHistory(id: Long) = dao.deleteHistory(id)
    suspend fun deleteAllHistory() = dao.deleteAllHistory()

    // Aggregations
    val totalSessionsCount: Flow<Int> = dao.getTotalSessionsCount()
    val totalItemsCounted: Flow<Int> = dao.getTotalItemsCounted()
    val totalScansCount: Flow<Int> = dao.getTotalScansCount()
    val totalInventoryCount: Flow<Int> = dao.getTotalInventoryCount()
}
