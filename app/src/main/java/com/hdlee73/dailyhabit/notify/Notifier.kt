package com.hdlee73.dailyhabit.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hdlee73.dailyhabit.BuildConfig
import com.hdlee73.dailyhabit.MainActivity
import com.hdlee73.dailyhabit.data.Quotes
import com.hdlee73.dailyhabit.R
import com.hdlee73.dailyhabit.data.CalendarEvent
import com.hdlee73.dailyhabit.data.FamilyPrayers
import com.hdlee73.dailyhabit.data.Gospel
import com.hdlee73.dailyhabit.data.Routine
import com.hdlee73.dailyhabit.data.Todo
import com.hdlee73.dailyhabit.ui.formatEventTime
import com.hdlee73.dailyhabit.ui.formatKoreanDate
import com.hdlee73.dailyhabit.ui.formatMinute
import com.hdlee73.dailyhabit.ui.Section
import java.time.LocalDate

object Notifier {
    private const val CHANNEL_DAILY = "daily_morning"
    private const val CHANNEL_ROUTINE = "routine"
    private const val CHANNEL_TODO = "todo"
    private const val CHANNEL_WORK = "background"
    private const val DAILY_ID = 9001
    const val PREPARING_ID = 9002

    const val EXTRA_TAB = "tab"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_DAILY, "아침 알림", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = if (BuildConfig.CATHOLIC) "오늘의 복음 요약, 오늘 일정" else "오늘 일정, 오늘 루틴"
                },
                NotificationChannel(CHANNEL_ROUTINE, "루틴 알림", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "매일 루틴을 정한 시각에 알려줍니다"
                },
                NotificationChannel(CHANNEL_TODO, "할 일 마감 알림", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "마감 시각이 된 할 일을 알려줍니다"
                },
                NotificationChannel(CHANNEL_WORK, "알림 준비", NotificationManager.IMPORTANCE_MIN),
            )
        )
    }

    private fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openApp(context: Context, tab: Int, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_TAB, tab)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun notifyId(type: String, id: Long): Int =
        (if (type == Reminders.TYPE_ROUTINE) 20_000 else 60_000) + (id % 40_000).toInt()

    private fun doneAction(context: Context, type: String, id: Long): NotificationCompat.Action {
        val pi = PendingIntent.getBroadcast(
            context,
            notifyId(type, id),
            Intent(context, ReminderActionReceiver::class.java)
                .putExtra(Reminders.EXTRA_TYPE, type)
                .putExtra(Reminders.EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action.Builder(0, "완료", pi).build()
    }

    private fun post(context: Context, id: Int, n: Notification) {
        if (!canPost(context)) return
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) {
        }
    }

    fun cancel(context: Context, type: String, id: Long) =
        NotificationManagerCompat.from(context).cancel(notifyId(type, id))

    fun preparing(context: Context): Notification =
        NotificationCompat.Builder(context, CHANNEL_WORK)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("오늘의 말씀을 준비하고 있어요")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

    fun showRoutine(context: Context, routine: Routine) {
        val n = NotificationCompat.Builder(context, CHANNEL_ROUTINE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${routine.emoji} ${routine.title}")
            .setContentText("오늘의 루틴을 할 시간이에요 · ${formatMinute(routine.reminderMinute)}")
            .setContentIntent(openApp(context, Section.ROUTINE.ordinal, notifyId(Reminders.TYPE_ROUTINE, routine.id)))
            .addAction(doneAction(context, Reminders.TYPE_ROUTINE, routine.id))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        post(context, notifyId(Reminders.TYPE_ROUTINE, routine.id), n)
    }

    fun showTodo(context: Context, todo: Todo) {
        val text = buildString {
            append("마감 시각이에요")
            todo.dueMinute?.let { append(" · ").append(formatMinute(it)) }
            if (todo.memo.isNotBlank()) append("\n").append(todo.memo)
        }
        val n = NotificationCompat.Builder(context, CHANNEL_TODO)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(todo.title)
            .setContentText(text.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp(context, Section.TODO.ordinal, notifyId(Reminders.TYPE_TODO, todo.id)))
            .addAction(doneAction(context, Reminders.TYPE_TODO, todo.id))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        post(context, notifyId(Reminders.TYPE_TODO, todo.id), n)
    }

    fun showDaily(
        context: Context,
        date: LocalDate,
        gospel: Gospel?,
        events: List<CalendarEvent>,
        calendarAllowed: Boolean,
        pendingTodos: List<Todo>,
        routines: List<Routine> = emptyList(),
    ) {
        val text = buildString {
            if (BuildConfig.CATHOLIC) {
                // 복음 본문 전체 대신 요약: 매일미사의 주제 문장, 없으면 본문 첫머리
                append("✠ 오늘의 복음")
                if (gospel != null) {
                    append(" · ").append(gospel.reference).append('\n')
                    val digest = gospel.title.ifBlank {
                        val body = gospel.body.replace('\n', ' ')
                        if (body.length > 90) body.take(90) + "…" else body
                    }
                    append(digest)
                } else {
                    append("\n복음을 불러오지 못했어요. 앱에서 다시 시도해 주세요.")
                }
                append("\n\n")
            }
            append("📅 오늘 일정\n")
            when {
                !calendarAllowed -> append("캘린더 권한이 필요해요. 앱을 열어 허용해 주세요.")
                events.isEmpty() -> append("오늘은 등록된 일정이 없어요.")
                else -> events.forEach { append("• ").append(formatEventTime(it, date)).append("  ").append(it.title).append('\n') }
            }
            if (!BuildConfig.CATHOLIC) {
                append("\n\n🔁 오늘 루틴\n")
                if (routines.isEmpty()) append("오늘 예정된 루틴이 없어요.")
                else append(routines.joinToString("  ") { "${it.emoji} ${it.title}" })
            }
        }.trim()

        val summary = buildString {
            if (BuildConfig.CATHOLIC) append(gospel?.reference ?: "오늘의 복음").append(" · ")
            append("일정 ")
            append(if (calendarAllowed) "${events.size}개" else "-")
            if (!BuildConfig.CATHOLIC) append(" · 루틴 ${routines.size}개")
        }

        val n = NotificationCompat.Builder(context, CHANNEL_DAILY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("좋은 아침이에요 · ${formatKoreanDate(date)}")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp(context, Section.HOME.ordinal, 0))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        post(context, DAILY_ID, n)
    }
}
