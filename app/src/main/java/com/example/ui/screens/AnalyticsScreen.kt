package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FocusSessionEntity
import com.example.data.TaskCategory
import com.example.ui.components.CategoryDistributionCard
import com.example.ui.components.StatCard
import com.example.ui.components.WeeklyFocusBarChart
import com.example.ui.theme.Amber400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Rose500
import com.example.ui.theme.Teal400
import com.example.viewmodel.AnalyticsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.example.data.SubscriptionState

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    subscriptionState: SubscriptionState,
    onOpenPayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        item {
            Column {
                Text(
                    text = "Productivity Analytics",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Track focus hours, study consistency, and completion metrics",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Pro / Recharge Membership Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("analytics_pro_banner"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (subscriptionState.isPro) Emerald500.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (subscriptionState.isPro) Emerald500.copy(alpha = 0.2f) else Amber400.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = if (subscriptionState.isPro) Emerald500 else Amber400,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (subscriptionState.isPro) "PRO Active: ${subscriptionState.planName}" else "Upgrade to Task & Focus PRO",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (subscriptionState.isPro) Emerald500 else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (subscriptionState.isPro) "${subscriptionState.daysRemaining} days remaining • All features unlocked" else "1 Year (₹2,000) or 1 Month (₹200) via UPI",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onOpenPayment,
                        shape = RoundedCornerShape(10.dp),
                        colors = if (subscriptionState.isPro) {
                            ButtonDefaults.buttonColors(containerColor = Emerald500)
                        } else {
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier.testTag("analytics_recharge_btn")
                    ) {
                        Text(
                            text = if (subscriptionState.isPro) "Manage" else "Recharge",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // 2x2 Stat Cards Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val formattedHours = if (uiState.totalFocusHours >= 1.0f) {
                        String.format(Locale.getDefault(), "%.1fh", uiState.totalFocusHours)
                    } else {
                        "${(uiState.totalFocusHours * 60).toInt()}m"
                    }

                    StatCard(
                        title = "Focus Time",
                        value = formattedHours,
                        subtitle = "${uiState.todayFocusMinutes}m focused today",
                        icon = Icons.Default.Timer,
                        iconTint = Indigo500,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Study Streak",
                        value = "${uiState.currentStreakDays} ${if (uiState.currentStreakDays == 1) "Day" else "Days"}",
                        subtitle = "Best: ${uiState.longestStreakDays} days",
                        icon = Icons.Default.LocalFireDepartment,
                        iconTint = Amber400,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Tasks Completed",
                        value = "${uiState.taskCompletionRatePercent}%",
                        subtitle = "${uiState.completedTasksCount} of ${uiState.totalTasksCount} done",
                        icon = Icons.Default.CheckCircle,
                        iconTint = Emerald500,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Focus Rating",
                        value = String.format(Locale.getDefault(), "%.1f ★", uiState.averageRating),
                        subtitle = "${uiState.totalSessionsCount} total sessions",
                        icon = Icons.Default.Star,
                        iconTint = Rose500,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 7-day Focus Minutes Bar Chart
        item {
            WeeklyFocusBarChart(dailyData = uiState.dailyTrend)
        }

        // Category / Subject Distribution
        item {
            CategoryDistributionCard(categoryShares = uiState.categoryShares)
        }

        // Session History Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recent Study Sessions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Recent Sessions Items
        if (uiState.recentSessions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "No study sessions recorded yet. Finish a timer session to see your activity logs here!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(
                items = uiState.recentSessions,
                key = { it.id }
            ) { session ->
                SessionHistoryItem(session = session)
            }
        }

        // Bottom space for bottom nav
        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun SessionHistoryItem(session: FocusSessionEntity) {
    val category = TaskCategory.fromString(session.category)
    val mins = (session.actualSeconds / 60).coerceAtLeast(1)
    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(session.timestampMillis))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(category.color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = session.taskTitle ?: "${session.category} Study",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "${mins}m",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Star Rating
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (i in 1..session.rating) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Amber400,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            if (session.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "“${session.notes}”",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
