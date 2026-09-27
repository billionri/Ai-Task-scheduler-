package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TASK_REMINDER = "com.aistudio.taskscheduler.ACTION_TASK_REMINDER"
        const val ACTION_FOCUS_TIMER_ALERT = "com.aistudio.taskscheduler.ACTION_FOCUS_TIMER_ALERT"
        const val ACTION_MARK_COMPLETED = "com.aistudio.taskscheduler.ACTION_MARK_COMPLETED"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_CATEGORY = "extra_task_category"
        const val EXTRA_TASK_PRIORITY = "extra_task_priority"

        const val CHANNEL_TASKS_ID = "channel_task_reminders"
        const val CHANNEL_FOCUS_ID = "channel_focus_sessions"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            ACTION_MARK_COMPLETED -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                if (taskId != -1L) {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(taskId.toInt())

                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppDatabase.getDatabase(context)
                        db.taskDao().updateTaskCompletion(taskId, true, System.currentTimeMillis())
                    }
                }
            }
            ACTION_FOCUS_TIMER_ALERT -> {
                val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Focus Session"
                showFocusCompletedNotification(context, title)
            }
            ACTION_TASK_REMINDER -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, 0L)
                val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task Reminder"
                val category = intent.getStringExtra(EXTRA_TASK_CATEGORY) ?: "Study"
                val priority = intent.getStringExtra(EXTRA_TASK_PRIORITY) ?: "MEDIUM"
                showTaskReminderNotification(context, taskId, title, category, priority)
            }
        }
    }

    private fun showTaskReminderNotification(
        context: Context,
        taskId: Long,
        title: String,
        category: String,
        priority: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannels(notificationManager)

        // Intent to open app
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to start focus study session
        val focusIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_FOCUS_TAB", true)
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val focusPendingIntent = PendingIntent.getActivity(
            context,
            (taskId + 10000).toInt(),
            focusIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action to mark completed
        val doneIntent = Intent(context, TaskReminderReceiver::class.java).apply {
            this.action = ACTION_MARK_COMPLETED
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId + 20000).toInt(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TASKS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Task Due: $title")
            .setContentText("Priority: $priority • Category: $category")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Start Focus", focusPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Mark Done", donePendingIntent)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .build()

        notificationManager.notify(taskId.toInt(), notification)
    }

    private fun showFocusCompletedNotification(context: Context, title: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannels(notificationManager)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_FOCUS_TAB", true)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            99998,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_FOCUS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Session Complete!")
            .setContentText("Great job! You finished your $title focus session.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setVibrate(longArrayOf(0, 300, 200, 300, 200, 400))
            .build()

        notificationManager.notify(99999, notification)
    }

    private fun createChannels(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val taskChannel = NotificationChannel(
                CHANNEL_TASKS_ID,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for scheduled tasks and upcoming deadlines"
                enableVibration(true)
            }
            val focusChannel = NotificationChannel(
                CHANNEL_FOCUS_ID,
                "Focus Timer & Study Sessions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when study sessions or break intervals complete"
                enableVibration(true)
            }
            manager.createNotificationChannel(taskChannel)
            manager.createNotificationChannel(focusChannel)
        }
    }
}
