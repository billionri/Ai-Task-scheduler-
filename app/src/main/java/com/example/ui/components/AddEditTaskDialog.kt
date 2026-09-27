package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.TaskCategory
import com.example.data.TaskEntity
import com.example.data.TaskPriority
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskDialog(
    initialTask: TaskEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        title: String,
        description: String,
        category: String,
        priority: TaskPriority,
        dueDateMillis: Long,
        dueHour: Int,
        dueMinute: Int,
        estimatedDurationMinutes: Int,
        reminderEnabled: Boolean,
        reminderOffsetMinutes: Int
    ) -> Unit
) {
    val now = Calendar.getInstance()
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var selectedCategory by remember {
        mutableStateOf(
            if (initialTask != null) TaskCategory.fromString(initialTask.category) else TaskCategory.STUDY
        )
    }
    var selectedPriority by remember {
        mutableStateOf(
            try {
                if (initialTask != null) TaskPriority.valueOf(initialTask.priority) else TaskPriority.MEDIUM
            } catch (_: Exception) {
                TaskPriority.MEDIUM
            }
        )
    }

    var dueDateMillis by remember {
        mutableLongStateOf(initialTask?.dueDateMillis ?: now.timeInMillis)
    }
    var dueHour by remember {
        mutableIntStateOf(initialTask?.dueTimeHour ?: ((now.get(Calendar.HOUR_OF_DAY) + 1) % 24))
    }
    var dueMinute by remember {
        mutableIntStateOf(initialTask?.dueTimeMinute ?: 0)
    }
    var durationMinutes by remember {
        mutableIntStateOf(initialTask?.estimatedDurationMinutes ?: 30)
    }
    var reminderEnabled by remember {
        mutableStateOf(initialTask?.reminderEnabled ?: true)
    }
    var reminderOffsetMinutes by remember {
        mutableIntStateOf(initialTask?.reminderOffsetMinutes ?: 15)
    }

    var isTitleError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .testTag("add_edit_task_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (initialTask == null) "Schedule New Task" else "Edit Task",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) isTitleError = false
                    },
                    label = { Text("Task Title *") },
                    placeholder = { Text("e.g. Study Discrete Math Chapter 3") },
                    isError = isTitleError,
                    supportingText = if (isTitleError) {
                        { Text("Title is required", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes / Description") },
                    placeholder = { Text("Goals, topics, questions to solve...") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_description_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selection
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskCategory.entries.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = category.color.copy(alpha = 0.25f),
                                selectedLabelColor = category.color
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Priority Selection
                Text(
                    text = "Priority Level",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskPriority.entries.forEach { priority ->
                        FilterChip(
                            selected = selectedPriority == priority,
                            onClick = { selectedPriority = priority },
                            label = { Text(priority.displayName) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = priority.color.copy(alpha = 0.2f),
                                selectedLabelColor = priority.color
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ==================== DATE PICKER SECTION ====================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Scheduled Date",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Text(
                        text = "Tap to open calendar",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Clickable Primary Date Display Card
                val fullDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                val todayCal = Calendar.getInstance()
                val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                val isToday = isSameDay(dueDateMillis, todayCal.timeInMillis)
                val isTomorrow = isSameDay(dueDateMillis, tomorrowCal.timeInMillis)

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showDatePicker = true }
                        .testTag("date_picker_trigger_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditCalendar,
                                    contentDescription = "Pick Date",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = fullDateFormat.format(Date(dueDateMillis)),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isToday || isTomorrow) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = if (isToday) "Today" else "Tomorrow",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Tap to pick date from calendar",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("open_date_picker_btn")
                        ) {
                            Text("Pick Date", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Preset Chips for convenience
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isToday,
                        onClick = { dueDateMillis = todayCal.timeInMillis },
                        label = { Text("Today") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("date_chip_today")
                    )
                    FilterChip(
                        selected = isTomorrow,
                        onClick = { dueDateMillis = tomorrowCal.timeInMillis },
                        label = { Text("Tomorrow") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("date_chip_tomorrow")
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            val inThreeDays = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 3) }
                            dueDateMillis = inThreeDays.timeInMillis
                        },
                        label = { Text("+3 Days") },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            val nextWeek = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
                            dueDateMillis = nextWeek.timeInMillis
                        },
                        label = { Text("+1 Week") },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = showDatePicker,
                        onClick = { showDatePicker = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Custom Date...") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("date_chip_custom")
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ==================== TIME PICKER SECTION ====================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Scheduled Time: ${formatDisplayTime(dueHour, dueMinute)}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showTimePicker = true }
                            .testTag("open_time_picker_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Set Time",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Times row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickTimes = listOf(
                        Pair(9, 0), Pair(11, 0), Pair(14, 0),
                        Pair(16, 30), Pair(19, 0), Pair(21, 0)
                    )
                    quickTimes.forEach { (h, m) ->
                        val selected = dueHour == h && dueMinute == m
                        FilterChip(
                            selected = selected,
                            onClick = {
                                dueHour = h
                                dueMinute = m
                            },
                            label = { Text(formatDisplayTime(h, m)) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    FilterChip(
                        selected = showTimePicker,
                        onClick = { showTimePicker = true },
                        leadingIcon = {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("More...") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Estimated Duration Chips
                Text(
                    text = "Target Study Duration",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 25, 45, 60, 90).forEach { mins ->
                        FilterChip(
                            selected = durationMinutes == mins,
                            onClick = { durationMinutes = mins },
                            label = { Text("${mins}m") },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reminder Notification Section
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Task Reminder Alert",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { reminderEnabled = it }
                            )
                        }

                        if (reminderEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Notify me:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(0 to "At time", 5 to "5m before", 15 to "15m before", 30 to "30m before").forEach { (offset, lbl) ->
                                    FilterChip(
                                        selected = reminderOffsetMinutes == offset,
                                        onClick = { reminderOffsetMinutes = offset },
                                        label = { Text(lbl, style = MaterialTheme.typography.labelSmall) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                isTitleError = true
                            } else {
                                onSave(
                                    initialTask?.id ?: 0L,
                                    title,
                                    description,
                                    selectedCategory.displayName,
                                    selectedPriority,
                                    dueDateMillis,
                                    dueHour,
                                    dueMinute,
                                    durationMinutes,
                                    reminderEnabled,
                                    reminderOffsetMinutes
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_task_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (initialTask == null) "Schedule" else "Update")
                    }
                }
            }
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        val initialUtcMillis = remember(dueDateMillis) {
            val localCal = Calendar.getInstance().apply { timeInMillis = dueDateMillis }
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(localCal.get(Calendar.YEAR), localCal.get(Calendar.MONTH), localCal.get(Calendar.DAY_OF_MONTH))
            }.timeInMillis
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialUtcMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            val localCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            dueDateMillis = localCal.timeInMillis
                        }
                        showDatePicker = false
                    },
                    modifier = Modifier.testTag("date_picker_confirm_button")
                ) {
                    Text("Select Date")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("material_date_picker_dialog")
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = true
            )
        }
    }

    // Material 3 Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = dueHour,
            initialMinute = dueMinute,
            is24Hour = false
        )

        Dialog(onDismissRequest = { showTimePicker = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("material_time_picker_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Select Due Time",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { showTimePicker = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TimePicker(state = timePickerState)

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showTimePicker = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                dueHour = timePickerState.hour
                                dueMinute = timePickerState.minute
                                showTimePicker = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("time_picker_confirm_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Confirm Time")
                        }
                    }
                }
            }
        }
    }
}

private fun formatDisplayTime(hour: Int, minute: Int): String {
    val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val amPm = if (hour < 12) "AM" else "PM"
    return String.format(Locale.getDefault(), "%02d:%02d %s", h, minute, amPm)
}

private fun isSameDay(t1: Long, t2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}
