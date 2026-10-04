# Contributing to TouchCore

Thank you for your interest in contributing to **TouchCore**! TouchCore is an open-source, ultra-lightweight, brutalist floating assistive touch and navigation overlay for Android.

We welcome contributions of all kinds: bug fixes, performance optimizations, accessibility improvements, and documentation enhancements.

---

## ◈ Development Setup

### Prerequisites
* **JDK 17** (Amazon Corretto, OpenJDK, or Azul Zulu)
* **Android SDK**: Compile SDK `35`, Min SDK `29` (Android 10+)
* **Android Studio** (Koala / Ladybug or newer recommended)
* Optional: A physical device or Android emulator with USB debugging enabled.

### Clone & Build
```bash
git clone https://github.com/Xdashio/Touch-Core.git
cd Touch-Core

# Build Debug APK
./gradlew assembleDebug

# Build Optimized Release APK (R8 minified)
./gradlew assembleRelease

# Run the test suite
./gradlew test
```

### Deploying to a Connected Device
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## ◈ Architecture & Design Principles

When proposing or making changes, please follow these core principles:

1. **Ultra-Lightweight Footprint (< 3 MB)**:
   * Keep dependencies minimal. Avoid pulling in heavy third-party libraries for trivial tasks.
   * Do not add raw raster bitmaps (`.png`/`.jpg`); use clean vector drawables (`.xml`) with Lucide or Android vector paths.

2. **Aesthetic Consistency**:
   * **Brutalist 0dp Radius**: Cards, buttons, sliders, dialogs, and panels feature sharp, clean `0dp` border radii with generous breathing room.
   * **Concentric Floating Bubble**: Only the floating button core, its live previews, and the notification/launcher icons feature the authentic concentric circular design.
   * **Monochrome & High Contrast**: Built primarily around dark obsidian `#101216`, deep surface `#181A20`, pure white `#FFFFFF`, and functional accent states.

3. **Privacy & Permission Discipline**:
   * TouchCore requires **zero internet access** and contains **no analytics, ads, or trackers**.
   * Do not add broad permissions such as `QUERY_ALL_PACKAGES`. App discovery must use targeted intent queries.

4. **Robust Window & Lifecycle Handling**:
   * All window overlay operations in `FloatingButtonService` must handle orientation changes, screen bounds, and safe removal gracefully to ensure zero crashes across OEM skins (OneUI, MIUI, Pixel, ColorOS, etc.).

---

## ◈ Testing

Before submitting a Pull Request, verify that all unit tests pass cleanly:

```bash
./gradlew test
```

If adding new menu actions or preferences, please add corresponding unit tests in `app/src/test/java/com/example/assistivetouch/`.

---

## ◈ Call for Contributors: Universal Screen Recording

We are actively seeking contributions to implement a **universal, device-agnostic screen recording engine**:
* **Goal**: Seamless screen capture across all Android devices (Samsung OneUI, Xiaomi MIUI, Google Pixel, OnePlus OxygenOS, etc.) without OEM-specific crashes or restrictions.
* **Key Areas**:
  * `MediaProjection` permissions flow and foreground service orchestration.
  * Video and internal audio encoding (`MediaRecorder` or `MediaCodec`).
  * Dynamic floating recording indicator and one-tap stop controls from the overlay.
* Feel free to open an issue or submit a pull request if you'd like to take the lead or collaborate on this feature!

---

## ◈ Pull Request Process

1. Fork the repository and create your feature branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
2. Commit your changes with clear, descriptive commit messages.
3. Test your changes on a physical device or emulator (`./gradlew test`).
4. Push to your fork and submit a Pull Request against the `main` branch.
5. Provide a clear description and attach screenshots or screen recordings where applicable.

---

## ◈ License

By contributing to TouchCore, you agree that your contributions will be licensed under the [Apache License, Version 2.0](LICENSE).
