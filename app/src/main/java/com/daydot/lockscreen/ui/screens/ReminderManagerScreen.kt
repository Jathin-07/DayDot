package com.daydot.lockscreen.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.daydot.lockscreen.data.db.ReminderEntity
import com.daydot.lockscreen.ui.viewmodel.DayDotUiState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ROW_DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())
private val NOTIFY_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

@Composable
fun ReminderManagerScreen(
    uiState: DayDotUiState,
    initialEditDate: LocalDate?,
    onClearInitialEditDate: () -> Unit,
    onSaveReminder: (
        date: LocalDate,
        title: String,
        description: String,
        isImportant: Boolean,
        notificationTimeMinutes: Int?
    ) -> Unit,
    onDeleteReminder: (LocalDate) -> Unit
) {
    var editingDate by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(initialEditDate) {
        if (initialEditDate != null) {
            editingDate = initialEditDate
            onClearInitialEditDate()
        }
    }

    val upcoming = uiState.allReminders.filter { !it.localDate().isBefore(uiState.today) }
    val past = uiState.allReminders
        .filter { it.localDate().isBefore(uiState.today) }
        .sortedByDescending { it.dateIso }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Reminders",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "One primary reminder per day. Past reminders stay preserved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
            }

            if (upcoming.isEmpty() && past.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No reminders yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Tap + to add your primary reminder for today or any upcoming date.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (upcoming.isNotEmpty()) {
                item {
                    Text(
                        text = "TODAY & UPCOMING",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.4.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(upcoming, key = { it.dateIso }) { item ->
                    ReminderRowCard(
                        reminder = item,
                        isToday = item.localDate().isEqual(uiState.today),
                        isPast = false,
                        onEdit = { editingDate = item.localDate() },
                        onDelete = { onDeleteReminder(item.localDate()) }
                    )
                }
            }

            if (past.isNotEmpty()) {
                item {
                    Text(
                        text = "PAST ARCHIVE",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.4.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                items(past, key = { it.dateIso }) { item ->
                    ReminderRowCard(
                        reminder = item,
                        isToday = false,
                        isPast = true,
                        onEdit = { editingDate = item.localDate() },
                        onDelete = { onDeleteReminder(item.localDate()) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(88.dp)) }
        }

        FloatingActionButton(
            onClick = { editingDate = uiState.today },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Add reminder")
        }
    }

    val activeDate = editingDate
    if (activeDate != null) {
        val existingByDate = uiState.allReminders.associateBy { it.dateIso }
        ReminderEditorDialog(
            initialDate = activeDate,
            existingRemindersByDate = existingByDate,
            onDismiss = { editingDate = null },
            onSave = { date, title, description, isImportant, notifyMinutes ->
                onSaveReminder(date, title, description, isImportant, notifyMinutes)
                editingDate = null
            },
            onDelete = { date ->
                onDeleteReminder(date)
                editingDate = null
            }
        )
    }
}

@Composable
private fun ReminderRowCard(
    reminder: ReminderEntity,
    isToday: Boolean,
    isPast: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isToday) Color.White.copy(alpha = 0.28f) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onEdit)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val dotColor = when {
                isPast -> Color(0xFF3F3F42)
                reminder.isImportant -> Color.White
                isToday -> Color.White
                else -> Color(0xFF777777)
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isToday) "TODAY · ${reminder.localDate().format(ROW_DATE_FORMAT)}"
                        else reminder.localDate().format(ROW_DATE_FORMAT),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (reminder.isImportant) {
                        Text(
                            text = "IMPORTANT",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                if (reminder.description.isNotBlank()) {
                    Text(
                        text = reminder.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                reminder.notificationLocalTime()?.let { time ->
                    Text(
                        text = "Notify at ${time.format(NOTIFY_TIME_FORMAT)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete reminder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderEditorDialog(
    initialDate: LocalDate,
    existingRemindersByDate: Map<String, ReminderEntity>,
    onDismiss: () -> Unit,
    onSave: (LocalDate, String, String, Boolean, Int?) -> Unit,
    onDelete: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf(initialDate) }
    val existingForSelectedDate = existingRemindersByDate[selectedDate.toString()]

    var title by remember(selectedDate) { mutableStateOf(existingForSelectedDate?.title ?: "") }
    var description by remember(selectedDate) { mutableStateOf(existingForSelectedDate?.description ?: "") }
    var isImportant by remember(selectedDate) { mutableStateOf(existingForSelectedDate?.isImportant ?: false) }
    var notifyMinutes by remember(selectedDate) { mutableStateOf(existingForSelectedDate?.notificationTimeMinutes) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        // Always open time picker so the button visibly responds even if permission was declined
        showTimePicker = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingForSelectedDate != null) "Edit Daily Reminder" else "New Daily Reminder",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(selectedDate.format(ROW_DATE_FORMAT))
                }

                if (existingForSelectedDate != null) {
                    Text(
                        text = "Editing the single primary reminder for this date.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 80) title = it },
                    label = { Text("Primary reminder") },
                    placeholder = { Text("e.g. Submit thesis draft") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 140) description = it },
                    label = { Text("Short note (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Important deadline", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Glows bright white on the lock screen",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isImportant,
                        onCheckedChange = { isImportant = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val timeLabel = notifyMinutes?.let {
                        LocalTime.of(it / 60, it % 60).format(NOTIFY_TIME_FORMAT)
                    } ?: "Off"

                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!granted) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    showTimePicker = true
                                }
                            } else {
                                showTimePicker = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (notifyMinutes != null) Icons.Outlined.NotificationsActive
                            else Icons.Outlined.NotificationsOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Notify: $timeLabel")
                    }

                    if (notifyMinutes != null) {
                        TextButton(onClick = { notifyMinutes = null }) {
                            Text("Clear")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(selectedDate, title, description, isImportant, notifyMinutes)
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (existingForSelectedDate != null) {
                    TextButton(onClick = { onDelete(selectedDate) }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )

    if (showDatePicker) {
        val initialMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val currentHour = (notifyMinutes ?: (9 * 60)) / 60
        val currentMinute = (notifyMinutes ?: (9 * 60)) % 60
        val timePickerState = rememberTimePickerState(
            initialHour = currentHour,
            initialMinute = currentMinute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Notification Time") },
            text = {
                Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        notifyMinutes = timePickerState.hour * 60 + timePickerState.minute
                        showTimePicker = false
                    }
                ) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}
