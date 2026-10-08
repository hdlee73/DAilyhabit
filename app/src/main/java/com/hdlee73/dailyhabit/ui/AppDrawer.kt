package com.hdlee73.dailyhabit.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdlee73.dailyhabit.R
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// 에버노트 사이드바 느낌의 어두운 메뉴
private val DrawerBg = Color(0xFF1A1A1A)
private val DrawerSelected = Color(0xFF333333)
private val DrawerText = Color(0xFFCCCCCC)
private val DrawerMuted = Color(0xFF8A8A8A)
private val Accent = Color(0xFF00A82D)

fun Section.icon(): ImageVector = when (this) {
    Section.GOSPEL -> Icons.AutoMirrored.Outlined.MenuBook
    Section.PRAYER -> Icons.Outlined.VolunteerActivism
    Section.SCHEDULE -> Icons.Outlined.CalendarMonth
    Section.TODO -> Icons.Outlined.CheckCircle
    Section.ROUTINE -> Icons.Outlined.Repeat
    Section.SETTINGS -> Icons.Outlined.Settings
}

@Composable
fun AppDrawer(
    current: Section,
    badges: Map<Section, String>,
    onSelect: (Section) -> Unit,
    onCreate: (Section) -> Unit,
) {
    val today = LocalDate.now()
    var newMenu by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(DrawerBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp),
    ) {
        // 계정 영역
        Row(Modifier.padding(start = 8.dp, top = 20.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape)) {
                Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.size(36.dp))
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(36.dp))
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text("DailyHabit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "${today.monthValue}월 ${today.dayOfMonth}일 ${today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)}",
                    color = DrawerMuted,
                    fontSize = 12.sp,
                )
            }
        }

        // + 새로 만들기 (에버노트의 초록 버튼)
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(Accent)
                    .clickable { newMenu = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Add, null, tint = Color.White)
                Text("새로 만들기", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    modifier = Modifier.padding(start = 8.dp))
            }
            DropdownMenu(expanded = newMenu, onDismissRequest = { newMenu = false }) {
                DropdownMenuItem(
                    text = { Text("할 일") },
                    leadingIcon = { Icon(Section.TODO.icon(), null) },
                    onClick = { newMenu = false; onCreate(Section.TODO) },
                )
                DropdownMenuItem(
                    text = { Text("루틴") },
                    leadingIcon = { Icon(Section.ROUTINE.icon(), null) },
                    onClick = { newMenu = false; onCreate(Section.ROUTINE) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf(Section.GOSPEL, Section.PRAYER).forEach { DrawerItem(it, current == it, badges[it], onSelect) }
            DrawerGroupLabel("하루 관리")
            listOf(Section.SCHEDULE, Section.TODO, Section.ROUTINE).forEach { DrawerItem(it, current == it, badges[it], onSelect) }
        }
        HorizontalDivider(color = Color(0xFF2C2C2C))
        DrawerItem(Section.SETTINGS, current == Section.SETTINGS, null, onSelect, Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun DrawerGroupLabel(text: String) {
    Text(
        text,
        color = DrawerMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 12.dp, top = 18.dp, bottom = 6.dp),
    )
}

@Composable
private fun DrawerItem(
    section: Section,
    selected: Boolean,
    badge: String?,
    onSelect: (Section) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) DrawerSelected else Color.Transparent)
            .clickable { onSelect(section) }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(section.icon(), null, tint = if (selected) Accent else DrawerText, modifier = Modifier.size(22.dp))
        Text(
            section.label,
            color = if (selected) Color.White else DrawerText,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f).padding(start = 14.dp),
        )
        if (!badge.isNullOrEmpty()) Text(badge, color = DrawerMuted, fontSize = 13.sp)
    }
}
