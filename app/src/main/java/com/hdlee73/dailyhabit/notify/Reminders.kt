package com.hdlee73.dailyhabit.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.hdlee73.dailyhabit.data.AppDatabase
import com.hdlee73.dailyhabit.data.Routine
import com.hdlee73.dailyhabit.data.RoutineCheck
import com.hdlee73.dailyhabit.data.Todo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** 루틴·할 일 개별 알림 예약 */
object Reminders {
    const val EXTRA_TYPE = "type"
    const val EXTRA_ID = "id"
    const val TYPE_ROUTINE = "routine"
    const val TYPE_TODO = "todo"

    private fun requestCode(type: String, id: Long): Int =
        (if (type == TYPE_ROUTINE) 100_000 else 500_000) + (id % 400_000).toInt()

    private fun pending(context: Context, type: String, id: Long, flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            requestCode(type, id),
            Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_TYPE, type).putExtra(EXTRA_ID, id),
            flags or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun setAlarm(context: Context, at: LocalDateTime, pi: PendingIntent) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
    }

    private fun cancel(context: Context, type: String, id: Long) {
        val pi = pending(context, type, id, PendingIntent.FLAG_NO_CREATE) ?: return
        context.getSystemService(AlarmManager::class.java)?.cancel(pi)
        pi.cancel()
    }

    fun nextRoutineTime(routine: Routine, now: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        if (routine.days == 0) return null
        val time = LocalTime.of(routine.reminderMinute / 60, routine.reminderMinute % 60)
        for (i in 0..7) {
            val day = now.toLocalDate().plusDays(i.toLong())
            val at = day.atTime(time)
            if (routine.activeOn(day.dayOfWeek) && at.isAfter(now)) return at
        }
        return null
    }

    fun scheduleRoutine(context: Context, routine: Routine) {
        cancel(context, TYPE_ROUTINE, routine.id)
        if (!routine.reminderEnabled || routine.archived) return
        val at = nextRoutineTime(routine) ?: return
        setAlarm(context, at, pending(context, TYPE_ROUTINE, routine.id, PendingIntent.FLAG_UPDATE_CURRENT)!!)
    }

    fun cancelRoutine(context: Context, id: Long) = cancel(context, TYPE_ROUTINE, id)

    fun scheduleTodo(context: Context, todo: Todo) {
        cancel(context, TYPE_TODO, todo.id)
        val day = todo.dueEpochDay ?: return
        val minute = todo.dueMinute ?: return
        if (!todo.remind || todo.done) return
        val at = LocalDate.ofEpochDay(day).atTime(minute / 60, minute % 60)
        if (!at.isAfter(LocalDateTime.now())) return
        setAlarm(context, at, pending(context, TYPE_TODO, todo.id, PendingIntent.FLAG_UPDATE_CURRENT)!!)
    }

    fun cancelTodo(context: Context, id: Long) = cancel(context, TYPE_TODO, id)

    suspend fun rescheduleAll(context: Context) {
        val db = AppDatabase.get(context)
        db.routineDao().active().forEach { scheduleRoutine(context, it) }
        db.todoDao().withReminders().forEach { scheduleTodo(context, it) }
    }
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/** 루틴·할 일 알림 시각에 호출 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(Reminders.EXTRA_TYPE) ?: return
        val id = intent.getLongExtra(Reminders.EXTRA_ID, -1)
        val result = goAsync()
        receiverScope.launch {
            try {
                val db = AppDatabase.get(context)
                val today = LocalDate.now()
                when (type) {
                    Reminders.TYPE_ROUTINE -> db.routineDao().byId(id)?.let { r ->
                        if (r.activeOn(today.dayOfWeek) && db.routineDao().isChecked(r.id, today.toEpochDay()) == 0) {
                            Notifier.showRoutine(context, r)
                        }
                        Reminders.scheduleRoutine(context, r)
                    }
                    Reminders.TYPE_TODO -> db.todoDao().byId(id)?.let { t ->
                        if (!t.done) Notifier.showTodo(context, t)
                    }
                }
            } finally {
                result.finish()
            }
        }
    }
}

/** 알림의 '완료' 버튼 */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(Reminders.EXTRA_TYPE) ?: return
        val id = intent.getLongExtra(Reminders.EXTRA_ID, -1)
        val result = goAsync()
        receiverScope.launch {
            try {
                val db = AppDatabase.get(context)
                when (type) {
                    Reminders.TYPE_ROUTINE -> db.routineDao().check(RoutineCheck(id, LocalDate.now().toEpochDay()))
                    Reminders.TYPE_TODO -> db.todoDao().byId(id)?.let {
                        db.todoDao().update(it.copy(done = true, completedAt = System.currentTimeMillis()))
                    }
                }
                Notifier.cancel(context, type, id)
            } finally {
                result.finish()
            }
        }
    }
}
