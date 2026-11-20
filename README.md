# Lumina

**Lumina** (`com.cancellls.lumina`) is a 100% local-first, FOSS (GPL-3.0) Android reminders application built around a **BitChord-inspired iOS 26 Liquid Glass** aesthetic in Jetpack Compose.

## Features
- **BitChord-Inspired Liquid Glass Engine**:
  - **Optical Refraction & Specular Rims**: Powered by `dev.chrisbanes.haze:haze-glass:2.1.0` with `GlassStyle.regular` refraction, `hazeGlassContainer` organic pill merging, and Apple-style progressive `TopFadeBlur` scroll-edge frosting.
  - **Dual-Island Floating Dock**: Spring-animated travelling glass selection pill across *Home*, *Today*, *Scheduled*, and *All* paired with a detached circular Quick-Add orb that morphs into an NLP capture capsule.
  - **Glass Fidelity Controls**: Switch between *Liquid Glass*, *Lighter Frost*, and *Reduced Blur* directly in Settings alongside OLED Dark / Crystal Light modes and 4 ambient mesh palettes.
- **Smart Lists & Bento Dashboard**: *Today*, *Scheduled*, *Flagged*, *All*, and *Completed* smart views alongside an *Up Next* preview and customizable lists with 12 vector icons and 8 accent colors.
- **Natural-Language Quick-Add**: Instant inline parsing for dates (`tomorrow 9am`, `in 30m`, `next monday`, `2026-12-01`), recurrence (`every weekday 8am`, `every 3 days`), priority (`!`, `!!`, `!!!`), and flags (`#flag`).
- **Full Productivity Workflow**: Subtask checklists, swipe right to complete (with Undo + automatic recurring occurrence spawning), swipe left to delete (with Undo), instant cross-field search, sorting, and priority/flag controls.
- **Exact-Time Alarms & Notifications**: `AlarmManager` scheduling with `Done`, `Snooze 15m`, and `Snooze 1h` notification actions plus automatic rescheduling across device reboots and timezone changes.
- **Frosted Glass Home Widget**: Resizable 4x2 DayNight glass widget supporting 1-tap task completion and direct quick-add launch.
- **100% Offline & Private**: Zero telemetry, zero analytics, zero Play Services. Local Room SQLite database with schema-versioned JSON backup export/import via Android SAF.

## Tech Stack
- **Runtime & Build**: JDK 21 LTS, Gradle `9.1.0`, AGP `9.0.1` (built-in Kotlin), Kotlin `2.4.21`, KSP `2.3.12`
- **UI**: Jetpack Compose (BOM `2026.11.01`), Material 3, Haze `2.1.0` (`haze`, `haze-blur`, `haze-blur-materials`, `haze-glass`)
- **Storage**: Room `2.8.5`, SharedPreferences
- **Background**: `AlarmManager`, `BroadcastReceiver`, `AppWidgetProvider`
- **CI/CD**: GitHub Actions (`ubuntu-latest`, JDK 21)

## License
Licensed under the [GNU General Public License v3.0](LICENSE).
