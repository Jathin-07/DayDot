package com.daydot.lockscreen.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daydot.lockscreen.domain.DayCircleState
import com.daydot.lockscreen.ui.components.WeeklyDotsRow
import com.daydot.lockscreen.ui.viewmodel.DayDotUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FULL_DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
private val TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

@Composable
fun DashboardScreen(
    uiState: DayDotUiState,
    onQuickEditDate: (LocalDate) -> Unit,
    onOpenLockScreenPreview: () -> Unit,
    onApplyNow: () -> Unit
) {
    val todayReminder = uiState.todayReminder

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = uiState.today.format(FULL_DATE_FORMAT).uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Today",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Primary Today Reminder Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                .clickable { onQuickEditDate(uiState.today) }
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (todayReminder?.isImportant == true) {
                            "IMPORTANT DEADLINE"
                        } else {
                            "PRIMARY REMINDER"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.4.sp,
                        color = if (todayReminder?.isImportant == true) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Icon(
                        imageVector = if (todayReminder == null) Icons.Outlined.Add else Icons.Outlined.Edit,
                        contentDescription = "Edit today's reminder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = todayReminder?.title ?: "No reminder today",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = if (todayReminder == null) FontWeight.Normal else FontWeight.SemiBold,
                    color = if (todayReminder == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                if (!todayReminder?.description.isNullOrBlank()) {
                    Text(
                        text = todayReminder!!.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val notifyTime = todayReminder?.notificationLocalTime()
                if (notifyTime != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Notification at ${notifyTime.format(TIME_FORMAT)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Seven-Circle Weekly Preview Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(uiState.appearance.backgroundColor),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(vertical = 22.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "THIS WEEK (MON – SUN)",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.4.sp,
                        color = Color(uiState.appearance.secondaryTextColor)
                    )
                    Text(
                        text = "Tap a day below",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(uiState.appearance.secondaryTextColor)
                    )
                }

                WeeklyDotsRow(
                    weekSlots = uiState.weekSlots,
                    settings = uiState.appearance,
                    dotSize = 24.dp,
                    dotSpacing = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick-tap day list for the current Mon–Sun week
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    uiState.weekSlots.forEach { slot ->
                        val isToday = slot.date.isEqual(uiState.today)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isToday) Color.White.copy(alpha = 0.08f)
                                    else Color.Transparent
                                )
                                .clickable { onQuickEditDate(slot.date) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val dotColor = when (slot.state) {
                                DayCircleState.PAST -> Color(uiState.appearance.pastDayColor)
                                DayCircleState.TODAY_NORMAL -> Color(uiState.appearance.todayColor)
                                DayCircleState.TODAY_IMPORTANT -> Color.White
                                DayCircleState.UPCOMING_NORMAL -> Color(uiState.appearance.upcomingDayColor)
                                DayCircleState.UPCOMING_IMPORTANT -> Color(uiState.appearance.importantDayColor)
                            }
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Text(
                                text = "${slot.date.dayOfWeek.name.take(3)} ${slot.date.dayOfMonth}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isToday) Color(uiState.appearance.primaryTextColor)
                                else Color(uiState.appearance.secondaryTextColor),
                                modifier = Modifier.size(width = 58.dp, height = 18.dp)
                            )
                            Text(
                                text = slot.reminder?.title ?: "—",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (slot.reminder != null) {
                                    Color(uiState.appearance.primaryTextColor)
                                } else {
                                    Color(uiState.appearance.secondaryTextColor).copy(alpha = 0.55f)
                                },
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Quick Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onQuickEditDate(uiState.today) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = if (todayReminder == null) Icons.Outlined.Add else Icons.Outlined.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(if (todayReminder == null) "Set Today" else "Edit Today")
            }

            OutlinedButton(
                onClick = onApplyNow,
                enabled = !uiState.isApplyingWallpaper,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Wallpaper,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(if (uiState.isApplyingWallpaper) "Updating..." else "Sync Lock Screen")
            }
        }

        OutlinedButton(
            onClick = onOpenLockScreenPreview,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Open Full Lock Screen Preview")
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
