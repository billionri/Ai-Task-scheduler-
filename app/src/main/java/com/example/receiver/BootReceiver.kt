package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.alarm.AlarmScheduler
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val scheduler = AlarmScheduler(context)
            val db = AppDatabase.getDatabase(context)

            CoroutineScope(Dispatchers.IO).launch {
                val activeTasks = db.taskDao().getActiveReminders()
                for (task in activeTasks) {
                    scheduler.scheduleTaskReminder(task)
                }
            }
        }
    }
}
