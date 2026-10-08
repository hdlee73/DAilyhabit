package com.hdlee73.dailyhabit.ui

/** 메뉴 항목. 순서(ordinal)는 알림 인텐트에서도 쓰이므로 바꾸지 말 것 */
enum class Section(val label: String) {
    GOSPEL("오늘의 말씀"),
    PRAYER("기도"),
    SCHEDULE("일정"),
    TODO("할 일"),
    ROUTINE("루틴"),
    SETTINGS("설정"),
}
