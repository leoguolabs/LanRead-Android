# LanRead for Android (澜阅)

[![Platform](https://img.shields.io/badge/platform-Android-brightgreen.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202025.10.01-purple.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

**LanRead (澜阅)** is an AI-powered, privacy-first EPUB reader for Android, ported from Isla Reader with Kotlin and Jetpack Compose.

> “把每本书变成一位可对话的导师：获取、阅读、理解与交流，一站式完成。”

---

## Features

### 📖 Reading & Library
- **EPUB Engine**: Fast parsing, table of contents, pagination, font/typography adjustment, reading themes (Light, Dark, Sepia).
- **Library Management**: Filter by status (*Want to Read*, *Reading*, *Finished*, *Paused*), search by title/author, book details & file statistics.
- **Highlights & Notes**: Inline text selection, multi-color highlighting, note editing, and quick navigation.
- **Bookmarks & Progress**: Chapter progress tracking, bookmarks management, reading stats & daily reading goals.

### 🤖 AI-Assisted Reading
- **Start-Reading Summary**: AI-generated chapter summaries and key takeaways to warm up your reading session.
- **Contextual In-Book AI**: Select text to translate, explain concepts, or ask questions directly with streaming AI responses.
- **Local-First & Privacy**: Local database caching (Room), offline capability, and secure credential handling.

### 🌐 Internationalization & Accessibility
- Localized in **English**, **简体中文**, **日本語**, and **한국어**.
- Dynamic Material 3 theming following system preferences.

---

## Tech Stack & Architecture

- **Language & Runtime**: Kotlin 2.2.20, Java 17
- **UI Framework**: Jetpack Compose (BOM 2025.10.01) + Material 3
- **Architecture**: MVVM + Clean Architecture with Coroutines & StateFlow
- **Data Persistence**: AndroidX Room 2.8.4 + DataStore Preferences 1.1.7
- **Build System**: Gradle 9.2.1, Android Gradle Plugin 8.13.0, KSP 2.2.20-2.0.4
- **Target SDK**: Compile SDK 36, Target SDK 36, Min SDK 26 (Android 8.0+)

---

## Project Structure

```
LanRead-Android/
├── app/                  # Main Android application module
│   ├── src/main/
│   │   ├── java/com/lanread/app/
│   │   │   ├── ads/          # AdMob integration
│   │   │   ├── data/         # Room DB, entities, DAOs, repositories
│   │   │   ├── ui/           # Jetpack Compose screens, components, viewmodels
│   │   │   └── util/         # EPUB parser, AI services, preferences
│   │   ├── res/              # Multi-language string resources, drawables, themes
│   │   └── assets/           # Sample starter EPUB book
│   └── build.gradle
├── e2e-tests/            # End-to-end test cases and verification scripts
├── tools/                # Developer utilities and inspection scripts
├── SPEC.md               # Full architecture and porting specification
└── build.gradle          # Root build configuration
```

---

## Building & Running

### Prerequisites
- JDK 17+
- Android SDK (API level 36)
- Android Studio Ladybug / Meerkat or Command Line Tools

### Build via Gradle
```bash
# Build debug APK
./gradlew assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

### Debug & Testing
You can run automated E2E tests and debugging helper scripts:
```powershell
# Run debug launcher
.\debug.bat

# Run E2E automation suite
cd e2e-tests
.\run_e2e_suite.ps1
```

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
