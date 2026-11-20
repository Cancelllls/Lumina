# Lumina

**Lumina** (`com.cancellls.lumina`) is a 100% local-first, FOSS (GPL-3.0) Android reminders application built around an **Adaptive Liquid Glass** aesthetic in Jetpack Compose.

## Features
- **Adaptive Liquid Glass Design System**: Dual Dark Obsidian and Light Crystal frosted surfaces with real-time backdrop blur (`dev.chrisbanes.haze`), specular top-left rim highlights, and 4 animated ambient mesh palettes (*Aurora Teal*, *Cobalt Glass*, *Amber Dusk*, *Emerald Mist*).
- **Smart Lists & Bento Dashboard**: *Today*, *Scheduled*, *Flagged*, *All*, and *Completed* smart views alongside an *Up Next* preview and customizable lists with 12 vector icons and 8 accent colors.
- **Natural-Language Quick-Add Capsule**: Instant inline parsing for dates (`tomorrow 9am`, `in 30m`, `next monday`, `2026-12-01`), recurrence (`every weekday 8am`, `every 3 days`), priority (`!`, `!!`, `!!!`), and flags (`#flag`).
- **Full Productivity Workflow**: Subtask checklists, swipe right to complete (with Undo + automatic recurring occurrence spawning), swipe left to delete (with Undo), instant cross-field search, sorting, and priority/flag controls.
- **Exact-Time Alarms & Notifications**: `AlarmManager` scheduling with `Done`, `Snooze 15m`, and `Snooze 1h` notification actions plus automatic rescheduling across device reboots and timezone changes.
- **Frosted Glass Home Widget**: Resizable 4x2 DayNight glass widget supporting 1-tap task completion and direct quick-add launch.
- **100% Offline & Private**: Zero telemetry, zero analytics, zero Play Services. Local Room SQLite database with schema-versioned JSON backup export/import via Android SAF.

## Tech Stack
- **UI**: Kotlin 2.1, Jetpack Compose (BOM `2024.12.01`), Material 3, Haze (`1.5.4`)
- **Storage**: Room (`2.6.1`) with KSP, SharedPreferences
- **Background**: `AlarmManager`, `BroadcastReceiver`, `AppWidgetProvider`
- **Build & CI**: Gradle `8.14.2`, AGP `8.11.1`, Java 17, GitHub Actions CI

## License
Licensed under the [GNU General Public License v3.0](LICENSE).
