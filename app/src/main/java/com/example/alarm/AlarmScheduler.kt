package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.TaskEntity
import com.example.receiver.TaskReminderReceiver
import java.util.Calendar

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleTaskReminder(task: TaskEntity) {
        if (!task.reminderEnabled || task.isCompleted) return

        val calendar = Calendar.getInstance().apply {
            timeInMillis = task.dueDateMillis
            set(Calendar.HOUR_OF_DAY, task.dueTimeHour)
            set(Calendar.MINUTE, task.dueTimeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -task.reminderOffsetMinutes)
        }

        val triggerAtMillis = calendar.timeInMillis
        if (triggerAtMillis <= System.currentTimeMillis()) {
            return // Skip past alarms
        }

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = TaskReminderReceiver.ACTION_TASK_REMINDER
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, task.title)
            putExtra(TaskReminderReceiver.EXTRA_TASK_CATEGORY, task.category)
            putExtra(TaskReminderReceiver.EXTRA_TASK_PRIORITY, task.priority)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            flags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelTaskReminder(taskId: Long) {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = TaskReminderReceiver.ACTION_TASK_REMINDER
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            flags
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleFocusTimerNotification(durationSeconds: Int, title: String) {
        val triggerAtMillis = System.currentTimeMillis() + (durationSeconds * 1000L)
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = TaskReminderReceiver.ACTION_FOCUS_TIMER_ALERT
            putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, title)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            99999,
            intent,
            flags
        )

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (_: Exception) {}
    }

    fun cancelFocusTimerNotification() {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = TaskReminderReceiver.ACTION_FOCUS_TIMER_ALERT
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            99999,
            intent,
            flags
        )
        alarmManager.cancel(pendingIntent)
    }
}
