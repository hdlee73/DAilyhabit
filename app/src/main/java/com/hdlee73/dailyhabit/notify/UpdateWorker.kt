package com.hdlee73.dailyhabit.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hdlee73.dailyhabit.BuildConfig
import com.hdlee73.dailyhabit.data.AppSettings
import com.hdlee73.dailyhabit.data.Updater
import java.util.concurrent.TimeUnit

/** 12시간마다 새 버전을 확인하고, 있으면 설치할 때까지 알림을 다시 보낸다 */
class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!AppSettings(ctx).autoUpdateCheck) {
            Notifier.cancelUpdate(ctx)
            return Result.success()
        }
        val latest = runCatching { Updater.fetchLatest() }.getOrNull() ?: return Result.success()
        if (Updater.isNewer(latest.version, BuildConfig.VERSION_NAME)) Notifier.showUpdate(ctx, latest.version)
        else Notifier.cancelUpdate(ctx)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<UpdateWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("update-check", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
