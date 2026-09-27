package com.example.data

import com.example.alarm.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TaskRepository(
    private val taskDao: TaskDao,
    private val focusSessionDao: FocusSessionDao,
    private val alarmScheduler: AlarmScheduler
) {
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val pendingTasks: Flow<List<TaskEntity>> = taskDao.getPendingTasks()
    val allSessions: Flow<List<FocusSessionEntity>> = focusSessionDao.getAllSessions()
    val recentSessions: Flow<List<FocusSessionEntity>> = focusSessionDao.getRecentSessions(50)

    fun getTasksForDate(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>> {
        return taskDao.getTasksForDate(startOfDay, endOfDay)
    }

    fun getSessionsBetween(startTime: Long, endTime: Long): Flow<List<FocusSessionEntity>> {
        return focusSessionDao.getSessionsBetween(startTime, endTime)
    }

    suspend fun addTask(task: TaskEntity): Long {
        val id = taskDao.insertTask(task)
        val createdTask = task.copy(id = id)
        if (createdTask.reminderEnabled && !createdTask.isCompleted) {
            alarmScheduler.scheduleTaskReminder(createdTask)
        }
        return id
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
        if (task.reminderEnabled && !task.isCompleted) {
            alarmScheduler.scheduleTaskReminder(task)
        } else {
            alarmScheduler.cancelTaskReminder(task.id)
        }
    }

    suspend fun deleteTask(task: TaskEntity) {
        alarmScheduler.cancelTaskReminder(task.id)
        taskDao.deleteTask(task)
    }

    suspend fun toggleTaskCompleted(task: TaskEntity) {
        val newCompleted = !task.isCompleted
        val completedAt = if (newCompleted) System.currentTimeMillis() else null
        taskDao.updateTaskCompletion(task.id, newCompleted, completedAt)
        if (newCompleted) {
            alarmScheduler.cancelTaskReminder(task.id)
        } else if (task.reminderEnabled) {
            alarmScheduler.scheduleTaskReminder(task.copy(isCompleted = false))
        }
    }

    suspend fun recordFocusSession(session: FocusSessionEntity): Long {
        val sessionId = focusSessionDao.insertSession(session)
        if (session.taskId != null && session.actualSeconds > 0) {
            taskDao.addFocusTime(session.taskId, session.actualSeconds.toLong())
        }
        return sessionId
    }

    suspend fun seedInitialDataIfEmpty() {
        val existingTasks = taskDao.getPendingTasks()
        // Check if DB is brand new
        val count = taskDao.getActiveReminders().size
        val allDirect = focusSessionDao.getAllSessionsDirect()
        if (count == 0 && allDirect.isEmpty()) {
            val now = Calendar.getInstance()
            val todayMillis = now.timeInMillis

            val sampleTasks = listOf(
                TaskEntity(
                    title = "Review Machine Learning Notes",
                    description = "Chapter 4: Gradient descent, backpropagation, and loss surfaces",
                    category = "Study",
                    priority = "HIGH",
                    dueDateMillis = todayMillis,
                    dueTimeHour = 14,
                    dueTimeMinute = 30,
                    estimatedDurationMinutes = 45,
                    reminderEnabled = true,
                    reminderOffsetMinutes = 15
                ),
                TaskEntity(
                    title = "Complete Software Architecture Assignment",
                    description = "Write design document for distributed caching microservices",
                    category = "Project",
                    priority = "URGENT",
                    dueDateMillis = todayMillis,
                    dueTimeHour = 17,
                    dueTimeMinute = 0,
                    estimatedDurationMinutes = 60,
                    reminderEnabled = true,
                    reminderOffsetMinutes = 30
                ),
                TaskEntity(
                    title = "Algorithm Practice: Dynamic Programming",
                    description = "Solve 3 LeetCode medium questions on memoization & knapsack",
                    category = "Coding",
                    priority = "MEDIUM",
                    dueDateMillis = todayMillis + 86400000L,
                    dueTimeHour = 10,
                    dueTimeMinute = 0,
                    estimatedDurationMinutes = 50,
                    reminderEnabled = true,
                    reminderOffsetMinutes = 10
                ),
                TaskEntity(
                    title = "Read Research Paper on Attention Mechanisms",
                    description = "Annotate sections on Multi-head Self-Attention & FlashAttention",
                    category = "Reading",
                    priority = "LOW",
                    dueDateMillis = todayMillis + 86400000L * 2,
                    dueTimeHour = 16,
                    dueTimeMinute = 0,
                    estimatedDurationMinutes = 40,
                    reminderEnabled = false
                ),
                TaskEntity(
                    title = "Calculus Problem Set #3",
                    description = "Integration by parts and trigonometric substitution problems 1-15",
                    category = "Homework",
                    priority = "HIGH",
                    dueDateMillis = todayMillis - 86400000L,
                    dueTimeHour = 11,
                    dueTimeMinute = 0,
                    estimatedDurationMinutes = 60,
                    isCompleted = true,
                    completedAtMillis = todayMillis - 86000000L,
                    totalFocusSeconds = 3600
                )
            )

            for (task in sampleTasks) {
                val id = taskDao.insertTask(task)
                if (task.reminderEnabled && !task.isCompleted) {
                    alarmScheduler.scheduleTaskReminder(task.copy(id = id))
                }
            }

            // Seed a few past focus sessions for productivity analytics demonstration
            val sampleSessions = listOf(
                FocusSessionEntity(
                    taskTitle = "Calculus Problem Set #3",
                    category = "Homework",
                    durationSeconds = 1800,
                    actualSeconds = 1800,
                    sessionType = "POMODORO",
                    isCompleted = true,
                    rating = 5,
                    notes = "Finished questions 1 to 8 without distractions!",
                    timestampMillis = todayMillis - (86400000L * 2) + 36000000L
                ),
                FocusSessionEntity(
                    taskTitle = "Calculus Problem Set #3",
                    category = "Homework",
                    durationSeconds = 1800,
                    actualSeconds = 1800,
                    sessionType = "POMODORO",
                    isCompleted = true,
                    rating = 4,
                    notes = "Solved difficult trig substitutions",
                    timestampMillis = todayMillis - (86400000L * 2) + 40000000L
                ),
                FocusSessionEntity(
                    taskTitle = "Review Machine Learning Notes",
                    category = "Study",
                    durationSeconds = 2700,
                    actualSeconds = 2700,
                    sessionType = "CUSTOM_TIMER",
                    isCompleted = true,
                    rating = 5,
                    notes = "Great deep work session on backpropagation proofs",
                    timestampMillis = todayMillis - 86400000L + 25000000L
                ),
                FocusSessionEntity(
                    taskTitle = "Algorithm Practice: Dynamic Programming",
                    category = "Coding",
                    durationSeconds = 1500,
                    actualSeconds = 1500,
                    sessionType = "POMODORO",
                    isCompleted = true,
                    rating = 4,
                    notes = "Knapsack DP table visualization",
                    timestampMillis = todayMillis - 18000000L
                )
            )

            for (session in sampleSessions) {
                focusSessionDao.insertSession(session)
            }
        }
    }
}
