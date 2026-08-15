package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY isPaused ASC, id DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    fun getGoalById(id: Long): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun getGoalByIdSync(id: Long): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: Long)

    @Query("SELECT * FROM goal_history WHERE goalId = :goalId ORDER BY timestamp DESC LIMIT 50")
    fun getGoalHistory(goalId: Long): Flow<List<GoalHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalHistory(history: GoalHistoryEntity)

    @Query("DELETE FROM goal_history WHERE goalId = :goalId")
    suspend fun clearGoalHistory(goalId: Long)

    @Query("DELETE FROM goal_history WHERE goalId = :goalId")
    suspend fun deleteHistoryByGoalId(goalId: Long)

    @Transaction
    suspend fun deleteGoalAndHistory(goalId: Long) {
        deleteHistoryByGoalId(goalId)
        deleteGoalById(goalId)
    }

    @Transaction
    suspend fun incrementGoal(goalId: Long, step: Int = 1): GoalEntity? {
        val goal = getGoalByIdSync(goalId) ?: return null
        if (goal.isPaused) return goal

        val prevValue = goal.currentCount
        val newValue = prevValue + step
        val isNowCompleted = goal.targetCount > 0 && newValue >= goal.targetCount
        val wasCompleted = goal.targetCount > 0 && prevValue >= goal.targetCount
        val updatedGoal = goal.copy(
            currentCount = newValue,
            lastUpdatedAt = System.currentTimeMillis()
        )
        updateGoal(updatedGoal)

        val operation = if (!wasCompleted && isNowCompleted) "COMPLETED" else "INCREMENT"
        val progress = if (goal.targetCount > 0) ((newValue.toFloat() / goal.targetCount) * 100).toInt() else 0

        insertGoalHistory(
            GoalHistoryEntity(
                goalId = goalId,
                previousValue = prevValue,
                newValue = newValue,
                targetValue = goal.targetCount,
                operation = operation,
                progressPercent = progress,
                isCompleted = isNowCompleted,
                timestamp = System.currentTimeMillis()
            )
        )
        return updatedGoal
    }

    @Transaction
    suspend fun decrementGoal(goalId: Long, step: Int = 1): GoalEntity? {
        val goal = getGoalByIdSync(goalId) ?: return null
        if (goal.isPaused) return goal
        if (goal.currentCount <= 0) return goal

        val prevValue = goal.currentCount
        val newValue = (prevValue - step).coerceAtLeast(0)
        val isNowCompleted = goal.targetCount > 0 && newValue >= goal.targetCount
        val updatedGoal = goal.copy(
            currentCount = newValue,
            lastUpdatedAt = System.currentTimeMillis()
        )
        updateGoal(updatedGoal)

        val progress = if (goal.targetCount > 0) ((newValue.toFloat() / goal.targetCount) * 100).toInt() else 0

        insertGoalHistory(
            GoalHistoryEntity(
                goalId = goalId,
                previousValue = prevValue,
                newValue = newValue,
                targetValue = goal.targetCount,
                operation = "DECREMENT",
                progressPercent = progress,
                isCompleted = isNowCompleted,
                timestamp = System.currentTimeMillis()
            )
        )
        return updatedGoal
    }

    @Transaction
    suspend fun resetGoal(goalId: Long): GoalEntity? {
        val goal = getGoalByIdSync(goalId) ?: return null
        val prevValue = goal.currentCount
        val resetValue = goal.startingCount
        val updatedGoal = goal.copy(
            currentCount = resetValue,
            lastUpdatedAt = System.currentTimeMillis()
        )
        updateGoal(updatedGoal)

        val progress = if (goal.targetCount > 0) ((resetValue.toFloat() / goal.targetCount) * 100).toInt() else 0

        insertGoalHistory(
            GoalHistoryEntity(
                goalId = goalId,
                previousValue = prevValue,
                newValue = resetValue,
                targetValue = goal.targetCount,
                operation = "RESET",
                progressPercent = progress,
                isCompleted = false,
                timestamp = System.currentTimeMillis()
            )
        )
        return updatedGoal
    }

    @Transaction
    suspend fun togglePauseGoal(goalId: Long): GoalEntity? {
        val goal = getGoalByIdSync(goalId) ?: return null
        val updatedGoal = goal.copy(
            isPaused = !goal.isPaused,
            lastUpdatedAt = System.currentTimeMillis()
        )
        updateGoal(updatedGoal)
        return updatedGoal
    }
}
