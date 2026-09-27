package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.WorkspacePremium
import com.example.data.SubscriptionState
import com.example.ui.theme.Amber400
import com.example.ui.theme.Emerald500
import com.example.data.TaskCategory
import com.example.data.TaskEntity
import com.example.ui.components.AddEditTaskDialog
import com.example.ui.components.TaskCard
import com.example.viewmodel.TaskFilter
import com.example.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TaskViewModel,
    subscriptionState: SubscriptionState,
    onOpenPayment: () -> Unit,
    onStartFocusForTask: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var showSearchBar by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.testTag("tasks_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_task")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header: Title & Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Task Scheduler",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Plan, schedule & conquer your study goals",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pro / Upgrade Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (subscriptionState.isPro) Emerald500.copy(alpha = 0.15f) else Amber400.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, if (subscriptionState.isPro) Emerald500 else Amber400),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenPayment)
                            .testTag("pro_badge_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = if (subscriptionState.isPro) Emerald500 else Amber400,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (subscriptionState.isPro) "PRO" else "Upgrade",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (subscriptionState.isPro) Emerald500 else Amber400
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { showSearchBar = !showSearchBar },
                        modifier = Modifier.testTag("toggle_search_btn")
                    ) {
                        Icon(
                            imageVector = if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Search Bar (collapsible)
            AnimatedVisibility(visible = showSearchBar) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search tasks by title, note, or subject...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .testTag("task_search_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // Demo Mode & Milestone Tracker
            if (!subscriptionState.isPro) {
                val isMilestoneDone = subscriptionState.isDemoMilestoneReached || uiState.totalCompletedCount >= 2
                if (isMilestoneDone) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .clickable(onClick = onOpenPayment)
                            .testTag("demo_tasks_completed_banner"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Amber400.copy(alpha = 0.18f)
                        ),
                        border = BorderStroke(1.dp, Amber400)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Amber400),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🎉 2 Demo Tasks Added & Completed!",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Recharge at Rashid.alam@oksbi: ₹200 (1 Month) or ₹2,000 (1 Year) to unlock unlimited task schedules & timers.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            androidx.compose.material3.Button(
                                onClick = onOpenPayment,
                                shape = RoundedCornerShape(10.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Amber400,
                                    contentColor = androidx.compose.ui.graphics.Color.Black
                                ),
                                modifier = Modifier.testTag("demo_banner_recharge_btn")
                            ) {
                                Text("Recharge", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    // Demo Mode in progress: show added/completed progress and quick add demo task
                    val tasksAdded = subscriptionState.demoTasksAdded
                    val tasksCompleted = maxOf(subscriptionState.demoTasksCompleted, uiState.totalCompletedCount)
                    val progress = ((tasksAdded.coerceAtMost(2) + tasksCompleted.coerceAtMost(2)) / 4f).coerceIn(0f, 1f)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .testTag("demo_mode_tracker_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "🎯",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    Text(
                                        text = "Demo Trial: Add & Complete 2 Tasks",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Added: $tasksAdded/2  •  Completed: $tasksCompleted/2",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        val sampleNames = listOf(
                                            "Review Physics Formulas",
                                            "Organize Weekly Study Plan",
                                            "Practice Python Coding",
                                            "Read Biology Chapter 3"
                                        )
                                        val nextName = sampleNames[(tasksAdded) % sampleNames.size]
                                        viewModel.addQuickDemoTask(
                                            title = nextName,
                                            category = "Study",
                                            estimatedMinutes = 30
                                        )
                                    },
                                    modifier = Modifier.testTag("quick_add_demo_task_btn")
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Add Demo Task",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Today's Progress Card
            if (uiState.todayTasksCount > 0) {
                val progress = uiState.completedTodayCount.toFloat() / uiState.todayTasksCount.toFloat()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Today's Schedule Progress",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${uiState.completedTodayCount} of ${uiState.todayTasksCount} tasks completed (${(progress * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }

            // Filter Tabs (All, Today, Upcoming, Overdue, Completed)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.activeFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter.displayName) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("filter_${filter.name.lowercase()}")
                    )
                }
            }

            // Category Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedCategory == null,
                    onClick = { viewModel.selectCategory(null) },
                    label = { Text("All Subjects") },
                    shape = RoundedCornerShape(8.dp)
                )
                TaskCategory.entries.forEach { category ->
                    FilterChip(
                        selected = uiState.selectedCategory == category,
                        onClick = {
                            viewModel.selectCategory(if (uiState.selectedCategory == category) null else category)
                        },
                        label = { Text(category.displayName) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Task List
            if (uiState.filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AssignmentTurnedIn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) "No tasks match your search"
                            else if (uiState.activeFilter == TaskFilter.COMPLETED) "No completed tasks yet"
                            else "No tasks scheduled",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Tap the + button to add a study session or assignment",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.filteredTasks,
                        key = { it.id }
                    ) { task ->
                        TaskCard(
                            task = task,
                            onToggleComplete = { viewModel.toggleTaskCompletion(task, subscriptionState.isPro) },
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
        AddEditTaskDialog(
            initialTask = taskToEdit,
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
                    reminderOffsetMinutes = remOffset,
                    isPro = subscriptionState.isPro
                )
                showAddDialog = false
                taskToEdit = null
            }
        )
    }
}
