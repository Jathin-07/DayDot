package com.daydot.lockscreen.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appearanceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "daydot_appearance_prefs"
)

class AppearanceRepository(private val context: Context) {

    private object Keys {
        val BACKGROUND_COLOR = longPreferencesKey("background_color")
        val PAST_DAY_COLOR = longPreferencesKey("past_day_color")
        val TODAY_COLOR = longPreferencesKey("today_color")
        val UPCOMING_DAY_COLOR = longPreferencesKey("upcoming_day_color")
        val IMPORTANT_DAY_COLOR = longPreferencesKey("important_day_color")
        val PRIMARY_TEXT_COLOR = longPreferencesKey("primary_text_color")
        val SECONDARY_TEXT_COLOR = longPreferencesKey("secondary_text_color")
        val FONT_STYLE = stringPreferencesKey("font_style")
        val LAYOUT_STYLE = stringPreferencesKey("layout_style")
        val CIRCLE_DIAMETER = floatPreferencesKey("circle_diameter_dp")
        val CIRCLE_SPACING = floatPreferencesKey("circle_spacing_dp")
        val REMINDER_FONT_SIZE = floatPreferencesKey("reminder_font_size_sp")
        val VERTICAL_OFFSET = floatPreferencesKey("vertical_offset_percent")
        val SHOW_DAY_LETTERS = booleanPreferencesKey("show_day_letters")
        val SHOW_DESCRIPTION = booleanPreferencesKey("show_description")
        val UPPERCASE_TITLE = booleanPreferencesKey("uppercase_title")
        val GLOW_ON_IMPORTANT = booleanPreferencesKey("glow_on_important")
        val AUTO_APPLY = booleanPreferencesKey("auto_apply_lock_screen")
    }

    val settingsFlow: Flow<AppearanceSettings> = context.appearanceDataStore.data.map { prefs ->
        val defaults = AppearanceSettings()
        AppearanceSettings(
            backgroundColor = prefs[Keys.BACKGROUND_COLOR] ?: defaults.backgroundColor,
            pastDayColor = prefs[Keys.PAST_DAY_COLOR] ?: defaults.pastDayColor,
            todayColor = prefs[Keys.TODAY_COLOR] ?: defaults.todayColor,
            upcomingDayColor = prefs[Keys.UPCOMING_DAY_COLOR] ?: defaults.upcomingDayColor,
            importantDayColor = prefs[Keys.IMPORTANT_DAY_COLOR] ?: defaults.importantDayColor,
            primaryTextColor = prefs[Keys.PRIMARY_TEXT_COLOR] ?: defaults.primaryTextColor,
            secondaryTextColor = prefs[Keys.SECONDARY_TEXT_COLOR] ?: defaults.secondaryTextColor,
            fontStyle = prefs[Keys.FONT_STYLE]?.let {
                runCatching { FontStyleOption.valueOf(it) }.getOrNull()
            } ?: defaults.fontStyle,
            layoutStyle = prefs[Keys.LAYOUT_STYLE]?.let {
                runCatching { LayoutStyleOption.valueOf(it) }.getOrNull()
            } ?: defaults.layoutStyle,
            circleDiameterDp = prefs[Keys.CIRCLE_DIAMETER] ?: defaults.circleDiameterDp,
            circleSpacingDp = prefs[Keys.CIRCLE_SPACING] ?: defaults.circleSpacingDp,
            reminderFontSizeSp = prefs[Keys.REMINDER_FONT_SIZE] ?: defaults.reminderFontSizeSp,
            verticalOffsetPercent = prefs[Keys.VERTICAL_OFFSET] ?: defaults.verticalOffsetPercent,
            showDayLetters = prefs[Keys.SHOW_DAY_LETTERS] ?: defaults.showDayLetters,
            showDescription = prefs[Keys.SHOW_DESCRIPTION] ?: defaults.showDescription,
            uppercaseTitle = prefs[Keys.UPPERCASE_TITLE] ?: defaults.uppercaseTitle,
            glowOnImportant = prefs[Keys.GLOW_ON_IMPORTANT] ?: defaults.glowOnImportant,
            autoApplyLockScreen = prefs[Keys.AUTO_APPLY] ?: defaults.autoApplyLockScreen
        )
    }

    suspend fun getCurrentSettings(): AppearanceSettings = settingsFlow.first()

    suspend fun saveSettings(settings: AppearanceSettings) {
        context.appearanceDataStore.edit { prefs ->
            prefs[Keys.BACKGROUND_COLOR] = settings.backgroundColor
            prefs[Keys.PAST_DAY_COLOR] = settings.pastDayColor
            prefs[Keys.TODAY_COLOR] = settings.todayColor
            prefs[Keys.UPCOMING_DAY_COLOR] = settings.upcomingDayColor
            prefs[Keys.IMPORTANT_DAY_COLOR] = settings.importantDayColor
            prefs[Keys.PRIMARY_TEXT_COLOR] = settings.primaryTextColor
            prefs[Keys.SECONDARY_TEXT_COLOR] = settings.secondaryTextColor
            prefs[Keys.FONT_STYLE] = settings.fontStyle.name
            prefs[Keys.LAYOUT_STYLE] = settings.layoutStyle.name
            prefs[Keys.CIRCLE_DIAMETER] = settings.circleDiameterDp
            prefs[Keys.CIRCLE_SPACING] = settings.circleSpacingDp
            prefs[Keys.REMINDER_FONT_SIZE] = settings.reminderFontSizeSp
            prefs[Keys.VERTICAL_OFFSET] = settings.verticalOffsetPercent
            prefs[Keys.SHOW_DAY_LETTERS] = settings.showDayLetters
            prefs[Keys.SHOW_DESCRIPTION] = settings.showDescription
            prefs[Keys.UPPERCASE_TITLE] = settings.uppercaseTitle
            prefs[Keys.GLOW_ON_IMPORTANT] = settings.glowOnImportant
            prefs[Keys.AUTO_APPLY] = settings.autoApplyLockScreen
        }
    }
}
