package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Amber400
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Teal400
import com.example.viewmodel.SessionPhase
import com.example.viewmodel.TimerMode
import com.example.viewmodel.TimerStatus
import java.util.Locale

@Composable
fun CircularTimerDisplay(
    remainingSeconds: Int,
    totalSeconds: Int,
    status: TimerStatus,
    phase: SessionPhase,
    mode: TimerMode,
    modifier: Modifier = Modifier
) {
    val progress = if (mode == TimerMode.STOPWATCH) {
        // Lap circle loops every 60 seconds in stopwatch
        (remainingSeconds % 60) / 60f
    } else {
        if (totalSeconds > 0) {
            (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
        } else 0f
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (status == TimerStatus.RUNNING) 1.03f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val primaryArcColor = when (phase) {
        SessionPhase.WORK -> Indigo500
        SessionPhase.SHORT_BREAK -> Teal400
        SessionPhase.LONG_BREAK -> Amber400
    }

    val secondaryArcColor = when (phase) {
        SessionPhase.WORK -> Teal400
        SessionPhase.SHORT_BREAK -> Indigo500
        SessionPhase.LONG_BREAK -> Teal400
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    Box(
        modifier = modifier
            .size(270.dp)
            .testTag("circular_timer_display"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.minDimension - strokeWidth * 2
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)

            // Background Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Animated Foreground Progress Arc
            val sweepAngle = progress * 360f
            if (sweepAngle > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(primaryArcColor, secondaryArcColor, primaryArcColor),
                        center = Offset(size.width / 2f, size.height / 2f)
                    ),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Center Text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = primaryArcColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = phase.displayName.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = primaryArcColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = formattedTime,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("timer_time_text")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = when (status) {
                    TimerStatus.IDLE -> "Ready"
                    TimerStatus.RUNNING -> "Focusing..."
                    TimerStatus.PAUSED -> "Paused"
                    TimerStatus.COMPLETED -> "Finished!"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
