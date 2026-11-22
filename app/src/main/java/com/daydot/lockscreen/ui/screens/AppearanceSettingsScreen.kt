package com.daydot.lockscreen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daydot.lockscreen.data.prefs.AppearancePresets
import com.daydot.lockscreen.data.prefs.AppearanceSettings
import com.daydot.lockscreen.data.prefs.FontStyleOption
import com.daydot.lockscreen.data.prefs.LayoutStyleOption
import com.daydot.lockscreen.ui.components.LockScreenPreviewCard
import com.daydot.lockscreen.ui.viewmodel.DayDotUiState

private val BG_SWATCHES = listOf(
    0xFF000000L, // Pure OLED Black
    0xFF0E0E10L, // Deep Charcoal (Default)
    0xFF17181CL, // Graphite Slate
    0xFF121820L, // Midnight Navy
    0xFF1A1614L, // Warm Espresso
    0xFFECEAE4L  // Warm Paper (Reference Light)
)

private val ACCENT_SWATCHES = listOf(
    0xFFFFFFFFL, // Bright White (Default)
    0xFFD98282L, // Muted Coral
    0xFFEE6322L, // Dieter Rams Orange
    0xFF8AB4F8L, // Soft Ice Blue
    0xFFA5D6A7L, // Muted Sage
    0xFFE6C87EL  // Warm Sand
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceSettingsScreen(
    uiState: DayDotUiState,
    onUpdateAppearance: (AppearanceSettings, Boolean) -> Unit
) {
    // Wait until DataStore finishes loading persisted settings so defaults can never overwrite saved values
    if (!uiState.isAppearanceLoaded) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val persisted = uiState.appearance

    // Lightweight per-slider drag overrides so dragging is 60fps without overwriting unrelated fields
    var dragCircleSize by remember { mutableStateOf<Float?>(null) }
    var dragCircleSpacing by remember { mutableStateOf<Float?>(null) }
    var dragTextSize by remember { mutableStateOf<Float?>(null) }
    var dragVerticalOffset by remember { mutableStateOf<Float?>(null) }
    var dragGlowIntensity by remember { mutableStateOf<Float?>(null) }

    val effective = persisted.copy(
        circleDiameterDp = dragCircleSize ?: persisted.circleDiameterDp,
        circleSpacingDp = dragCircleSpacing ?: persisted.circleSpacingDp,
        reminderFontSizeSp = dragTextSize ?: persisted.reminderFontSizeSp,
        verticalOffsetPercent = dragVerticalOffset ?: persisted.verticalOffsetPercent,
        glowIntensity = dragGlowIntensity ?: persisted.glowIntensity
    )

    val hasTodayNote = !uiState.todayReminder?.description.isNullOrBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Live 9:19.5 phone-aspect preview powered by the exact LockScreenBitmapRenderer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(248.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LockScreenPreviewCard(
                today = uiState.today,
                weekSlots = uiState.weekSlots,
                todayReminder = uiState.todayReminder,
                settings = effective,
                showSystemClockPlaceholder = true,
                scaleFactor = 0.52f,
                modifier = Modifier
                    .height(248.dp)
                    .aspectRatio(9f / 19.5f)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "LIVE PREVIEW",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.4.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Circles: ${effective.circleDiameterDp.toInt()}dp · Gap: ${effective.circleSpacingDp.toInt()}dp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Text: ${effective.reminderFontSizeSp.toInt()}sp · Pos: ${(effective.verticalOffsetPercent * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Glow: ${if (effective.glowOnImportant) "${(effective.glowIntensity * 100).toInt()}%" else "Off"} · Letters: ${if (effective.showDayLetters) "On" else "Off"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Short note: ${if (effective.showDescription) "On" else "Off"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = { onUpdateAppearance(effective, true) },
                    enabled = !uiState.isApplyingWallpaper,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (uiState.isApplyingWallpaper) "Applying..." else "Apply Now")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Color Presets (preserve circle size, spacing, text size, glow, and toggles!)
            SectionLabel("COLOR PRESETS")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = effective.backgroundColor == AppearancePresets.ObsidianGlow.backgroundColor &&
                        effective.importantDayColor == AppearancePresets.ObsidianGlow.importantDayColor,
                    onClick = {
                        onUpdateAppearance(effective.applyColorPreset(AppearancePresets.ObsidianGlow), false)
                    },
                    label = { Text("Obsidian Glow") }
                )
                FilterChip(
                    selected = effective.importantDayColor == AppearancePresets.CharcoalCoral.importantDayColor &&
                        effective.backgroundColor == AppearancePresets.CharcoalCoral.backgroundColor,
                    onClick = {
                        onUpdateAppearance(effective.applyColorPreset(AppearancePresets.CharcoalCoral), false)
                    },
                    label = { Text("Charcoal Coral") }
                )
                FilterChip(
                    selected = effective.backgroundColor == AppearancePresets.WarmPaper.backgroundColor,
                    onClick = {
                        onUpdateAppearance(effective.applyColorPreset(AppearancePresets.WarmPaper), false)
                    },
                    label = { Text("Warm Paper") }
                )
            }

            // Background Color Swatches
            SectionLabel("BACKGROUND COLOR")
            ColorSwatchRow(
                colors = BG_SWATCHES,
                selected = effective.backgroundColor,
                onSelect = { color ->
                    val isLight = color == 0xFFECEAE4L
                    onUpdateAppearance(
                        effective.copy(
                            backgroundColor = color,
                            primaryTextColor = if (isLight) 0xFF181818L else 0xFFFFFFFFL,
                            secondaryTextColor = if (isLight) 0xFF6E6C66L else 0xFF8E8E93L
                        ),
                        false
                    )
                }
            )

            // Important Deadline Accent Color
            SectionLabel("IMPORTANT DAY COLOR")
            ColorSwatchRow(
                colors = ACCENT_SWATCHES,
                selected = effective.importantDayColor,
                onSelect = { color ->
                    onUpdateAppearance(effective.copy(importantDayColor = color), false)
                }
            )

            // Typography
            SectionLabel("TYPOGRAPHY")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FontStyleOption.entries.forEach { option ->
                    FilterChip(
                        selected = effective.fontStyle == option,
                        onClick = { onUpdateAppearance(effective.copy(fontStyle = option), false) },
                        label = { Text(option.label) }
                    )
                }
            }

            // Layout
            SectionLabel("LAYOUT")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LayoutStyleOption.entries.forEach { option ->
                    FilterChip(
                        selected = effective.layoutStyle == option,
                        onClick = { onUpdateAppearance(effective.copy(layoutStyle = option), false) },
                        label = { Text(option.label) }
                    )
                }
            }

            // Size & Spacing Sliders
            LabeledSlider(
                label = "Circle size (${effective.circleDiameterDp.toInt()} dp)",
                value = effective.circleDiameterDp,
                range = 14f..44f,
                onValueChange = { dragCircleSize = it },
                onValueChangeFinished = {
                    val finalVal = dragCircleSize ?: effective.circleDiameterDp
                    dragCircleSize = null
                    onUpdateAppearance(effective.copy(circleDiameterDp = finalVal), false)
                }
            )

            LabeledSlider(
                label = "Circle spacing (${effective.circleSpacingDp.toInt()} dp)",
                value = effective.circleSpacingDp,
                range = 4f..24f,
                onValueChange = { dragCircleSpacing = it },
                onValueChangeFinished = {
                    val finalVal = dragCircleSpacing ?: effective.circleSpacingDp
                    dragCircleSpacing = null
                    onUpdateAppearance(effective.copy(circleSpacingDp = finalVal), false)
                }
            )

            LabeledSlider(
                label = "Reminder text size (${effective.reminderFontSizeSp.toInt()} sp)",
                value = effective.reminderFontSizeSp,
                range = 14f..36f,
                onValueChange = { dragTextSize = it },
                onValueChangeFinished = {
                    val finalVal = dragTextSize ?: effective.reminderFontSizeSp
                    dragTextSize = null
                    onUpdateAppearance(effective.copy(reminderFontSizeSp = finalVal), false)
                }
            )

            LabeledSlider(
                label = "Vertical position (${(effective.verticalOffsetPercent * 100).toInt()}% from top)",
                value = effective.verticalOffsetPercent,
                range = 0.42f..0.78f,
                onValueChange = { dragVerticalOffset = it },
                onValueChangeFinished = {
                    val finalVal = dragVerticalOffset ?: effective.verticalOffsetPercent
                    dragVerticalOffset = null
                    onUpdateAppearance(effective.copy(verticalOffsetPercent = finalVal), false)
                }
            )

            // Display & Glow Controls
            ToggleRow(
                title = "Glow on important days",
                subtitle = if (effective.glowOnImportant) {
                    "Soft luminous halo around important circles"
                } else {
                    "Off — important days show a crisp outline ring instead"
                },
                checked = effective.glowOnImportant,
                onCheckedChange = { onUpdateAppearance(effective.copy(glowOnImportant = it), false) }
            )

            if (effective.glowOnImportant) {
                LabeledSlider(
                    label = "Glow intensity (${(effective.glowIntensity * 100).toInt()}%)",
                    value = effective.glowIntensity,
                    range = 0.20f..1.0f,
                    onValueChange = { dragGlowIntensity = it },
                    onValueChangeFinished = {
                        val finalVal = dragGlowIntensity ?: effective.glowIntensity
                        dragGlowIntensity = null
                        onUpdateAppearance(effective.copy(glowIntensity = finalVal), false)
                    }
                )
            }

            ToggleRow(
                title = "Show weekday letters (M–S)",
                subtitle = "Minimalist weekday initials above each circle",
                checked = effective.showDayLetters,
                onCheckedChange = { onUpdateAppearance(effective.copy(showDayLetters = it), false) }
            )

            ToggleRow(
                title = "Show short note on lock screen",
                subtitle = if (hasTodayNote) {
                    "Displays today's saved note below the reminder"
                } else {
                    "No short note on today's reminder yet (add one in Reminders)"
                },
                checked = effective.showDescription,
                onCheckedChange = { onUpdateAppearance(effective.copy(showDescription = it), false) }
            )

            ToggleRow(
                title = "Uppercase reminder title",
                subtitle = "Editorial all-caps styling",
                checked = effective.uppercaseTitle,
                onCheckedChange = { onUpdateAppearance(effective.copy(uppercaseTitle = it), false) }
            )

            ToggleRow(
                title = "Auto-update lock screen",
                subtitle = "Automatically syncs lock screen after edits and at midnight",
                checked = effective.autoApplyLockScreen,
                onCheckedChange = { onUpdateAppearance(effective.copy(autoApplyLockScreen = it), false) }
            )

            Button(
                onClick = { onUpdateAppearance(effective, true) },
                enabled = !uiState.isApplyingWallpaper,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (uiState.isApplyingWallpaper) "Applying..." else "Apply Appearance to Lock Screen")
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 1.4.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun ColorSwatchRow(
    colors: List<Long>,
    selected: Long,
    onSelect: (Long) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        colors.forEach { hex ->
            val isSelected = hex == selected
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(hex))
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.25f),
                        shape = CircleShape
                    )
                    .clickable { onSelect(hex) }
            )
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = range
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
