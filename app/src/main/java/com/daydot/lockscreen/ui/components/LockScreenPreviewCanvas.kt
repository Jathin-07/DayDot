package com.daydot.lockscreen.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daydot.lockscreen.data.db.ReminderEntity
import com.daydot.lockscreen.data.prefs.AppearanceSettings
import com.daydot.lockscreen.domain.DayCircleState
import com.daydot.lockscreen.domain.WeekDaySlot
import com.daydot.lockscreen.wallpaper.LockScreenBitmapRenderer
import java.time.LocalDate

/**
 * Live lock-screen preview powered directly by [LockScreenBitmapRenderer] so that
 * circle size, circle spacing, reminder text size, glow, important-day color,
 * weekday letters, and short note match the actual Android lock-screen wallpaper 1:1.
 */
@Composable
fun LockScreenPreviewCard(
    today: LocalDate,
    weekSlots: List<WeekDaySlot>,
    todayReminder: ReminderEntity?,
    settings: AppearanceSettings,
    showSystemClockPlaceholder: Boolean,
    scaleFactor: Float = 0.65f,
    modifier: Modifier = Modifier
) {
    val previewBitmap = remember(today, weekSlots, todayReminder, settings) {
        val previewWidthPx = 450
        val previewHeightPx = 975 // 9:19.5 phone aspect ratio
        val previewDensity = previewWidthPx / 390f
        LockScreenBitmapRenderer.render(
            widthPx = previewWidthPx,
            heightPx = previewHeightPx,
            density = previewDensity,
            today = today,
            weekSlots = weekSlots,
            todayReminder = todayReminder,
            settings = settings
        ).asImageBitmap()
    }

    val primaryText = Color(settings.primaryTextColor)
    val secondaryText = Color(settings.secondaryTextColor)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(settings.backgroundColor))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
    ) {
        Image(
            bitmap = previewBitmap,
            contentDescription = "Lock screen live preview",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )

        // Preview-only placeholder showing where Android's native lock-screen clock sits
        if (showSystemClockPlaceholder) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = (24f * scaleFactor).coerceAtLeast(12f).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "09:41",
                    color = primaryText.copy(alpha = 0.28f),
                    fontSize = (42f * scaleFactor).coerceIn(22f, 44f).sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "SYSTEM CLOCK",
                    color = secondaryText.copy(alpha = 0.45f),
                    fontSize = (8f * scaleFactor).coerceIn(6.5f, 9f).sp,
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}

/**
 * Reusable 7-circle Monday–Sunday strip used on the Dashboard.
 * Strictly honors importantDayColor for any important date (past, today, or upcoming),
 * glowOnImportant, glowIntensity, and showDayLetters.
 */
@Composable
fun WeeklyDotsRow(
    weekSlots: List<WeekDaySlot>,
    settings: AppearanceSettings,
    dotSize: Dp,
    dotSpacing: Dp,
    fontFamily: FontFamily = FontFamily.SansSerif,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Center,
    modifier: Modifier = Modifier
) {
    val primaryText = Color(settings.primaryTextColor)
    val secondaryText = Color(settings.secondaryTextColor)
    val intensity = settings.glowIntensity.coerceIn(0.2f, 1.0f)

    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        weekSlots.take(7).forEachIndexed { index, slot ->
            if (index > 0) {
                Spacer(modifier = Modifier.size(dotSpacing))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (settings.showDayLetters) {
                    Text(
                        text = slot.shortLetter,
                        color = if (slot.isToday) primaryText else secondaryText.copy(alpha = 0.78f),
                        fontSize = 10.sp,
                        fontFamily = fontFamily,
                        fontWeight = if (slot.isToday) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                val fillColor = when (slot.state) {
                    DayCircleState.PAST -> Color(settings.pastDayColor)
                    DayCircleState.PAST_IMPORTANT -> Color(settings.importantDayColor)
                    DayCircleState.TODAY_NORMAL -> Color(settings.todayColor)
                    DayCircleState.TODAY_IMPORTANT -> Color(settings.importantDayColor)
                    DayCircleState.UPCOMING_NORMAL -> Color(settings.upcomingDayColor)
                    DayCircleState.UPCOMING_IMPORTANT -> Color(settings.importantDayColor)
                }

                val isImportantDay = slot.isImportant
                val accentColor = Color(settings.importantDayColor)

                Canvas(modifier = Modifier.size(dotSize * 1.45f)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = (dotSize.toPx() / 2f)
                    if (isImportantDay && settings.glowOnImportant) {
                        val glowRadius = radius * (1.45f + 0.55f * intensity)
                        drawCircle(
                            brush = Brush.radialGradient(
                                0.32f to accentColor.copy(alpha = (0.30f + 0.45f * intensity).coerceIn(0f, 1f)),
                                0.66f to accentColor.copy(alpha = (0.12f + 0.18f * intensity).coerceIn(0f, 1f)),
                                1.0f to Color.Transparent,
                                center = center,
                                radius = glowRadius
                            ),
                            radius = glowRadius,
                            center = center
                        )
                    } else if (isImportantDay) {
                        drawCircle(
                            color = accentColor.copy(alpha = 0.85f),
                            radius = radius + 3.dp.toPx(),
                            center = center,
                            style = Stroke(width = 1.6.dp.toPx())
                        )
                    }
                    drawCircle(
                        color = fillColor,
                        radius = radius,
                        center = center
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (slot.isToday) Color(settings.todayColor) else Color.Transparent)
                )
            }
        }
    }
}
