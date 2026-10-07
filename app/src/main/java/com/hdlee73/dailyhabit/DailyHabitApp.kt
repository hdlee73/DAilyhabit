package com.hdlee73.dailyhabit

import android.app.Application
import com.hdlee73.dailyhabit.notify.DailyScheduler
import com.hdlee73.dailyhabit.notify.Notifier

class DailyHabitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
        DailyScheduler.schedule(this)
    }
}
