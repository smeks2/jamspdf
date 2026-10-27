# JAMS PDF Reader v1.4 — Teaching Edition

Android Java source project based on v1.3. Includes PDF page swipe, pinch zoom, rotate via Android orientation, automatic reader menu hide, print, share, camera scan with post-capture crop, and freehand teaching annotations.

## Annotation

Open a PDF, choose **TULIS** to enter writing mode. In writing mode, one finger draws on the PDF page, with red/blue/green pen and a translucent yellow freehand highlighter. **UNDO** and **REDO** affect the currently selected page. Press **TULIS** again to return to reader gestures. Use **SIMPAN NOTA** to export a new flattened PDF with drawings.

Annotations are held in memory until exported; they are not embedded into the source PDF and are not persisted across app restarts. Highlighter is freehand, **not** selectable-text highlighting. For the best results, use page flip mode while annotating. Scanning still uses post-capture crop, **not live edge detection**.

## GitHub APK

Upload extracted files to the repository root. GitHub Actions workflow `.github/workflows/android-apk.yml` builds `:app:assembleDebug` and uploads the APK as an artifact. This source was not compiled or device-tested in this environment.
