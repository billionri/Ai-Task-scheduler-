package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, dueDateMillis ASC, dueTimeHour ASC, dueTimeMinute ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dueDateMillis ASC, dueTimeHour ASC, dueTimeMinute ASC")
    fun getPendingTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDateMillis >= :startOfDay AND dueDateMillis <= :endOfDay ORDER BY dueTimeHour ASC, dueTimeMinute ASC")
    fun getTasksForDate(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskById(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskByIdDirect(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND reminderEnabled = 1")
    suspend fun getActiveReminders(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("UPDATE tasks SET isCompleted = :completed, completedAtMillis = :completedAt WHERE id = :id")
    suspend fun updateTaskCompletion(id: Long, completed: Boolean, completedAt: Long?)

    @Query("UPDATE tasks SET totalFocusSeconds = totalFocusSeconds + :seconds WHERE id = :id")
    suspend fun addFocusTime(id: Long, seconds: Long)
}
