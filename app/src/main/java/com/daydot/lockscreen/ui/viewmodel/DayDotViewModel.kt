package com.daydot.lockscreen.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daydot.lockscreen.DayDotApplication
import com.daydot.lockscreen.data.db.ReminderEntity
import com.daydot.lockscreen.data.prefs.AppearanceSettings
import com.daydot.lockscreen.domain.WeekCalculator
import com.daydot.lockscreen.domain.WeekDaySlot
import com.daydot.lockscreen.scheduling.AlarmScheduler
import com.daydot.lockscreen.wallpaper.WallpaperApplyResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DayDotUiState(
    val today: LocalDate = LocalDate.now(),
    val allReminders: List<ReminderEntity> = emptyList(),
    val weekSlots: List<WeekDaySlot> = emptyList(),
    val todayReminder: ReminderEntity? = null,
    val appearance: AppearanceSettings = AppearanceSettings(),
    val isApplyingWallpaper: Boolean = false,
    val fallbackReason: String? = null
)

class DayDotViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DayDotApplication
    private val reminderRepo = app.reminderRepository
    private val appearanceRepo = app.appearanceRepository
    private val wallpaperManager = app.wallpaperManager

    private val _today = MutableStateFlow(LocalDate.now())
    private val _isApplying = MutableStateFlow(false)
    private val _fallbackReason = MutableStateFlow<String?>(null)
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _pendingEditDate = MutableStateFlow<LocalDate?>(null)
    val pendingEditDate: StateFlow<LocalDate?> = _pendingEditDate.asStateFlow()

    private var applyJob: Job? = null

    val uiState: StateFlow<DayDotUiState> = combine(
        _today,
        reminderRepo.observeAllReminders(),
        appearanceRepo.settingsFlow,
        _isApplying,
        _fallbackReason
    ) { today, allReminders, appearance, isApplying, fallbackReason ->
        val (monday, sunday) = WeekCalculator.weekBounds(today)
        val weekReminders = allReminders.filter {
            val d = it.localDate()
            !d.isBefore(monday) && !d.isAfter(sunday)
        }
        val weekSlots = WeekCalculator.buildCurrentWeek(today, weekReminders)
        val todayReminder = weekSlots.firstOrNull { it.date.isEqual(today) }?.reminder

        DayDotUiState(
            today = today,
            allReminders = allReminders,
            weekSlots = weekSlots,
            todayReminder = todayReminder,
            appearance = appearance,
            isApplyingWallpaper = isApplying,
            fallbackReason = fallbackReason
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DayDotUiState()
    )

    fun openEditorForDate(date: LocalDate) {
        _pendingEditDate.value = date
    }

    fun consumePendingEditDate() {
        _pendingEditDate.value = null
    }

    fun refreshDateIfChanged() {
        val now = LocalDate.now()
        if (!now.isEqual(_today.value)) {
            _today.value = now
            viewModelScope.launch {
                val settings = appearanceRepo.getCurrentSettings()
                if (settings.autoApplyLockScreen) {
                    applyToLockScreen(silent = true)
                }
            }
        }
    }

    fun saveReminder(
        date: LocalDate,
        title: String,
        description: String,
        isImportant: Boolean,
        notificationTimeMinutes: Int?
    ) {
        if (title.isBlank()) {
            _snackbarMessage.value = "Please enter a short reminder title."
            return
        }
        viewModelScope.launch {
            val saved = reminderRepo.saveReminder(
                date = date,
                title = title,
                description = description,
                isImportant = isImportant,
                notificationTimeMinutes = notificationTimeMinutes
            )
            AlarmScheduler.scheduleReminderNotification(app, saved)

            val settings = appearanceRepo.getCurrentSettings()
            if (settings.autoApplyLockScreen) {
                applyToLockScreen(
                    silent = false,
                    customSuccess = "Reminder saved and lock screen updated."
                )
            } else {
                _snackbarMessage.value = "Reminder saved."
            }
        }
    }

    fun deleteReminder(date: LocalDate) {
        viewModelScope.launch {
            reminderRepo.deleteReminder(date)
            AlarmScheduler.cancelReminderNotification(app, date.toString())

            val settings = appearanceRepo.getCurrentSettings()
            if (settings.autoApplyLockScreen) {
                applyToLockScreen(
                    silent = false,
                    customSuccess = "Reminder deleted and lock screen updated."
                )
            } else {
                _snackbarMessage.value = "Reminder deleted."
            }
        }
    }

    /**
     * Saves appearance settings to DataStore without spamming WallpaperManager on every
     * slider drag. Only applies to the Android lock screen when [applyImmediately] is true.
     */
    fun updateAppearance(settings: AppearanceSettings, applyImmediately: Boolean = false) {
        viewModelScope.launch {
            appearanceRepo.saveSettings(settings)
            if (applyImmediately) {
                applyToLockScreen(
                    silent = false,
                    customSuccess = "Appearance applied to lock screen."
                )
            }
        }
    }

    fun applyToLockScreen(
        silent: Boolean = false,
        customSuccess: String = "Lock screen wallpaper updated."
    ) {
        if (applyJob?.isActive == true) return
        applyJob = viewModelScope.launch {
            _isApplying.value = true
            try {
                val result = wallpaperManager.renderAndApplyLockScreen(_today.value)
                if (!silent) {
                    handleApplyResult(result, customSuccess = customSuccess)
                } else if (result is WallpaperApplyResult.RestrictedFallbackNeeded) {
                    _fallbackReason.value = result.reason
                }
            } finally {
                _isApplying.value = false
            }
        }
    }

    fun exportAndLaunchFallbackPicker() {
        if (applyJob?.isActive == true) return
        applyJob = viewModelScope.launch {
            _isApplying.value = true
            try {
                val exportResult = wallpaperManager.exportLockScreenImageToGallery(_today.value)
                exportResult.onSuccess { uri ->
                    val launched = wallpaperManager.launchSystemWallpaperPicker(uri)
                    _snackbarMessage.value = if (launched) {
                        "Saved to Pictures/DayDot. Choose 'Lock screen' in the picker."
                    } else {
                        "Saved PNG to Pictures/DayDot. Open your Gallery app to set it as your lock screen."
                    }
                }.onFailure { err ->
                    _snackbarMessage.value = "Could not save image: ${err.message ?: "Unknown error"}"
                }
            } finally {
                _isApplying.value = false
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    private fun handleApplyResult(result: WallpaperApplyResult, customSuccess: String) {
        when (result) {
            WallpaperApplyResult.Success -> {
                _fallbackReason.value = null
                _snackbarMessage.value = customSuccess
            }
            is WallpaperApplyResult.RestrictedFallbackNeeded -> {
                _fallbackReason.value = result.reason
                _snackbarMessage.value = "Automatic lock-screen update is restricted on this device. Use manual apply."
            }
            is WallpaperApplyResult.Error -> {
                _snackbarMessage.value = result.message
            }
        }
    }
}
