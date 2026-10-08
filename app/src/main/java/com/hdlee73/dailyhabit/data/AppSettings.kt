package com.hdlee73.dailyhabit.data

import android.content.Context

/** 사용자 설정 */
class AppSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var dailyEnabled: Boolean
        get() = prefs.getBoolean(KEY_DAILY_ENABLED, true)
        set(v) = prefs.edit().putBoolean(KEY_DAILY_ENABLED, v).apply()

    /** 아침 알림 시각 (자정부터 분) */
    var dailyMinute: Int
        get() = prefs.getInt(KEY_DAILY_MINUTE, 9 * 60)
        set(v) = prefs.edit().putInt(KEY_DAILY_MINUTE, v).apply()

    companion object {
        private const val KEY_DAILY_ENABLED = "daily_enabled"
        private const val KEY_DAILY_MINUTE = "daily_minute"
    }
}
