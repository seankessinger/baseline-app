# Baseline

A little habit tracker for Android. Instead of streaks and guilt trips, every habit keeps a moving
baseline: you give a rough percent for how likely you are to actually do the thing, mark whether you
did or didn't, and that estimate drifts up or down over time. No clock, no streaks, no
notifications. A cycle only ends when you hit Confirm.

Native Kotlin + Jetpack Compose.

## Download

Grab the latest APK from the [Releases page](https://github.com/seankessinger/baseline-app/releases/latest)
and open it on your phone (Android 8.0 or newer) to install. The first time you'll have to let your
phone install from unknown sources and tap past the "unknown developer" warning. It's one universal
APK, so it works on any device.

## Building from source

Open the project folder in Android Studio, let Gradle sync, and run the app on a device or emulator
(API 26+).

Or from a terminal:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug   # build
./gradlew :app:installDebug    # push to a connected device/emulator
```

## Versions

Pinned to what I have installed locally:

- Gradle 8.14.3
- Android Gradle Plugin 8.13.0
- Kotlin 2.0.21
- Compose BOM 2024.10.01
- compileSdk / targetSdk 36, minSdk 26 (variable fonts need 26)

Newer AndroidX wants compileSdk 37 / AGP 9.1, which I don't have set up, so this is the newest set
that still builds against SDK 36. To go newer, install platform 37, bump AGP, and bump the versions
in `gradle/libs.versions.toml`.

## How it works

The roster (your list of groups) is the only thing that actually gets saved. Everything else (status,
which rung is "live", the baseline divider, surprise) is computed at render time and never stored.

```
com.baseline
  MainActivity.kt          edge-to-edge host, grabs the ViewModel
  model/
    Model.kt               Forecast / Series / Group / AppState, all immutable + serializable
    Logic.kt               the pure engine: clamp, showUp, baselineIndex, surprise,
                           mark/undo/confirm, the edit mutations, reorder, and the seed roster
  state/
    BaselineViewModel.kt   holds AppState; saves on every change except mid-edit or mid-drag
    Persistence.kt         DataStore, just one JSON blob
  ui/
    BaselineApp.kt         state-driven navigation (no Nav-Compose) + back handling
    theme/                 colors, type (bundled fonts), theme
    components/            bar, glyphs, confetti, haptics, controls
    screens/               roster, group, review, the edit screens, settings
```

Estimates are a whole percent from 5 to 95, with 50 as the baseline. A check bumps it +5, a cross
-5, N/A leaves it alone. "Surprise" is just how unexpected the result was, and it drives both the
color wash and the confetti, so a long-shot win celebrates harder than an expected one. The haptics
scale off the same number.

## Fonts

Newsreader and Archivo, bundled as variable TTFs under `res/font` and loaded per weight with
FontVariation. No Google Fonts dependency, so it works fully offline.

## Screenshots

In [`screenshots/`](screenshots/), grabbed off a Pixel 6 emulator: light and dark, the marking
cycle, the review/confirm step, and edit mode.
