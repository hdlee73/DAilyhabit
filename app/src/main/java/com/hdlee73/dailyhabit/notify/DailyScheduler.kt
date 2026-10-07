package com.hdlee73.dailyhabit.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** 매일 아침 9시 알림 예약 */
object DailyScheduler {
    val NOTIFY_TIME: LocalTime = LocalTime.of(9, 0)
    private const val REQUEST_CODE = 900

    fun nextTrigger(now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        val today = LocalDate.from(now).atTime(NOTIFY_TIME)
        return if (now.isBefore(today)) today else today.plusDays(1)
    }

    fun schedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, DailyAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val triggerAt = nextTrigger().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }
}
