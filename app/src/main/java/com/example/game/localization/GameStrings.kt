package com.example.game.localization

import com.example.game.data.AttachmentSlot
import com.example.game.data.ChallengeMedal
import com.example.game.data.CharacterSlot
import com.example.game.data.CombatPhase
import com.example.game.data.CrosshairStyle
import com.example.game.data.Difficulty
import com.example.game.data.EnemyArchetype
import com.example.game.data.GameLanguage
import com.example.game.data.GameMode
import com.example.game.data.GraphicsQuality
import com.example.game.data.WeatherCondition

object GameStrings {
    const val DEVELOPER_CREDIT_AR = "برمجة وتطوير معتز العلقمي – تعز – اليمن – 2026م"
    const val DEVELOPER_CREDIT_EN = "Developed by Moataz Al-Alqami – Taiz – Yemen – 2026"
    const val GAME_VERSION = "v2.1.0"

    fun appTitle(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "جبهات القتال" else "COMBAT FRONTS"
    fun appTagline(lang: GameLanguage) = if (lang == GameLanguage.ARABIC)
        "الإصدار الكامل المتكامل — ثلاثية الأبعاد v2.1.0" else "Total Battlefield Edition — 3D Tactical Action v2.1.0"

    // Main Menu 8 items (exact Arabic strings required)
    fun menuPlay(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "بدء اللعب" else "Play Modes"
    fun menuContinue(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "متابعة" else "Continue"
    fun menuMissions(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "المهمات" else "Missions"
    fun menuArmory(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "ترسانة الأسلحة" else "Armory"
    fun menuCustomization(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "تخصيص الشخصية" else "Character Studio"
    fun menuAchievements(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "الإنجازات" else "Achievements"
    fun menuSettings(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "الإعدادات" else "Settings"
    fun menuAbout(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "حول اللعبة" else "About"

    // v2.1.0 Four Playable Game Modes (exact Arabic names mandated)
    fun gameModeName(mode: GameMode, lang: GameLanguage): String = when (mode) {
        GameMode.CAMPAIGN -> if (lang == GameLanguage.ARABIC) "حملة القتال" else "Campaign"
        GameMode.SURVIVAL -> if (lang == GameLanguage.ARABIC) "البقاء" else "Survival"
        GameMode.DEFENSE -> if (lang == GameLanguage.ARABIC) "الدفاع" else "Defense"
        GameMode.CHALLENGES -> if (lang == GameLanguage.ARABIC) "التحديات" else "Challenges"
    }

    fun gameModeSubtitle(mode: GameMode, lang: GameLanguage): String = when (mode) {
        GameMode.CAMPAIGN -> if (lang == GameLanguage.ARABIC) "12 مهمة تكتيكية عبر 4 مدن" else "12 Tactical Missions across 4 Cities"
        GameMode.SURVIVAL -> if (lang == GameLanguage.ARABIC) "موجات متصاعدة لا تنتهي مع إمدادات دورية" else "Endless Escalating Waves with Resupply Drops"
        GameMode.DEFENSE -> if (lang == GameLanguage.ARABIC) "احمِ برج القيادة خلال 5 موجات شرسة" else "Defend Command Core across 5 Waves"
        GameMode.CHALLENGES -> if (lang == GameLanguage.ARABIC) "4 تحديات زمنية وميداليات ذهبية" else "4 Timed Tactical Trials with Medals"
    }

    fun weatherName(w: WeatherCondition, lang: GameLanguage): String = when (w) {
        WeatherCondition.CLEAR_DAY -> if (lang == GameLanguage.ARABIC) "نهار صافٍ" else "Daylight"
        WeatherCondition.SUNSET -> if (lang == GameLanguage.ARABIC) "غروب" else "Sunset"
        WeatherCondition.NIGHT -> if (lang == GameLanguage.ARABIC) "ليل" else "Night"
        WeatherCondition.RAIN -> if (lang == GameLanguage.ARABIC) "مطر" else "Rain"
        WeatherCondition.FOG -> if (lang == GameLanguage.ARABIC) "ضباب" else "Fog"
        WeatherCondition.DUST -> if (lang == GameLanguage.ARABIC) "غبار" else "Dust"
        WeatherCondition.SANDSTORM -> if (lang == GameLanguage.ARABIC) "عاصفة رملية" else "Sandstorm"
    }

    fun medalName(m: ChallengeMedal, lang: GameLanguage): String = when (m) {
        ChallengeMedal.NONE -> if (lang == GameLanguage.ARABIC) "بدون ميدالية" else "No Medal"
        ChallengeMedal.BRONZE -> if (lang == GameLanguage.ARABIC) "برونزية" else "Bronze"
        ChallengeMedal.SILVER -> if (lang == GameLanguage.ARABIC) "فضية" else "Silver"
        ChallengeMedal.GOLD -> if (lang == GameLanguage.ARABIC) "ذهبية" else "Gold"
    }

    // Common UI
    fun back(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "رجوع" else "Back"
    fun startMission(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "بدء العملية" else "Deploy"
    fun locked(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "مغلق" else "Locked"
    fun unlocked(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "مفتوح" else "Unlocked"
    fun equipped(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "مجهّز" else "Equipped"
    fun equip(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "تجهيز" else "Equip"
    fun unlockWithXp(lang: GameLanguage, cost: Int) =
        if (lang == GameLanguage.ARABIC) "فتح ($cost خبرة)" else "Unlock ($cost XP)"
    fun xpLabel(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "نقاط الخبرة" else "XP"
    fun rankLabel(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "الرتبة" else "Rank"
    fun completed(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "مكتملة" else "Completed"
    fun saveChanges(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "حفظ الإعدادات" else "Save Layout"
    fun resetDefaults(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "استعادة الافتراضي" else "Reset Defaults"

    // Difficulty
    fun difficultyName(d: Difficulty, lang: GameLanguage): String = when (d) {
        Difficulty.EASY -> if (lang == GameLanguage.ARABIC) "سهل" else "Easy"
        Difficulty.MEDIUM -> if (lang == GameLanguage.ARABIC) "متوسط" else "Medium"
        Difficulty.HARD -> if (lang == GameLanguage.ARABIC) "صعب" else "Hard"
    }

    fun graphicsName(g: GraphicsQuality, lang: GameLanguage): String = when (g) {
        GraphicsQuality.LOW -> if (lang == GameLanguage.ARABIC) "منخفض" else "Low"
        GraphicsQuality.MEDIUM -> if (lang == GameLanguage.ARABIC) "متوسط" else "Medium"
        GraphicsQuality.HIGH -> if (lang == GameLanguage.ARABIC) "عالي" else "High"
    }

    fun crosshairName(c: CrosshairStyle, lang: GameLanguage): String = when (c) {
        CrosshairStyle.DYNAMIC -> if (lang == GameLanguage.ARABIC) "ديناميكي" else "Dynamic"
        CrosshairStyle.TACTICAL_DOT -> if (lang == GameLanguage.ARABIC) "نقطة تكتيكية" else "Tactical Dot"
        CrosshairStyle.MILITARY_CROSS -> if (lang == GameLanguage.ARABIC) "صليب عسكري" else "Military Cross"
    }

    fun attachmentSlotName(s: AttachmentSlot, lang: GameLanguage): String = when (s) {
        AttachmentSlot.OPTIC -> if (lang == GameLanguage.ARABIC) "المنظار البصري" else "Optic Sight"
        AttachmentSlot.GRIP -> if (lang == GameLanguage.ARABIC) "المقبض الأمامي" else "Foregrip"
        AttachmentSlot.MAGAZINE -> if (lang == GameLanguage.ARABIC) "المخزن الموسّع" else "Magazine"
        AttachmentSlot.BARREL -> if (lang == GameLanguage.ARABIC) "السبطانة" else "Barrel"
        AttachmentSlot.STABILIZER -> if (lang == GameLanguage.ARABIC) "مثبّت الارتداد" else "Stabilizer"
        AttachmentSlot.SKIN -> if (lang == GameLanguage.ARABIC) "التمويه والطلاء" else "Finish & Skin"
    }

    fun characterSlotName(s: CharacterSlot, lang: GameLanguage): String = when (s) {
        CharacterSlot.OUTFIT -> if (lang == GameLanguage.ARABIC) "الزي العسكري" else "Outfit"
        CharacterSlot.HELMET -> if (lang == GameLanguage.ARABIC) "الخوذة" else "Helmet"
        CharacterSlot.VEST -> if (lang == GameLanguage.ARABIC) "الدرع الواقي" else "Tactical Vest"
        CharacterSlot.GLOVES -> if (lang == GameLanguage.ARABIC) "القفازات" else "Gloves"
        CharacterSlot.BOOTS -> if (lang == GameLanguage.ARABIC) "الأحذية" else "Boots"
        CharacterSlot.BACKPACK -> if (lang == GameLanguage.ARABIC) "الحقيبة" else "Backpack"
    }

    fun enemyName(e: EnemyArchetype, lang: GameLanguage): String = when (e) {
        EnemyArchetype.STANDARD_INFANTRY -> if (lang == GameLanguage.ARABIC) "جندي مشاة" else "Standard Infantry"
        EnemyArchetype.FAST_ASSAULT -> if (lang == GameLanguage.ARABIC) "مهاجم سريع" else "Fast Assault"
        EnemyArchetype.ARMORED_HEAVY -> if (lang == GameLanguage.ARABIC) "مدرّع ثقيل" else "Armored Heavy"
        EnemyArchetype.PRECISION_MARKSMAN -> if (lang == GameLanguage.ARABIC) "قناص محترف" else "Precision Marksman"
        EnemyArchetype.SHIELD_DEFENDER -> if (lang == GameLanguage.ARABIC) "حامل الدرع" else "Shield Defender"
        EnemyArchetype.CLOSE_AGGRESSOR -> if (lang == GameLanguage.ARABIC) "مقتحم قريب" else "Close Aggressor"
        EnemyArchetype.SQUAD_COORDINATOR -> if (lang == GameLanguage.ARABIC) "قائد فرقة" else "Squad Coordinator"
        EnemyArchetype.ELITE_BOSS -> if (lang == GameLanguage.ARABIC) "القائد المدرّع" else "Armored Elite Commander"
    }

    // HUD Controls
    fun hudFire(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "إطلاق" else "FIRE"
    fun hudAim(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "تصويب" else "AIM"
    fun hudReload(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "ذخيرة" else "RELOAD"
    fun hudJump(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "قفز" else "JUMP"
    fun hudCrouch(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "انحناء" else "CROUCH"
    fun hudSprint(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "ركض" else "SPRINT"
    fun hudCover(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "ساتر" else "COVER"
    fun hudVault(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "قفز حاجز" else "VAULT"
    fun hudInteract(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "تفاعل" else "USE"
    fun hudExitVehicle(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "نزول" else "EXIT"
    fun hudVehicleHull(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "هيكل المدرّعة" else "ROVER HULL"
    fun hudDefenseCore(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "برج القيادة" else "COMMAND CORE"
    fun hudWave(lang: GameLanguage, wave: Int) = if (lang == GameLanguage.ARABIC) "الموجة $wave" else "WAVE $wave"
    fun hudScore(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "النقاط" else "SCORE"
    fun hudShoulder(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "الكتف" else "CAM"
    fun hudSwitch(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "تبديل" else "SWAP"
    fun hudHealth(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "الصحة" else "HP"
    fun hudArmor(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "الدرع" else "ARMOR"
    fun hudObjective(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "الهدف الحالي" else "OBJECTIVE"
    fun hudReloading(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "جاري إعادة التلقيم..." else "RELOADING..."
    fun hudInCover(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "في ساتر تكتيكي" else "IN COVER"
    fun hudRespawnShield(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "حماية مؤقتة" else "SHIELD ACTIVE"
    fun hudBarrelDetonated(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "انفجار برميل وقود!" else "FUEL BARREL DETONATED!"
    fun hudCheckpointReached(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "تم حفظ نقطة التفتيش" else "CHECKPOINT REACHED"
    fun hudReinforcements(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "تعزيزات معادية قادمة!" else "HOSTILE REINFORCEMENTS INBOUND!"
    fun hudAmmoResupplied(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "تم التزود بالذخيرة والدرع" else "AMMO & ARMOR RESUPPLIED"
    fun hudVehicleBoarded(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "تم ركوب المدرّعة التكتيكية" else "TACTICAL ROVER BOARDED"
    fun hudVehicleDisabled(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "تضررت المدرّعة — إخلاء فوري!" else "ROVER DISABLED — EJECTING!"
    fun hudCoreUnderAttack(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "تحذير: برج القيادة يتعرض للهجوم!" else "WARNING: COMMAND CORE UNDER ATTACK!"

    fun combatPhaseLabel(phase: CombatPhase, lang: GameLanguage): String = when (phase) {
        CombatPhase.QUIET -> if (lang == GameLanguage.ARABIC) "هادئ" else "QUIET"
        CombatPhase.EXPLORATION -> if (lang == GameLanguage.ARABIC) "استطلاع" else "RECON"
        CombatPhase.ALERT -> if (lang == GameLanguage.ARABIC) "استنفار" else "ALERT"
        CombatPhase.COMBAT -> if (lang == GameLanguage.ARABIC) "اشتباك" else "COMBAT"
        CombatPhase.RECOVERY -> if (lang == GameLanguage.ARABIC) "استعادة" else "RECOVERY"
        CombatPhase.FINAL_ENCOUNTER -> if (lang == GameLanguage.ARABIC) "المواجهة الأخيرة" else "FINAL ENGAGEMENT"
    }

    // Pause / Victory / Defeat
    fun pauseTitle(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "إيقاف مؤقت" else "PAUSED"
    fun resume(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "استئناف القتال" else "Resume"
    fun loadCheckpoint(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "آخر نقطة حفظ" else "Last Checkpoint"
    fun restartMission(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "إعادة العملية" else "Restart"
    fun exitToMenu(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "القائمة الرئيسية" else "Main Menu"
    fun victoryTitle(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "نجحت العملية!" else "OPERATION COMPLETE"
    fun defeatTitle(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "سقط المقاتل" else "OPERATOR DOWN"
    fun nextMission(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "المهمة التالية" else "Next Mission"
    fun campaignCompleteTitle(lang: GameLanguage) =
        if (lang == GameLanguage.ARABIC) "اكتملت الحملة العسكرية بنجاح!" else "CAMPAIGN COMPLETE!"

    // Settings headers
    fun settingsGraphics(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "الرسوميات" else "Graphics"
    fun settingsAudio(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "الصوت" else "Audio"
    fun settingsGameplay(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "أسلوب اللعب" else "Gameplay"
    fun settingsAccessibility(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "إمكانية الوصول" else "Accessibility"
    fun settingsControls(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "تخصيص أزرار اللمس" else "Touch Controls"
    fun settingsLanguage(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "اللغة" else "Language"
    fun offlineBadge(lang: GameLanguage) = if (lang == GameLanguage.ARABIC) "وضع أوفلاين كامل" else "100% OFFLINE"
}
