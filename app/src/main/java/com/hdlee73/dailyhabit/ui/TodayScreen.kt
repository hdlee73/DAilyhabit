package com.hdlee73.dailyhabit.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdlee73.dailyhabit.data.FamilyPrayers
import com.hdlee73.dailyhabit.data.Gospel
import com.hdlee73.dailyhabit.data.GospelRepository
import java.time.LocalDate

@Composable
fun TodayScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repo = remember { GospelRepository(context) }
    val today = remember { LocalDate.now() }
    var gospel by remember { mutableStateOf(repo.cached(today)) }
    var loading by remember { mutableStateOf(gospel == null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload) {
        loading = true
        error = null
        repo.get(today, forceRefresh = reload > 0)
            .onSuccess { gospel = it }
            .onFailure { error = it.message ?: "알 수 없는 오류" }
        loading = false
    }

    val prayer = remember(today) { FamilyPrayers.forDate(today) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(formatKoreanDate(today), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            gospel?.liturgicalDay?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✠ 오늘의 복음", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { reload++ }, enabled = !loading) {
                        Icon(Icons.Filled.Refresh, contentDescription = "새로고침")
                    }
                }
                when {
                    loading && gospel == null -> Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    gospel != null -> GospelBody(gospel!!)
                    else -> Column {
                        Text(
                            "복음을 불러오지 못했어요. 인터넷 연결을 확인하고 다시 시도해 주세요.\n($error)",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { reload++ }) { Text("다시 시도") }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(repo.sourceUrl(today))))
                }) {
                    Text("매일미사에서 전체 독서 보기")
                    Spacer(Modifier.padding(start = 4.dp))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                }
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("🙏 가정을 위한 기도", style = MaterialTheme.typography.titleLarge)
                Text(prayer.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                Text(prayer.text, style = MaterialTheme.typography.bodyLarge, lineHeight = 26.sp)
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Text(FamilyPrayers.holyFamily.title, style = MaterialTheme.typography.titleSmall)
                Text(FamilyPrayers.holyFamilyText(), style = MaterialTheme.typography.bodyLarge, lineHeight = 26.sp)
            }
        }
    }
}

@Composable
private fun GospelBody(g: Gospel) {
    Column {
        Text(g.reference, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        if (g.title.isNotBlank()) {
            Text("<${g.title}>", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 6.dp))
        }
        Text(g.body, style = MaterialTheme.typography.bodyLarge, lineHeight = 27.sp)
        Text("주님의 말씀입니다.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
    }
}
