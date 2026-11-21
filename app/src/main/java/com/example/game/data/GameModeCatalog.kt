package com.example.game.data

/**
 * v2.1.0 Complete Game Modes Catalog:
 * Supports Survival, Defense, and Challenges modes across all 4 fictional cities.
 */
enum class ChallengeRule {
    HEADSHOT_PRECISION,
    TIMED_BLITZ,
    IRONCLAD_SURVIVAL,
    ELITE_GAUNTLET
}

data class ChallengeDefinition(
    val id: String,
    val title: LocalizedText,
    val description: LocalizedText,
    val cityId: CityId,
    val weather: WeatherCondition,
    val rule: ChallengeRule,
    val timeLimitSeconds: Float,
    val targetKills: Int,
    val requiredHeadshots: Int = 0,
    val minAccuracyPercent: Int = 0,
    val xpReward: Int = 260,
    val bronzeScore: Int = 400,
    val silverScore: Int = 750,
    val goldScore: Int = 1150
) {
    fun evaluateMedal(score: Int, victory: Boolean): ChallengeMedal {
        if (!victory) return ChallengeMedal.NONE
        return when {
            score >= goldScore -> ChallengeMedal.GOLD
            score >= silverScore -> ChallengeMedal.SILVER
            score >= bronzeScore -> ChallengeMedal.BRONZE
            else -> ChallengeMedal.BRONZE
        }
    }
}

object GameModeCatalog {
    val challenges: List<ChallengeDefinition> = listOf(
        ChallengeDefinition(
            id = "chal_river_marksman",
            title = LocalizedText("تحدي دقة القناصة", "Precision Marksman Trial"),
            description = LocalizedText(
                "اقضِ على 8 أعداء خلال 120 ثانية مع إصابتَي رأس على الأقل في أجواء ممطرة بمدينة النهر.",
                "Eliminate 8 hostiles within 120s with at least 2 headshots in rainy River City."
            ),
            cityId = CityId.RIVER_CITY,
            weather = WeatherCondition.RAIN,
            rule = ChallengeRule.HEADSHOT_PRECISION,
            timeLimitSeconds = 120f,
            targetKills = 8,
            requiredHeadshots = 2,
            minAccuracyPercent = 25,
            xpReward = 260,
            bronzeScore = 500,
            silverScore = 850,
            goldScore = 1200
        ),
        ChallengeDefinition(
            id = "chal_mirage_blitz",
            title = LocalizedText("تحدي العاصفة الزمنية", "Sandstorm Blitz Trial"),
            description = LocalizedText(
                "صفِّ 10 مقاتلين قبل نفاد 110 ثوانٍ وسط عاصفة رملية تضرب مدينة السراب.",
                "Eliminate 10 hostiles in under 110s during a blinding sandstorm in Mirage City."
            ),
            cityId = CityId.MIRAGE_CITY,
            weather = WeatherCondition.SANDSTORM,
            rule = ChallengeRule.TIMED_BLITZ,
            timeLimitSeconds = 110f,
            targetKills = 10,
            xpReward = 290,
            bronzeScore = 600,
            silverScore = 950,
            goldScore = 1350
        ),
        ChallengeDefinition(
            id = "chal_harbor_ironclad",
            title = LocalizedText("تحدي الصمود الضبابي", "Foggy Harbor Ironclad"),
            description = LocalizedText(
                "اقضِ على 10 وحدات مهاجمة ومدرعة خلال 135 ثانية تحت الضباب الكثيف في مدينة المرسى.",
                "Eliminate 10 heavy & assault hostiles within 135s under dense Harbor City fog."
            ),
            cityId = CityId.HARBOR_CITY,
            weather = WeatherCondition.FOG,
            rule = ChallengeRule.IRONCLAD_SURVIVAL,
            timeLimitSeconds = 135f,
            targetKills = 10,
            xpReward = 320,
            bronzeScore = 650,
            silverScore = 1050,
            goldScore = 1450
        ),
        ChallengeDefinition(
            id = "chal_mountain_gauntlet",
            title = LocalizedText("تحدي تصفية النخبة", "Summit Elite Gauntlet"),
            description = LocalizedText(
                "اقتحم قلعة الجبل ليلاً واقضِ على 12 مقاتلاً من النخبة خلال 150 ثانية.",
                "Assault Mountain City at night and neutralize 12 elite hostiles within 150s."
            ),
            cityId = CityId.MOUNTAIN_CITY,
            weather = WeatherCondition.NIGHT,
            rule = ChallengeRule.ELITE_GAUNTLET,
            timeLimitSeconds = 150f,
            targetKills = 12,
            xpReward = 380,
            bronzeScore = 750,
            silverScore = 1200,
            goldScore = 1650
        )
    )

    fun challengeById(id: String): ChallengeDefinition =
        challenges.firstOrNull { it.id == id } ?: challenges.first()

