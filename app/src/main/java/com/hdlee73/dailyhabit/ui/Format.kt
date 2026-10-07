package com.hdlee73.dailyhabit.ui

import com.hdlee73.dailyhabit.data.CalendarEvent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val TIME = DateTimeFormatter.ofPattern("HH:mm")

fun formatKoreanDate(date: LocalDate): String =
    "${date.monthValue}월 ${date.dayOfMonth}일 (${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)})"

fun relativeDayLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
    today -> "오늘"
    today.plusDays(1) -> "내일"
    today.plusDays(2) -> "모레"
    else -> ""
}

/** 특정 날짜 기준으로 일정 시간을 표시 */
fun formatEventTime(e: CalendarEvent, day: LocalDate): String {
    if (e.allDay) return "종일"
    val startsToday = e.start.toLocalDate() == day
    val endsToday = e.end.toLocalDate() == day
    return when {
        startsToday && endsToday -> "${e.start.format(TIME)}–${e.end.format(TIME)}"
        startsToday -> "${e.start.format(TIME)}~"
        endsToday -> "~${e.end.format(TIME)}"
        else -> "종일"
    }
}
