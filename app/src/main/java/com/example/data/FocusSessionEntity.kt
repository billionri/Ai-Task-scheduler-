package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long? = null,
    val taskTitle: String? = null,
    val category: String = "Study",
    val durationSeconds: Int = 1500, // 25 mins pomodoro default
    val actualSeconds: Int = 1500,
    val sessionType: String = "POMODORO", // POMODORO, SHORT_BREAK, LONG_BREAK, CUSTOM_TIMER, OPEN_STUDY
    val isCompleted: Boolean = true,
    val rating: Int = 5,
    val notes: String = "",
    val timestampMillis: Long = System.currentTimeMillis()
)
