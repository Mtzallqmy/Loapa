package com.example.game.save

import com.example.game.data.AttachmentSlot
import com.example.game.data.CameraShoulder
import com.example.game.data.CharacterSlot
import com.example.game.data.CrosshairStyle
import com.example.game.data.CustomizationCatalog
import com.example.game.data.Difficulty
import com.example.game.data.GameLanguage
import com.example.game.data.GraphicsQuality
import com.example.game.data.WeaponCatalog
import com.example.game.data.WeaponId

import com.example.game.data.ChallengeMedal
import com.example.game.data.CityId

const val SAVE_SCHEMA_VERSION = 4

data class ControlOffset(
    val dx: Float = 0f, // normalized offset -0.25..0.25
    val dy: Float = 0f
)

data class ControlLayoutSettings(
    val leftHanded: Boolean = false,
    val buttonScale: Float = 1.0f,
    val buttonOpacity: Float = 0.84f,
    val joystickScale: Float = 1.0f,
    val joystickDeadzone: Float = 0.12f,
    val fireOffset: ControlOffset = ControlOffset(),
    val aimOffset: ControlOffset = ControlOffset(),
    val reloadOffset: ControlOffset = ControlOffset(),
    val jumpOffset: ControlOffset = ControlOffset(),
    val crouchOffset: ControlOffset = ControlOffset(),
    val coverOffset: ControlOffset = ControlOffset()
)

data class GameSettings(
    val language: GameLanguage = GameLanguage.ARABIC,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.MEDIUM,
    val masterVolume: Float = 0.85f,
    val sfxVolume: Float = 0.90f,
    val musicVolume: Float = 0.45f,
    val ambientVolume: Float = 0.65f,
    val cameraSensitivity: Float = 1.0f,
    val aimSensitivity: Float = 0.65f,
    val invertY: Boolean = false,
    val aimAssist: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val crosshairStyle: CrosshairStyle = CrosshairStyle.DYNAMIC,
    val subtitlesEnabled: Boolean = true,
    val textScale: Float = 1.0f,
    val reduceScreenShake: Boolean = false,
    val reduceIntenseEffects: Boolean = false,
    val cameraShoulder: CameraShoulder = CameraShoulder.RIGHT,
    val controls: ControlLayoutSettings = ControlLayoutSettings()
)

data class SavedCheckpoint(
    val missionNumber: Int = 1,
    val objectiveIndex: Int = 0,
    val playerX: Float = 0f,
    val playerZ: Float = 32f,
    val health: Float = 100f,
    val armor: Float = 100f,
    val ammoInMag: Int = 30,
    val ammoReserve: Int = 150
)

data class PlayerSaveData(
    val schemaVersion: Int = SAVE_SCHEMA_VERSION,
    val highestUnlockedMission: Int = 1,
    val lastPlayedMission: Int = 1,
    val completedMissions: Set<Int> = emptySet(),
    val missionBestStars: Map<Int, Int> = emptyMap(),
    val claimedMissionRewards: Set<Int> = emptySet(),
    val xp: Int = 0,
    val totalKills: Int = 0,
    val totalHeadshots: Int = 0,
    val totalShotsFired: Int = 0,
    val totalShotsHit: Int = 0,
    val weaponKills: Map<WeaponId, Int> = emptyMap(),
    val unlockedWeapons: Set<WeaponId> = setOf(
        WeaponId.VX74_ASSAULT,
        WeaponId.KSR9_SMG,
        WeaponId.SR308_PRECISION,
        WeaponId.M9S_SIDEARM
    ),
    val equippedPrimary: WeaponId = WeaponId.VX74_ASSAULT,
    val equippedSecondary: WeaponId = WeaponId.KSR9_SMG,
    val equippedSidearm: WeaponId = WeaponId.M9S_SIDEARM,
    val unlockedAttachments: Set<String> = WeaponCatalog.defaultAttachments().values.toSet(),
    val weaponAttachments: Map<WeaponId, Map<AttachmentSlot, String>> = emptyMap(),
    val unlockedCharacterGear: Set<String> = CustomizationCatalog.defaultGear().values.toSet() + setOf("pack_none"),
    val equippedCharacterGear: Map<CharacterSlot, String> = CustomizationCatalog.defaultGear(),
    val unlockedAchievements: Set<String> = emptySet(),
    val lastCheckpoint: SavedCheckpoint? = null,
    // v2.1.0 Multi-Mode Persistence Records
    val survivalBestWaves: Map<CityId, Int> = emptyMap(),
    val survivalBestScore: Int = 0,
    val defenseBestWaves: Map<CityId, Int> = emptyMap(),
    val defenseVictories: Int = 0,
    val challengeBestScores: Map<String, Int> = emptyMap(),
    val challengeMedals: Map<String, ChallengeMedal> = emptyMap(),
    val completedChallenges: Set<String> = emptySet(),
    val settings: GameSettings = GameSettings()
) {
    val rankLevel: Int get() = 1 + (xp / 350)
    val rankXpProgress: Float get() = (xp % 350) / 350f
    val accuracyPercent: Int
        get() = if (totalShotsFired <= 0) 0 else ((totalShotsHit.toFloat() / totalShotsFired) * 100f).toInt().coerceIn(0, 100)

    fun attachmentsFor(weaponId: WeaponId): Map<AttachmentSlot, String> {
        val base = WeaponCatalog.defaultAttachments().toMutableMap()
        weaponAttachments[weaponId]?.forEach { (slot, id) -> base[slot] = id }
        return base
    }
}
