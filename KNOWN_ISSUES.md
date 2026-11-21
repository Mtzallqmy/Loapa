# COMBAT FRONTS (جبهات القتال) — KNOWN ISSUES & ENVIRONMENT NOTES (v1.1.0)

1. **Native Android Kotlin/OpenGL ES 3.0 Engine (No Godot/Blender Binaries in Container):**
   - This cloud build environment provides Android SDK/Gradle/Kotlin tools only (no Godot 4 editor/export templates or Blender CLI). All 3D graphics, GLSL ES 3.0 shaders, physics, AI 2.0, interactive worlds, procedural PCM audio, and Compose UI are built natively in Kotlin.
2. **Physical Device Telemetry Not Executed Locally:**
   - The execution environment has no physical Android device or ADB access. Runtime UI launch, adaptive/mipmap launcher icon resolution, save migration, and all 12 campaign missions are verified via Gradle compilation (`compile_applet`), JUnit 4, and Robolectric 4 (API 36).
   - Real-device sustained thermal/FPS profiling on target Snapdragon/Dimensity ARM64 hardware should be measured by exporting the APK and profiling with Android Studio GPU Inspector.
3. **Procedural Geometry vs. High-Poly Rigged Meshes:**
   - Characters, weapons, and city architecture are rendered using articulated procedural OpenGL ES 3.0 meshes and procedural shaders to guarantee zero missing textures and minimal APK footprint. External `.glb` asset pipelines can be plugged into `MeshFactory.kt` in future releases.
