package com.hdlee73.dailyhabit

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hdlee73.dailyhabit.data.AppDatabase
import com.hdlee73.dailyhabit.notify.DailyScheduler
import com.hdlee73.dailyhabit.notify.Notifier
import com.hdlee73.dailyhabit.ui.AppDrawer
import com.hdlee73.dailyhabit.ui.DailyHabitTheme
import com.hdlee73.dailyhabit.ui.PrayerScreen
import com.hdlee73.dailyhabit.ui.QuoteScreen
import com.hdlee73.dailyhabit.ui.RestaurantScreen
import com.hdlee73.dailyhabit.ui.RoutineScreen
import com.hdlee73.dailyhabit.ui.ScheduleScreen
import com.hdlee73.dailyhabit.ui.Section
import com.hdlee73.dailyhabit.ui.SettingsScreen
import com.hdlee73.dailyhabit.ui.TodayScreen
import com.hdlee73.dailyhabit.ui.TodoScreen
import com.hdlee73.dailyhabit.ui.icon
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private var sectionIndex by mutableIntStateOf(Section.HOME.ordinal)
    /** 메뉴의 '새로 만들기'로 편집 화면을 열어야 하는 섹션 */
    private var createIn by mutableStateOf<Section?>(null)
    /** 알림으로 열면 메뉴 없이 바로 해당 화면 */
    private var openedFromNotification by mutableStateOf(false)
    private var closeDrawerSignal by mutableIntStateOf(0)

    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        DailyScheduler.schedule(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openedFromNotification = intent.hasExtra(Notifier.EXTRA_TAB)
        sectionIndex = Section.fromIndex(
            savedInstanceState?.getInt("section") ?: intent.getIntExtra(Notifier.EXTRA_TAB, Section.HOME.ordinal)
        ).ordinal
        val showMenuFirst = savedInstanceState == null && !openedFromNotification
        requestInitialPermissions()
        DailyScheduler.schedule(this)

        setContent {
            DailyHabitTheme {
                // 앱을 처음 열면 메뉴부터 보여준다
                val drawer = rememberDrawerState(if (showMenuFirst) DrawerValue.Open else DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val section = Section.fromIndex(sectionIndex)

                val db = remember { AppDatabase.get(this) }
                val todos by db.todoDao().observeAll().collectAsState(initial = emptyList())
                val routines by db.routineDao().observeActive().collectAsState(initial = emptyList())
                val checks by db.routineDao().observeChecks().collectAsState(initial = emptyList())
                val places by db.restaurantDao().observeAll().collectAsState(initial = emptyList())
                val today = LocalDate.now()
                val todaysRoutines = routines.filter { it.activeOn(today.dayOfWeek) }
                val badges = mapOf(
                    Section.TODO to todos.count { !it.done }.takeIf { it > 0 }?.toString().orEmpty(),
                    Section.ROUTINE to if (todaysRoutines.isEmpty()) "" else
                        "${todaysRoutines.count { r -> checks.any { it.routineId == r.id && it.epochDay == today.toEpochDay() } }}/${todaysRoutines.size}",
                    Section.RESTAURANT to places.size.takeIf { it > 0 }?.toString().orEmpty(),
                )

                fun go(s: Section) {
                    sectionIndex = s.ordinal
                    scope.launch { drawer.close() }
                }

                androidx.compose.runtime.LaunchedEffect(closeDrawerSignal) {
                    if (closeDrawerSignal > 0) drawer.close()
                }
                BackHandler(enabled = drawer.isOpen) { scope.launch { drawer.close() } }

                ModalNavigationDrawer(
                    drawerState = drawer,
                    scrimColor = Color.Black.copy(alpha = 0.32f),
                    drawerContent = {
                        AppDrawer(
                            current = section,
                            badges = badges,
                            onSelect = ::go,
                            onCreate = { s -> createIn = s; go(s) },
                        )
                    },
                ) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
                            // 상단: 왼쪽 메뉴 버튼, 가운데 아이콘 + 화면 이름
                            Box(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp)) {
                                IconButton(
                                    onClick = { scope.launch { drawer.open() } },
                                    modifier = Modifier.align(Alignment.CenterStart),
                                ) { Icon(Icons.Filled.Menu, contentDescription = "메뉴 열기") }
                                Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(section.icon(), null, Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(section.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                                }
                            }
                            AnimatedContent(
                                targetState = section,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                label = "section",
                            ) { s ->
                                Box(Modifier.fillMaxSize()) {
                                    when (s) {
                                        Section.GOSPEL -> TodayScreen(onNavigate = ::go)
                                        Section.QUOTE -> QuoteScreen(onNavigate = ::go)
                                        Section.PRAYER -> PrayerScreen()
                                        Section.SCHEDULE -> ScheduleScreen()
                                        Section.TODO -> TodoScreen(
                                            openEditor = createIn == Section.TODO,
                                            onEditorOpened = { createIn = null },
                                        )
                                        Section.ROUTINE -> RoutineScreen(
                                            openEditor = createIn == Section.ROUTINE,
                                            onEditorOpened = { createIn = null },
                                        )
                                        Section.RESTAURANT -> RestaurantScreen(
                                            openEditor = createIn == Section.RESTAURANT,
                                            onEditorOpened = { createIn = null },
                                        )
                                        Section.SETTINGS -> SettingsScreen()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasExtra(Notifier.EXTRA_TAB)) {
            sectionIndex = Section.fromIndex(intent.getIntExtra(Notifier.EXTRA_TAB, Section.HOME.ordinal)).ordinal
            closeDrawerSignal++
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("section", sectionIndex)
    }

    private fun requestInitialPermissions() {
        val wanted = buildList {
            add(Manifest.permission.READ_CALENDAR)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.filter { checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (wanted.isNotEmpty()) permissions.launch(wanted.toTypedArray())
    }
}
