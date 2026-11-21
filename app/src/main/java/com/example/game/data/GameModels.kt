package com.example.game.data

enum class GameLanguage(val code: String, val isRtl: Boolean) {
    ARABIC("ar", true),
    ENGLISH("en", false)
}

enum class Difficulty(
    val id: String,
    val enemyHealthMult: Float,
    val enemyDamageMult: Float,
    val enemyAccuracy: Float,
    val enemyReactionTime: Float,
    val xpMultiplier: Float,
    val maxSimultaneousAttackers: Int
) {
    EASY("easy", 0.75f, 0.65f, 0.52f, 0.72f, 0.9f, 2),
    MEDIUM("medium", 1.0f, 1.0f, 0.72f, 0.45f, 1.0f, 3),
    HARD("hard", 1.35f, 1.35f, 0.88f, 0.28f, 1.35f, 4)
}

enum class GraphicsQuality(
    val id: String,
    val drawDistance: Float,
    val maxParticles: Int,
    val detailLevel: Int,
    val targetFps: Int
) {
    LOW("low", 85f, 40, 1, 30),
    MEDIUM("medium", 130f, 90, 2, 45),
    HIGH("high", 190f, 160, 3, 60)
}

enum class CrosshairStyle(val id: String) {
    DYNAMIC("dynamic"),
    TACTICAL_DOT("dot"),
    MILITARY_CROSS("cross")
}

enum class CameraShoulder(val id: String, val sign: Float) {
    RIGHT("right", 1f),
    LEFT("left", -1f)
}

enum class CityId(val id: String) {
    RIVER_CITY("river_city"),
    MIRAGE_CITY("mirage_city"),
    HARBOR_CITY("harbor_city"),
    MOUNTAIN_CITY("mountain_city")
}

enum class MissionType {
    TUTORIAL_COMBAT,
    URBAN_COMBAT,
    LOW_VISIBILITY,
    DESERT_PATROL,
    EQUIPMENT_RECOVERY,
    DEFENSIVE_SURVIVAL,
    OBJECTIVE_CAPTURE,
    RESCUE_EXTRACTION,
    BOSS_ENCOUNTER
}

enum class WeaponId(val id: String, val mobilityMultiplier: Float = 1.0f) {
    VX74_ASSAULT("vx74_assault", 0.96f),
    KSR9_SMG("ksr9_smg", 1.04f),
    SR308_PRECISION("sr308_precision", 0.88f),
    DMR14_MARKSMAN("dmr14_marksman", 0.92f),
    BR12_SHOTGUN("br12_shotgun", 0.98f),
    MG48_LMG("mg48_lmg", 0.82f),
    M9S_SIDEARM("m9s_sidearm", 1.08f),
    RAIL_X1_HEAVY("rail_x1_heavy", 0.78f)
}

enum class AttachmentSlot(val id: String) {
    OPTIC("optic"),
    GRIP("grip"),
    MAGAZINE("magazine"),
    BARREL("barrel"),
    STABILIZER("stabilizer"),
    SKIN("skin")
}

enum class CharacterSlot(val id: String) {
    OUTFIT("outfit"),
    HELMET("helmet"),
    VEST("vest"),
    GLOVES("gloves"),
    BOOTS("boots"),
    BACKPACK("backpack")
}

/** Combined legacy/HUD animation state enum (includes v1.1.0 FALL and VAULT). */
enum class PlayerAnimState {
    IDLE,
    WALK,
    RUN,
    SPRINT,
    CROUCH,
    AIM,
    SHOOT,
    RELOAD,
    TAKE_DAMAGE,
    DEATH,
    COVER,
    JUMP,
    FALL,
    LAND,
    VAULT
}

/** v1.1.0 Layered Lower-Body Locomotion State Machine. */
enum class LowerBodyAnimState {
    IDLE,
    WALK,
    RUN,
    SPRINT,
    CROUCH,
    COVER,
    JUMP,
    FALL,
    LAND,
    VAULT
}

/** v1.1.0 Layered Upper-Body Combat State Machine. */
enum class UpperBodyCombatState {
    HIP_FIRE,
    AIM,
    FIRE,
    RELOAD,
    WEAPON_SWITCH,
    HIT_REACTION,
    STAGGER,
    DEATH
}

