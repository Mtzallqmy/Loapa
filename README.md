# جبهات القتال — COMBAT FRONTS (v2.1.0)

**لعبة أكشن عسكرية ثلاثية الأبعاد للأندرويد — تعمل 100% دون اتصال بالإنترنت**  
**100% Offline Native Android 3D Tactical Third-Person Military Shooter**

> **برمجة وتطوير معتز العلقمي – تعز – اليمن – 2026م**

---

## المميزات الرئيسية | Key Features (v2.1.0)

- **4 أطوار لعب كاملة (4 Full Game Modes):**
  1. **حملة القتال (Campaign):** 12 مهمة قتالية متدرجة عبر 4 مدن خيالية مع مواجهات زعماء وطقس ديناميكي.
  2. **البقاء (Survival):** موجات لا نهائية متصاعدة الصعوبة مع إعادة إمداد بين الموجات وحفظ أعلى موجة ونتيجة لكل مدينة.
  3. **الدفاع (Defense):** 5 موجات تكتيكية لحماية نواة القيادة ثلاثية الأبعاد (400 HP) ضد وحدات ذكاء اصطناعي تستهدف النواة واللاعب.
  4. **التحديات (Challenges):** 4 تحديات زمنية عبر المدن الأربع مع ميداليات برونزية وفضية وذهبية.
- **4 مدن خيالية مميزة (4 Fictional Cities):**
  - مدينة النهر (*River City*) — مدينة السراب (*Mirage City*) — مدينة المرسى (*Harbor City*) — مدينة الجبل (*Mountain City*).
- **نظام الطقس الديناميكي (7 Weather Conditions):** نهار، غروب، ليل، مطر، ضباب، غبار، عاصفة رملية — مع جزيئات 3D، إضاءة وضباب، مؤثرات صوتية، وتأثير مباشر على مدى رؤية الذكاء الاصطناعي.
- **مركبة تكتيكية قابلة للقيادة (Tactical Armored Rover):** متوفرة في المدن الأربع مع ركوب ونزول، قيادة وفيزياء تصادم، درع 280 HP، ومدفع رشاش علوي، وكاميرا قيادة مخصصة.
- **8 أسلحة + تخصيص كامل و360°:** 6 خانات تعديل (منظار، مقبض، مخزن، سبطانة، مثبّت، طلاء) + استوديو تخصيص الشخصية (6 خانات عتاد).
- **ذكاء اصطناعي تكتيكي (Enemy AI 2.0):** 14 حالة قتالية، خط رؤية محجوب بالجدران، حجز سواتر، وتنظيم نيران متدرج.
- **خصوصية كاملة 100% أوفلاين:** بدون إذن `INTERNET` نهائياً، مع نظام حفظ ذري محلي (Schema v4) وترقية تلقائية للملفات القديمة.
- **واجهة عربية RTL أصلية + إنجليزية LTR:** تحكم لمسي قابل للتخصيص (الوضع الأيمن/الأيسر، حجم وشفافية الأزرار، تبديل كتف الكاميرا).

---

## التقنية والبنية | Architecture & Build

- **Platform:** Native Android (Kotlin, OpenGL ES 3.0, Jetpack Compose Material 3, Procedural PCM Audio).
- **Version:** `2.1.0` (`versionCode = 3`), Landscape (`sensorLandscape`), `minSdk = 24`, `targetSdk = 36`.
- **Automated Tests:** `26 / 26` JUnit & Robolectric tests passing (`gradle :app:testDebugUnitTest`).

### Build & Test Commands
```bash
# Run all 26 unit & Robolectric tests
gradle :app:testDebugUnitTest

# Build debug APK (ARM64 compatible)
gradle :app:assembleDebug
```

---

## المخرجات والتقارير | Deliverables & Reports

- **Verified APK:** `/COMBAT-FRONTS-v2.1.0-ARM64.apk` (debug-signed with `debug.keystore`)
- **Clean Source Archive:** `/COMBAT-FRONTS-v2.1.0-SOURCE.zip`
- **Engineering Reports:** `PHASE_03_AUDIT.md`, `PHASE_04_AUDIT.md`, `REQUIREMENTS_TRACEABILITY.md`, `RECOVERY_BACKLOG.md`, `FIXED_DEFECTS.md`, `REMAINING_ISSUES.md`, `GAMEPLAY_TEST_REPORT.md`, `AI_TEST_REPORT.md`, `MISSION_VALIDATION_REPORT.md`, `PERFORMANCE_REPORT.md`, `ANDROID_BUILD_REPORT.md`, `ASSET_LICENSES.md`, `CHANGELOG.md`.
