package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.alarm.AlarmScheduler
import com.example.data.AppDatabase
import com.example.data.TaskRepository
import com.example.receiver.TaskReminderReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskSchedulerApp : Application() {

    lateinit var repository: TaskRepository
        private set
    lateinit var alarmScheduler: AlarmScheduler
        private set
    lateinit var subscriptionManager: com.example.data.SubscriptionManager
        private set

    override fun onCreate() {
        super.onCreate()

        subscriptionManager = com.example.data.SubscriptionManager(this)
        val database = AppDatabase.getDatabase(this)
        alarmScheduler = AlarmScheduler(this)
        repository = TaskRepository(
            database.taskDao(),
            database.focusSessionDao(),
            alarmScheduler
        )

        createNotificationChannels()

        // Seed initial data if empty
        CoroutineScope(Dispatchers.IO).launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val taskChannel = NotificationChannel(
                TaskReminderReceiver.CHANNEL_TASKS_ID,
                getString(R.string.channel_task_reminders),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_task_reminders_desc)
                enableVibration(true)
            }

            val focusChannel = NotificationChannel(
                TaskReminderReceiver.CHANNEL_FOCUS_ID,
                getString(R.string.channel_focus_sessions),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_focus_sessions_desc)
                enableVibration(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(taskChannel)
            manager.createNotificationChannel(focusChannel)
        }
    }
}