    /** Builds a playable MissionDefinition wrapper for Survival mode on any of the 4 cities. */
    fun buildSurvivalMissionDef(cityId: CityId, weather: WeatherCondition): MissionDefinition {
        val city = MissionCatalog.cityById(cityId)
        return MissionDefinition(
            number = 101,
            id = "mode_survival_${cityId.id}",
            cityId = cityId,
            title = LocalizedText("البقاء — ${city.name.ar}", "Survival — ${city.name.en}"),
            missionType = MissionType.DEFENSIVE_SURVIVAL,
            briefing = LocalizedText(
                "اصمد أمام موجات العدو المتصاعدة، استخدم السواتر والمدرّعة التكتيكية، واحصد أعلى نتيجة.",
                "Endure escalating enemy waves, use cover and the tactical rover, and set a high score."
            ),
            introAnnouncement = LocalizedText(
                "وضع البقاء — الموجة 1 تبدأ الآن",
                "SURVIVAL MODE — WAVE 1 INBOUND"
            ),
            xpReward = 150,
            atmosphere = AtmosphereOverride(
                isNight = weather.isNight,
                isSandstorm = weather.isSandstorm,
                fogDensity = weather.fogDensity
            ),
            playerSpawnX = 0f,
            playerSpawnZ = 24f,
            objectives = listOf(
                MissionObjectiveDef(
                    id = "surv_endless",
                    kind = ObjectiveKind.SURVIVE_REINFORCEMENTS,
                    title = LocalizedText("اصمد أمام موجات العدو المتصاعدة", "Survive Escalating Hostile Waves"),
                    targetX = 0f,
                    targetZ = 0f,
                    radius = 6f,
                    requiredCount = 999
                )
            ),
            spawns = emptyList()
        )
    }

    /** Builds a playable MissionDefinition wrapper for 5-wave Core Defense mode on any of the 4 cities. */
    fun buildDefenseMissionDef(cityId: CityId, weather: WeatherCondition): MissionDefinition {
        val city = MissionCatalog.cityById(cityId)
        return MissionDefinition(
            number = 201,
            id = "mode_defense_${cityId.id}",
            cityId = cityId,
            title = LocalizedText("الدفاع — ${city.name.ar}", "Defense — ${city.name.en}"),
            missionType = MissionType.DEFENSIVE_SURVIVAL,
            briefing = LocalizedText(
                "احمِ برج الاتصالات القيادي من 5 موجات معادية متتالية قبل تدميره.",
                "Defend the Command Relay Core against 5 consecutive enemy waves before it is destroyed."
            ),
            introAnnouncement = LocalizedText(
                "وضع الدفاع — احمِ برج القيادة",
                "DEFENSE MODE — PROTECT THE COMMAND CORE"
            ),
            xpReward = 340,
            atmosphere = AtmosphereOverride(
                isNight = weather.isNight,
                isSandstorm = weather.isSandstorm,
                fogDensity = weather.fogDensity
            ),
            playerSpawnX = 0f,
            playerSpawnZ = 24f,
            objectives = listOf(
                MissionObjectiveDef(
                    id = "def_core",
                    kind = ObjectiveKind.DEFEND_CORE,
                    title = LocalizedText("احمِ برج القيادة عبر 5 موجات", "Protect Command Core for 5 Waves"),
                    targetX = 0f,
                    targetZ = 18f,
                    radius = 5f,
                    requiredCount = 5
                )
            ),
            spawns = emptyList()
        )
    }

    /** Builds a playable MissionDefinition wrapper for a specific Challenge trial. */
    fun buildChallengeMissionDef(challenge: ChallengeDefinition): MissionDefinition {
        return MissionDefinition(
            number = 301,
            id = challenge.id,
            cityId = challenge.cityId,
            title = challenge.title,
            missionType = MissionType.URBAN_COMBAT,
            briefing = challenge.description,
            introAnnouncement = LocalizedText(
                "تحدٍ تكتيكي — ${challenge.title.ar}",
                "TACTICAL CHALLENGE — ${challenge.title.en}"
            ),
            xpReward = challenge.xpReward,
            atmosphere = AtmosphereOverride(
                isNight = challenge.weather.isNight,
                isSandstorm = challenge.weather.isSandstorm,
                fogDensity = challenge.weather.fogDensity
            ),
            playerSpawnX = 0f,
            playerSpawnZ = 28f,
            objectives = listOf(
                MissionObjectiveDef(
                    id = challenge.id,
                    kind = ObjectiveKind.ELIMINATE_HOSTILES,
                    title = challenge.title,
                    targetX = 0f,
                    targetZ = -10f,
                    radius = 6f,
                    requiredCount = challenge.targetKills
                )
            ),
            spawns = emptyList()
        )
    }

    /** Resolves default weather preset for a campaign mission based on its atmosphere/city. */
    fun weatherForCampaignMission(mission: MissionDefinition): WeatherCondition {
        return when {
            mission.atmosphere.isSandstorm -> WeatherCondition.SANDSTORM
            mission.atmosphere.isNight -> WeatherCondition.NIGHT
            mission.atmosphere.fogDensity >= 0.020f -> WeatherCondition.FOG
            mission.cityId == CityId.HARBOR_CITY && mission.number == 7 -> WeatherCondition.RAIN
            mission.cityId == CityId.MIRAGE_CITY && mission.number == 4 -> WeatherCondition.DUST
            mission.cityId == CityId.MOUNTAIN_CITY && mission.number == 10 -> WeatherCondition.SUNSET
            else -> WeatherCondition.CLEAR_DAY
        }
    }
}
