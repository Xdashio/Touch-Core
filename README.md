# TouchCore

<div align="center">

**Ultra-lightweight, brutalist floating assistive touch overlay and navigation hub for Android.**

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-purple.svg)](https://kotlinlang.org/)
[![Min SDK](https://img.shields.io/badge/Min_SDK-29-brightgreen.svg)](https://developer.android.com/about/versions/10)
[![Target SDK](https://img.shields.io/badge/Target_SDK-35-orange.svg)](https://developer.android.com/about/versions/15)
[![APK Size](https://img.shields.io/badge/APK_Size-2.5_MB-success.svg)](app/build/outputs/apk/release/)
[![Trackers](https://img.shields.io/badge/Trackers-0-brightgreen.svg)](#privacy--security)

</div>

---

## 💡 About TouchCore

**TouchCore** is a free, open-source alternative to Apple's AssistiveTouch and bulky third-party Android overlay apps. Built from scratch with Kotlin and clean architecture, TouchCore pairs a concentric circular floating bubble with a minimalist, high-contrast, brutalist interface.

Unlike generic overlay apps that bloat your device with advertisements, tracking SDKs, and 50MB+ download sizes, TouchCore weighs in at just **2.5 MB**, operates **100% offline**, and requests only the minimal permissions required to function.

---

## ✨ Features

* **Authentic Concentric Floating Core**:
  * Precision-crafted concentric circular floating button.
  * Natural dragging physics with smooth edge snapping and boundary detection.
  * Idle auto-dimming to 40% opacity to prevent screen distraction and save battery.
  * Customizable button size (40–80 dp) and active/idle opacity levels.

* **Global System Navigation**:
  * Instant access to **Home**, **Back**, **App Switcher (Recents)**, **Lock Screen**, **Screenshot**, and **Notification Shade** via Android's Accessibility framework.

* **Interactive Hardware Controls**:
  * Direct touchscreen **Volume Slider** with instant audio feedback.
  * Direct **Brightness Slider** (with fine-grained adjustment).
  * One-tap **Torch (Flashlight)** toggle using camera flash hardware.
  * Fast toggles for **Wi-Fi** and **Bluetooth** settings.
  * Screen orientation lock and native **Screen Recording** via MediaProjection.

* **Pinned App Favorites**:
  * Pin your most frequently used apps directly to the radial menu for instant launching from anywhere.

* **Zero-Radius Brutalist Design**:
  * Sharp `0dp` border radii on all cards, buttons, dialogs, and panels.
  * Deep OLED obsidian black (`#101216`) background with high-contrast monochrome accents.
  * Clean, unified iconography using lightweight Lucide stroke vectors.

* **Ultra-Lightweight & Private**:
  * **2.5 MB** release binary size with R8 code optimization and resource shrinking.
  * **Zero Analytics, Zero Ads, Zero Telemetry**.
  * **Zero Internet Permission** (`android.permission.INTERNET` is not requested).

---

## 📸 Overview

| Dashboard & Controls | Radial Action Menu | Hardware Sliders |
| :---: | :---: | :---: |
| High-contrast brutalist dashboard with service activation & sliders | Central circular menu for instant navigation & actions | Direct on-screen volume & brightness adjustments |

---

## 🔒 Permissions & Security

TouchCore is designed around the principle of minimal privilege:

| Permission | Purpose |
| :--- | :--- |
| `SYSTEM_ALERT_WINDOW` | Required to draw the floating touch core over other applications. |
| `BIND_ACCESSIBILITY_SERVICE` | Required to execute system navigation actions (Home, Back, Recents, Lock Screen, Screenshot). Service is unexported and runs strictly locally. |
| `WRITE_SETTINGS` | *(Optional)* Required only for adjusting screen brightness directly from the slider. |
| `CAMERA` | *(Optional)* Required only to toggle the camera flash as a flashlight/torch. |
| `FOREGROUND_SERVICE` | Keeps the overlay service alive and responsive in the background. |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | Enables screen recording via Android's native MediaProjection API. |
| `POST_NOTIFICATIONS` | Displays the persistent foreground service notification in the drawer. |

> **Note on Android 13+ Restricted Settings**:
> When sideloading on Android 13 or later, Android may initially restrict accessibility permissions for sideloaded APKs. To enable:
> Go to **App Info** → Tap the **Three Dots (⋮)** in the top right → Select **Allow restricted settings** → Return to Settings and enable the Accessibility Service.

---

## 🏗️ Architecture

TouchCore follows clean architecture and separation of concerns:

```text
app/src/main/java/com/example/assistivetouch/
├── action/              # Decoupled action dispatcher & executor
│   ├── ActionDispatcher.kt
│   └── ActionExecutor.kt
├── model/               # Immutable models & domain state
│   ├── AssistiveAction.kt
│   ├── AssistiveItem.kt
│   └── MenuPage.kt
├── repository/          # Dynamic menu composition & page graphs
│   └── MenuRepository.kt
├── prefs/               # Local SharedPreferences & favorites storage
│   └── FavoritesManager.kt
├── service/             # Window management & system hooks
│   ├── FloatingButtonService.kt
│   ├── MyAccessibilityService.kt
│   └── ScreenRecordingService.kt
└── ui/                  # Activities, custom views, and canvas widgets
    ├── MainActivity.kt
    ├── SettingsActivity.kt
    ├── FavoritesActivity.kt
    ├── WelcomeActivity.kt
    └── view/
        ├── AssistiveRadialMenuView.kt
        ├── CapsuleSliderView.kt
        └── PillSegmentedGroup.kt
```

---

## 🚀 Building from Source

### Prerequisites
* **JDK 17**
* **Android SDK** with Platform Tools for API 35 (Android 15)
* **Gradle Wrapper** (included)

### Build Commands

```bash
# Clone the repository
git clone https://github.com/Xdashio/Touch-Core.git
cd Touch-Core

# Build Debug APK
./gradlew assembleDebug

# Build Optimized Release APK (2.5 MB)
./gradlew assembleRelease

# Run Unit Tests
./gradlew test
```

### Installation via ADB
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## 🤝 Contributing

Contributions are welcome! Please check out [CONTRIBUTING.md](CONTRIBUTING.md) for development guidelines, coding conventions, and pull request procedures.

---

## 📄 License

TouchCore is open-source software licensed under the [Apache License, Version 2.0](LICENSE).
