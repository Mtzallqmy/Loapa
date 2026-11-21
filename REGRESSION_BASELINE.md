# COMBAT FRONTS (جبهات القتال) — REGRESSION BASELINE (v1.1.0)

## Preserved v1.0.0 Features Verified Against Regression
- **Package & Identity:** `applicationId = "com.example"`, Arabic title `"جبهات القتال"`, English title `"Combat Fronts"`, developer credit `"برمجة وتطوير معتز العلقمي – تعز – اليمن – 2026م"`.
- **100% Offline Enforcement:** Zero `INTERNET` permission in `AndroidManifest.xml`, verified by automated Robolectric manifest inspection.
- **4 Original Cities:** `مدينة النهر` (River), `مدينة السراب` (Mirage), `مدينة المرسى` (Harbor), `مدينة الجبل` (Mountain).
- **12 Playable Campaign Missions:** All 12 missions load, progress through every objective phase, save/restore checkpoints, and complete cleanly across Easy, Medium, and Hard difficulties.
- **8 Customizable Weapons + 6 Attachment Slots:** All 8 weapon platforms compute effective stats and render in the 360° 3D Armory.
- **360° Character Studio:** All 6 character gear slots + 3 tactical presets preserved.
- **Arabic RTL / English LTR Localization:** All 8 main menu buttons and full HUD verified.
- **Atomic Offline Save Recovery:** Corrupt primary file automatically self-heals from `.bak`, plus seamless migration from legacy v1 and v2 save payloads to schema v3.
