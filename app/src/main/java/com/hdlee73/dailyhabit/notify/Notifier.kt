package com.hdlee73.dailyhabit.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hdlee73.dailyhabit.MainActivity
import com.hdlee73.dailyhabit.R
import com.hdlee73.dailyhabit.data.CalendarEvent
import com.hdlee73.dailyhabit.data.FamilyPrayers
import com.hdlee73.dailyhabit.data.Gospel
import com.hdlee73.dailyhabit.data.Todo
import com.hdlee73.dailyhabit.ui.formatEventTime
import com.hdlee73.dailyhabit.ui.formatKoreanDate
import java.time.LocalDate

object Notifier {
    private const val CHANNEL_DAILY = "daily_morning"
    private const val CHANNEL_WORK = "background"
    private const val DAILY_ID = 9001
    const val PREPARING_ID = 9002

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_DAILY, "아침 9시 알림", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "오늘의 복음, 가정을 위한 기도, 오늘 일정"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_WORK, "알림 준비", NotificationManager.IMPORTANCE_MIN)
        )
    }

    fun preparing(context: Context): Notification =
        NotificationCompat.Builder(context, CHANNEL_WORK)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("오늘의 말씀을 준비하고 있어요")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

    fun showDaily(
        context: Context,
        date: LocalDate,
        gospel: Gospel?,
        events: List<CalendarEvent>,
        calendarAllowed: Boolean,
        pendingTodos: List<Todo>,
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= 33
        ) return

        val prayer = FamilyPrayers.forDate(date)
        val text = buildString {
            append("✠ 오늘의 복음")
            if (gospel != null) {
                append(" · ").append(gospel.reference).append('\n')
                if (gospel.title.isNotBlank()) append("<").append(gospel.title).append(">\n")
                val body = gospel.body.replace('\n', ' ')
                append(if (body.length > 300) body.take(300) + "…" else body)
            } else {
                append("\n복음을 불러오지 못했어요. 앱에서 다시 시도해 주세요.")
            }
            append("\n\n🙏 ").append(prayer.title).append('\n')
            append(prayer.text)
            append("\n\n📅 오늘 일정\n")
            when {
                !calendarAllowed -> append("캘린더 권한이 필요해요. 앱을 열어 허용해 주세요.")
                events.isEmpty() -> append("오늘은 등록된 일정이 없어요.")
                else -> events.forEach { append("• ").append(formatEventTime(it, date)).append("  ").append(it.title).append('\n') }
            }
            if (pendingTodos.isNotEmpty()) {
                append("\n✅ 남은 할 일 ").append(pendingTodos.size).append("개")
                pendingTodos.take(3).forEach { append("\n• ").append(it.title) }
            }
        }.trim()

        val summary = buildString {
            append(gospel?.reference ?: "오늘의 복음")
            append(" · 일정 ")
            append(if (calendarAllowed) "${events.size}개" else "-")
        }

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_DAILY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("좋은 아침이에요 · ${formatKoreanDate(date)}")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(DAILY_ID, n)
        } catch (_: SecurityException) {
        }
    }
}
