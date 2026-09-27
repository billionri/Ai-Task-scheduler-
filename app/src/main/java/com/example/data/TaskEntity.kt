package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "Study",
    val priority: String = "MEDIUM",
    val dueDateMillis: Long,
    val dueTimeHour: Int = 12,
    val dueTimeMinute: Int = 0,
    val estimatedDurationMinutes: Int = 30,
    val recurrence: String = "NONE",
    val reminderEnabled: Boolean = true,
    val reminderOffsetMinutes: Int = 15,
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val totalFocusSeconds: Long = 0L,
    val createdAtMillis: Long = System.currentTimeMillis()
)
