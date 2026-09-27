package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SubscriptionManager
import com.example.data.TaskCategory
import com.example.data.TaskEntity
import com.example.data.TaskPriority
import com.example.data.TaskRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class TaskFilter(val displayName: String) {
    ALL("All"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    OVERDUE("Overdue"),
    COMPLETED("Completed")
}

data class TaskUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val filteredTasks: List<TaskEntity> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: TaskFilter = TaskFilter.ALL,
    val selectedCategory: TaskCategory? = null,
    val todayTasksCount: Int = 0,
    val completedTodayCount: Int = 0,
    val totalCompletedCount: Int = 0
)

class TaskViewModel(
    private val repository: TaskRepository,
    private val subscriptionManager: SubscriptionManager? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow(TaskFilter.ALL)
    private val _selectedCategory = MutableStateFlow<TaskCategory?>(null)
    private val _triggerSubscriptionPrompt = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val triggerSubscriptionPrompt = _triggerSubscriptionPrompt.asSharedFlow()

    val uiState: StateFlow<TaskUiState> = combine(
        repository.allTasks,
        _searchQuery,
        _activeFilter,
        _selectedCategory
    ) { tasks, query, filter, category ->
        val startOfToday = getStartOfDayMillis(0)
        val endOfToday = getEndOfDayMillis(0)

        var filtered = tasks

        // Apply Category filter
        if (category != null) {
            filtered = filtered.filter { it.category.equals(category.displayName, ignoreCase = true) || it.category.equals(category.name, ignoreCase = true) }
        }

        // Apply Status / Date Filter
        filtered = when (filter) {
            TaskFilter.ALL -> filtered
            TaskFilter.TODAY -> filtered.filter {
                it.dueDateMillis in startOfToday..endOfToday
            }
            TaskFilter.UPCOMING -> filtered.filter {
                !it.isCompleted && it.dueDateMillis > endOfToday
            }
            TaskFilter.OVERDUE -> filtered.filter {
                !it.isCompleted && it.dueDateMillis < startOfToday
            }
            TaskFilter.COMPLETED -> filtered.filter { it.isCompleted }
        }

        // Apply Search query
        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true)
            }
        }

        val todayTasks = tasks.filter { it.dueDateMillis in startOfToday..endOfToday }
        val completedToday = todayTasks.count { it.isCompleted }
        val totalCompleted = tasks.count { it.isCompleted }

        TaskUiState(
            tasks = tasks,
            filteredTasks = filtered,
            searchQuery = query,
            activeFilter = filter,
            selectedCategory = category,
            todayTasksCount = todayTasks.size,
            completedTodayCount = completedToday,
            totalCompletedCount = totalCompleted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskUiState()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: TaskFilter) {
        _activeFilter.value = filter
    }

    fun selectCategory(category: TaskCategory?) {
        _selectedCategory.value = category
    }

    fun toggleTaskCompletion(task: TaskEntity, isPro: Boolean = false) {
        viewModelScope.launch {
            val wasCompleted = task.isCompleted
            repository.toggleTaskCompleted(task)
            if (!wasCompleted && !isPro) {
                val milestoneReached = subscriptionManager?.recordDemoTaskCompleted() ?: false
                val newCompletedCount = uiState.value.totalCompletedCount + 1
                if (milestoneReached || newCompletedCount >= 2) {
                    _triggerSubscriptionPrompt.tryEmit(true)
                }
            }
        }
    }

    fun addQuickDemoTask(title: String, category: String, estimatedMinutes: Int = 30) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val task = TaskEntity(
                id = 0L,
                title = title,
                description = "Demo task created to test Task Scheduler & focus workflow",
                category = category,
                priority = TaskPriority.HIGH.name,
                dueDateMillis = cal.timeInMillis,
                dueTimeHour = cal.get(Calendar.HOUR_OF_DAY) + 1,
                dueTimeMinute = 0,
                estimatedDurationMinutes = estimatedMinutes,
                reminderEnabled = true,
                reminderOffsetMinutes = 15,
                isCompleted = false
            )
            repository.addTask(task)
            val milestoneReached = subscriptionManager?.recordDemoTaskAdded() ?: false
            if (milestoneReached) {
                _triggerSubscriptionPrompt.tryEmit(true)
            }
        }
    }

    fun saveTask(
        id: Long = 0,
        title: String,
        description: String,
        category: String,
        priority: TaskPriority,
        dueDateMillis: Long,
        dueHour: Int,
        dueMinute: Int,
        estimatedDurationMinutes: Int,
        reminderEnabled: Boolean,
        reminderOffsetMinutes: Int,
        isPro: Boolean = false
    ) {
        viewModelScope.launch {
            val task = TaskEntity(
                id = id,
                title = title.trim(),
                description = description.trim(),
                category = category,
                priority = priority.name,
                dueDateMillis = dueDateMillis,
                dueTimeHour = dueHour,
                dueTimeMinute = dueMinute,
                estimatedDurationMinutes = estimatedDurationMinutes,
                reminderEnabled = reminderEnabled,
                reminderOffsetMinutes = reminderOffsetMinutes,
                isCompleted = false
            )
            if (id == 0L) {
                repository.addTask(task)
                val milestoneReached = subscriptionManager?.recordDemoTaskAdded() ?: false
                if (!isPro && (milestoneReached || uiState.value.totalCompletedCount >= 2)) {
                    _triggerSubscriptionPrompt.tryEmit(true)
                }
            } else {
                repository.updateTask(task)
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    private fun getStartOfDayMillis(daysOffset: Int): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysOffset)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getEndOfDayMillis(daysOffset: Int): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysOffset)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    class Factory(
        private val repository: TaskRepository,
        private val subscriptionManager: SubscriptionManager? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TaskViewModel(repository, subscriptionManager) as T
        }
    }
}
