# COMBAT FRONTS (جبهات القتال) — SYSTEM AUDIT (v1.1.0)

**Project:** COMBAT FRONTS — جبهات القتال  
**Version Audited:** v1.0.0 → v1.1.0 (`versionCode = 2`, `versionName = "1.1.0"`)  
**Platform:** Native Android (Kotlin + OpenGL ES 3.0 + Jetpack Compose, ARM64-compatible, Min SDK 28)  
**Developer Credit:** برمجة وتطوير معتز العلقمي – تعز – اليمن – 2026م

## 1. Critical Runtime Defects Identified & Resolved
1. **Zero-Byte Corrupted Raster Drawables (Launch Crash Root Cause):**
   - Five `.jpg` files (`img_launcher_crest.jpg`, `img_hero_main_menu.jpg`, `img_city_river.jpg`, `img_city_mirage.jpg`, `img_city_harbor.jpg`) in `app/src/main/res/drawable/` were 0 bytes on disk, causing immediate image decoding failure on `SplashScreen` and breaking `ic_launcher_foreground.xml`.
   - **Resolution:** Deleted all 0-byte JPEGs; implemented crisp vector/Compose Canvas artwork (`TacticalArtwork.kt` with `TacticalCrestEmblem`, `HeroMainMenuBackdrop`, `CityArtworkCanvas` for all 4 cities) and replaced the adaptive icon with a clean Android Vector Drawable (`ic_launcher_foreground.xml`) + diagonal obsidian-slate gradient (`ic_launcher_background.xml`) + regenerated non-zero PNG32 legacy mipmaps (`mdpi`–`xxxhdpi`).
2. **Package ID Mismatch on Streaming Emulator:**
   - `applicationId` had diverged from `"com.example"`, risking launch mismatch on the container preview runner.
   - **Resolution:** Restored `applicationId = "com.example"` while keeping `namespace = "com.example"` and preserving all signing configurations.
3. **Adaptive Icon Monochrome Tag Defect:**
   - `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` referenced a raster layer-list in `<monochrome>`, causing solid-square themed icons on Android 13+.
   - **Resolution:** Removed `<monochrome>` and switched `<foreground>` to a clean safe-zone vector drawable.
4. **OpenGL ES 3.0 Driver Resilience:**
   - Added `glDisableVertexAttribArray(1)` before drawing 2-float sky quads and wrapped `setEGLConfigChooser(8, 8, 8, 8, 16, 0)` in `runCatching` across `GameplayScreen`, `ArmoryScreen`, and `CustomizationScreen`.
   - Set `<uses-feature android:glEsVersion="0x00030000" android:required="false" />` in `AndroidManifest.xml`.

## 2. Architecture Gaps Audited Before v1.1.0 Integration
- No central event bus connecting combat, world interaction, AI alerts, objectives, and UI.
- Damage rules were split between `PlayerController`, `EnemyUnit`, and `CombatSimulation` without respawn protection or fall damage.
- Enemy AI lacked progressive perception awareness, cover-point reservation (allowing cover stacking), and staggered firing tokens.
- World environments had static cover and resupply stations but lacked interactive doors, locked switch-controlled gates, destructible explosive barrels, and objective terminals.
- Camera lacked Left/Right shoulder switching and smooth cover-peek elevation.
- Save schema v2 needed backward-compatible v3 migration for new camera preferences.

## 3. Platform Architecture Disclosure
The build container provides a native Android Kotlin/Gradle toolchain without Godot 4 or Blender binaries. All 3D gameplay, shaders, physics, AI 2.0, interactive worlds, audio synthesis, and UI are implemented natively in Kotlin + OpenGL ES 3.0 + Jetpack Compose with zero network permissions (`100% offline`).
