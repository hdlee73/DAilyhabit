package com.hdlee73.dailyhabit.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DailyAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        DailyWorker.enqueue(context)
        DailyScheduler.schedule(context) // 다음 날 9시 예약
    }
}
