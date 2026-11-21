package com.example.game.data

data class CityDefinition(
    val id: CityId,
    val name: LocalizedText,
    val subtitle: LocalizedText,
    val description: LocalizedText,
    val skyZenithRgb: FloatArray,
    val skyHorizonRgb: FloatArray,
    val fogRgb: FloatArray,
    val sunColorRgb: FloatArray,
    val ambientRgb: FloatArray
)

enum class ObjectiveKind {
    MOVE_TUTORIAL,
    REACH_CHECKPOINT,
    ACQUIRE_AMMO,
    ELIMINATE_HOSTILES,
    SECURE_ZONE,
    COLLECT_INTEL,
    SURVIVE_REINFORCEMENTS,
    DEFEND_CORE,
    DEFEAT_BOSS,
    EXTRACT
}

data class MissionObjectiveDef(
    val id: String,
    val kind: ObjectiveKind,
    val title: LocalizedText,
    val targetX: Float,
    val targetZ: Float,
    val radius: Float = 5.5f,
    val requiredCount: Int = 1,
    val triggerReinforcementWave: Boolean = false
)

data class EnemySpawnSpec(
    val archetype: EnemyArchetype,
    val x: Float,
    val z: Float,
    val waveIndex: Int = 0, // 0 = initial, 1+ = reinforcement wave
    val patrolOffsetX: Float = 6f,
    val patrolOffsetZ: Float = 4f
)

data class AtmosphereOverride(
    val isNight: Boolean = false,
    val isSandstorm: Boolean = false,
    val fogDensity: Float = 0.0085f
)

data class MissionDefinition(
    val number: Int,
    val id: String,
    val cityId: CityId,
    val title: LocalizedText,
    val missionType: MissionType,
    val briefing: LocalizedText,
    val introAnnouncement: LocalizedText,
    val xpReward: Int,
    val unlockWeaponReward: WeaponId? = null,
    val atmosphere: AtmosphereOverride = AtmosphereOverride(),
    val playerSpawnX: Float = 0f,
    val playerSpawnZ: Float = 32f,
    val objectives: List<MissionObjectiveDef>,
    val spawns: List<EnemySpawnSpec>
)

object MissionCatalog {
    val cities: List<CityDefinition> = listOf(
        CityDefinition(
            id = CityId.RIVER_CITY,
            name = LocalizedText("مدينة النهر", "River City"),
            subtitle = LocalizedText("العمليات الحضرية", "Urban Operations"),
            description = LocalizedText(
                "شوارع حديثة، مبانٍ متعددة الطوابق، تقاطعات مزدحمة، أسواق، ونقاط تفتيش عسكرية.",
                "Modern avenues, multi-level buildings, markets, parking structures, and urban checkpoints."
            ),
            skyZenithRgb = floatArrayOf(0.18f, 0.30f, 0.44f),
            skyHorizonRgb = floatArrayOf(0.78f, 0.62f, 0.46f),
            fogRgb = floatArrayOf(0.58f, 0.54f, 0.49f),
            sunColorRgb = floatArrayOf(1.00f, 0.88f, 0.72f),
            ambientRgb = floatArrayOf(0.32f, 0.35f, 0.40f)
        ),
        CityDefinition(
            id = CityId.MIRAGE_CITY,
            name = LocalizedText("مدينة السراب", "Mirage City"),
            subtitle = LocalizedText("العمليات الصحراوية", "Desert Operations"),
            description = LocalizedText(
                "أطراف صحراوية، طرق رملية، مجمعات صناعية، مستودعات وأبراج مراقبة مكشوفة.",
                "Arid desert outskirts, sandy roads, industrial compounds, warehouses, and watchtowers."
            ),
            skyZenithRgb = floatArrayOf(0.32f, 0.44f, 0.56f),
            skyHorizonRgb = floatArrayOf(0.88f, 0.72f, 0.48f),
            fogRgb = floatArrayOf(0.76f, 0.64f, 0.46f),
            sunColorRgb = floatArrayOf(1.00f, 0.90f, 0.68f),
            ambientRgb = floatArrayOf(0.42f, 0.38f, 0.30f)
        ),
        CityDefinition(
            id = CityId.HARBOR_CITY,
            name = LocalizedText("مدينة المرسى", "Harbor City"),
            subtitle = LocalizedText("عمليات الميناء", "Harbor Operations"),
            description = LocalizedText(
                "طرق ساحلية، حاويات شحن، مستودعات الميناء، أرصفة صناعية وجسور مطلّة على البحر.",
                "Coastal commercial docks, stacked shipping containers, warehouses, cranes, and bridges."
            ),
            skyZenithRgb = floatArrayOf(0.16f, 0.25f, 0.36f),
            skyHorizonRgb = floatArrayOf(0.56f, 0.64f, 0.70f),
            fogRgb = floatArrayOf(0.46f, 0.53f, 0.58f),
            sunColorRgb = floatArrayOf(0.88f, 0.90f, 0.94f),
            ambientRgb = floatArrayOf(0.30f, 0.36f, 0.42f)
        ),
        CityDefinition(
            id = CityId.MOUNTAIN_CITY,
            name = LocalizedText("مدينة الجبل", "Mountain City"),
            subtitle = LocalizedText("العمليات الجبلية", "Mountain Operations"),
            description = LocalizedText(
                "طرق مرتفعة ومنحدرات صخرية، أنفاق جبلية، أحياء سكنية على السفوح وحصن القمة.",
                "Elevated rocky passes, steep hillside districts, tactical tunnels, and summit strongholds."
            ),
            skyZenithRgb = floatArrayOf(0.14f, 0.22f, 0.34f),
            skyHorizonRgb = floatArrayOf(0.62f, 0.56f, 0.52f),
            fogRgb = floatArrayOf(0.44f, 0.46f, 0.50f),
            sunColorRgb = floatArrayOf(0.95f, 0.86f, 0.76f),
            ambientRgb = floatArrayOf(0.28f, 0.32f, 0.38f)
        )
    )

