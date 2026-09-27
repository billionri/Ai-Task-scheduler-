package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TaskEntity
import com.example.ui.components.AddEditTaskDialog
import com.example.ui.components.TaskCard
import com.example.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: TaskViewModel,
    onStartFocusForTask: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedDateMillis by remember {
        mutableLongStateOf(getStartOfDay(Calendar.getInstance()).timeInMillis)
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }

    // Generate two weeks of dates (previous 3 days, today, next 10 days)
    val calendarDays = remember {
        val list = mutableListOf<Calendar>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -3)
        for (i in 0..14) {
            list.add(cal.clone() as Calendar)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val selectedDayStart = getStartOfDay(Calendar.getInstance().apply { timeInMillis = selectedDateMillis }).timeInMillis
    val selectedDayEnd = getEndOfDay(Calendar.getInstance().apply { timeInMillis = selectedDateMillis }).timeInMillis

    val dayTasks = uiState.tasks.filter {
        it.dueDateMillis in selectedDayStart..selectedDayEnd
    }.sortedWith(compareBy({ it.dueTimeHour }, { it.dueTimeMinute }))

    val completedDayTasks = dayTasks.count { it.isCompleted }
    val totalDayEstimatedMins = dayTasks.sumOf { it.estimatedDurationMinutes }

    Scaffold(
        modifier = modifier.testTag("calendar_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("calendar_fab_add_task")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task for Date")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Daily Planner & Agenda",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "View upcoming deadlines & daily study allocations",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Horizontal Date Strip Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                val numFormat = SimpleDateFormat("d", Locale.getDefault())
                val todayCal = Calendar.getInstance()

                calendarDays.forEach { cal ->
                    val dateStart = getStartOfDay(cal).timeInMillis
                    val isSelected = isSameDay(dateStart, selectedDateMillis)
                    val isToday = isSameDay(dateStart, todayCal.timeInMillis)

                    val hasTasks = uiState.tasks.any {
                        it.dueDateMillis in dateStart..getEndOfDay(cal).timeInMillis
                    }

                    Card(
                        modifier = Modifier
                            .width(62.dp)
                            .height(84.dp)
                            .clickable { selectedDateMillis = dateStart }
                            .testTag("cal_day_${numFormat.format(cal.time)}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else if (isToday) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = dayFormat.format(cal.time),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = numFormat.format(cal.time),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )

                            // Task indicator dot
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (hasTasks) {
                                            if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                        } else Color.Transparent
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selected Day Summary Card
            val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = sdf.format(Date(selectedDateMillis)),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (dayTasks.isEmpty()) "No scheduled tasks"
                            else "$completedDayTasks of ${dayTasks.size} tasks done • ${totalDayEstimatedMins}m planned",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.EventAvailable,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tasks for selected day
            if (dayTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No tasks scheduled for this day",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Schedule a study session or homework assignment",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Task for Date")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = dayTasks,
                        key = { it.id }
                    ) { task ->
                        TaskCard(
                            task = task,
                            onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                            onStartFocus = { onStartFocusForTask(task) },
                            onEdit = { taskToEdit = task },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }
            }
        }
    }

    // Add or Edit Dialog
    if (showAddDialog || taskToEdit != null) {
        val initialTaskWithDate = taskToEdit ?: TaskEntity(
            title = "",
            dueDateMillis = selectedDateMillis
        )
        AddEditTaskDialog(
            initialTask = if (taskToEdit != null) taskToEdit else initialTaskWithDate,
            onDismiss = {
                showAddDialog = false
                taskToEdit = null
            },
            onSave = { id, title, desc, cat, priority, date, h, m, dur, rem, remOffset ->
                viewModel.saveTask(
                    id = id,
                    title = title,
                    description = desc,
                    category = cat,
                    priority = priority,
                    dueDateMillis = date,
                    dueHour = h,
                    dueMinute = m,
                    estimatedDurationMinutes = dur,
                    reminderEnabled = rem,
                    reminderOffsetMinutes = remOffset
                )
                showAddDialog = false
                taskToEdit = null
            }
        )
    }
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

private fun isSameDay(t1: Long, t2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}
