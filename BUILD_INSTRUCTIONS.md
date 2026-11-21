# Build & Test Instructions — COMBAT FRONTS v1.0.0

## Prerequisites
- JDK 17+ / Android SDK (Compile SDK 36, Min SDK 24 — compatible with Android 9.0 / API 28+ ARM64 devices)
- Gradle (Android Gradle Plugin 9.1.1, Kotlin 2.4.21)

## Run Automated Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```

## Build Debug & Release Android APKs
```bash
# Debug APK
gradle :app:assembleDebug

# Release APK (requires KEYSTORE_PATH, STORE_PASSWORD, KEY_PASSWORD env vars for release signing)
gradle :app:assembleRelease
```

## Output Artifact Locations
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`

## Device Testing Checklist (Real Android Hardware)
- Verify sustained frame pacing at Low (30 FPS), Medium (45 FPS), and High (60 FPS) quality presets on target ARM64 devices.
- Verify multi-finger simultaneous touch (virtual joystick + look drag + fire/aim buttons) on physical touch digitizer.
- Verify offline Airplane Mode startup and save persistence across process death.
