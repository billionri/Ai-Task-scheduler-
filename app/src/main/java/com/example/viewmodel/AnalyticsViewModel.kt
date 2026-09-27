package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.FocusSessionEntity
import com.example.data.TaskEntity
import com.example.data.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DailyFocusData(
    val dayLabel: String,
    val dateMillis: Long,
    val focusMinutes: Int,
    val isToday: Boolean
)

data class CategoryShare(
    val categoryName: String,
    val minutes: Int,
    val percentage: Float
)

data class ProductivityAnalyticsUiState(
    val todayFocusMinutes: Int = 0,
    val weekFocusMinutes: Int = 0,
    val totalFocusHours: Float = 0f,
    val totalSessionsCount: Int = 0,
    val completedTasksCount: Int = 0,
    val totalTasksCount: Int = 0,
    val taskCompletionRatePercent: Int = 0,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val averageRating: Float = 5.0f,
    val dailyTrend: List<DailyFocusData> = emptyList(),
    val categoryShares: List<CategoryShare> = emptyList(),
    val recentSessions: List<FocusSessionEntity> = emptyList()
)

class AnalyticsViewModel(private val repository: TaskRepository) : ViewModel() {

    val uiState: StateFlow<ProductivityAnalyticsUiState> = combine(
        repository.allSessions,
        repository.allTasks
    ) { sessions, tasks ->
        computeAnalytics(sessions, tasks)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProductivityAnalyticsUiState()
    )

    private fun computeAnalytics(
        sessions: List<FocusSessionEntity>,
        tasks: List<TaskEntity>
    ): ProductivityAnalyticsUiState {
        val now = Calendar.getInstance()
        val todayStart = getStartOfDay(now).timeInMillis
        val todayEnd = getEndOfDay(now).timeInMillis

        // 7 days ago start
        val weekAgoCal = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -6)
        }
        val weekStart = getStartOfDay(weekAgoCal).timeInMillis

        // Today minutes
        val todaySessions = sessions.filter { it.timestampMillis in todayStart..todayEnd }
        val todayMinutes = todaySessions.sumOf { it.actualSeconds } / 60

        // Week minutes
        val weekSessions = sessions.filter { it.timestampMillis >= weekStart }
        val weekMinutes = weekSessions.sumOf { it.actualSeconds } / 60

        // Total hours
        val totalSeconds = sessions.sumOf { it.actualSeconds }
        val totalHours = totalSeconds / 3600f

        // Task Completion
        val totalTasks = tasks.size
        val completedTasks = tasks.count { it.isCompleted }
        val completionRate = if (totalTasks > 0) ((completedTasks.toFloat() / totalTasks) * 100).toInt() else 0

        // 7-day trend
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dailyList = mutableListOf<DailyFocusData>()
        for (i in 6 downTo 0) {
            val dayCal = (now.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val dStart = getStartOfDay(dayCal).timeInMillis
            val dEnd = getEndOfDay(dayCal).timeInMillis
            val dayMins = sessions
                .filter { it.timestampMillis in dStart..dEnd }
                .sumOf { it.actualSeconds } / 60

            val label = if (i == 0) "Today" else dayFormat.format(dayCal.time)
            dailyList.add(
                DailyFocusData(
                    dayLabel = label,
                    dateMillis = dStart,
                    focusMinutes = dayMins,
                    isToday = (i == 0)
                )
            )
        }

        // Category breakdown
        val categoryMinutesMap = mutableMapOf<String, Int>()
        for (session in sessions) {
            val cat = session.category.ifBlank { "Study" }
            val mins = session.actualSeconds / 60
            categoryMinutesMap[cat] = (categoryMinutesMap[cat] ?: 0) + mins
        }
        val allCategoryMins = categoryMinutesMap.values.sum().coerceAtLeast(1)
        val categoryShares = categoryMinutesMap.map { (cat, mins) ->
            CategoryShare(
                categoryName = cat,
                minutes = mins,
                percentage = (mins.toFloat() / allCategoryMins) * 100f
            )
        }.sortedByDescending { it.minutes }

        // Streak calculation
        val (currentStreak, maxStreak) = calculateStreaks(sessions)

        // Average rating
        val ratings = sessions.map { it.rating }.filter { it > 0 }
        val avgRating = if (ratings.isNotEmpty()) ratings.average().toFloat() else 5.0f

        return ProductivityAnalyticsUiState(
            todayFocusMinutes = todayMinutes,
            weekFocusMinutes = weekMinutes,
            totalFocusHours = totalHours,
            totalSessionsCount = sessions.size,
            completedTasksCount = completedTasks,
            totalTasksCount = totalTasks,
            taskCompletionRatePercent = completionRate,
            currentStreakDays = currentStreak,
            longestStreakDays = maxStreak,
            averageRating = avgRating,
            dailyTrend = dailyList,
            categoryShares = categoryShares,
            recentSessions = sessions.take(25)
        )
    }

    private fun calculateStreaks(sessions: List<FocusSessionEntity>): Pair<Int, Int> {
        if (sessions.isEmpty()) return Pair(0, 0)

        val uniqueDaySet = sessions.map {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestampMillis }
            "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
        }.toSet()

        val now = Calendar.getInstance()
        var currentStreak = 0
        var checkCal = now.clone() as Calendar

        // Check if studied today
        val todayKey = "${checkCal.get(Calendar.YEAR)}-${checkCal.get(Calendar.DAY_OF_YEAR)}"
        var hasToday = uniqueDaySet.contains(todayKey)
        if (hasToday) {
            currentStreak++
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            // Check if studied yesterday to keep streak active
            val yesterdayCal = (checkCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
            val yKey = "${yesterdayCal.get(Calendar.YEAR)}-${yesterdayCal.get(Calendar.DAY_OF_YEAR)}"
            if (uniqueDaySet.contains(yKey)) {
                checkCal = yesterdayCal
            } else {
                return Pair(0, 1)
            }
        }

        while (true) {
            val key = "${checkCal.get(Calendar.YEAR)}-${checkCal.get(Calendar.DAY_OF_YEAR)}"
            if (uniqueDaySet.contains(key)) {
                if (!hasToday || currentStreak > 1) {
                    currentStreak++
                }
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        val longestStreak = currentStreak.coerceAtLeast(1)
        return Pair(currentStreak, longestStreak)
    }

    private fun getStartOfDay(cal: Calendar): Calendar {
        return (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun getEndOfDay(cal: Calendar): Calendar {
        return (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
    }

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AnalyticsViewModel(repository) as T
        }
    }
}
