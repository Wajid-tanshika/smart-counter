package com.example.data

import kotlinx.coroutines.flow.Flow

class CounterRepository(private val counterDao: CounterDao) {
    val counterState: Flow<CounterState?> = counterDao.getCounterState()
    val history: Flow<List<CounterHistoryEntry>> = counterDao.getHistory()

    suspend fun getCounterStateSync(): CounterState? {
        return counterDao.getCounterStateSync()
    }

    suspend fun increment(): Int {
        return counterDao.increment()
    }

    suspend fun decrement(): Int {
        return counterDao.decrement()
    }

    suspend fun reset(): Int {
        return counterDao.reset()
    }

    suspend fun saveCountValue(value: Int, operation: String = "SYNC") {
        counterDao.insertOrUpdateState(CounterState(id = 1, countValue = value, updatedAt = System.currentTimeMillis()))
        // Temporarily removed history insertion
    }

    suspend fun saveSessionToHistory(finalCount: Int) {
        counterDao.insertHistoryEntry(
            CounterHistoryEntry(
                operation = "Saved Count",
                valueAfter = finalCount,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearHistory() {
        counterDao.clearHistory()
    }
}
