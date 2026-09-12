package com.example.data

import kotlinx.coroutines.flow.Flow

class GoalRepository(private val goalDao: GoalDao) {
    val allGoals: Flow<List<GoalEntity>> = goalDao.getAllGoals()

    fun getGoalById(id: Long): Flow<GoalEntity?> = goalDao.getGoalById(id)

    fun getGoalHistory(goalId: Long): Flow<List<GoalHistoryEntity>> = goalDao.getGoalHistory(goalId)

    suspend fun insertGoal(goal: GoalEntity): Long = goalDao.insertGoal(goal)

    suspend fun updateGoal(goal: GoalEntity) = goalDao.updateGoal(goal)

    suspend fun deleteGoal(id: Long) = goalDao.deleteGoalAndHistory(id)

    suspend fun incrementGoal(goalId: Long, step: Int = 1): GoalEntity? = goalDao.incrementGoal(goalId, step)

    suspend fun decrementGoal(goalId: Long, step: Int = 1): GoalEntity? = goalDao.decrementGoal(goalId, step)

    suspend fun resetGoal(goalId: Long): GoalEntity? = goalDao.resetGoal(goalId)

    suspend fun togglePauseGoal(goalId: Long): GoalEntity? = goalDao.togglePauseGoal(goalId)

    suspend fun clearGoalHistory(goalId: Long) = goalDao.clearGoalHistory(goalId)
}
