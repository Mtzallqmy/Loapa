# ANDROID BUILD REPORT — COMBAT FRONTS (جبهات القتال) v2.1.0

**برمجة وتطوير معتز العلقمي – تعز – اليمن – 2026م**

## 1. Build Configuration Summary
- **Application Name:** `جبهات القتال` (`Combat Fronts` in English locale)
- **Package / Application ID:** `com.example`
- **Version:** `2.1.0` (`versionCode = 3`)
- **Min SDK / Target SDK / Compile SDK:** `24` (supports Android 9.0 API 28+) / `36` / `36.1`
- **Rendering Backend:** Universal OpenGL ES 2.0/3.0 GLSL + Jetpack Compose Material 3
- **Connectivity:** **100% Offline** (zero `INTERNET` permission in `AndroidManifest.xml`; only `VIBRATE` declared)
- **Build Type & Signing:** Debug-signed (`debugConfig` / `debug.keystore`) — *Release keystore (`my-upload-key.jks`) is externally supplied via environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) for production store signing.*

## 2. Verified Build & Packaging Evidence
- **Gradle Build Command:** `gradle :app:assembleDebug` — **BUILD SUCCESSFUL**
- **Unit & Robolectric Tests:** `gradle :app:testDebugUnitTest --rerun-tasks` — **30 / 30 PASSED**
- **Archive Integrity:** `unzip -tq COMBAT-FRONTS-v2.1.0-SOURCE.zip` — **No errors detected**

## 3. Delivered Artifacts
| Artifact | Size | SHA-256 Checksum | Status |
|---|---:|---|---|
| `COMBAT-FRONTS-v2.1.0-ARM64.apk` | 17 MB | `c9f7587bbee0487324d41ba595f4c4a39d6054f4ca3d8a6e5efe216bf614f6eb` | **VERIFIED BUILT** (Debug-signed installable APK) |
| `COMBAT-FRONTS-v2.1.0-SOURCE.zip` | 385 KB | `01b6711ff5f32ce223456bd051a2754a37bab55a9bb5920e28d24736f1d9cca1` | **VERIFIED ARCHIVE** (`unzip -tq` PASS) |

## 4. Acceptance Criteria Status
- **Project Compilation & APK Packaging:** VERIFIED (`BUILD SUCCESSFUL`)
- **Automated Core Gameplay, AI, Equipment, Save/Load & Audio Tests:** VERIFIED (`30/30 PASS`)
- **Offline Permission Audit:** VERIFIED (zero `INTERNET` permission)
- **Physical ARM64 Device Installation & FPS Profiling:** **NOT VERIFIED** (no physical device / ADB available in cloud build container)
- **Production Release Keystore Signing:** **NOT VERIFIED** (debug-signed APK produced; release signing requires user's private upload keystore credentials)
