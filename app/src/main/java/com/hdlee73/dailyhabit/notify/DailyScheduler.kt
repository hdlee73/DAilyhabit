package com.hdlee73.dailyhabit.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.hdlee73.dailyhabit.data.AppSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** 매일 아침 알림 예약 (시각은 설정에서 변경) */
object DailyScheduler {
    private const val REQUEST_CODE = 900

    fun nextTrigger(minute: Int, now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        val today = LocalDate.from(now).atTime(LocalTime.of(minute / 60, minute % 60))
        return if (now.isBefore(today)) today else today.plusDays(1)
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, DailyAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun schedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val settings = AppSettings(context)
        val pi = pendingIntent(context)
        if (!settings.dailyEnabled) {
            am.cancel(pi)
            return
        }
        val triggerAt = nextTrigger(settings.dailyMinute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }
}
