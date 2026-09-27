package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.SubscriptionManager
import com.example.data.SubscriptionPlan
import com.example.data.SubscriptionState
import com.example.ui.theme.Amber400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Teal400
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PaymentDialog(
    subscriptionState: SubscriptionState,
    isMilestonePrompt: Boolean = false,
    onActivatePlan: (SubscriptionPlan, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedPlan by remember { mutableStateOf(SubscriptionPlan.ANNUAL) }
    var utrNumber by remember { mutableStateOf("") }
    var showUtrInput by remember { mutableStateOf(false) }
    var paymentActivatedSuccess by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp)
                .testTag("payment_dialog"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                // Milestone alert banner if triggered after 2 demo tasks
                if (isMilestonePrompt && !subscriptionState.isPro) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Amber400.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Amber400),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("dialog_milestone_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎉",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Column {
                                Text(
                                    text = "2 Demo Tasks Completed!",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "You've finished your 2 demo tasks. Recharge to unlock full Pro benefits without limits.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(Amber400, Color(0xFFF59E0B)))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Pro",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Task & Focus PRO",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Unlock premium study & task features",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Active Subscription Status Banner (if user already active)
                if (subscriptionState.isPro) {
                    val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                    val expiryStr = dateFormat.format(Date(subscriptionState.expiryDateMillis))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Emerald500.copy(alpha = 0.12f),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Emerald500,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Active Subscription: ${subscriptionState.planName}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Emerald500
                                )
                                Text(
                                    text = "${subscriptionState.daysRemaining} days remaining (Expires $expiryStr)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Plan Selection Cards (1 Month vs 1 Year)
                Text(
                    text = "Select Recharge Plan",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1 Month Card (₹200)
                    PlanCard(
                        plan = SubscriptionPlan.MONTHLY,
                        isSelected = selectedPlan == SubscriptionPlan.MONTHLY,
                        onSelect = { selectedPlan = SubscriptionPlan.MONTHLY },
                        modifier = Modifier.weight(1f)
                    )

                    // 1 Year Card (₹2000)
                    PlanCard(
                        plan = SubscriptionPlan.ANNUAL,
                        isSelected = selectedPlan == SubscriptionPlan.ANNUAL,
                        onSelect = { selectedPlan = SubscriptionPlan.ANNUAL },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Pro Features Checklist
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Included with Pro Membership:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        ProFeatureRow(
                            icon = Icons.Default.Timer,
                            title = "Custom Study Timers & Open Sessions",
                            desc = "Preset chips from 10m to 90m + unlimited stopwatch"
                        )
                        ProFeatureRow(
                            icon = Icons.Default.NotificationsActive,
                            title = "High Priority Exact Task Alarms",
                            desc = "Never miss a submission with custom offset alerts"
                        )
                        ProFeatureRow(
                            icon = Icons.Default.GraphicEq,
                            title = "Synthesized Ambient Focus Audio",
                            desc = "Procedural Rain, Soft Noise, and Ocean Waves"
                        )
                        ProFeatureRow(
                            icon = Icons.Default.Insights,
                            title = "Productivity Trends & Habit Streaks",
                            desc = "Weekly bar charts & multi-subject distribution"
                        )
                        ProFeatureRow(
                            icon = Icons.Default.Shield,
                            title = "Ad-Free & Unlimited Schedules",
                            desc = "Fast, private Room database offline synchronization"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Payment Destination UPI Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Payee UPI ID",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = SubscriptionManager.UPI_ID,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Beneficiary: ${SubscriptionManager.PAYEE_NAME}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Copy UPI ID button
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("UPI ID", SubscriptionManager.UPI_ID)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "UPI ID copied: ${SubscriptionManager.UPI_ID}", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("copy_upi_btn")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Primary Action: Pay via UPI Apps
                Button(
                    onClick = {
                        val upiUri = Uri.Builder()
                            .scheme("upi")
                            .authority("pay")
                            .appendQueryParameter("pa", SubscriptionManager.UPI_ID)
                            .appendQueryParameter("pn", SubscriptionManager.PAYEE_NAME)
                            .appendQueryParameter("am", selectedPlan.priceRupees.toString())
                            .appendQueryParameter("cu", "INR")
                            .appendQueryParameter("tn", "Task Scheduler ${selectedPlan.title}")
                            .build()

                        val upiIntent = Intent(Intent.ACTION_VIEW, upiUri)
                        val chooser = Intent.createChooser(upiIntent, "Pay via UPI App")
                        try {
                            context.startActivity(chooser)
                            showUtrInput = true
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "No UPI app found. Please send ${selectedPlan.amountFormatted} to ${SubscriptionManager.UPI_ID} & enter UTR below.",
                                Toast.LENGTH_LONG
                            ).show()
                            showUtrInput = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("pay_via_upi_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pay ${selectedPlan.amountFormatted} via UPI",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Toggle UTR Verification input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (showUtrInput) "Hide UTR Input" else "Already paid? Enter 12-digit UTR / Reference ID",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .clickable { showUtrInput = !showUtrInput }
                            .padding(vertical = 4.dp)
                            .testTag("toggle_utr_input_btn")
                    )
                }

                // UTR Reference Input Form
                AnimatedVisibility(visible = showUtrInput) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        OutlinedTextField(
                            value = utrNumber,
                            onValueChange = { utrNumber = it },
                            label = { Text("12-Digit UTR / Transaction ID") },
                            placeholder = { Text("e.g. 426819284729") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("utr_input_field"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val txn = if (utrNumber.isNotBlank()) utrNumber.trim() else "UPI-MANUAL-${System.currentTimeMillis() % 1000000}"
                                onActivatePlan(selectedPlan, txn)
                                paymentActivatedSuccess = true
                                Toast.makeText(context, "${selectedPlan.title} activated successfully!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("verify_activate_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Emerald500
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Verify & Unlock Pro Now",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                if (paymentActivatedSuccess) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Emerald500.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald500)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🎉 You are now a PRO member! All features unlocked.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Emerald500
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlanCard(
    plan: SubscriptionPlan,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(width = if (isSelected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect)
            .testTag("plan_card_${plan.id}"),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Badge (e.g. BEST VALUE)
            if (plan.badge != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Amber400
                ) {
                    Text(
                        text = plan.badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = Color.Black
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            } else {
                Spacer(modifier = Modifier.height(18.dp))
            }

            Text(
                text = plan.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = plan.amountFormatted,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = plan.durationText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (plan.savings != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (plan == SubscriptionPlan.ANNUAL) Emerald500.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = plan.savings,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (plan == SubscriptionPlan.ANNUAL) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProFeatureRow(icon: ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
