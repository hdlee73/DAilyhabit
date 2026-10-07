package com.hdlee73.dailyhabit

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.hdlee73.dailyhabit.notify.DailyScheduler
import com.hdlee73.dailyhabit.notify.DailyWorker
import com.hdlee73.dailyhabit.ui.DailyHabitTheme
import com.hdlee73.dailyhabit.ui.ScheduleScreen
import com.hdlee73.dailyhabit.ui.TodayScreen
import com.hdlee73.dailyhabit.ui.TodoScreen

private data class Tab(val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("오늘의 말씀", Icons.AutoMirrored.Filled.MenuBook),
    Tab("일정", Icons.Filled.CalendarMonth),
    Tab("할 일", Icons.Filled.Checklist),
)

class MainActivity : ComponentActivity() {

    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        DailyScheduler.schedule(this)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestInitialPermissions()
        DailyScheduler.schedule(this)

        setContent {
            DailyHabitTheme {
                var selected by rememberSaveable { mutableIntStateOf(0) }
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text(getString(R.string.app_name)) },
                            actions = {
                                IconButton(onClick = {
                                    DailyWorker.enqueue(this@MainActivity)
                                    Toast.makeText(this@MainActivity, "아침 알림을 미리 보내드릴게요", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Filled.NotificationsActive, contentDescription = "알림 미리보기")
                                }
                            },
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            TABS.forEachIndexed { i, tab ->
                                NavigationBarItem(
                                    selected = selected == i,
                                    onClick = { selected = i },
                                    icon = { Icon(tab.icon, contentDescription = null) },
                                    label = { Text(tab.label) },
                                )
                            }
                        }
                    },
                ) { padding ->
                    val m = Modifier.padding(padding)
                    when (selected) {
                        0 -> TodayScreen(m)
                        1 -> ScheduleScreen(m)
                        else -> TodoScreen(m)
                    }
                }
            }
        }
    }

    private fun requestInitialPermissions() {
        val wanted = buildList {
            add(Manifest.permission.READ_CALENDAR)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.filter { checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (wanted.isNotEmpty()) permissions.launch(wanted.toTypedArray())
    }
}
