# JAMS PDF Reader v1.2 (Android)

Source upgrade from v1.1. Android Java, no advertisements or account.

## Implemented
- Open PDF through Android document picker and VIEW intent.
- Vertical multi-page rendering and horizontal swipe in Flip mode.
- Two-finger pinch zoom (up to 5x), plus zoom buttons.
- Android system-bar safe insets; labeled buttons.
- Print via Android PrintManager, share original PDF URI.
- Camera capture, post-capture heuristic crop, multipage scan-to-PDF.
- GitHub Actions debug APK build.

## Known limitations
- Live edge tracking, automatic shutter, perspective correction, and reliable auto-orientation are **not yet implemented**. Scanner remains post-capture heuristic crop.
- PDF rendering currently eagerly renders pages; large PDFs may use excessive RAM. Lazy page rendering is needed for production use.
- Pinch zoom redraws full document; may be slow on large files. Rendering quality and gesture conflicts need device testing.
- No OCR or search, no thumbnails or last-page memory yet.
- APK build has not been verified in this environment. Upload to GitHub and inspect Actions output.

## Build
Upload all extracted files to the root of the GitHub repository, including hidden `.github` folder. Workflow: `.github/workflows/android-apk.yml`. In Actions download `JAMS-PDF-Reader-v1.2-debug-APK` artifact after successful build.
