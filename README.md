# Lumina

**Lumina** (`com.cancellls.lumina`) is a 100% local-first, FOSS (GPL-3.0) Android reminders application built around a **BitChord-inspired iOS 26 Liquid Glass** aesthetic in Jetpack Compose.

## Features
- **BitChord-Inspired Liquid Glass Engine**:
  - **Optical Refraction & Specular Rims**: Powered by `dev.chrisbanes.haze:haze-glass:2.1.0` with `GlassStyle.regular` refraction, `hazeGlassContainer` organic pill merging, Apple-style progressive `TopFadeBlur` + `BottomFadeBlur` scroll-edge frosting, and floating `44dp` grouped action pills.
  - **Scroll-Collapsing Dual-Island Dock**: Spring-animated horizontal drag & lift selection pill across *Home*, *Today*, *Scheduled*, and *All* with scroll-driven inline collapse (`NestedScrollConnection`) and a detached Quick-Add orb that morphs into an NLP capture capsule.
  - **Hardware Composition Haptics & Glass Toasts**: API 30+ `VibrationEffect.Composition` tactile engine (`LuminaHaptics`), floating glass Undo toast pills, and Settings **Glass Fidelity** toggle (*Liquid Glass*, *Lighter Frost*, *Reduced Blur*).
- **Smart Lists & Today Focus Ring**: *Today*, *Scheduled*, *Flagged*, *All*, and *Completed* smart views, animated Today completion ring, *Up Next* preview, and customizable lists with 12 vector icons and 8 accent colors.
- **Natural-Language Quick-Add & Long-Press Actions**: Instant inline parsing (`tomorrow 9am`, `in 30m`, `every weekday 8am`, `!!!`, `#flag`), swipe-to-complete/delete with Undo, long-press context menu (*Snooze*, *Duplicate*, *Share*, *Flag*, *Delete*), and launcher app shortcuts.
- **Exact-Time Alarms & Notifications**: `AlarmManager` scheduling with `Done`, `Snooze 15m`, and `Snooze 1h` notification actions plus automatic rescheduling across device reboots and timezone changes.
- **Frosted Glass Home Widget**: Resizable 4x2 DayNight glass widget supporting 1-tap task completion and direct quick-add launch.
- **100% Offline & Private**: Zero telemetry, zero analytics, zero Play Services. Local Room SQLite database with exported schemas and JSON backup export/import via Android SAF.

## Tech Stack & Production Build
- **Runtime & Build**: JDK 21 LTS, Gradle `9.3.1`, AGP `9.1.0` (built-in Kotlin, `compileSdk 37`), Kotlin `2.4.21`, KSP `2.3.12`
- **Release Optimization**: R8 minification + resource shrinking (`isMinifyEnabled = true`, `isShrinkResources = true`), per-ABI (`arm64-v8a`, `armeabi-v7a`, `x86_64`) & universal APK splits, strict Android Lint gate
- **UI**: Jetpack Compose (BOM `2026.11.01`), Material 3, Haze `2.1.0` (`haze`, `haze-blur`, `haze-blur-materials`, `haze-glass`)
- **Storage**: Room `2.8.5` (schema export enabled), SharedPreferences
- **Background**: `AlarmManager`, `BroadcastReceiver`, `AppWidgetProvider`
- **CI/CD**: GitHub Actions (`ubuntu-latest`, JDK 21)

## License
Licensed under the [GNU General Public License v3.0](LICENSE).
