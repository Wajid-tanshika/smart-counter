package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val currentCount: Int = 0,
    val targetCount: Int,
    val startingCount: Int = 0,
    val unit: String = "times", // e.g., "glasses", "reps", "questions", "counts", "pages", "steps"
    val iconName: String = "flag", // "water", "fitness", "book", "prayer", "walk", "study", "flag", "star"
    val colorHex: String = "#3B82F6",
    val isDaily: Boolean = true,
    val isPaused: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdatedAt: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = if (targetCount > 0) (currentCount.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = (progressFraction * 100).toInt()

    val isCompleted: Boolean
        get() = targetCount > 0 && currentCount >= targetCount
}

@Entity(tableName = "goal_history")
data class GoalHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val previousValue: Int,
    val newValue: Int,
    val targetValue: Int,
    val operation: String, // "INCREMENT", "DECREMENT", "RESET", "EDIT", "COMPLETED"
    val progressPercent: Int,
    val isCompleted: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
