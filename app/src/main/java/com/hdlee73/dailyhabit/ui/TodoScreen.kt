package com.hdlee73.dailyhabit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.hdlee73.dailyhabit.data.Todo
import com.hdlee73.dailyhabit.data.TodoDatabase
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dao = remember { TodoDatabase.get(context).todoDao() }
    val todos by dao.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var due by remember { mutableStateOf<LocalDate?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    fun add() {
        val title = input.trim()
        if (title.isEmpty()) return
        scope.launch { dao.insert(Todo(title = title, dueEpochDay = due?.toEpochDay())) }
        input = ""
        due = null
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (due ?: today).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        due = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showPicker = false
                }) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("취소") } },
        ) { DatePicker(state) }
    }

    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            Text("할 일", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("새 할 일 입력") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { add() }),
                    modifier = Modifier.weight(1f),
                )
                FilledIconButton(onClick = { add() }, modifier = Modifier.padding(start = 8.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = "추가")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { showPicker = true },
                    label = { Text(due?.let { "마감: ${formatKoreanDate(it)}" } ?: "마감일 선택") },
                    leadingIcon = { Icon(Icons.Filled.Event, contentDescription = null) },
                )
                if (due != null) AssistChip(onClick = { due = null }, label = { Text("마감일 없음") })
            }
        }

        val doneCount = todos.count { it.done }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
            if (todos.isEmpty()) {
                item {
                    Text("아직 할 일이 없어요.", modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(todos, key = { it.id }) { todo ->
                TodoRow(
                    todo = todo,
                    today = today,
                    onToggle = { scope.launch { dao.update(todo.copy(done = !todo.done)) } },
                    onDelete = { scope.launch { dao.delete(todo) } },
                )
            }
            if (doneCount > 0) {
                item {
                    TextButton(onClick = { scope.launch { dao.clearDone() } }, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text("완료한 항목 ${doneCount}개 지우기")
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoRow(todo: Todo, today: LocalDate, onToggle: () -> Unit, onDelete: () -> Unit) {
    val dueDate = todo.dueEpochDay?.let { LocalDate.ofEpochDay(it) }
    ListItem(
        leadingContent = { Checkbox(checked = todo.done, onCheckedChange = { onToggle() }) },
        headlineContent = {
            Text(
                todo.title,
                textDecoration = if (todo.done) TextDecoration.LineThrough else null,
                color = if (todo.done) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
            )
        },
        supportingContent = dueDate?.let { d ->
            {
                val overdue = !todo.done && d.isBefore(today)
                val rel = relativeDayLabel(d, today)
                Text(
                    (if (overdue) "기한 지남 · " else "") + formatKoreanDate(d) + (if (rel.isNotEmpty()) " · $rel" else ""),
                    color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "삭제") }
        },
    )
}
