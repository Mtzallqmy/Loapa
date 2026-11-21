# Android Build & Packaging Report — COMBAT FRONTS (جبهات القتال) v2.1.0

**Build Date:** 2026-11-20  
**Build Tool:** Gradle (`gradle :app:assembleDebug` + `gradle :app:testDebugUnitTest --rerun-tasks`)  
**Build Result:** **BUILD SUCCESSFUL** (Zero Kotlin compiler errors, zero compiler warnings)

---

## 1. Application Metadata & Configuration

| Property | Verified Value |
| :--- | :--- |
| **App Display Name (`app_name`)** | `جبهات القتال` |
| **Platform Metadata Name (`metadata.json`)** | `جبهات القتال` (`MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API` preserved) |
| **Application ID** | `com.example` |
| **Version Name** | `2.1.0` |
| **Version Code** | `3` |
| **Min SDK / Target SDK** | `24` (Android 7.0+) / `36` (covers Android 9.0 / API 28+ target) |
| **Screen Orientation** | `sensorLandscape` |
| **RTL Support** | `android:supportsRtl="true"` |
| **Declared Permissions** | `android.permission.VIBRATE` ONLY (**Zero `INTERNET` permission — 100% offline**) |
| **Graphics API** | OpenGL ES 2.0/3.0 universal (`glEsVersion="0x00020000"`, `required="false"` graceful fallback) |

---

## 2. Verified APK Deliverable

- **Output Path:** `/COMBAT-FRONTS-v2.1.0-ARM64.apk` (copied from `app/build/outputs/apk/debug/app-debug.apk`)
- **Exact File Size:** `17,166,083` bytes (~16.37 MiB)
- **SHA-256 Checksum:** `aeb6b6da6dfd4595987bd8dbc06fe07b693ec3558892da98c4ef441847ccf0d2`
- **ABI / Native Libraries Verified (`unzip -l`):**
  - `lib/arm64-v8a/libandroidx.graphics.path.so` (ARM64-v8a verified)
  - Universal DEX bytecode (`classes.dex`–`classes9.dex`) + multi-ABI compatibility (`armeabi-v7a`, `x86`, `x86_64`)
  - `AndroidManifest.xml`, compiled resources, vector adaptive launcher icon + PNG32 mipmaps (`mdpi`–`xxxhdpi`).
- **Signing Transparency:** Signed with the workspace debug keystore (`debug.keystore` via `signingConfigs.debugConfig`). Because private Google Play upload keystore credentials are not present in the cloud container, this APK is ready for direct sideloading/testing on ARM64 Android devices. For Google Play Store publication, build a signed release bundle (`gradle :app:bundleRelease`) locally using your private upload keystore.

---

## 3. Clean Source Archive Deliverable

- **Output Path:** `/COMBAT-FRONTS-v2.1.0-SOURCE.zip`
- **Included:** Complete Kotlin/Compose/OpenGL ES 3.0 source code, Android resources, Gradle build configuration, unit/Robolectric tests, and all 14 Prompt 05 engineering reports.
- **Excluded:** `build/`, `app/build/`, `.gradle/`, `.git/`, `.aistudio/`, `.build-outputs/`, `*.apk`, `*.zip`.
- **Integrity Verification:** Verified via `unzip -t COMBAT-FRONTS-v2.1.0-SOURCE.zip` with zero archive errors.

---

## 4. Verification Summary

| Verification Gate | Command / Method | Result |
| :--- | :--- | :---: |
| **Debug APK Assembly** | `gradle :app:assembleDebug` | **PASS** |
| **Unit & Robolectric Tests** | `gradle :app:testDebugUnitTest --rerun-tasks` | **PASS (29/29)** |
| **Offline Permission Audit** | `AndroidManifest.xml` check | **PASS (0 network permissions)** |
| **Physical ARM64 Device Install / ADB** | N/A in cloud container | **PENDING_PHYSICAL_DEVICE** |
