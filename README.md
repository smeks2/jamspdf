# JAMS PDF Reader v1.5.1 PRO — BuildConfig Hotfix

This patch fixes the GitHub Actions Java compilation error `cannot find symbol: BuildConfig` by enabling `buildFeatures { buildConfig true }` in `app/build.gradle`. `versionName` is now `1.5.1` and `versionCode` is `7`. The About screen version note and artifact name are updated. All other application source files are carried over from v1.5. This is source code, not a verified APK.

# JAMS PDF Reader v1.5 — PRO Teaching Edition (source preview)

Coded by J-AIms. Android Java, offline, no ads.

## Included
- PDF reader with swipe flip, scroll, pinch zoom and page navigation
- Android system print/share; camera capture + post-capture AutoCrop (not live edge tracking)
- Pen/highlight with adjustable stroke size, undo/redo, PDF export (flattened)
- Signature drawing pad, internal PNG library, signature placement presets and PDF export (flattened image signature)
- Persisted SAF document URI and last page for reopening when Android grants persistent access
- Back navigation confirmation, app exit confirmation, Android system insets, app icon
- GitHub Actions debug APK workflow

## Known limitations / not implemented
- This is a source preview, **not build-tested in this environment**. GitHub Actions must compile and device testing is required.
- PRO licensing, biometrics, per-signature naming, import PNG, drag-to-resize, live scanner edge tracking, and multi-page automatic signature application are **not implemented**.
- Annotations/signatures are held in memory until exported. The signature PNG library is saved locally, but page placements and ink are not restored after process death.
- Scanning uses Android camera and crops after capture; inspect the crop before saving.
- Export produces flattened rasterized PDF pages; text selection and existing PDF interactive elements may not be preserved.

## Build
Upload all project files to your GitHub repository root. Actions > Build JAMS PDF Reader APK > Run workflow, then download the artifact.

Do not publish as a production release before build verification, PDF compatibility testing, and privacy/security review.
