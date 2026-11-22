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
 * Single unified renderer used by BOTH the in-app previews and the real Android
 * lock-screen wallpaper so every customization setting matches 1:1.
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
        val safeWidth = widthPx.coerceAtLeast(320)
        val safeHeight = heightPx.coerceAtLeast(560)
        val safeDensity = density.coerceAtLeast(0.75f)
        val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Solid minimalist background
        canvas.drawColor(settings.backgroundColor.toInt())

        val horizontalMarginPx = safeWidth * 0.09f
        val contentWidthPx = (safeWidth - horizontalMarginPx * 2f).toInt().coerceAtLeast(160)

        val baseTypeface = when (settings.fontStyle) {
            FontStyleOption.SANS -> Typeface.create("sans-serif", Typeface.NORMAL)
            FontStyleOption.MONO -> Typeface.MONOSPACE
            FontStyleOption.SERIF -> Typeface.SERIF
        }
        val boldTypeface = Typeface.create(baseTypeface, Typeface.BOLD)

        val layoutAlignment = when (settings.layoutStyle) {
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

        // 2. Subtle date caption (e.g., "SUNDAY, 22 NOV") — never draws a clock
        val datePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = settings.secondaryTextColor.toInt()
            textSize = 12f * safeDensity
            typeface = baseTypeface
            letterSpacing = 0.14f
        }
        val dateText = today.format(DATE_SUBTITLE_FORMATTER).uppercase(Locale.getDefault())
        val dateLayout = StaticLayout.Builder
            .obtain(dateText, 0, dateText.length, datePaint, contentWidthPx)
            .setAlignment(layoutAlignment)
            .setMaxLines(1)
            .build()

        canvas.save()
        canvas.translate(horizontalMarginPx, cursorY)
        dateLayout.draw(canvas)
        canvas.restore()
        cursorY += dateLayout.height + (12f * safeDensity)

        // 3. Today's primary reminder title (or subtle empty fallback)
        val rawTitle = todayReminder?.title?.takeIf { it.isNotBlank() } ?: "No reminder today"
        val displayTitle = if (settings.uppercaseTitle) rawTitle.uppercase(Locale.getDefault()) else rawTitle
        val isEmpty = todayReminder == null
        val clampedFontSizeSp = settings.reminderFontSizeSp.coerceIn(14f, 36f)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isEmpty) {
                applyAlpha(settings.secondaryTextColor.toInt(), 0.72f)
            } else {
                settings.primaryTextColor.toInt()
            }
            textSize = clampedFontSizeSp * safeDensity
            typeface = if (isEmpty) baseTypeface else boldTypeface
            letterSpacing = if (settings.fontStyle == FontStyleOption.MONO) 0.02f else -0.01f
        }
        val titleLayout = StaticLayout.Builder
            .obtain(displayTitle, 0, displayTitle.length, titlePaint, contentWidthPx)
            .setAlignment(layoutAlignment)
            .setLineSpacing(4f * safeDensity, 1.0f)
            .setMaxLines(3)
            .build()

        canvas.save()
        canvas.translate(horizontalMarginPx, cursorY)
        titleLayout.draw(canvas)
        canvas.restore()
        cursorY += titleLayout.height

        // 4. Optional short note (strictly honors settings.showDescription)
        val description = todayReminder?.description?.takeIf { it.isNotBlank() }
        if (settings.showDescription && description != null) {
            cursorY += 8f * safeDensity
            val descPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = settings.secondaryTextColor.toInt()
                textSize = (clampedFontSizeSp * 0.60f).coerceAtLeast(11f) * safeDensity
                typeface = baseTypeface
            }
            val descLayout = StaticLayout.Builder
                .obtain(description, 0, description.length, descPaint, contentWidthPx)
                .setAlignment(layoutAlignment)
                .setLineSpacing(3f * safeDensity, 1.0f)
                .setMaxLines(2)
                .build()

            canvas.save()
            canvas.translate(horizontalMarginPx, cursorY)
            descLayout.draw(canvas)
            canvas.restore()
            cursorY += descLayout.height
        }

        // 5. Seven-circle Monday–Sunday weekly tracker
        cursorY += 26f * safeDensity

        val maxRowWidth = contentWidthPx.toFloat()
        var diameterPx = settings.circleDiameterDp.coerceIn(14f, 44f) * safeDensity
        var spacingPx = settings.circleSpacingDp.coerceIn(4f, 24f) * safeDensity
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
            textSize = 12f * safeDensity
            typeface = baseTypeface
            this.textAlign = Paint.Align.CENTER
        }

        val glowIntensity = settings.glowIntensity.coerceIn(0.2f, 1.0f)
        val extraTopGlowSpace = if (settings.glowOnImportant) {
            (radiusPx * 0.45f * glowIntensity).coerceAtMost(12f * safeDensity)
        } else {
            4f * safeDensity
        }
        val letterOffsetY = if (settings.showDayLetters) {
            (20f * safeDensity) + extraTopGlowSpace
        } else {
            0f
        }
        val circleCenterY = cursorY + letterOffsetY + radiusPx

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (1.8f * safeDensity).coerceAtLeast(1.8f)
        }
        val todayMarkerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = settings.todayColor.toInt()
        }

        weekSlots.take(7).forEachIndexed { index, slot ->
            val centerX = rowStartX + radiusPx + index * (diameterPx + spacingPx)

            if (settings.showDayLetters) {
                letterPaint.color = if (slot.isToday) {
                    settings.primaryTextColor.toInt()
                } else {
                    applyAlpha(settings.secondaryTextColor.toInt(), 0.78f)
                }
                letterPaint.typeface = if (slot.isToday) boldTypeface else baseTypeface
                canvas.drawText(slot.shortLetter, centerX, cursorY + (12f * safeDensity), letterPaint)
            }

            // Important-day color ALWAYS uses settings.importantDayColor, independent of today/past/upcoming
            val fillColor = resolveCircleColor(slot.state, settings)
            val isImportantDay = slot.isImportant

            if (isImportantDay && settings.glowOnImportant) {
                val glowColor = settings.importantDayColor.toInt()
                val glowRadius = radiusPx * (1.55f + 0.85f * glowIntensity)
                val innerAlpha = (0.30f + 0.45f * glowIntensity).coerceIn(0f, 1f)
                val midAlpha = (0.12f + 0.18f * glowIntensity).coerceIn(0f, 1f)

                glowPaint.shader = RadialGradient(
                    centerX,
                    circleCenterY,
                    glowRadius,
                    intArrayOf(
                        applyAlpha(glowColor, innerAlpha),
                        applyAlpha(glowColor, midAlpha),
                        applyAlpha(glowColor, 0.0f)
                    ),
                    floatArrayOf(0.32f, 0.66f, 1.0f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawCircle(centerX, circleCenterY, glowRadius, glowPaint)
            } else if (isImportantDay) {
                // Distinct crisp outer ring when glow is turned OFF so important days stay recognizable
                ringPaint.color = applyAlpha(settings.importantDayColor.toInt(), 0.85f)
                canvas.drawCircle(centerX, circleCenterY, radiusPx + (3.5f * safeDensity), ringPaint)
            }

            circlePaint.color = fillColor
            canvas.drawCircle(centerX, circleCenterY, radiusPx, circlePaint)

            // Independent subtle current-day indicator dot below today's circle
            if (slot.isToday) {
                val markerY = circleCenterY + radiusPx + (8.5f * safeDensity)
                canvas.drawCircle(centerX, markerY, (2.4f * safeDensity).coerceAtLeast(2.2f), todayMarkerPaint)
            }
        }

        return bitmap
    }

    /**
     * Resolves fill color strictly by calendar state.
     * Any date marked important (past, today, or upcoming) ALWAYS receives settings.importantDayColor.
     */
    fun resolveCircleColor(state: DayCircleState, settings: AppearanceSettings): Int {
        return when (state) {
            DayCircleState.PAST -> settings.pastDayColor.toInt()
            DayCircleState.PAST_IMPORTANT -> settings.importantDayColor.toInt()
            DayCircleState.TODAY_NORMAL -> settings.todayColor.toInt()
            DayCircleState.TODAY_IMPORTANT -> settings.importantDayColor.toInt()
            DayCircleState.UPCOMING_NORMAL -> settings.upcomingDayColor.toInt()
            DayCircleState.UPCOMING_IMPORTANT -> settings.importantDayColor.toInt()
        }
    }

    private fun applyAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
