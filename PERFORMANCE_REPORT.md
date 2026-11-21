# Performance & Optimization Report — COMBAT FRONTS (جبهات القتال) v2.1.0

**Version:** `2.1.0`  
**Audit Date:** 2026-11-20  
**Hardware Profiling Status:** **JVM/Engine Verified; Real Physical Device GPU/Thermal Profiling `PENDING_PHYSICAL_DEVICE`**

---

## 1. Runtime Performance Architecture (`PerformanceManager.kt`)

v2.1.0 introduces a dedicated `PerformanceManager` wired directly into `CombatSimulation` and `CombatGLRenderer` to keep frame pacing stable across Low, Medium, and High quality tiers:

- **Frame-Time EMA Tracking:** Computes exponential moving average (`smoothedFrameMs`) from every frame's `dt` (clamped to `[0.004s, 0.05s]`).
- **Adaptive LOD Scaling (`effectiveDetailLevel`):** Automatically steps down visual detail level (`lodOffset`) if `smoothedFrameMs` sustains above target budget (`> 34ms`) and recovers when headroom returns (`< 24ms`).
- **Strict Memory & Draw Pool Caps:**
  - `maxTracers`: bounded per quality tier (24 / 40 / 64) — prevents unbounded bullet tracer allocations during intense firefights or MG-48 bursts.
  - `maxImpactParticles`: bounded per quality tier (36 / 72 / 120).
  - `maxWeatherParticles`: dynamically scaled by `PerformanceManager` (0 in Low if throttled, up to 140 in High Rain/Sandstorm) using a fixed pre-allocated `ArrayList` in `WeatherSystem` (zero per-frame heap churn).

---

## 2. OpenGL ES 3.0 & Memory Optimizations (`CombatGLRenderer.kt`)

- **Shared Static Mesh VBOs:** Procedural unit cube, cylinder, cone, and pyramid vertex buffers are allocated once in `onSurfaceCreated` and reused for all city architecture, characters, weapons, Command Core, and Tactical Armored Rover sub-parts.
- **Zero Per-Frame Matrix Allocations:** Reusable `FloatArray(16)` MVP/Model/View/Projection scratch matrices prevent GC spikes during 60 FPS rendering.
- **Clean Attribute State:** Every draw call cleanly disables vertex attribute arrays after drawing to avoid driver state leaks.
- **Lightweight APK Footprint:** Procedural 3D meshes, procedural PCM audio, and Compose Canvas UI artwork keep the complete APK at **17,265,999 bytes (~16.47 MiB)** with zero bloated raster packs.

---

## 3. Automated Stress Benchmark Results (JVM / Robolectric)

Executed in `EndToEndAndStressTest.crossSystemStressTest600TicksZeroExceptionsZeroNaNAndBoundedPools`:
- **Scenario:** 600 consecutive simulation ticks (`30.0` simulated seconds) under `WeatherCondition.SANDSTORM` with active enemies, vehicle boarding/driving, turret firing, player firing/reloading, and weather particle simulation.
- **Observed Result:**
  - Exceptions thrown: **0**
  - NaN / Infinite vectors: **0**
  - Active tracer & particle counts: **Strictly within `PerformanceManager` caps on every tick**
  - Total JVM test execution time for 600 ticks: **82 ms** (`0.137 ms / simulated tick` on host JVM).

---

## 4. Physical Device Verification Checklist (`PENDING_PHYSICAL_DEVICE`)

Because no physical ARM64 Android handset or ADB connection exists in this cloud container, **no real-device FPS or thermal numbers are fabricated**. To complete physical hardware sign-off:
1. Install `/COMBAT-FRONTS-v2.1.0-ARM64.apk` on a mid-range ARM64 Android 9.0+ device.
2. Verify sustained ≥ 30 FPS on Medium/High across River, Mirage (Sandstorm), Harbor (Fog), and Mountain (Night) cities.
3. Run a 20-minute Survival soak session and confirm stable memory and no thermal crash.
