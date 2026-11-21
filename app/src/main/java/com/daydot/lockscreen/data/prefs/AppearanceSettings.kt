package com.daydot.lockscreen.data.prefs

enum class FontStyleOption(val label: String) {
    SANS("Clean Sans"),
    MONO("Geometric Mono"),
    SERIF("Editorial Serif")
}

enum class LayoutStyleOption(val label: String) {
    CENTERED("Centered Minimal"),
    LEFT_EDITORIAL("Left Editorial"),
    COMPACT_BOTTOM("Compact Lower")
}

data class AppearanceSettings(
    val backgroundColor: Long = 0xFF0E0E10L,
    val pastDayColor: Long = 0xFF3F3F42L,
    val todayColor: Long = 0xFFFFFFFFL,
    val upcomingDayColor: Long = 0xFF777777L,
    val importantDayColor: Long = 0xFFFFFFFFL,
    val primaryTextColor: Long = 0xFFFFFFFFL,
    val secondaryTextColor: Long = 0xFF8E8E93L,
    val fontStyle: FontStyleOption = FontStyleOption.SANS,
    val layoutStyle: LayoutStyleOption = LayoutStyleOption.CENTERED,
    val circleDiameterDp: Float = 26f,
    val circleSpacingDp: Float = 12f,
    val reminderFontSizeSp: Float = 22f,
    /** Fraction of screen height (0.42..0.78) to place the block cleanly below the system clock. */
    val verticalOffsetPercent: Float = 0.56f,
    val showDayLetters: Boolean = true,
    val showDescription: Boolean = true,
    val uppercaseTitle: Boolean = false,
    val glowOnImportant: Boolean = true,
    val autoApplyLockScreen: Boolean = true
)

object AppearancePresets {
    /** Default dark minimalist theme with dimmed past circles and bright glowing white important days. */
    val ObsidianGlow = AppearanceSettings()

    /** Dark charcoal theme with muted coral (#D98282) accent for important deadlines. */
    val CharcoalCoral = AppearanceSettings(
        backgroundColor = 0xFF121214L,
        pastDayColor = 0xFF3A3A3EL,
        todayColor = 0xFFFFFFFFL,
        upcomingDayColor = 0xFF777777L,
        importantDayColor = 0xFFD98282L,
        primaryTextColor = 0xFFFFFFFFL,
        secondaryTextColor = 0xFF8C8C92L,
        glowOnImportant = true
    )

    /** Warm off-white editorial theme inspired by classic Dieter Rams geometric calendar prints. */
    val WarmPaper = AppearanceSettings(
        backgroundColor = 0xFFECEAE4L,
        pastDayColor = 0xFF222222L,
        todayColor = 0xFFEE6322L,
        upcomingDayColor = 0xFFBDBAB3L,
        importantDayColor = 0xFFEE6322L,
        primaryTextColor = 0xFF181818L,
        secondaryTextColor = 0xFF6E6C66L,
        layoutStyle = LayoutStyleOption.LEFT_EDITORIAL,
        glowOnImportant = false
    )
}
