package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CountingSessionEntity::class,
        InventoryItemEntity::class,
        ActivityHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SmartCounterDatabase : RoomDatabase() {
    abstract fun smartCounterDao(): SmartCounterDao

    companion object {
        @Volatile
        private var INSTANCE: SmartCounterDatabase? = null

        fun getDatabase(context: Context): SmartCounterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmartCounterDatabase::class.java,
                    "smart_counter_db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
