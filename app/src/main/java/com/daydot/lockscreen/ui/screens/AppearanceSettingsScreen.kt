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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val settings = uiState.appearance

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Live Preview pinned at top so every slider/color tweak updates immediately
        LockScreenPreviewCard(
            today = uiState.today,
            weekSlots = uiState.weekSlots,
            todayReminder = uiState.todayReminder,
            settings = settings,
            showSystemClockPlaceholder = true,
            scaleFactor = 0.56f,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Theme Presets
            SectionLabel("STYLE PRESETS")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = settings.backgroundColor == AppearancePresets.ObsidianGlow.backgroundColor &&
                        settings.importantDayColor == AppearancePresets.ObsidianGlow.importantDayColor,
                    onClick = { onUpdateAppearance(AppearancePresets.ObsidianGlow.copy(autoApplyLockScreen = settings.autoApplyLockScreen), false) },
                    label = { Text("Obsidian Glow") }
                )
                FilterChip(
                    selected = settings.importantDayColor == AppearancePresets.CharcoalCoral.importantDayColor &&
                        settings.backgroundColor == AppearancePresets.CharcoalCoral.backgroundColor,
                    onClick = { onUpdateAppearance(AppearancePresets.CharcoalCoral.copy(autoApplyLockScreen = settings.autoApplyLockScreen), false) },
                    label = { Text("Charcoal Coral") }
                )
                FilterChip(
                    selected = settings.backgroundColor == AppearancePresets.WarmPaper.backgroundColor,
                    onClick = { onUpdateAppearance(AppearancePresets.WarmPaper.copy(autoApplyLockScreen = settings.autoApplyLockScreen), false) },
                    label = { Text("Warm Paper") }
                )
            }

            // Background Color Swatches
            SectionLabel("BACKGROUND COLOR")
            ColorSwatchRow(
                colors = BG_SWATCHES,
                selected = settings.backgroundColor,
                onSelect = { color ->
                    val isLight = color == 0xFFECEAE4L
                    onUpdateAppearance(
                        settings.copy(
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
                selected = settings.importantDayColor,
                onSelect = { color ->
                    onUpdateAppearance(settings.copy(importantDayColor = color), false)
                }
            )

            // Typography
            SectionLabel("TYPOGRAPHY")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FontStyleOption.entries.forEach { option ->
                    FilterChip(
                        selected = settings.fontStyle == option,
                        onClick = { onUpdateAppearance(settings.copy(fontStyle = option), false) },
                        label = { Text(option.label) }
                    )
                }
            }

            // Layout
            SectionLabel("LAYOUT")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LayoutStyleOption.entries.forEach { option ->
                    FilterChip(
                        selected = settings.layoutStyle == option,
                        onClick = { onUpdateAppearance(settings.copy(layoutStyle = option), false) },
                        label = { Text(option.label) }
                    )
                }
            }

            // Sliders
            LabeledSlider(
                label = "Circle size (${settings.circleDiameterDp.toInt()} dp)",
                value = settings.circleDiameterDp,
                range = 16f..42f,
                onValueChange = { onUpdateAppearance(settings.copy(circleDiameterDp = it), false) }
            )

            LabeledSlider(
                label = "Circle spacing (${settings.circleSpacingDp.toInt()} dp)",
                value = settings.circleSpacingDp,
                range = 6f..22f,
                onValueChange = { onUpdateAppearance(settings.copy(circleSpacingDp = it), false) }
            )

            LabeledSlider(
                label = "Reminder text size (${settings.reminderFontSizeSp.toInt()} sp)",
                value = settings.reminderFontSizeSp,
                range = 16f..34f,
                onValueChange = { onUpdateAppearance(settings.copy(reminderFontSizeSp = it), false) }
            )

            LabeledSlider(
                label = "Vertical position (${(settings.verticalOffsetPercent * 100).toInt()}% from top)",
                value = settings.verticalOffsetPercent,
                range = 0.42f..0.78f,
                onValueChange = { onUpdateAppearance(settings.copy(verticalOffsetPercent = it), false) }
            )

            // Toggles
            ToggleRow(
                title = "Glow on important days",
                subtitle = "Soft luminous halo around important circles",
                checked = settings.glowOnImportant,
                onCheckedChange = { onUpdateAppearance(settings.copy(glowOnImportant = it), false) }
            )
            ToggleRow(
                title = "Show weekday letters (M–S)",
                subtitle = "Minimalist labels above each circle",
                checked = settings.showDayLetters,
                onCheckedChange = { onUpdateAppearance(settings.copy(showDayLetters = it), false) }
            )
            ToggleRow(
                title = "Show short note on lock screen",
                subtitle = "Display optional subtitle below reminder",
                checked = settings.showDescription,
                onCheckedChange = { onUpdateAppearance(settings.copy(showDescription = it), false) }
            )
            ToggleRow(
                title = "Uppercase reminder title",
                subtitle = "Editorial all-caps styling",
                checked = settings.uppercaseTitle,
                onCheckedChange = { onUpdateAppearance(settings.copy(uppercaseTitle = it), false) }
            )
            ToggleRow(
                title = "Auto-update lock screen",
                subtitle = "Refresh lock screen automatically on changes and midnight",
                checked = settings.autoApplyLockScreen,
                onCheckedChange = { onUpdateAppearance(settings.copy(autoApplyLockScreen = it), false) }
            )

            Button(
                onClick = { onUpdateAppearance(settings, true) },
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
    onValueChange: (Float) -> Unit
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