/** v1.1.0 Enemy AI 2.0 14-State Tactical FSM. */
enum class EnemyAiState {
    IDLE,
    PATROL,
    SUSPICIOUS,
    INVESTIGATE,
    ALERT,
    SEARCH,
    TAKE_COVER,
    ENGAGE,
    RELOAD,
    REPOSITION,
    FLANK,
    RETREAT,
    REQUEST_SUPPORT,
    DEFEATED
}

/** v1.1.0 Squad Coordinator Tactical Role. */
enum class SquadRole {
    SQUAD_LEADER,
    STANDARD_ATTACKER,
    MOBILE_FLANKER,
    DEFENSIVE_UNIT,
    PRECISION_SUPPORT,
    HEAVY_UNIT
}

/** v1.1.0 CombatDirector Pacing Phases. */
enum class CombatPhase {
    QUIET,
    EXPLORATION,
    ALERT,
    COMBAT,
    RECOVERY,
    FINAL_ENCOUNTER
}

/** v1.1.0 Authoritative Damage Source taxonomy. */
enum class DamageSource {
    PLAYER_WEAPON,
    ENEMY_WEAPON,
    ENVIRONMENTAL_HAZARD,
    FALL_DAMAGE,
    MISSION_HAZARD
}

/** v2.1.0 Playable Game Modes. */
enum class GameMode(val id: String) {
    CAMPAIGN("campaign"),
    SURVIVAL("survival"),
    DEFENSE("defense"),
    CHALLENGES("challenges")
}

/** v2.1.0 Dynamic Weather & Time-of-Day presets with gameplay AI visibility scaling. */
enum class WeatherCondition(
    val id: String,
    val aiVisionMultiplier: Float,
    val fogDensity: Float,
    val particleDensity: Float,
    val isNight: Boolean = false,
    val isPrecipitation: Boolean = false,
    val isSandstorm: Boolean = false
) {
    CLEAR_DAY("clear_day", 1.0f, 0.0085f, 0.0f),
    SUNSET("sunset", 0.92f, 0.0115f, 0.15f),
    NIGHT("night", 0.68f, 0.0145f, 0.0f, isNight = true),
    RAIN("rain", 0.78f, 0.0175f, 1.0f, isPrecipitation = true),
    FOG("fog", 0.58f, 0.0320f, 0.35f),
    DUST("dust", 0.80f, 0.0185f, 0.55f),
    SANDSTORM("sandstorm", 0.52f, 0.0285f, 1.0f, isSandstorm = true)
}

/** v2.1.0 Challenge Mode medal ranking. */
enum class ChallengeMedal(val id: String, val tier: Int) {
    NONE("none", 0),
    BRONZE("bronze", 1),
    SILVER("silver", 2),
    GOLD("gold", 3)
}

/** v1.1.0 / v2.1.0 Interactive World Object kinds. */
enum class InteractiveKind {
    DOOR,
    GATE,
    SWITCH,
    AMMO_CONTAINER,
    SUPPLY_BOX,
    DESTRUCTIBLE_BARREL,
    OBJECTIVE_DEVICE,
    CHECKPOINT_MARKER,
    EXTRACTION_ZONE,
    VEHICLE
}

