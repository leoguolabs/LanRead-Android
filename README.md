# LanRead for Android — AI-assisted EPUB reader

English | [简体中文](README_CN.md)

[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

LanRead (澜阅) is an EPUB reading app for Android, built with Kotlin and Jetpack Compose. Import books, organize a library, highlight passages and take notes, track reading progress, and use AI to summarize, translate, explain, or ask questions about what you read.

**Platforms:** Android (this repository) · [iOS and iPadOS source](https://github.com/visiontrail/LanRead) · [Website](https://isla-reader.top)

The Android app is a sibling implementation of LanRead for iOS. Features and release availability can differ between platforms; the repositories are maintained separately.

## Features

- **EPUB reading:** import EPUB files, navigate chapters and the table of contents, turn pages, and adjust typography and reading themes.
- **Library:** search by title or author and organize books by reading status or favorites.
- **Highlights and notes:** select passages, highlight them, add notes, and return to saved locations.
- **Bookmarks and progress:** save positions and review reading progress, time, and daily goals.
- **AI-assisted reading:** generate a start-reading summary or chapter skimming summaries, and use selected text for translation, explanations, and questions. AI availability depends on network access and a working service configuration.
- **Languages:** app resources include English, Simplified Chinese, Japanese, and Korean.

Reading data is stored on the device using Room and DataStore. AI requests send relevant content to a network service; see the app's AI consent and privacy information before using those features. The project also integrates Google Mobile Ads. The iOS project has a Notion integration; check the Android implementation before assuming feature or sync parity.

## Get started

Android requires **Android 8.0 (API 26) or later**. This repository currently provides source code; check [Releases](https://github.com/visiontrail/LanRead-Android/releases) for any published builds. You can build a debug APK as follows.

### Requirements

- JDK 17
- Android SDK with API 36 and the Android command-line tools, or Android Studio
- Internet access to download Gradle and dependencies

```bash
git clone https://github.com/visiontrail/LanRead-Android.git
cd LanRead-Android
./gradlew assembleDebug
```

On Windows, use `gradlew.bat assembleDebug`. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`; install it on a compatible device or emulator. Open the project in Android Studio if you prefer an IDE.

AI requests require a reachable AI configuration service and provider credentials. The current source tries the configured service first and falls back to an empty API key if that request fails, so a successful build alone does not guarantee working AI features. Review the configuration and credential handling before deploying a fork; do not commit secrets.

## Technology

- Kotlin 2.2.20, Java 17, Jetpack Compose and Material 3
- AndroidX Room, DataStore, Kotlin coroutines and StateFlow
- Android WebView for reader content
- Gradle wrapper 9.2.1, Android Gradle Plugin 8.13.0, compile/target SDK 36

## Project structure

```text
app/                  Android app, resources, EPUB parser, AI and data services
e2e-tests/            End-to-end test cases and Windows automation
gradle/               Gradle wrapper
tools/                Developer utilities
SPEC.md               iOS-to-Android porting specification
```

`SPEC.md` records the porting specification and may describe intended behavior as well as implemented behavior. Consult the source for current details.

## Development and feedback

Build with `./gradlew assembleDebug`; on Windows, `debug.bat` and `e2e-tests/run_e2e_suite.ps1` provide additional development and end-to-end workflows. See the scripts for their environment requirements before running them.

Report bugs and feature requests in [GitHub Issues](https://github.com/visiontrail/LanRead-Android/issues). Include your device, Android version, steps to reproduce, and any relevant logs without private book content or credentials.

## License

[MIT](LICENSE).
