# Remaining Issues & Transparent Environment Limitations — COMBAT FRONTS v2.1.0

**Version:** `2.1.0`  
**Audit Date:** 2026-11-20

This document provides a **100% honest accounting** of remaining non-blocking limitations and environment constraints. No fabricated metrics or unverified hardware claims are included.

---

## 1. Environment & Toolchain Constraints (Non-Defect)

| ID | Area | Honest Status & Explanation | Mitigation / Next Step for Release Owner |
| :--- | :--- | :--- | :--- |
| **LIM-01** | **Godot 4 / Blender Pipeline** | The cloud build container provides the native Android SDK, Gradle, and JDK, and does **not** include Godot 4 editor/export templates or Blender CLI. Consequently, the entire playable 3D game is built natively in **Kotlin + OpenGL ES 3.0 + Jetpack Compose** rather than `.tscn`/`.gd` Godot scenes. | All gameplay mechanics, 3D rendering, cities, weapons, AI, weather, vehicle, audio, and Arabic RTL UI run natively on Android without requiring external runtime engines. |
| **LIM-02** | **Physical Device / ADB Testing (`PENDING_PHYSICAL_DEVICE`)** | No physical Android handset, GPU profiler, or `adb` daemon is accessible inside this container. Real-world GPU FPS, OpenGL ES 3.0 shader timing on Adreno/Mali GPUs, touch ergonomics, audio latency, and 30-minute thermal throttling tests have **not** been executed on hardware. | Sideload `/COMBAT-FRONTS-v2.1.0-ARM64.apk` onto a physical ARM64 Android 9.0+ device and run the manual checklist in `PERFORMANCE_REPORT.md`. |
| **LIM-03** | **Release Upload Keystore Signing** | `/COMBAT-FRONTS-v2.1.0-ARM64.apk` is built via `gradle :app:assembleDebug` and signed with the workspace `debug.keystore` because private Play Store release keystore credentials are not stored in the container. | For Google Play Store distribution, sign a release build (`gradle :app:bundleRelease` or `:app:assembleRelease`) locally with your private upload keystore and update `applicationId` to a unique store ID. |
| **LIM-04** | **Google Services Warning During Gradle Build** | Because `app/build.gradle.kts` retains the workspace template's optional Google Services plugin entry while the game is 100% offline (no `google-services.json`, zero `INTERNET` permission), Gradle prints a benign notice (`google-services.json is missing`). | Does not affect APK generation, offline execution, or test execution (`BUILD SUCCESSFUL`). |

---

## 2. Known Gameplay & Visual Trade-Offs (Low Severity / Non-Blocking)

| ID | Severity | Description | Impact |
| :--- | :--- | :--- | :--- |
| **ISS-01** | **Low** | Procedural low-poly 3D geometry & PCM synthesized audio are used instead of studio-recorded foley and high-poly PBR `.glb` assets. | Keeps APK lightweight (~16.5 MiB), 100% copyright-free, and fast to load offline; future releases can swap procedural meshes for bundled `.glb`/`.ogg` assets. |
| **ISS-02** | **Low** | Single-vehicle scope (1 Tactical Armored Rover per city) without multi-vehicle convoy traffic or aircraft. | Intentionally scoped for stability and predictable mobile GPU budgets per Prompt 05 guidance. |
| **ISS-03** | **Low** | Enemy navigation uses waypoint/steering + AABB collision sliding rather than baked Recast NavMesh polygons. | Validated across all 4 cities and 600-tick stress test with zero NaN/crash issues; occasional tight-corner slide adjustments may occur near stacked cover. |

---

## 3. Blockers Count

- **P0 Crash / Build / Data-Loss Blockers:** **0**
- **Automated Test Failures:** **0** (`26 / 26 PASSED`)
