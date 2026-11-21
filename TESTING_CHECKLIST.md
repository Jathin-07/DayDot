# DayDot — Beginner Setup, Testing Checklist & Bug Reporting Guide

> **Verification Transparency Note:** Because this project was authored in an offline workspace without the Android SDK or a physical Android phone attached, **none of the on-device checks below have been executed on physical hardware yet**. Run through this checklist after building `app-debug.apk` (via GitHub Actions or Android Studio) and share any issues so we can fix them in Phase 6 before producing your final signed Release APK.

---

## 1. How to Get Your Working Debug APK (Beginner Step-by-Step)

### Path A: Free Cloud Build via GitHub Actions (No Android Studio Required)
1. Unzip `DayDot-Android-Project.zip` on your computer.
2. Go to [github.com](https://github.com), sign in, and click **New repository** → name it `DayDot` → click **Create repository**.
3. Upload everything inside the unzipped `DayDot` folder (make sure the hidden `.github/workflows/build-apk.yml` file is included — using Git CLI or GitHub Desktop is the easiest way to ensure `.github/` is uploaded):
   ```bash
   cd DayDot
   git init
   git add .
   git commit -m "Initial DayDot Android app"
   git branch -M main
   git remote add origin https://github.com/<YOUR-USERNAME>/DayDot.git
   git push -u origin main
   ```
4. In your GitHub repo, click the **Actions** tab → open the running **Build DayDot Android APKs** workflow.
5. Wait ~2 minutes for the green checkmark, scroll to **Artifacts**, and download **DayDot-Debug-APK** (unzip it to get `app-debug.apk`).

### Path B: Local Build in Android Studio
1. Open **Android Studio** → **Open** → select the unzipped `DayDot` folder.
2. Wait for Gradle Sync to finish (uses Gradle 8.9, AGP 8.7.2, Kotlin 2.0.21, SDK 35).
3. Click **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. Find your file at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 2. How to Install the APK on Your Android Phone
1. Transfer `app-debug.apk` to your phone (or download it directly in your phone's browser from GitHub Actions).
2. Open your phone's **Files** app and tap `app-debug.apk`.
3. If Android asks to allow unknown apps, tap **Settings → Allow from this source**, return, and tap **Install**.
4. Open **DayDot** from your app drawer.

---

## 3. Physical Device Testing Checklist

Mark each item `[x]` as you test on your Android phone.

### A. Installation & Launch
- [ ] **APK installs cleanly** on Android 8.0+ (API 26+) with no parsing or package errors.
- [ ] **Clean first launch:** App opens to the **Today** dashboard with no hardcoded sample reminders and displays *"No reminder today"*.
- [ ] **Permissions check:** App does not request unnecessary permissions (only asks for Notifications when you set a reminder notification time).

### B. Reminder Management (Single Primary Reminder Per Day)
- [ ] **Create today's reminder:** Add a reminder for today with a short title and optional note; confirm it appears immediately on the Dashboard.
- [ ] **One-reminder-per-day enforcement:** Tap `+` and select today's date again; confirm the editor loads your existing reminder for editing rather than creating a second reminder for the same day.
- [ ] **Edit reminder:** Update the title, description, and Important toggle; verify changes save cleanly.
- [ ] **Delete reminder:** Delete a reminder and confirm today reverts to *"No reminder today"*.
- [ ] **Multiple dates:** Create reminders for tomorrow and 2–3 days later in the week; confirm they appear under **Today & Upcoming** in chronological order.

### C. Weekly 7-Circle Tracker & Visual States
- [ ] **Past / completed days:** Days earlier in the current Mon–Sun week appear as dimmed filled circles.
- [ ] **Present day (normal):** Today's circle is solid crisp white (`#FFFFFF`).
- [ ] **Present day (important):** Marking today as **Important deadline** turns today's circle bright glowing white with a soft radial halo.
- [ ] **Upcoming days:** Future days in the current week appear muted grey (`#777777`), or glowing white/accent when marked Important.
- [ ] **Switching days (manual date test):** In Android Settings → System → Date & time, temporarily disable *Set time automatically*, advance the date by 1 day, and check that the lock screen and Dashboard switch to tomorrow's reminder.
- [ ] **Monday weekly rollover:** Temporarily advance the phone date to next Monday; verify a fresh Monday–Sunday week appears **without deleting** previous reminders (check **Past Archive** in Reminders). *(Remember to re-enable automatic date & time afterward.)*

### D. Appearance Customization & Live Preview
- [ ] **Theme presets:** Switch between *Obsidian Glow*, *Charcoal Coral*, and *Warm Paper* and verify the live preview updates instantaneously.
- [ ] **Fine-tuning controls:** Adjust background color, important accent color, typography (Sans / Mono / Serif), layout (Centered / Left Editorial / Compact Lower), circle size, circle spacing, text size, and vertical offset slider.
- [ ] **Settings persistence:** Close and reopen the app; verify your chosen style stays saved.

### E. Lock Screen Integration & OEM Fallback
- [ ] **Lock-screen-only targeting:** Tap **Apply to Lock Screen** on the Lock Screen tab, lock your phone, and verify:
  - Today's reminder and 7-circle row appear cleanly on the **lock screen**.
  - Your **home screen wallpaper** did NOT change.
  - Android's native system clock and notifications are clearly visible with **no overlap** and **no duplicate clock**.
- [ ] **Vertical alignment:** If your phone's native clock sits lower than usual, adjust the **Vertical position** slider in Style and re-apply.
- [ ] **Manual fallback (if OEM blocks auto-update):** Test **Save PNG & Set Manually**; verify a PNG is saved to `Pictures/DayDot/` and the system wallpaper picker opens. *(On Xiaomi/HyperOS, Samsung, Oppo, or Vivo phones, disable Wallpaper Carousel / Glance if your lock screen resets.)*

### F. Reboot, Persistence & Notifications
- [ ] **Phone restart:** Reboot your phone, reopen DayDot, and verify all reminders and appearance settings persist intact.
- [ ] **Notification delivery:** Create a reminder for today with a notification time 2 minutes from now, grant notification permission if prompted, lock your phone, and verify the notification fires on time and opens DayDot when tapped.
- [ ] **Cross-version check (if available):** Test on a second Android device or Android Studio Emulator running a different Android version (e.g., Android 11, 13, 14, or 15).

---

## 4. How to Report Bugs (for Phase 6 Fixes)
If anything fails or looks off during testing, reply with this short template and I will patch the exact file(s) and give you an updated project ZIP:

- **Phone model & Android version:** (e.g., Samsung Galaxy S23, Android 14 / One UI 6.1)
- **Which step failed:** (e.g., Lock screen didn't update automatically / clock overlapped text)
- **What happened vs. what you expected:**
- **Screenshot or error message (if any):**

---

## 5. Creating Your Final Signed Release APK (Phase 7)
Once all checklist items pass:
1. In Android Studio, choose **Build → Generate Signed App Bundle or APK → APK**.
2. Create a keystore file (keep it safe and never upload `.jks`/`.keystore` to GitHub).
3. Select the **release** build variant and click **Create** to output `app/release/app-release.apk`.
