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

    /** 앱을 열 때 새 버전이 있는지 확인 */
    var autoUpdateCheck: Boolean
        get() = prefs.getBoolean(KEY_AUTO_UPDATE, true)
        set(v) = prefs.edit().putBoolean(KEY_AUTO_UPDATE, v).apply()

    var lastUpdateCheck: Long
        get() = prefs.getLong(KEY_LAST_UPDATE_CHECK, 0L)
        set(v) = prefs.edit().putLong(KEY_LAST_UPDATE_CHECK, v).apply()

    var lastBackup: Long
        get() = prefs.getLong(KEY_LAST_BACKUP, 0L)
        set(v) = prefs.edit().putLong(KEY_LAST_BACKUP, v).apply()

    /** 장보기에 최근 넣은 품목 이름 (최근 것이 앞) */
    var shoppingHistory: List<String>
        get() = prefs.getString(KEY_SHOPPING_HISTORY, "").orEmpty().split('\n').filter { it.isNotBlank() }
        set(v) = prefs.edit().putString(KEY_SHOPPING_HISTORY, v.joinToString("\n")).apply()

    companion object {
        private const val KEY_AUTO_UPDATE = "auto_update_check"
        private const val KEY_LAST_UPDATE_CHECK = "last_update_check"
        private const val KEY_LAST_BACKUP = "last_backup"
        private const val KEY_SHOPPING_HISTORY = "shopping_history"
        private const val KEY_DAILY_ENABLED = "daily_enabled"
        private const val KEY_DAILY_MINUTE = "daily_minute"
    }
}
