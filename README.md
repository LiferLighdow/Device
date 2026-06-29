<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="120" height="120" alt="Device Icon">
</p>

# Device

**Device** is a professional, ultra-lightweight Android system monitor and hardware diagnostic tool. Powered by **Kotlin** and built with a "Lightweight & High-Performance" philosophy, it provides deep insights into your device with virtually no overhead.

[![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg)](https://android-arsenal.com/api?level=23)
[![Language](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Size](https://img.shields.io/badge/Size-Extremely%20Small-orange.svg)](#)

## 🚀 Key Features

### 🛠️ Hardware & System Diagnostic
*   **Device Identity**: Detailed manufacturer, model, board, and bootloader information.
*   **System Insight**: Android version, API level, kernel details, and enhanced **Triple-Check Root Detection**.
*   **Processor Analytics**: Real-time (1s update) per-core CPU frequencies, architecture, and instruction sets.
*   **GPU & AI**: OpenGL ES and Vulkan support details, along with NPU/TPU identification for modern chipsets.

### 📊 Performance Monitoring
*   **Memory (RAM)**: Real-time usage, Zram status, RAM type (LPDDR), and clock frequency.
*   **Storage Management**: Partition-level analysis (System, Data, SD Card) including hardware IDs and filesystem types.
*   **Battery Intelligence**: Health tracking, voltage, temperature, capacity (mAh), and real-time charging current (mA).
*   **Thermal Monitor**: Live system temperature tracking with non-blocking updates.

### 🔍 Advanced Specifications
*   **Display**: Resolution, density (DPI), refresh rate, HDR support, and wide color gamut diagnostics.
*   **Camera Suite**: Full lens specifications including Megapixels, focal length, aperture, and physical lens counts.
*   **Wireless & Connectivity**: Status for Wi-Fi (2.4/5/6/60GHz), Bluetooth LE, NFC, GPS, and IR Emitter.
*   **Cellular Details**: Carrier information, SIM state, network type (4G/5G), and MCC/MNC data.
*   **Sensors**: Comprehensive list of all available hardware sensors.

### 🧪 Utility Tools
*   **Screen Test**: Integrated tool for dead pixels and color uniformity testing.
*   **App Manager**: Smooth app listing with **Async Icon Loading**, sorting, and APK extraction.
*   **Adaptive UI**: Modern interface with full support for System-aware Dark and Light modes.

## 💎 Why "Device"?

Instead of heavy frameworks, **Device** focuses on a modern, reactive, yet minimal stack:

*   **100% Kotlin**: Clean, safe, and modern codebase.
*   **Reactive Architecture**: Leverages **Kotlin Coroutines**, **StateFlow**, and **ViewModel** for non-blocking UI and efficient data updates.
*   **Minimalist Dependencies**: Only uses essential, lightweight libraries (Coil for async images, Core KTX) to keep the APK size near-zero.
*   **Performance Focused**: Real-time hardware polling (like CPU frequency) is handled locally to prevent flickering and minimize CPU wakeups.

## 🛠️ Development

### Prerequisites
*   Android Studio Ladybug (2024.2.1) or newer.
*   Android SDK 37+.
*   Minimum Android 6.0 (API 23).

### Build & Run
1. Clone the repository.
2. Open the project in Android Studio.
3. Sync Gradle (fast sync due to minimal dependencies).
4. Build and deploy to your device.

## 📂 Project Structure

*   `MainActivity`: Navigation hub using ViewBinding and modern AppCompat.
*   `HardwareViewModel`: Reactive data provider for real-time hardware metrics.
*   `HardwareProvider`: Centralized logic for system and hardware data retrieval.
*   `TemperatureActivity`: Background thermal monitoring with Coroutines.
*   `ThemeManager`: Global theme controller and UI consistency logic.

## 📜 License
**MIT License**. Keep it light, keep it fast. Stay in control.

---

**Developed with ❤️ by LiferLighdow**
