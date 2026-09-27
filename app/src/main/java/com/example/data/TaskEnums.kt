package com.example.data

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CategoryCoding
import com.example.ui.theme.CategoryExam
import com.example.ui.theme.CategoryOther
import com.example.ui.theme.CategoryPersonal
import com.example.ui.theme.CategoryProject
import com.example.ui.theme.CategoryReading
import com.example.ui.theme.CategoryStudy
import com.example.ui.theme.CategoryWork
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityMedium
import com.example.ui.theme.PriorityUrgent

enum class TaskPriority(val displayName: String, val color: Color) {
    LOW("Low", PriorityLow),
    MEDIUM("Medium", PriorityMedium),
    HIGH("High", PriorityHigh),
    URGENT("Urgent", PriorityUrgent)
}

enum class RecurrenceType(val displayName: String) {
    NONE("Does not repeat"),
    DAILY("Every day"),
    WEEKDAYS("Mon to Fri"),
    WEEKLY("Once a week")
}

enum class TaskCategory(val displayName: String, val color: Color) {
    STUDY("Study", CategoryStudy),
    READING("Reading", CategoryReading),
    CODING("Coding", CategoryCoding),
    PROJECT("Project", CategoryProject),
    EXAM("Exam Prep", CategoryExam),
    HOMEWORK("Homework", CategoryWork),
    PERSONAL("Personal", CategoryPersonal),
    OTHER("Other", CategoryOther);

    companion object {
        fun fromString(name: String): TaskCategory {
            return entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) || it.name.equals(name, ignoreCase = true) } ?: STUDY
        }
    }
}
