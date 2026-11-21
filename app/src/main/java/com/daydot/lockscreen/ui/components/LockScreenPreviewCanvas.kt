package com.daydot.lockscreen.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daydot.lockscreen.data.db.ReminderEntity
import com.daydot.lockscreen.data.prefs.AppearanceSettings
import com.daydot.lockscreen.data.prefs.FontStyleOption
import com.daydot.lockscreen.data.prefs.LayoutStyleOption
import com.daydot.lockscreen.domain.DayCircleState
import com.daydot.lockscreen.domain.WeekDaySlot
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PREVIEW_DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())

/**
 * Live Compose preview that mirrors LockScreenBitmapRenderer proportions, colors,
 * typography, and glowing-white important circle behavior.
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
    val bgColor = Color(settings.backgroundColor)
    val primaryText = Color(settings.primaryTextColor)
    val secondaryText = Color(settings.secondaryTextColor)

    val fontFamily = when (settings.fontStyle) {
        FontStyleOption.SANS -> FontFamily.SansSerif
        FontStyleOption.MONO -> FontFamily.Monospace
        FontStyleOption.SERIF -> FontFamily.Serif
    }

    val horizontalAlignment = when (settings.layoutStyle) {
        LayoutStyleOption.LEFT_EDITORIAL -> Alignment.Start
        LayoutStyleOption.CENTERED,
        LayoutStyleOption.COMPACT_BOTTOM -> Alignment.CenterHorizontally
    }
    val textAlign = when (settings.layoutStyle) {
        LayoutStyleOption.LEFT_EDITORIAL -> TextAlign.Start
        LayoutStyleOption.CENTERED,
        LayoutStyleOption.COMPACT_BOTTOM -> TextAlign.Center
    }

    val effectiveOffset = when (settings.layoutStyle) {
        LayoutStyleOption.COMPACT_BOTTOM -> settings.verticalOffsetPercent.coerceIn(0.62f, 0.80f)
        else -> settings.verticalOffsetPercent.coerceIn(0.42f, 0.78f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(bgColor)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = horizontalAlignment
        ) {
            // Top safe zone reserved for native Android clock & notifications
            if (showSystemClockPlaceholder) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "09:41",
                        color = primaryText.copy(alpha = 0.24f),
                        fontSize = (46f * scaleFactor).sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "NATIVE ANDROID CLOCK AREA",
                        color = secondaryText.copy(alpha = 0.45f),
                        fontSize = (8.5f * scaleFactor).coerceAtLeast(7f).sp,
                        letterSpacing = 1.4.sp
                    )
                }
            }

            // Dynamic spacer representing the vertical safe-zone offset
            val topWeight = (effectiveOffset - if (showSystemClockPlaceholder) 0.24f else 0.05f)
                .coerceIn(0.08f, 0.72f)
            val bottomWeight = (1f - effectiveOffset).coerceIn(0.12f, 0.58f)

            Spacer(modifier = Modifier.weight(topWeight))

            Text(
                text = today.format(PREVIEW_DATE_FORMAT).uppercase(Locale.getDefault()),
                color = secondaryText,
                fontSize = (11f * scaleFactor).coerceAtLeast(9f).sp,
                fontFamily = fontFamily,
                letterSpacing = 1.6.sp,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height((8f * scaleFactor).dp))

            val rawTitle = todayReminder?.title?.takeIf { it.isNotBlank() } ?: "No reminder today"
            val displayTitle = if (settings.uppercaseTitle) {
                rawTitle.uppercase(Locale.getDefault())
            } else {
                rawTitle
            }
            val isEmpty = todayReminder == null

            Text(
                text = displayTitle,
                color = if (isEmpty) secondaryText.copy(alpha = 0.72f) else primaryText,
                fontSize = (settings.reminderFontSizeSp * scaleFactor).coerceAtLeast(13f).sp,
                lineHeight = (settings.reminderFontSizeSp * scaleFactor * 1.24f).coerceAtLeast(16f).sp,
                fontWeight = if (isEmpty) FontWeight.Normal else FontWeight.SemiBold,
                fontFamily = fontFamily,
                textAlign = textAlign,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            val description = todayReminder?.description?.takeIf { it.isNotBlank() }
            if (settings.showDescription && description != null) {
                Spacer(modifier = Modifier.height((5f * scaleFactor).dp))
                Text(
                    text = description,
                    color = secondaryText,
                    fontSize = (settings.reminderFontSizeSp * 0.58f * scaleFactor).coerceAtLeast(10f).sp,
                    fontFamily = fontFamily,
                    textAlign = textAlign,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height((20f * scaleFactor).dp))

            WeeklyDotsRow(
                weekSlots = weekSlots,
                settings = settings,
                dotSize = (settings.circleDiameterDp * scaleFactor).coerceIn(14f, 34f).dp,
                dotSpacing = (settings.circleSpacingDp * scaleFactor).coerceIn(4f, 18f).dp,
                fontFamily = fontFamily,
                horizontalArrangement = when (settings.layoutStyle) {
                    LayoutStyleOption.LEFT_EDITORIAL -> Arrangement.Start
                    else -> Arrangement.Center
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(bottomWeight))
        }
    }
}

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
                        color = when (slot.state) {
                            DayCircleState.TODAY_NORMAL,
                            DayCircleState.TODAY_IMPORTANT -> primaryText
                            else -> secondaryText.copy(alpha = 0.78f)
                        },
                        fontSize = 10.sp,
                        fontFamily = fontFamily,
                        fontWeight = if (slot.state == DayCircleState.TODAY_NORMAL || slot.state == DayCircleState.TODAY_IMPORTANT) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                val fillColor = when (slot.state) {
                    DayCircleState.PAST -> Color(settings.pastDayColor)
                    DayCircleState.TODAY_NORMAL -> Color(settings.todayColor)
                    DayCircleState.TODAY_IMPORTANT -> Color.White
                    DayCircleState.UPCOMING_NORMAL -> Color(settings.upcomingDayColor)
                    DayCircleState.UPCOMING_IMPORTANT -> Color(settings.importantDayColor)
                }

                val shouldGlow = when (slot.state) {
                    DayCircleState.TODAY_IMPORTANT -> true
                    DayCircleState.UPCOMING_IMPORTANT -> settings.glowOnImportant
                    else -> false
                }

                val glowColor = if (slot.state == DayCircleState.TODAY_IMPORTANT) {
                    Color.White
                } else {
                    fillColor
                }

                // Canvas slightly larger than dotSize so radial glow never clips
                Canvas(modifier = Modifier.size(dotSize * 1.45f)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = (dotSize.toPx() / 2f)
                    if (shouldGlow) {
                        val glowRadius = radius * 1.9f
                        drawCircle(
                            brush = Brush.radialGradient(
                                0.35f to glowColor.copy(alpha = 0.58f),
                                0.68f to glowColor.copy(alpha = 0.22f),
                                1.0f to Color.Transparent,
                                center = center,
                                radius = glowRadius
                            ),
                            radius = glowRadius,
                            center = center
                        )
                    }
                    drawCircle(
                        color = fillColor,
                        radius = radius,
                        center = center
                    )
                }
            }
        }
    }
}
