package com.hdlee73.dailyhabit.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        DailyScheduler.schedule(context)
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Reminders.rescheduleAll(context)
            } finally {
                result.finish()
            }
        }
    }
}
