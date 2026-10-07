package com.hdlee73.dailyhabit

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.hdlee73.dailyhabit.notify.DailyScheduler
import com.hdlee73.dailyhabit.notify.Notifier
import com.hdlee73.dailyhabit.ui.DailyHabitTheme
import com.hdlee73.dailyhabit.ui.RoutineScreen
import com.hdlee73.dailyhabit.ui.ScheduleScreen
import com.hdlee73.dailyhabit.ui.TodayScreen
import com.hdlee73.dailyhabit.ui.TodoScreen

private data class Tab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val TABS = listOf(
    Tab("말씀", Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Filled.MenuBook),
    Tab("일정", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    Tab("할 일", Icons.Outlined.CheckCircle, Icons.Filled.CheckCircle),
    Tab("루틴", Icons.Outlined.Repeat, Icons.Filled.Repeat),
)

class MainActivity : ComponentActivity() {

    private var selectedTab by mutableIntStateOf(0)

    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        DailyScheduler.schedule(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        selectedTab = savedInstanceState?.getInt("tab") ?: intent.getIntExtra(Notifier.EXTRA_TAB, 0)
        requestInitialPermissions()
        DailyScheduler.schedule(this)

        setContent {
            DailyHabitTheme {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            tonalElevation = 0.dp,
                        ) {
                            TABS.forEachIndexed { i, tab ->
                                val selected = selectedTab == i
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { selectedTab = i },
                                    icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                                    label = { Text(tab.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            }
                        }
                    },
                ) { padding ->
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        modifier = Modifier.padding(padding).fillMaxSize(),
                        label = "tab",
                    ) { tab ->
                        when (tab) {
                            0 -> TodayScreen(onNavigate = { selectedTab = it })
                            1 -> ScheduleScreen()
                            2 -> TodoScreen()
                            else -> RoutineScreen()
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasExtra(Notifier.EXTRA_TAB)) selectedTab = intent.getIntExtra(Notifier.EXTRA_TAB, 0)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("tab", selectedTab)
    }

    private fun requestInitialPermissions() {
        val wanted = buildList {
            add(Manifest.permission.READ_CALENDAR)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.filter { checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (wanted.isNotEmpty()) permissions.launch(wanted.toTypedArray())
    }
}
