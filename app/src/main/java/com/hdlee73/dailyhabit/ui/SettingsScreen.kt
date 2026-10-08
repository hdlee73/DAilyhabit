package com.hdlee73.dailyhabit.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.hdlee73.dailyhabit.data.AppSettings
import com.hdlee73.dailyhabit.notify.DailyScheduler
import com.hdlee73.dailyhabit.notify.DailyWorker

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    var enabled by remember { mutableStateOf(settings.dailyEnabled) }
    var minute by remember { mutableIntStateOf(settings.dailyMinute) }
    var pickTime by remember { mutableStateOf(false) }

    // 권한 상태 (설정 앱에서 돌아오면 다시 확인)
    var notifOk by remember { mutableStateOf(true) }
    var calendarOk by remember { mutableStateOf(true) }
    var exactOk by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        notifOk = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        calendarOk = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        exactOk = Build.VERSION.SDK_INT < 31 || (context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() ?: true)
        onPauseOrDispose { }
    }

    fun openAppSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SoftCard {
            SectionLabel("아침 알림", color = MaterialTheme.colorScheme.primary)
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("매일 아침 알림 받기", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (com.hdlee73.dailyhabit.BuildConfig.CATHOLIC) "오늘의 복음, 가정을 위한 기도, 오늘 일정과 루틴을 알려드려요" else "오늘의 명언, 오늘 일정과 루틴, 남은 할 일을 알려드려요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = enabled, onCheckedChange = {
                    enabled = it
                    settings.dailyEnabled = it
                    DailyScheduler.schedule(context)
                })
            }
            HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { pickTime = true }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.AccessTime, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("알림 시간", Modifier.weight(1f).padding(start = 14.dp), style = MaterialTheme.typography.bodyLarge)
                Text(
                    formatMinute(minute),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
            }
            Text(
                if (com.hdlee73.dailyhabit.BuildConfig.CATHOLIC) "알림을 누르면 오늘의 복음 화면이 열려요." else "알림을 누르면 오늘의 명언 화면이 열려요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
            FilledTonalButton(
                onClick = {
                    DailyWorker.enqueue(context)
                    Toast.makeText(context, "아침 알림을 지금 보내드릴게요", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            ) { Text("지금 미리 받아보기") }
        }

        SoftCard {
            SectionLabel("권한")
            PermissionRow(Icons.Filled.Notifications, "알림", notifOk) { openAppSettings() }
            PermissionRow(Icons.Filled.CalendarMonth, "캘린더 읽기", calendarOk) { openAppSettings() }
            PermissionRow(Icons.Filled.Alarm, "정확한 시간 알림", exactOk) {
                if (Build.VERSION.SDK_INT >= 31) {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.fromParts("package", context.packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        }

        SoftCard {
            SectionLabel("앱 정보")
            val version = remember {
                runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "-"
            }
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (com.hdlee73.dailyhabit.BuildConfig.CATHOLIC) "DailyHabit 천주교인용" else "DailyHabit", Modifier.weight(1f).padding(start = 14.dp), style = MaterialTheme.typography.bodyLarge)
                Text("v$version", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                (if (com.hdlee73.dailyhabit.BuildConfig.CATHOLIC) "복음: 한국천주교주교회의 매일미사 · 기도문: 가톨릭 기도서 · " else "") + "일정: 휴대폰에 동기화된 구글 캘린더 · 할 일, 루틴, 맛집은 이 휴대폰에만 저장돼요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }

    if (pickTime) {
        TimePickDialog(minute, onDismiss = { pickTime = false }) {
            minute = it
            settings.dailyMinute = it
            DailyScheduler.schedule(context)
            Toast.makeText(context, "매일 ${formatMinute(it)}에 알려드릴게요", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun PermissionRow(icon: ImageVector, label: String, ok: Boolean, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, Modifier.weight(1f).padding(start = 14.dp), style = MaterialTheme.typography.bodyLarge)
        if (ok) Pill("허용됨", MaterialTheme.colorScheme.primary)
        else TextButton(onClick = onFix) { Text("허용하기") }
    }
}
