package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GoalEntity
import com.example.data.GoalHistoryEntity
import com.example.data.GoalRepository
import com.example.data.GoalsDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class GoalFilter { ALL, ACTIVE, DAILY, COMPLETED }

data class GoalStats(
    val totalGoals: Int = 0,
    val activeGoals: Int = 0,
    val completedGoals: Int = 0,
    val totalProgressPercent: Int = 0,
    val bestPerformingGoalName: String? = null,
    val bestPerformingGoalProgress: Int = 0,
    val dailyProgressPercent: Int = 0,
    val dailyGoalsCount: Int = 0
)

class GoalsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GoalRepository(GoalsDatabase.getDatabase(application).goalDao())
    private val prefs = application.getSharedPreferences("goals_prefs", Context.MODE_PRIVATE)

    private val _selectedFilter = MutableStateFlow(GoalFilter.ALL)
    val selectedFilter: StateFlow<GoalFilter> = _selectedFilter.asStateFlow()

    private val _selectedGoalForHistory = MutableStateFlow<Long?>(null)
    val selectedGoalForHistory: StateFlow<Long?> = _selectedGoalForHistory.asStateFlow()

    private val _goalHistoryList = MutableStateFlow<List<GoalHistoryEntity>>(emptyList())
    val goalHistoryList: StateFlow<List<GoalHistoryEntity>> = _goalHistoryList.asStateFlow()

    val allGoals: StateFlow<List<GoalEntity>> = repository.allGoals.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredGoals: StateFlow<List<GoalEntity>> = combine(allGoals, _selectedFilter) { list, filter ->
        when (filter) {
            GoalFilter.ALL -> list
            GoalFilter.ACTIVE -> list.filter { !it.isPaused && !it.isCompleted }
            GoalFilter.DAILY -> list.filter { it.isDaily }
            GoalFilter.COMPLETED -> list.filter { it.isCompleted }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val stats: StateFlow<GoalStats> = allGoals.combine(_selectedFilter) { list, _ ->
        if (list.isEmpty()) {
            GoalStats()
        } else {
            val total = list.size
            val active = list.count { !it.isPaused && !it.isCompleted }
            val completed = list.count { it.isCompleted }
            val avgProgress = (list.sumOf { it.progressPercent } / total.toDouble()).toInt()
            val bestGoal = list.maxByOrNull { it.progressPercent }
            val dailyList = list.filter { it.isDaily }
            val dailyProgress = if (dailyList.isNotEmpty()) {
                (dailyList.sumOf { it.progressPercent } / dailyList.size.toDouble()).toInt()
            } else 0

            GoalStats(
                totalGoals = total,
                activeGoals = active,
                completedGoals = completed,
                totalProgressPercent = avgProgress,
                bestPerformingGoalName = bestGoal?.name,
                bestPerformingGoalProgress = bestGoal?.progressPercent ?: 0,
                dailyProgressPercent = dailyProgress,
                dailyGoalsCount = dailyList.size
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalStats()
    )

    init {
        // Observe selected goal's history
        viewModelScope.launch {
            _selectedGoalForHistory.collect { goalId ->
                if (goalId != null) {
                    repository.getGoalHistory(goalId).collect { history ->
                        _goalHistoryList.value = history
                    }
                } else {
                    _goalHistoryList.value = emptyList()
                }
            }
        }
    }

    fun resetSelectedState() {
        _selectedFilter.value = GoalFilter.ALL
        _selectedGoalForHistory.value = null
    }

    fun setFilter(filter: GoalFilter) {
        _selectedFilter.value = filter
    }

    fun selectGoalForHistory(goalId: Long?) {
        _selectedGoalForHistory.value = goalId
    }

    fun addGoal(
        name: String,
        targetCount: Int,
        startingCount: Int = 0,
        unit: String = "times",
        iconName: String = "flag",
        colorHex: String = "#3B82F6",
        isDaily: Boolean = true
    ) {
        viewModelScope.launch {
            val newGoal = GoalEntity(
                name = name.trim(),
                targetCount = targetCount.coerceAtLeast(1),
                startingCount = startingCount.coerceAtLeast(0),
                currentCount = startingCount.coerceAtLeast(0),
                unit = if (unit.isBlank()) "times" else unit.trim(),
                iconName = iconName,
                colorHex = colorHex,
                isDaily = isDaily,
                isPaused = false,
                createdAt = System.currentTimeMillis(),
                lastUpdatedAt = System.currentTimeMillis()
            )
            val id = repository.insertGoal(newGoal)
            if (startingCount > 0) {
                repository.getGoalHistory(id) // initialized
            }
        }
    }

    fun updateGoal(
        id: Long,
        name: String,
        targetCount: Int,
        unit: String,
        iconName: String,
        colorHex: String,
        isDaily: Boolean
    ) {
        viewModelScope.launch {
            val current = allGoals.value.find { it.id == id } ?: return@launch
            val updated = current.copy(
                name = name.trim(),
                targetCount = targetCount.coerceAtLeast(1),
                unit = if (unit.isBlank()) "times" else unit.trim(),
                iconName = iconName,
                colorHex = colorHex,
                isDaily = isDaily,
                lastUpdatedAt = System.currentTimeMillis()
            )
            repository.updateGoal(updated)
        }
    }

    fun incrementGoal(goalId: Long, step: Int = 1, onResult: ((GoalEntity?) -> Unit)? = null) {
        viewModelScope.launch {
            val updated = repository.incrementGoal(goalId, step)
            onResult?.invoke(updated)
        }
    }

    fun decrementGoal(goalId: Long, step: Int = 1, onResult: ((GoalEntity?) -> Unit)? = null) {
        viewModelScope.launch {
            val updated = repository.decrementGoal(goalId, step)
            onResult?.invoke(updated)
        }
    }

    fun resetGoal(goalId: Long) {
        viewModelScope.launch {
            repository.resetGoal(goalId)
        }
    }

    fun togglePause(goalId: Long) {
        viewModelScope.launch {
            repository.togglePauseGoal(goalId)
        }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            if (_selectedGoalForHistory.value == goalId) {
                _selectedGoalForHistory.value = null
            }
            repository.deleteGoal(goalId)
        }
    }

    fun clearGoalHistory(goalId: Long) {
        viewModelScope.launch {
            repository.clearGoalHistory(goalId)
        }
    }
}
