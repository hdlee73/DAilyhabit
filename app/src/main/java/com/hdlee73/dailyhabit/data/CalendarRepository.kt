package com.hdlee73.dailyhabit.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

data class CalendarEvent(
    val id: Long,
    val title: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val allDay: Boolean,
    val location: String,
    val calendarName: String,
    val color: Int,
)

/**
 * 기기에 동기화된 캘린더(구글 캘린더 포함)를 CalendarContract로 읽는다.
 * 별도 로그인 없이, 휴대폰에 구글 계정이 있고 캘린더 동기화가 켜져 있으면 된다.
 */
class CalendarRepository(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun events(from: LocalDate, days: Int): List<CalendarEvent> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val zone = ZoneId.systemDefault()
        val startMillis = from.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = from.plusDays(days.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, startMillis)
            ContentUris.appendId(it, endMillis)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.DISPLAY_COLOR,
        )
        val result = mutableListOf<CalendarEvent>()
        context.contentResolver.query(
            uri,
            projection,
            "${CalendarContract.Instances.VISIBLE} = 1",
            null,
            "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { c ->
            while (c.moveToNext()) {
                val allDay = c.getInt(4) == 1
                // 종일 일정은 UTC 자정 기준으로 저장된다
                val tz = if (allDay) ZoneOffset.UTC else zone
                val start = LocalDateTime.ofInstant(Instant.ofEpochMilli(c.getLong(2)), tz)
                var end = LocalDateTime.ofInstant(Instant.ofEpochMilli(c.getLong(3)), tz)
                if (allDay) end = end.minusSeconds(1)
                result += CalendarEvent(
                    id = c.getLong(0),
                    title = c.getString(1)?.takeIf { it.isNotBlank() } ?: "(제목 없음)",
                    start = start,
                    end = end,
                    allDay = allDay,
                    location = c.getString(5).orEmpty(),
                    calendarName = c.getString(6).orEmpty(),
                    color = c.getInt(7),
                )
            }
        }
        result.sortedWith(compareBy({ it.start.toLocalDate() }, { !it.allDay }, { it.start }))
    }

    /** 날짜별로 묶기. 여러 날에 걸친 일정은 해당하는 모든 날에 표시한다. */
    fun groupByDay(events: List<CalendarEvent>, from: LocalDate, days: Int): Map<LocalDate, List<CalendarEvent>> =
        (0 until days).associate { offset ->
            val day = from.plusDays(offset.toLong())
            day to events.filter { !it.start.toLocalDate().isAfter(day) && !it.end.toLocalDate().isBefore(day) }
        }
}