    val missions: List<MissionDefinition> = listOf(
        // === CITY A: RIVER CITY ===
        MissionDefinition(
            number = 1,
            id = "m01_first_deployment",
            cityId = CityId.RIVER_CITY,
            title = LocalizedText("الانتشار الأول", "First Deployment"),
            missionType = MissionType.TUTORIAL_COMBAT,
            briefing = LocalizedText(
                "أكمل اختبار الحركة التكتيكية في قطاع النهر، وصل إلى نقطة التفتيش، تزوّد بالذخيرة، واقضِ على الوحدات المعادية قبل الإخلاء.",
                "Complete tactical movement drills in River Sector, reach the checkpoint, acquire ammunition, neutralize hostile units, and secure extraction."
            ),
            introAnnouncement = LocalizedText("العملية 01 — مدينة النهر: الانتشار الميداني الأول", "OP 01 — River City: First Field Deployment"),
            xpReward = 220,
            playerSpawnX = 0f, playerSpawnZ = 34f,
            objectives = listOf(
                MissionObjectiveDef("m1_o1", ObjectiveKind.MOVE_TUTORIAL, LocalizedText("اختبر الحركة والالتفاف التكتيكي", "Complete tactical movement check"), 0f, 24f, radius = 6f),
                MissionObjectiveDef("m1_o2", ObjectiveKind.REACH_CHECKPOINT, LocalizedText("تقدّم إلى نقطة التفتيش الأولى", "Reach the forward checkpoint"), -6f, 14f, radius = 5.5f),
                MissionObjectiveDef("m1_o3", ObjectiveKind.ACQUIRE_AMMO, LocalizedText("تزوّد من صندوق الذخيرة", "Acquire ammunition from resupply crate"), 8f, 8f, radius = 4.5f),
                MissionObjectiveDef("m1_o4", ObjectiveKind.ELIMINATE_HOSTILES, LocalizedText("اقضِ على وحدات الاشتباك المعادية", "Defeat hostile units in the plaza"), 0f, -12f, requiredCount = 6),
                MissionObjectiveDef("m1_o5", ObjectiveKind.EXTRACT, LocalizedText("أمّن منطقة الإخلاء الشمالية", "Secure the extraction zone"), 0f, -32f, radius = 6.5f)
            ),
            spawns = listOf(
                EnemySpawnSpec(EnemyArchetype.STANDARD_INFANTRY, -10f, -6f),
                EnemySpawnSpec(EnemyArchetype.STANDARD_INFANTRY, 10f, -8f),
                EnemySpawnSpec(EnemyArchetype.STANDARD_INFANTRY, 0f, -16f),
                EnemySpawnSpec(EnemyArchetype.FAST_ASSAULT, -14f, -18f),
                EnemySpawnSpec(EnemyArchetype.STANDARD_INFANTRY, 12f, -20f),
                EnemySpawnSpec(EnemyArchetype.FAST_ASSAULT, 2f, -26f)
            )
        ),
        MissionDefinition(
            number = 2,
            id = "m02_besieged_street",
            cityId = CityId.RIVER_CITY,
            title = LocalizedText("الشارع المحاصر", "Besieged Street"),
            missionType = MissionType.URBAN_COMBAT,
            briefing = LocalizedText(
                "تقدّم عبر الجادة الرئيسية المحاصرة، استخدم السواتر الخرسانية بحذر، وطهّر مناطق الأهداف للوصول إلى نقطة الأمان.",
                "Advance through the barricaded urban avenue, utilize concrete cover, clear objective zones, and reach the safe checkpoint."
            ),
            introAnnouncement = LocalizedText("العملية 02 — مدينة النهر: الشارع المحاصر", "OP 02 — River City: Besieged Street"),
            xpReward = 280,
            unlockWeaponReward = WeaponId.BR12_SHOTGUN,
            playerSpawnX = 0f, playerSpawnZ = 34f,
            objectives = listOf(
                MissionObjectiveDef("m2_o1", ObjectiveKind.SECURE_ZONE, LocalizedText("أمّن تقاطع السوق المركزي", "Clear the central market intersection"), -8f, 6f, radius = 7f),
                MissionObjectiveDef("m2_o2", ObjectiveKind.ELIMINATE_HOSTILES, LocalizedText("اقضِ على فرقة الكمين الحضرية", "Eliminate the urban ambush squad"), 6f, -10f, requiredCount = 7, triggerReinforcementWave = true),
                MissionObjectiveDef("m2_o3", ObjectiveKind.EXTRACT, LocalizedText("الوصول إلى نقطة التفتيش الآمنة", "Reach the final safe checkpoint"), 0f, -33f, radius = 6.5f)
            ),
            spawns = listOf(
                EnemySpawnSpec(EnemyArchetype.STANDARD_INFANTRY, -12f, 4f),
                EnemySpawnSpec(EnemyArchetype.FAST_ASSAULT, 11f, 2f),
                EnemySpawnSpec(EnemyArchetype.STANDARD_INFANTRY, -6f, -10f),
                EnemySpawnSpec(EnemyArchetype.SHIELD_DEFENDER, 4f, -12f),
                EnemySpawnSpec(EnemyArchetype.PRECISION_MARKSMAN, 15f, -22f),
                EnemySpawnSpec(EnemyArchetype.FAST_ASSAULT, -12f, -20f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.ARMORED_HEAVY, 2f, -26f, waveIndex = 1)
            )
        ),
        MissionDefinition(
            number = 3,
            id = "m03_city_night",
            cityId = CityId.RIVER_CITY,
            title = LocalizedText("ليلة المدينة", "City Night"),
            missionType = MissionType.LOW_VISIBILITY,
            briefing = LocalizedText(
                "عملية ليلية منخفضة الرؤية في أزقة مدينة النهر. استعد بيانات الاتصالات المشفرة وتجنب الكمائن الليلية.",
                "Low-visibility night operation through River City alleys. Recover encrypted comms data and survive night ambushes."
            ),
            introAnnouncement = LocalizedText("العملية 03 — مدينة النهر: عملية ليلة المدينة", "OP 03 — River City: Night Operation"),
            xpReward = 340,
            atmosphere = AtmosphereOverride(isNight = true, fogDensity = 0.014f),
            playerSpawnX = -14f, playerSpawnZ = 32f,
            objectives = listOf(
                MissionObjectiveDef("m3_o1", ObjectiveKind.REACH_CHECKPOINT, LocalizedText("تسلّل إلى ساحة المواقف الليلية", "Infiltrate the night parking plaza"), -10f, 8f, radius = 6f),
                MissionObjectiveDef("m3_o2", ObjectiveKind.COLLECT_INTEL, LocalizedText("استعد وحدة البيانات التكتيكية", "Recover the tactical intel module"), 12f, -8f, radius = 4.5f, triggerReinforcementWave = true),
                MissionObjectiveDef("m3_o3", ObjectiveKind.ELIMINATE_HOSTILES, LocalizedText("حيّد فرقة المطاردة الليلية", "Neutralize the night pursuit squad"), 0f, -18f, requiredCount = 8),
                MissionObjectiveDef("m3_o4", ObjectiveKind.EXTRACT, LocalizedText("انسحب إلى نقطة الإخلاء", "Extract at the north perimeter"), 0f, -33f, radius = 6.5f)
            ),
            spawns = listOf(
                EnemySpawnSpec(EnemyArchetype.STANDARD_INFANTRY, -8f, 4f),
                EnemySpawnSpec(EnemyArchetype.CLOSE_AGGRESSOR, 8f, 0f),
                EnemySpawnSpec(EnemyArchetype.PRECISION_MARKSMAN, -15f, -12f),
                EnemySpawnSpec(EnemyArchetype.SQUAD_COORDINATOR, 10f, -14f),
                EnemySpawnSpec(EnemyArchetype.FAST_ASSAULT, -4f, -20f),
                EnemySpawnSpec(EnemyArchetype.CLOSE_AGGRESSOR, 14f, -22f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.SHIELD_DEFENDER, 0f, -24f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.STANDARD_INFANTRY, -10f, -28f, waveIndex = 1)
            )
        ),

        // === CITY B: MIRAGE CITY ===
        MissionDefinition(
            number = 4,
            id = "m04_mirage_road",
            cityId = CityId.MIRAGE_CITY,
            title = LocalizedText("طريق السراب", "Mirage Road"),
            missionType = MissionType.DESERT_PATROL,
            briefing = LocalizedText(
                "اعبر المدخل الصحراوي المكشوف، اهزم الدوريات المتقدمة، اقتحم مجمع المستودعات، واستعد الجهاز التكتيكي.",
                "Cross the desert approach, defeat forward patrols, breach the warehouse compound, secure the objective item, and extract."
            ),
            introAnnouncement = LocalizedText("العملية 04 — مدينة السراب: طريق السراب", "OP 04 — Mirage City: Mirage Road"),
            xpReward = 380,
            unlockWeaponReward = WeaponId.DMR14_MARKSMAN,
            playerSpawnX = 0f, playerSpawnZ = 34f,
            objectives = listOf(
                MissionObjectiveDef("m4_o1", ObjectiveKind.REACH_CHECKPOINT, LocalizedText("اعبر الطريق الصحراوي المكشوف", "Cross the desert approach road"), 0f, 14f, radius = 6f),
                MissionObjectiveDef("m4_o2", ObjectiveKind.ELIMINATE_HOSTILES, LocalizedText("اقضِ على دوريات السراب", "Eliminate Mirage patrol units"), 0f, -4f, requiredCount = 7),
                MissionObjectiveDef("m4_o3", ObjectiveKind.COLLECT_INTEL, LocalizedText("استولِ على الحقيبة التكتيكية بالمستودع", "Secure the tactical case in the warehouse"), -12f, -18f, radius = 4.5f),
                MissionObjectiveDef("m4_o4", ObjectiveKind.EXTRACT, LocalizedText("انسحب إلى نقطة الإخلاء الصحراوية", "Reach desert extraction"), 12f, -32f, radius = 6.5f)
            ),
            spawns = listOf(
                EnemySpawnSpec(EnemyArchetype.STANDARD_INFANTRY, -10f, 8f),
                EnemySpawnSpec(EnemyArchetype.PRECISION_MARKSMAN, 14f, 4f),
                EnemySpawnSpec(EnemyArchetype.FAST_ASSAULT, 4f, -4f),
                EnemySpawnSpec(EnemyArchetype.STANDARD_INFANTRY, -12f, -12f),
                EnemySpawnSpec(EnemyArchetype.SQUAD_COORDINATOR, 8f, -14f),
                EnemySpawnSpec(EnemyArchetype.ARMORED_HEAVY, -6f, -22f),
                EnemySpawnSpec(EnemyArchetype.FAST_ASSAULT, 12f, -24f)
            )
        ),
        MissionDefinition(
            number = 5,
            id = "m05_abandoned_base",
            cityId = CityId.MIRAGE_CITY,
            title = LocalizedText("القاعدة المهجورة", "Abandoned Base"),
            missionType = MissionType.EQUIPMENT_RECOVERY,
            briefing = LocalizedText(
                "اقتحم المجمع العسكري الصحراوي متعدد المباني، صُد موجة التعزيزات، واهزم وحدة النخبة المدرّعة.",
                "Assault the multi-building desert industrial complex, survive a reinforcement wave, and defeat the armored elite unit."
            ),
            introAnnouncement = LocalizedText("العملية 05 — مدينة السراب: القاعدة المهجورة", "OP 05 — Mirage City: Abandoned Base"),
            xpReward = 440,
            playerSpawnX = 0f, playerSpawnZ = 34f,
            objectives = listOf(
                MissionObjectiveDef("m5_o1", ObjectiveKind.SECURE_ZONE, LocalizedText("سيطر على بوابة المجمع الصناعي", "Capture the industrial compound gate"), 0f, 8f, radius = 7f, triggerReinforcementWave = true),
                MissionObjectiveDef("m5_o2", ObjectiveKind.SURVIVE_REINFORCEMENTS, LocalizedText("صدّ موجة التعزيزات المعادية", "Defeat the hostile reinforcement wave"), 0f, -10f, requiredCount = 8),
                MissionObjectiveDef("m5_o3", ObjectiveKind.DEFEAT_BOSS, LocalizedText("اهزم القائد المدرّع في الفناء", "Defeat the Armored Elite in the yard"), 0f, -26f, requiredCount = 1)
            ),
            spawns = listOf(
                EnemySpawnSpec(EnemyArchetype.STANDARD_INFANTRY, -12f, 4f),
                EnemySpawnSpec(EnemyArchetype.FAST_ASSAULT, 12f, 2f),
                EnemySpawnSpec(EnemyArchetype.SHIELD_DEFENDER, 0f, -6f),
                EnemySpawnSpec(EnemyArchetype.PRECISION_MARKSMAN, 16f, -14f),
                EnemySpawnSpec(EnemyArchetype.CLOSE_AGGRESSOR, -14f, -14f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.SQUAD_COORDINATOR, 6f, -18f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.ARMORED_HEAVY, -6f, -22f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.FAST_ASSAULT, 12f, -24f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.ELITE_BOSS, 0f, -28f, waveIndex = 1)
            )
        ),
        MissionDefinition(
            number = 6,
            id = "m06_sandstorm",
            cityId = CityId.MIRAGE_CITY,
            title = LocalizedText("العاصفة الرملية", "Sandstorm"),
            missionType = MissionType.LOW_VISIBILITY,
            briefing = LocalizedText(
                "تضرب عاصفة رملية كثيفة أطراف السراب. تحرّك بين المستودعات تحت رؤية محدودة وأمّن مستشعر الطقس العسكري.",
                "A dense sandstorm blankets Mirage outskirts. Fight through low visibility and recover the tactical sensor array."
            ),
            introAnnouncement = LocalizedText("العملية 06 — مدينة السراب: العاصفة الرملية", "OP 06 — Mirage City: Sandstorm"),
            xpReward = 480,
            unlockWeaponReward = WeaponId.MG48_LMG,
            atmosphere = AtmosphereOverride(isSandstorm = true, fogDensity = 0.020f),
            playerSpawnX = 12f, playerSpawnZ = 33f,
            objectives = listOf(
                MissionObjectiveDef("m6_o1", ObjectiveKind.COLLECT_INTEL, LocalizedText("استعد مستشعر الرصد وسط العاصفة", "Recover the recon sensor inside the storm"), -10f, 0f, radius = 5f, triggerReinforcementWave = true),
                MissionObjectiveDef("m6_o2", ObjectiveKind.ELIMINATE_HOSTILES, LocalizedText("اقضِ على فرق الاقتحام الصحراوية", "Eliminate desert storm troopers"), 0f, -16f, requiredCount = 8),
                MissionObjectiveDef("m6_o3", ObjectiveKind.EXTRACT, LocalizedText("الوصول إلى مروحية الإخلاء", "Reach the extraction beacon"), -4f, -32f, radius = 6.5f)
            ),
            spawns = listOf(
                EnemySpawnSpec(EnemyArchetype.CLOSE_AGGRESSOR, -8f, 6f),
                EnemySpawnSpec(EnemyArchetype.FAST_ASSAULT, 6f, 2f),
                EnemySpawnSpec(EnemyArchetype.SHIELD_DEFENDER, -12f, -6f),
                EnemySpawnSpec(EnemyArchetype.STANDARD_INFANTRY, 10f, -10f),
                EnemySpawnSpec(EnemyArchetype.SQUAD_COORDINATOR, 0f, -18f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.ARMORED_HEAVY, -10f, -22f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.CLOSE_AGGRESSOR, 10f, -22f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.PRECISION_MARKSMAN, 0f, -28f, waveIndex = 1)
            )
        ),

        // === CITY C: HARBOR CITY ===
        MissionDefinition(
            number = 7,
            id = "m07_harbor_gate",
            cityId = CityId.HARBOR_CITY,
            title = LocalizedText("بوابة الميناء", "Harbor Gate"),
            missionType = MissionType.OBJECTIVE_CAPTURE,
            briefing = LocalizedText(
                "اخترق نقطة تفتيش بوابة المرسى، سيطر على برج التحكم الساحلي، وافتح الطريق نحو أرصفة الشحن.",
                "Breach the Harbor Gate checkpoint, capture the coastal control station, and open the route to the docks."
            ),
            introAnnouncement = LocalizedText("العملية 07 — مدينة المرسى: بوابة الميناء", "OP 07 — Harbor City: Harbor Gate"),
            xpReward = 520,
            playerSpawnX = 0f, playerSpawnZ = 34f,
            objectives = listOf(
                MissionObjectiveDef("m7_o1", ObjectiveKind.SECURE_ZONE, LocalizedText("سيطر على بوابة التفتيش الساحلية", "Capture the harbor checkpoint"), 0f, 10f, radius = 7f),
                MissionObjectiveDef("m7_o2", ObjectiveKind.ELIMINATE_HOSTILES, LocalizedText("طهّر ساحة الحاويات الأمامية", "Clear the forward container yard"), 0f, -12f, requiredCount = 8, triggerReinforcementWave = true),
                MissionObjectiveDef("m7_o3", ObjectiveKind.EXTRACT, LocalizedText("أمّن رصيف الدخول الرئيسي", "Secure the main dock entry"), 0f, -32f, radius = 6.5f)
            ),
            spawns = listOf(
                EnemySpawnSpec(EnemyArchetype.STANDARD_INFANTRY, -10f, 10f),
                EnemySpawnSpec(EnemyArchetype.SHIELD_DEFENDER, 8f, 6f),
                EnemySpawnSpec(EnemyArchetype.FAST_ASSAULT, -6f, -4f),
                EnemySpawnSpec(EnemyArchetype.PRECISION_MARKSMAN, 14f, -12f),
                EnemySpawnSpec(EnemyArchetype.SQUAD_COORDINATOR, -12f, -14f),
                EnemySpawnSpec(EnemyArchetype.CLOSE_AGGRESSOR, 4f, -18f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.ARMORED_HEAVY, -4f, -24f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.FAST_ASSAULT, 12f, -26f, waveIndex = 1)
            )
        ),
        MissionDefinition(
            number = 8,
            id = "m08_harbor_warehouses",
            cityId = CityId.HARBOR_CITY,
            title = LocalizedText("مستودعات المرسى", "Harbor Warehouses"),
            missionType = MissionType.EQUIPMENT_RECOVERY,
            briefing = LocalizedText(
                "قتال متلاحم بين حاويات الشحن ومستودعات المرسى. استعد الشحنة التكتيكية وسط كمائن المقتحمين.",
                "Close-quarters warfare through shipping containers and port warehouses. Recover the tactical shipment."
            ),
            introAnnouncement = LocalizedText("العملية 08 — مدينة المرسى: مستودعات المرسى", "OP 08 — Harbor City: Port Warehouses"),
            xpReward = 580,
            playerSpawnX = -12f, playerSpawnZ = 33f,
            objectives = listOf(
                MissionObjectiveDef("m8_o1", ObjectiveKind.COLLECT_INTEL, LocalizedText("استعد شحنة الإمداد من المستودع الغربي", "Recover cargo from the west warehouse"), -12f, -4f, radius = 5f, triggerReinforcementWave = true),
                MissionObjectiveDef("m8_o2", ObjectiveKind.ELIMINATE_HOSTILES, LocalizedText("اهزم حراس المستودعات والحاويات", "Defeat warehouse & container guards"), 4f, -16f, requiredCount = 9),
                MissionObjectiveDef("m8_o3", ObjectiveKind.EXTRACT, LocalizedText("توجّه إلى رصيف القارب السريع", "Reach the coastal extraction pier"), 10f, -32f, radius = 6.5f)
            ),
            spawns = listOf(
                EnemySpawnSpec(EnemyArchetype.CLOSE_AGGRESSOR, -10f, 8f),
                EnemySpawnSpec(EnemyArchetype.SHIELD_DEFENDER, 4f, 4f),
                EnemySpawnSpec(EnemyArchetype.FAST_ASSAULT, -14f, -6f),
                EnemySpawnSpec(EnemyArchetype.STANDARD_INFANTRY, 12f, -8f),
                EnemySpawnSpec(EnemyArchetype.SQUAD_COORDINATOR, 0f, -14f),
                EnemySpawnSpec(EnemyArchetype.ARMORED_HEAVY, -8f, -20f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.CLOSE_AGGRESSOR, 10f, -20f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.PRECISION_MARKSMAN, -15f, -26f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.FAST_ASSAULT, 6f, -28f, waveIndex = 1)
            )
        ),
        MissionDefinition(
            number = 9,
            id = "m09_final_corridor",
            cityId = CityId.HARBOR_CITY,
            title = LocalizedText("الممر الأخير", "The Final Corridor"),
            missionType = MissionType.RESCUE_EXTRACTION,
            briefing = LocalizedText(
                "معركة ساحلية متعددة المراحل لفتح الممر الأخير عبر جسر الميناء وإخلاء المنطقة قبل وصول التعزيزات الثقيلة.",
                "Multi-stage coastal battle across the harbor bridge corridor under heavy reinforcement pressure."
            ),
            introAnnouncement = LocalizedText("العملية 09 — مدينة المرسى: الممر الأخير", "OP 09 — Harbor City: The Final Corridor"),
            xpReward = 640,
            unlockWeaponReward = WeaponId.RAIL_X1_HEAVY,
            playerSpawnX = 0f, playerSpawnZ = 34f,
            objectives = listOf(
                MissionObjectiveDef("m9_o1", ObjectiveKind.REACH_CHECKPOINT, LocalizedText("اخترق جسر الميناء الأوسط", "Breach the central harbor bridge"), 0f, 8f, radius = 6f, triggerReinforcementWave = true),
                MissionObjectiveDef("m9_o2", ObjectiveKind.SURVIVE_REINFORCEMENTS, LocalizedText("اهزم جميع موجات الدفاع الساحلي", "Defeat all coastal defense waves"), 0f, -14f, requiredCount = 9),
                MissionObjectiveDef("m9_o3", ObjectiveKind.EXTRACT, LocalizedText("أكمل الإخلاء من الرصيف الشمالي", "Complete extraction at north pier"), 0f, -33f, radius = 6.5f)
            ),
            spawns = listOf(
                EnemySpawnSpec(EnemyArchetype.SHIELD_DEFENDER, -6f, 10f),
                EnemySpawnSpec(EnemyArchetype.SQUAD_COORDINATOR, 8f, 6f),
                EnemySpawnSpec(EnemyArchetype.PRECISION_MARKSMAN, -14f, -4f),
                EnemySpawnSpec(EnemyArchetype.ARMORED_HEAVY, 6f, -8f),
                EnemySpawnSpec(EnemyArchetype.FAST_ASSAULT, -8f, -14f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.CLOSE_AGGRESSOR, 10f, -16f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.SHIELD_DEFENDER, 0f, -22f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.ARMORED_HEAVY, -10f, -26f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.PRECISION_MARKSMAN, 14f, -28f, waveIndex = 1)
            )
        ),

        // === CITY D: MOUNTAIN CITY ===
        MissionDefinition(
            number = 10,
            id = "m10_highland_road",
            cityId = CityId.MOUNTAIN_CITY,
            title = LocalizedText("طريق المرتفعات", "Highland Road"),
            missionType = MissionType.URBAN_COMBAT,
            briefing = LocalizedText(
                "تسلّق الممر الجبلي الوعر تحت نيران القناصة، واعبر النفق الصخري لتأمين مدخل القمة.",
                "Ascend the steep highland road under marksman fire and push through the rock tunnel to secure the summit approach."
            ),
            introAnnouncement = LocalizedText("العملية 10 — مدينة الجبل: طريق المرتفعات", "OP 10 — Mountain City: Highland Road"),
            xpReward = 700,
            playerSpawnX = 0f, playerSpawnZ = 34f,
            objectives = listOf(
                MissionObjectiveDef("m10_o1", ObjectiveKind.REACH_CHECKPOINT, LocalizedText("اعبر النفق الجبلي الأول", "Cross the mountain rock tunnel"), 0f, 8f, radius = 6f),
                MissionObjectiveDef("m10_o2", ObjectiveKind.ELIMINATE_HOSTILES, LocalizedText("حيّد قناصة ومقاتلي المرتفعات", "Neutralize highland marksmen & troops"), 0f, -12f, requiredCount = 9, triggerReinforcementWave = true),
                MissionObjectiveDef("m10_o3", ObjectiveKind.EXTRACT, LocalizedText("أمّن موقع المعسكر المتقدم", "Secure the forward ridge camp"), 0f, -33f, radius = 6.5f)
            ),
            spawns = listOf(
                EnemySpawnSpec(EnemyArchetype.PRECISION_MARKSMAN, -14f, 6f),
                EnemySpawnSpec(EnemyArchetype.STANDARD_INFANTRY, 10f, 8f),
                EnemySpawnSpec(EnemyArchetype.FAST_ASSAULT, -4f, -4f),
                EnemySpawnSpec(EnemyArchetype.SQUAD_COORDINATOR, 10f, -10f),
                EnemySpawnSpec(EnemyArchetype.SHIELD_DEFENDER, -8f, -14f),
                EnemySpawnSpec(EnemyArchetype.PRECISION_MARKSMAN, 15f, -20f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.ARMORED_HEAVY, 0f, -22f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.CLOSE_AGGRESSOR, -12f, -26f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.FAST_ASSAULT, 8f, -28f, waveIndex = 1)
            )
        ),
        MissionDefinition(
            number = 11,
            id = "m11_summit_siege",
            cityId = CityId.MOUNTAIN_CITY,
            title = LocalizedText("حصار القمة", "Summit Siege"),
            missionType = MissionType.DEFENSIVE_SURVIVAL,
            briefing = LocalizedText(
                "اقتحم الحصن الجبلي المنيع، سيطر على منصة الرادار، وصُدّ الهجوم المضاد لقوات الحراسة الثقيلة.",
                "Storm the mountain stronghold, capture the radar terrace, and repel heavy garrison counterattacks."
            ),
            introAnnouncement = LocalizedText("العملية 11 — مدينة الجبل: حصار القمة", "OP 11 — Mountain City: Summit Siege"),
            xpReward = 780,
            playerSpawnX = 0f, playerSpawnZ = 34f,
            objectives = listOf(
                MissionObjectiveDef("m11_o1", ObjectiveKind.SECURE_ZONE, LocalizedText("سيطر على منصة الرادار الجبلية", "Capture the mountain radar terrace"), 0f, 2f, radius = 7f, triggerReinforcementWave = true),
                MissionObjectiveDef("m11_o2", ObjectiveKind.SURVIVE_REINFORCEMENTS, LocalizedText("صدّ الهجوم المضاد لحامية القمة", "Repel the summit garrison counterattack"), 0f, -16f, requiredCount = 10),
                MissionObjectiveDef("m11_o3", ObjectiveKind.EXTRACT, LocalizedText("ادخل بوابة الحصن الداخلي", "Breach the inner citadel gate"), 0f, -33f, radius = 6.5f)
            ),
            spawns = listOf(
                EnemySpawnSpec(EnemyArchetype.SHIELD_DEFENDER, -8f, 6f),
                EnemySpawnSpec(EnemyArchetype.ARMORED_HEAVY, 8f, 4f),
                EnemySpawnSpec(EnemyArchetype.SQUAD_COORDINATOR, 0f, -4f),
                EnemySpawnSpec(EnemyArchetype.PRECISION_MARKSMAN, -15f, -10f),
                EnemySpawnSpec(EnemyArchetype.CLOSE_AGGRESSOR, 12f, -12f),
                EnemySpawnSpec(EnemyArchetype.FAST_ASSAULT, -10f, -18f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.ARMORED_HEAVY, 6f, -20f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.SHIELD_DEFENDER, -4f, -24f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.SQUAD_COORDINATOR, 12f, -26f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.PRECISION_MARKSMAN, 0f, -30f, waveIndex = 1)
            )
        ),
        MissionDefinition(
            number = 12,
            id = "m12_final_showdown",
            cityId = CityId.MOUNTAIN_CITY,
            title = LocalizedText("المواجهة الأخيرة", "The Final Showdown"),
            missionType = MissionType.BOSS_ENCOUNTER,
            briefing = LocalizedText(
                "المواجهة الحاسمة فوق قمة الجبل! اخترق حرس النخبة، صُدّ التعزيزات النهائية، واهزم القائد المدرّع لإنهاء الحملة.",
                "The decisive summit showdown! Break through elite guards, survive final reinforcements, and defeat the Armored Commander."
            ),
            introAnnouncement = LocalizedText("العملية 12 — مدينة الجبل: المواجهة الأخيرة", "OP 12 — Mountain City: The Final Showdown"),
            xpReward = 1200,
            atmosphere = AtmosphereOverride(isNight = true, fogDensity = 0.011f),
            playerSpawnX = 0f, playerSpawnZ = 34f,
            objectives = listOf(
                MissionObjectiveDef("m12_o1", ObjectiveKind.REACH_CHECKPOINT, LocalizedText("ادخل ساحة القيادة العليا", "Enter the upper command courtyard"), 0f, 10f, radius = 6.5f),
                MissionObjectiveDef("m12_o2", ObjectiveKind.ELIMINATE_HOSTILES, LocalizedText("اهزم فرقة الحرس المدرّع", "Eliminate the elite guard squad"), 0f, -6f, requiredCount = 7, triggerReinforcementWave = true),
                MissionObjectiveDef("m12_o3", ObjectiveKind.DEFEAT_BOSS, LocalizedText("اهزم القائد الأعلى المدرّع", "Defeat the Armored Elite Commander"), 0f, -24f, requiredCount = 1),
                MissionObjectiveDef("m12_o4", ObjectiveKind.EXTRACT, LocalizedText("أكمل الإخلاء وأعلن النصر", "Complete final extraction"), 0f, -34f, radius = 7f)
            ),
            spawns = listOf(
                EnemySpawnSpec(EnemyArchetype.SHIELD_DEFENDER, -10f, 4f),
                EnemySpawnSpec(EnemyArchetype.SQUAD_COORDINATOR, 10f, 4f),
                EnemySpawnSpec(EnemyArchetype.PRECISION_MARKSMAN, -15f, -8f),
                EnemySpawnSpec(EnemyArchetype.CLOSE_AGGRESSOR, 12f, -8f),
                EnemySpawnSpec(EnemyArchetype.ARMORED_HEAVY, -6f, -14f),
                EnemySpawnSpec(EnemyArchetype.FAST_ASSAULT, 8f, -16f),
                EnemySpawnSpec(EnemyArchetype.STANDARD_INFANTRY, 0f, -18f),
                EnemySpawnSpec(EnemyArchetype.ARMORED_HEAVY, -10f, -24f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.SQUAD_COORDINATOR, 10f, -24f, waveIndex = 1),
                EnemySpawnSpec(EnemyArchetype.ELITE_BOSS, 0f, -26f, waveIndex = 1)
            )
        )
    )

    fun cityById(id: CityId): CityDefinition = cities.first { it.id == id }

    fun missionByNumber(num: Int): MissionDefinition = missions.first { it.number == num }

    fun missionsForCity(cityId: CityId): List<MissionDefinition> = missions.filter { it.cityId == cityId }
}
