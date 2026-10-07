package com.hdlee73.dailyhabit.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hdlee73.dailyhabit.data.CalendarRepository
import com.hdlee73.dailyhabit.data.GospelRepository
import com.hdlee73.dailyhabit.data.AppDatabase
import java.time.LocalDate

/** 복음·일정·할 일을 모아 아침 알림을 보낸다 */
class DailyWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val today = LocalDate.now()
        val gospel = GospelRepository(ctx).get(today).getOrNull()
        val calendar = CalendarRepository(ctx)
        val events = runCatching { calendar.groupByDay(calendar.events(today, 1), today, 1)[today].orEmpty() }
            .getOrDefault(emptyList())
        val pending = runCatching { AppDatabase.get(ctx).todoDao().pending() }.getOrDefault(emptyList())
        val routines = runCatching {
            AppDatabase.get(ctx).routineDao().active().filter { it.activeOn(today.dayOfWeek) }
        }.getOrDefault(emptyList())
        Notifier.showDaily(ctx, today, gospel, events, calendar.hasPermission(), pending, routines)
        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        ForegroundInfo(Notifier.PREPARING_ID, Notifier.preparing(applicationContext))

    companion object {
        fun enqueue(context: Context) {
            val req = OneTimeWorkRequestBuilder<DailyWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("daily-notify", ExistingWorkPolicy.REPLACE, req)
        }
    }
}
