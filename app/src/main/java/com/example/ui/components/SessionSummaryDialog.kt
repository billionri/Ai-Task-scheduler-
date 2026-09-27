package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.Amber400

@Composable
fun SessionSummaryDialog(
    completedSeconds: Int,
    taskTitle: String?,
    onDismiss: () -> Unit,
    onSave: (rating: Int, notes: String, markTaskDone: Boolean) -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var notes by remember { mutableStateOf("") }
    var markTaskDone by remember { mutableStateOf(taskTitle != null) }

    val focusMinutes = (completedSeconds / 60).coerceAtLeast(1)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("session_summary_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "Session Completed! 🎉",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "+$focusMinutes minutes of deep focus",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                if (taskTitle != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Task: $taskTitle",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Star Rating
                Text(
                    text = "How was your focus?",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (star in 1..5) {
                        val isFilled = star <= rating
                        Icon(
                            imageVector = if (isFilled) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Rating $star",
                            tint = if (isFilled) Amber400 else MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { rating = star }
                                .testTag("star_rating_$star")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Notes input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Session Notes / Reflections") },
                    placeholder = { Text("What did you accomplish? Any roadblocks?") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_notes_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (taskTitle != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { markTaskDone = !markTaskDone },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = markTaskDone,
                            onCheckedChange = { markTaskDone = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mark associated task as completed",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Skip")
                    }

                    Button(
                        onClick = { onSave(rating, notes, markTaskDone) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_session_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save & Log")
                    }
                }
            }
        }
    }
}
