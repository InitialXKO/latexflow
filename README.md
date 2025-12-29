# LatexFlow

[English](README.md) | [中文](README_CN.md)

LatexFlow is an Android application designed for researchers and geeks to effortlessly bridge the gap between handwritten mathematical formulas and LaTeX code. Using handwriting recognition and Bluetooth HID simulation, it allows you to "type" formulas directly into your computer without any client-side software.

## Features

- **Infinite Canvas:** Low-latency handwriting canvas with pressure sensitivity and stroke optimization.
- **Real-time Recognition:** Integrates high-performance OCR (like MyScript) to convert handwriting into LaTeX.
- **Live Preview:** Instant KaTeX rendering to verify your formulas.
- **Bluetooth HID "Magic":** Your phone acts as a Bluetooth keyboard. No computer software required.
- **One-Tap Injection:** Send formulas directly to your active cursor on Windows, macOS, Linux, or iPadOS.
- **GeoGebra Math Keyboard (NEW):** Integrated GeoGebra's official `keyboard-base` logic for professional symbol and template input.
- **History & Favorites:** Save frequent formulas and access recently recognized ones.

## Getting Started

1. **Pairing:** Open the app and connect "LatexFlow Keyboard" via your computer's Bluetooth settings.
2. **Write:** Handwrite your formula or use the new Math Keyboard.
3. **Verify:** Check the live preview at the bottom.
4. **Inject:** Click the cursor where you want the code, and tap "Inject" on the app.

## CI/CD Scaffolding

This project includes a robust CI/CD setup:

- **Build:** Automatic debug APK generation on push.
- **Release:** Automatic signed release APKs on tag push (`v*.*.*`).
- **Keystore Gen:** Helper workflow for managing signing keys.

For more details on CI/CD, see our [Workflow Documentation](.github/workflows/README.md).
