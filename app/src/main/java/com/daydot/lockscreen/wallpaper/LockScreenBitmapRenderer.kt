package com.daydot.lockscreen.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.daydot.lockscreen.data.db.ReminderEntity
import com.daydot.lockscreen.data.prefs.AppearanceSettings
import com.daydot.lockscreen.data.prefs.FontStyleOption
import com.daydot.lockscreen.data.prefs.LayoutStyleOption
import com.daydot.lockscreen.domain.DayCircleState
import com.daydot.lockscreen.domain.WeekDaySlot
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Renders the lock-screen wallpaper Bitmap using native Android Canvas.
 * Deliberately leaves the upper ~38% empty so Android's native system clock
 * and notification area are never obscured or duplicated.
 */
object LockScreenBitmapRenderer {

    private val DATE_SUBTITLE_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())

    fun render(
        widthPx: Int,
        heightPx: Int,
        density: Float,
        today: LocalDate,
        weekSlots: List<WeekDaySlot>,
        todayReminder: ReminderEntity?,
        settings: AppearanceSettings
    ): Bitmap {
        val safeWidth = widthPx.coerceAtLeast(720)
        val safeHeight = heightPx.coerceAtLeast(1280)
        val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Solid minimalist background
        canvas.drawColor(settings.backgroundColor.toInt())

        val horizontalMarginPx = safeWidth * 0.10f
        val contentWidthPx = (safeWidth - horizontalMarginPx * 2f).toInt().coerceAtLeast(200)

        val baseTypeface = when (settings.fontStyle) {
            FontStyleOption.SANS -> Typeface.create("sans-serif", Typeface.NORMAL)
            FontStyleOption.MONO -> Typeface.MONOSPACE
            FontStyleOption.SERIF -> Typeface.SERIF
        }
        val boldTypeface = Typeface.create(baseTypeface, Typeface.BOLD)

        val textAlign = when (settings.layoutStyle) {
            LayoutStyleOption.LEFT_EDITORIAL -> Layout.Alignment.ALIGN_NORMAL
            LayoutStyleOption.CENTERED,
            LayoutStyleOption.COMPACT_BOTTOM -> Layout.Alignment.ALIGN_CENTER
        }

        // Effective vertical anchor (stays safely below native Android lock-screen clock)
        val anchorFraction = when (settings.layoutStyle) {
            LayoutStyleOption.COMPACT_BOTTOM -> settings.verticalOffsetPercent.coerceIn(0.62f, 0.80f)
            else -> settings.verticalOffsetPercent.coerceIn(0.42f, 0.78f)
        }
        var cursorY = safeHeight * anchorFraction

        // 2. Subtle date caption (e.g., "SATURDAY, 21 NOV") — never draws a clock
        val datePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = settings.secondaryTextColor.toInt()
            textSize = 12f * density
            typeface = baseTypeface
            letterSpacing = 0.14f
        }
        val dateText = today.format(DATE_SUBTITLE_FORMATTER).uppercase(Locale.getDefault())
        val dateLayout = StaticLayout.Builder
            .obtain(dateText, 0, dateText.length, datePaint, contentWidthPx)
            .setAlignment(textAlign)
            .setMaxLines(1)
            .build()

        canvas.save()
        canvas.translate(horizontalMarginPx, cursorY)
        dateLayout.draw(canvas)
        canvas.restore()
        cursorY += dateLayout.height + (14f * density)

        // 3. Today's primary reminder title (or subtle empty fallback)
        val rawTitle = todayReminder?.title?.takeIf { it.isNotBlank() } ?: "No reminder today"
        val displayTitle = if (settings.uppercaseTitle) rawTitle.uppercase(Locale.getDefault()) else rawTitle
        val isEmpty = todayReminder == null

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isEmpty) {
                applyAlpha(settings.secondaryTextColor.toInt(), 0.72f)
            } else {
                settings.primaryTextColor.toInt()
            }
            textSize = settings.reminderFontSizeSp * density
            typeface = if (isEmpty) baseTypeface else boldTypeface
            letterSpacing = if (settings.fontStyle == FontStyleOption.MONO) 0.02f else -0.01f
        }
        val titleLayout = StaticLayout.Builder
            .obtain(displayTitle, 0, displayTitle.length, titlePaint, contentWidthPx)
            .setAlignment(textAlign)
            .setLineSpacing(4f * density, 1.0f)
            .setMaxLines(3)
            .build()

        canvas.save()
        canvas.translate(horizontalMarginPx, cursorY)
        titleLayout.draw(canvas)
        canvas.restore()
        cursorY += titleLayout.height

        // 4. Optional short description
        val description = todayReminder?.description?.takeIf { it.isNotBlank() }
        if (settings.showDescription && description != null) {
            cursorY += 8f * density
            val descPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = settings.secondaryTextColor.toInt()
                textSize = (settings.reminderFontSizeSp * 0.62f).coerceAtLeast(12f) * density
                typeface = baseTypeface
            }
            val descLayout = StaticLayout.Builder
                .obtain(description, 0, description.length, descPaint, contentWidthPx)
                .setAlignment(textAlign)
                .setLineSpacing(3f * density, 1.0f)
                .setMaxLines(2)
                .build()

            canvas.save()
            canvas.translate(horizontalMarginPx, cursorY)
            descLayout.draw(canvas)
            canvas.restore()
            cursorY += descLayout.height
        }

        // 5. Seven-circle Monday–Sunday weekly tracker
        cursorY += 28f * density

        val maxRowWidth = contentWidthPx.toFloat()
        var diameterPx = settings.circleDiameterDp * density
        var spacingPx = settings.circleSpacingDp * density
        val totalRequestedWidth = diameterPx * 7f + spacingPx * 6f
        if (totalRequestedWidth > maxRowWidth) {
            val scale = maxRowWidth / totalRequestedWidth
            diameterPx *= scale
            spacingPx *= scale
        }
        val radiusPx = diameterPx / 2f
        val actualRowWidth = diameterPx * 7f + spacingPx * 6f

        val rowStartX = when (settings.layoutStyle) {
            LayoutStyleOption.LEFT_EDITORIAL -> horizontalMarginPx
            LayoutStyleOption.CENTERED,
            LayoutStyleOption.COMPACT_BOTTOM -> (safeWidth - actualRowWidth) / 2f
        }

        val letterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = settings.secondaryTextColor.toInt()
            textSize = (10.5f * density)
            typeface = baseTypeface
            textAlign = Paint.Align.CENTER
        }

        val letterOffsetY = if (settings.showDayLetters) 18f * density else 0f
        val circleCenterY = cursorY + letterOffsetY + radiusPx

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        weekSlots.take(7).forEachIndexed { index, slot ->
            val centerX = rowStartX + radiusPx + index * (diameterPx + spacingPx)

            if (settings.showDayLetters) {
                letterPaint.color = when (slot.state) {
                    DayCircleState.TODAY_NORMAL,
                    DayCircleState.TODAY_IMPORTANT -> settings.primaryTextColor.toInt()
                    else -> applyAlpha(settings.secondaryTextColor.toInt(), 0.78f)
                }
                canvas.drawText(slot.shortLetter, centerX, cursorY + (10f * density), letterPaint)
            }

            val fillColor = resolveCircleColor(slot.state, settings)
            val shouldGlow = when (slot.state) {
                DayCircleState.TODAY_IMPORTANT -> true
                DayCircleState.UPCOMING_IMPORTANT -> settings.glowOnImportant
                else -> false
            }

            if (shouldGlow) {
                val glowColor = if (slot.state == DayCircleState.TODAY_IMPORTANT) {
                    Color.WHITE
                } else {
                    fillColor
                }
                val glowRadius = radiusPx * 2.15f
                glowPaint.shader = RadialGradient(
                    centerX,
                    circleCenterY,
                    glowRadius,
                    intArrayOf(
                        applyAlpha(glowColor, 0.58f),
                        applyAlpha(glowColor, 0.22f),
                        applyAlpha(glowColor, 0.0f)
                    ),
                    floatArrayOf(0.35f, 0.68f, 1.0f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawCircle(centerX, circleCenterY, glowRadius, glowPaint)
            }

            circlePaint.color = fillColor
            canvas.drawCircle(centerX, circleCenterY, radiusPx, circlePaint)
        }

        return bitmap
    }

    fun resolveCircleColor(state: DayCircleState, settings: AppearanceSettings): Int {
        return when (state) {
            DayCircleState.PAST -> settings.pastDayColor.toInt()
            DayCircleState.TODAY_NORMAL -> settings.todayColor.toInt()
            DayCircleState.TODAY_IMPORTANT -> Color.WHITE
            DayCircleState.UPCOMING_NORMAL -> settings.upcomingDayColor.toInt()
            DayCircleState.UPCOMING_IMPORTANT -> settings.importantDayColor.toInt()
        }
    }

    private fun applyAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