enum class EnemyArchetype(
    val id: String,
    val maxHealth: Float,
    val maxArmor: Float,
    val moveSpeed: Float,
    val fireRate: Float,
    val damage: Float,
    val preferredRange: Float,
    val magSize: Int,
    val squadRole: SquadRole = SquadRole.STANDARD_ATTACKER,
    val hasFrontalShield: Boolean = false,
    val canCoordinateSquad: Boolean = false,
    val isBoss: Boolean = false,
    val xpValue: Int = 60,
    val primaryColorRgb: FloatArray = floatArrayOf(0.32f, 0.36f, 0.28f),
    val accentColorRgb: FloatArray = floatArrayOf(0.75f, 0.25f, 0.20f)
) {
    STANDARD_INFANTRY(
        id = "standard", maxHealth = 95f, maxArmor = 20f, moveSpeed = 3.9f,
        fireRate = 3.2f, damage = 9f, preferredRange = 18f, magSize = 24,
        squadRole = SquadRole.STANDARD_ATTACKER, xpValue = 55,
        primaryColorRgb = floatArrayOf(0.35f, 0.38f, 0.31f), accentColorRgb = floatArrayOf(0.75f, 0.30f, 0.22f)
    ),
    FAST_ASSAULT(
        id = "assault", maxHealth = 85f, maxArmor = 15f, moveSpeed = 5.4f,
        fireRate = 5.5f, damage = 7.5f, preferredRange = 12f, magSize = 30,
        squadRole = SquadRole.MOBILE_FLANKER, xpValue = 70,
        primaryColorRgb = floatArrayOf(0.24f, 0.28f, 0.32f), accentColorRgb = floatArrayOf(0.92f, 0.52f, 0.18f)
    ),
    ARMORED_HEAVY(
        id = "heavy", maxHealth = 190f, maxArmor = 110f, moveSpeed = 2.6f,
        fireRate = 4.5f, damage = 12f, preferredRange = 16f, magSize = 48,
        squadRole = SquadRole.HEAVY_UNIT, xpValue = 130,
        primaryColorRgb = floatArrayOf(0.20f, 0.22f, 0.25f), accentColorRgb = floatArrayOf(0.85f, 0.18f, 0.18f)
    ),
    PRECISION_MARKSMAN(
        id = "marksman", maxHealth = 80f, maxArmor = 15f, moveSpeed = 3.4f,
        fireRate = 0.85f, damage = 26f, preferredRange = 36f, magSize = 6,
        squadRole = SquadRole.PRECISION_SUPPORT, xpValue = 95,
        primaryColorRgb = floatArrayOf(0.46f, 0.42f, 0.34f), accentColorRgb = floatArrayOf(0.95f, 0.82f, 0.25f)
    ),
    SHIELD_DEFENDER(
        id = "shield", maxHealth = 135f, maxArmor = 60f, moveSpeed = 2.9f,
        fireRate = 2.4f, damage = 10f, preferredRange = 11f, magSize = 15,
        squadRole = SquadRole.DEFENSIVE_UNIT, hasFrontalShield = true, xpValue = 115,
        primaryColorRgb = floatArrayOf(0.18f, 0.25f, 0.33f), accentColorRgb = floatArrayOf(0.32f, 0.68f, 0.90f)
    ),
    CLOSE_AGGRESSOR(
        id = "aggressor", maxHealth = 115f, maxArmor = 30f, moveSpeed = 5.1f,
        fireRate = 1.4f, damage = 18f, preferredRange = 8f, magSize = 8,
        squadRole = SquadRole.MOBILE_FLANKER, xpValue = 90,
        primaryColorRgb = floatArrayOf(0.42f, 0.24f, 0.20f), accentColorRgb = floatArrayOf(0.95f, 0.35f, 0.18f)
    ),
    SQUAD_COORDINATOR(
        id = "coordinator", maxHealth = 125f, maxArmor = 45f, moveSpeed = 3.7f,
        fireRate = 3.5f, damage = 11f, preferredRange = 22f, magSize = 26,
        squadRole = SquadRole.SQUAD_LEADER, canCoordinateSquad = true, xpValue = 125,
        primaryColorRgb = floatArrayOf(0.28f, 0.32f, 0.22f), accentColorRgb = floatArrayOf(0.95f, 0.72f, 0.18f)
    ),
    ELITE_BOSS(
        id = "elite_boss", maxHealth = 420f, maxArmor = 220f, moveSpeed = 3.4f,
        fireRate = 5.2f, damage = 16f, preferredRange = 18f, magSize = 60,
        squadRole = SquadRole.SQUAD_LEADER, canCoordinateSquad = true, isBoss = true, xpValue = 380,
        primaryColorRgb = floatArrayOf(0.14f, 0.14f, 0.17f), accentColorRgb = floatArrayOf(0.96f, 0.22f, 0.16f)
    )
}

enum class HitZone(val damageMultiplier: Float) {
    HEAD(2.0f),
    TORSO(1.0f),
    LIMB(0.72f),
    SHIELD(0.08f)
}

enum class SurfaceMaterial {
    ASPHALT,
    CONCRETE,
    BRICK,
    SAND,
    METAL,
    WOOD,
    ROCK,
    WATER,
    SANDBAG,
    VEHICLE,
    GLASS
}

data class LocalizedText(
    val ar: String,
    val en: String
) {
    fun get(language: GameLanguage): String = if (language == GameLanguage.ARABIC) ar else en
}
