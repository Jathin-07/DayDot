package com.daydot.lockscreen.wallpaper

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.WindowManager
import com.daydot.lockscreen.data.ReminderRepository
import com.daydot.lockscreen.data.prefs.AppearanceRepository
import com.daydot.lockscreen.domain.WeekCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

sealed class WallpaperApplyResult {
    data object Success : WallpaperApplyResult()
    data class RestrictedFallbackNeeded(val reason: String) : WallpaperApplyResult()
    data class Error(val message: String) : WallpaperApplyResult()
}

class LockScreenWallpaperManager(
    private val context: Context,
    private val reminderRepository: ReminderRepository,
    private val appearanceRepository: AppearanceRepository
) {

    /**
     * Renders today's reminder and Mon–Sun weekly tracker and applies it strictly
     * to the Android lock screen (WallpaperManager.FLAG_LOCK), leaving the home screen untouched.
     */
    suspend fun renderAndApplyLockScreen(today: LocalDate = LocalDate.now()): WallpaperApplyResult =
        withContext(Dispatchers.IO) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                if (!wallpaperManager.isWallpaperSupported || !wallpaperManager.isSetWallpaperAllowed) {
                    return@withContext WallpaperApplyResult.RestrictedFallbackNeeded(
                        "This device currently restricts automatic wallpaper updates. Use Save & Apply Manually below."
                    )
                }

                val bitmap = renderCurrentBitmap(today)
                try {
                    wallpaperManager.setBitmap(
                        bitmap,
                        null,
                        true,
                        WallpaperManager.FLAG_LOCK
                    )
                    WallpaperApplyResult.Success
                } finally {
                    bitmap.recycle()
                }
            } catch (sec: SecurityException) {
                WallpaperApplyResult.RestrictedFallbackNeeded(
                    "Your phone manufacturer blocked automatic lock-screen updates (${sec.message ?: "SecurityException"}). Use Save & Apply Manually."
                )
            } catch (e: Exception) {
                WallpaperApplyResult.Error(e.message ?: "Unable to update lock-screen wallpaper.")
            }
        }

    /**
     * Reliable fallback for OEMs (MIUI/HyperOS, ColorOS, FuntouchOS, EMUI) that block
     * third-party lock-screen wallpaper APIs: saves the rendered PNG to Pictures/DayDot.
     */
    suspend fun exportLockScreenImageToGallery(today: LocalDate = LocalDate.now()): Result<Uri> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = renderCurrentBitmap(today)
                try {
                    val filename = "DayDot_LockScreen_${today}.png"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val values = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                            put(
                                MediaStore.Images.Media.RELATIVE_PATH,
                                "${Environment.DIRECTORY_PICTURES}/DayDot"
                            )
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                        val resolver = context.contentResolver
                        val uri = resolver.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            values
                        ) ?: error("Could not create MediaStore image entry")

                        resolver.openOutputStream(uri)?.use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        } ?: error("Could not open output stream for image")

                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                        uri
                    } else {
                        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                            ?: context.filesDir
                        val outFile = File(dir, filename)
                        FileOutputStream(outFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        Uri.fromFile(outFile)
                    }
                } finally {
                    bitmap.recycle()
                }
            }
        }

    fun launchSystemWallpaperPicker(imageUri: Uri? = null) {
        val intent = if (imageUri != null) {
            Intent(Intent.ACTION_ATTACH_DATA).apply {
                setDataAndType(imageUri, "image/png")
                putExtra("mimeType", "image/png")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Intent.ACTION_SET_WALLPAPER).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Set as Lock Screen Wallpaper").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    private suspend fun renderCurrentBitmap(today: LocalDate): Bitmap {
        val settings = appearanceRepository.getCurrentSettings()
        val weekReminders = reminderRepository.getWeekReminders(today)
        val weekSlots = WeekCalculator.buildCurrentWeek(today, weekReminders)
        val todayReminder = weekSlots.firstOrNull { it.date.isEqual(today) }?.reminder
        val (widthPx, heightPx, density) = resolveDisplayMetrics()

        return LockScreenBitmapRenderer.render(
            widthPx = widthPx,
            heightPx = heightPx,
            density = density,
            today = today,
            weekSlots = weekSlots,
            todayReminder = todayReminder,
            settings = settings
        )
    }

    private fun resolveDisplayMetrics(): Triple<Int, Int, Float> {
        val density = Resources.getSystem().displayMetrics.density.coerceAtLeast(1f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = context.getSystemService(WindowManager::class.java)
            val bounds = wm?.maximumWindowMetrics?.bounds
            if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
                return Triple(bounds.width(), bounds.height(), density)
            }
        }
        val metrics = Resources.getSystem().displayMetrics
        return Triple(metrics.widthPixels, metrics.heightPixels, density)
    }
}
