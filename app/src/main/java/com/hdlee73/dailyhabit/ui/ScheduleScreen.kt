package com.hdlee73.dailyhabit.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hdlee73.dailyhabit.data.CalendarEvent
import com.hdlee73.dailyhabit.data.CalendarRepository
import java.time.LocalDate

private const val DAYS = 7

@Composable
fun ScheduleScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repo = remember { CalendarRepository(context) }
    var granted by remember { mutableStateOf(repo.hasPermission()) }
    var reload by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var grouped by remember { mutableStateOf<Map<LocalDate, List<CalendarEvent>>>(emptyMap()) }
    val today = LocalDate.now()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
    }

    LaunchedEffect(granted, reload) {
        if (!granted) return@LaunchedEffect
        loading = true
        grouped = repo.groupByDay(repo.events(today, DAYS), today, DAYS)
        loading = false
    }

    if (!granted) {
        Column(
            modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "휴대폰에 동기화된 구글 캘린더 일정을 보여드리려면 캘린더 읽기 권한이 필요해요.",
                textAlign = TextAlign.Center,
            )
            Button(onClick = { launcher.launch(Manifest.permission.READ_CALENDAR) }, modifier = Modifier.padding(top = 16.dp)) {
                Text("캘린더 권한 허용")
            }
        }
        return
    }

    if (loading) {
        Box(modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        return
    }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("앞으로 7일 일정", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = { reload++ }) { Text("새로고침") }
            }
        }
        items(grouped.entries.toList(), key = { it.key.toEpochDay() }) { (day, events) ->
            DayCard(day, events, today)
        }
        item {
            Text(
                "구글 캘린더 일정이 보이지 않으면 휴대폰 설정 > 계정 > Google에서 캘린더 동기화가 켜져 있는지 확인해 주세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DayCard(day: LocalDate, events: List<CalendarEvent>, today: LocalDate) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatKoreanDate(day), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val rel = relativeDayLabel(day, today)
                if (rel.isNotEmpty()) {
                    Text("  $rel", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (events.isEmpty()) {
                Text("일정 없음", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
            events.forEach { e ->
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.padding(top = 6.dp).size(10.dp).background(
                            if (e.color != 0) Color(e.color) else MaterialTheme.colorScheme.primary, CircleShape
                        )
                    )
                    Text(formatEventTime(e, day), style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp).width(96.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.title, style = MaterialTheme.typography.bodyLarge)
                        if (e.location.isNotBlank()) {
                            Text(e.location, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
