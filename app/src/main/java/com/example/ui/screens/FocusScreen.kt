package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AmbientSoundType
import com.example.data.TaskCategory
import com.example.ui.components.CircularTimerDisplay
import com.example.ui.components.SessionSummaryDialog
import com.example.ui.theme.Amber400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Teal400
import com.example.viewmodel.FocusViewModel
import com.example.viewmodel.SessionPhase
import com.example.viewmodel.TimerMode
import com.example.viewmodel.TimerStatus

@Composable
fun FocusScreen(
    viewModel: FocusViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingTasks by viewModel.pendingTasks.collectAsStateWithLifecycle()

    var showTaskDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Focus Study Room",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Deep work, custom timers & distraction-free audio",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Timer Mode Tabs: Pomodoro, Custom Timer, Open Study
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TimerMode.entries.forEach { mode ->
                val selected = uiState.mode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.surface else Color.Transparent
                        )
                        .clickable { viewModel.setTimerMode(mode) }
                        .padding(vertical = 10.dp)
                        .testTag("mode_tab_${mode.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.displayName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Linked Task Selector Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("task_selector_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTaskDropdown = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Task,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Focusing On",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.selectedTask?.title ?: "General Study (No task linked)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showTaskDropdown,
                    onDismissRequest = { showTaskDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("General Study (No Task Linked)") },
                        onClick = {
                            viewModel.selectTask(null)
                            showTaskDropdown = false
                        }
                    )
                    pendingTasks.forEach { task ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(task.title, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${task.category} • ${task.estimatedDurationMinutes}m",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                viewModel.selectTask(task)
                                showTaskDropdown = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Custom Timer Duration Selector (if Custom Mode & Idle)
        if (uiState.mode == TimerMode.CUSTOM && uiState.status == TimerStatus.IDLE) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(10, 15, 20, 25, 30, 45, 60, 90).forEach { mins ->
                    val isSelected = uiState.totalSeconds == mins * 60
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setCustomDurationMinutes(mins) },
                        label = { Text("${mins} min") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Pomodoro Cycle Indicator
        if (uiState.mode == TimerMode.POMODORO) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cycle: ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val completedInCycle = uiState.completedPomodorosCount % 4
                for (i in 0 until 4) {
                    val filled = i < completedInCycle
                    Surface(
                        shape = CircleShape,
                        color = if (filled) Amber400 else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(14.dp)
                    ) {}
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${uiState.completedPomodorosCount} total)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Circular Countdown Timer Display
        CircularTimerDisplay(
            remainingSeconds = uiState.remainingSeconds,
            totalSeconds = uiState.totalSeconds,
            status = uiState.status,
            phase = uiState.phase,
            mode = uiState.mode
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Primary Timer Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (uiState.status) {
                TimerStatus.IDLE, TimerStatus.PAUSED -> {
                    // Start / Resume button
                    Button(
                        onClick = { viewModel.startTimer() },
                        modifier = Modifier
                            .height(56.dp)
                            .width(180.dp)
                            .testTag("start_resume_timer_button"),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.status == TimerStatus.IDLE) "Start Focus" else "Resume",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (uiState.status == TimerStatus.PAUSED) {
                        Spacer(modifier = Modifier.width(12.dp))
                        // Reset button
                        FilledTonalButton(
                            onClick = { viewModel.resetTimer() },
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("reset_timer_button"),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset")
                        }
                    }
                }
                TimerStatus.RUNNING -> {
                    // Pause button
                    Button(
                        onClick = { viewModel.pauseTimer() },
                        modifier = Modifier
                            .height(56.dp)
                            .width(160.dp)
                            .testTag("pause_timer_button"),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Amber400
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pause",
                            color = Color.Black,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // +5 mins quick extend
                    FilledTonalButton(
                        onClick = { viewModel.addMinutes(5) },
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("add_5_mins_button"),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.MoreTime, contentDescription = "+5 min")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+5m")
                    }
                }
                TimerStatus.COMPLETED -> {
                    Button(
                        onClick = { viewModel.resetTimer() },
                        modifier = Modifier
                            .height(56.dp)
                            .width(160.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("Next Round")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Secondary Actions: Finish Early & Log / Skip Break
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.phase != SessionPhase.WORK) {
                OutlinedButton(
                    onClick = { viewModel.skipBreak() },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Skip Break")
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            if (uiState.status == TimerStatus.RUNNING || uiState.status == TimerStatus.PAUSED) {
                OutlinedButton(
                    onClick = { viewModel.stopAndFinishEarly() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("finish_early_button")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Finish & Log Session")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Ambient Background Sound Synthesizer Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Teal400,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Focus Ambient Sound",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (uiState.ambientSound != AmbientSoundType.NONE) {
                        Surface(
                            shape = CircleShape,
                            color = Teal400.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = Teal400,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AmbientSoundType.entries.forEach { sound ->
                        val isSelected = uiState.ambientSound == sound
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setAmbientSound(sound) },
                            label = { Text(sound.displayName, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // Session Completion Dialog
    if (uiState.showCompletionDialog) {
        SessionSummaryDialog(
            completedSeconds = uiState.lastCompletedSeconds,
            taskTitle = uiState.selectedTask?.title,
            onDismiss = { viewModel.dismissCompletionDialog() },
            onSave = { rating, notes, markDone ->
                viewModel.saveCompletedSession(rating, notes, markDone)
            }
        )
    }
}
