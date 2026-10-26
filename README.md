# JAMS PDF Reader v1.1 — Print & Auto Scan

Android Java app, no ads, no analytics, no login, no INTERNET permission. Min Android 8.0 (API 26).

## Features
- Open PDF with Android Storage Access Framework; page navigation and zoom.
- Share original PDF using Android share sheet.
- Print original PDF through Android Print Framework (printer/service support required).
- Camera document scanning using full-resolution image capture, **lightweight heuristic auto-crop**, multi-page PDF output, and save via system picker.

## Important limitations
- Auto Scan estimates rectangular document boundaries from image edge contrast; **not** a perspective-correcting, ML-based document scanner. Busy backgrounds, tilted documents and shadows may yield inaccurate crops. Check output before saving.
- Searchable text, OCR, manual crop adjustment, PDF merging and continuous scrolling are **not implemented**.
- Printing passes original PDF to the system print spooler. Some print services may not support this passthrough or requested page ranges; test with your printer.
- Camera app must support `ACTION_IMAGE_CAPTURE` and `EXTRA_OUTPUT`.
- No APK is included. Build is configured but has not been run in this environment.

## GitHub build
1. Create a new GitHub repository and upload **the contents** of this folder, including hidden `.github`.
2. Go to **Actions > Build JAMS PDF Reader APK > Run workflow**, or push to main.
3. After successful run, open the run > **Artifacts** > download `JAMS-PDF-Reader-v1.1-debug-APK`.
4. Extract APK from artifact zip and install on Android (allow installation from trusted source).

Debug APK is for testing, not signed production release. Gradle 8.9, Android Gradle Plugin 8.7.3, Java 17, SDK 35. Dependencies are downloaded during cloud build; the app itself requires no network.
