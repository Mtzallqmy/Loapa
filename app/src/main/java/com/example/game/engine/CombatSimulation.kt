package com.example.game.engine

import com.example.game.data.CameraShoulder
import com.example.game.data.ChallengeDefinition
import com.example.game.data.ChallengeMedal
import com.example.game.data.CharacterAppearance
import com.example.game.data.CityId
import com.example.game.data.CombatPhase
import com.example.game.data.CustomizationCatalog
import com.example.game.data.DamageSource
import com.example.game.data.Difficulty
import com.example.game.data.EffectiveWeaponStats
import com.example.game.data.EnemyArchetype
import com.example.game.data.GameLanguage
import com.example.game.data.GameMode
import com.example.game.data.GameModeCatalog
import com.example.game.data.HitZone
import com.example.game.data.InteractiveKind
import com.example.game.data.LocalizedText
import com.example.game.data.LowerBodyAnimState
import com.example.game.data.MissionDefinition
import com.example.game.data.MissionObjectiveDef
import com.example.game.data.ObjectiveKind
import com.example.game.data.PlayerAnimState
import com.example.game.data.SurfaceMaterial
import com.example.game.data.UpperBodyCombatState
import com.example.game.data.WeaponCatalog
import com.example.game.data.WeaponId
import com.example.game.data.WeatherCondition
import com.example.game.localization.GameStrings
import com.example.game.save.PlayerSaveData
import com.example.game.save.SavedCheckpoint
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

data class BulletTracer(
    val start: Vec3,
    val end: Vec3,
    val colorRgb: FloatArray,
    var life: Float = 0.085f
)

data class ImpactParticle(
    var position: Vec3,
    var velocity: Vec3,
    val colorRgb: FloatArray,
    var size: Float,
    var life: Float,
    val maxLife: Float
)

sealed class SimAudioEvent {
    data class WeaponFired(val weaponId: WeaponId) : SimAudioEvent()
    data object Reload : SimAudioEvent()
    data class Footstep(val material: SurfaceMaterial) : SimAudioEvent()
    data class HitConfirm(val headshot: Boolean) : SimAudioEvent()
    data class Impact(val material: SurfaceMaterial) : SimAudioEvent()
    data object PlayerDamaged : SimAudioEvent()
    data object Checkpoint : SimAudioEvent()
    data object ObjectiveComplete : SimAudioEvent()
    data object ReinforcementsAlert : SimAudioEvent()
    data object MissionVictory : SimAudioEvent()
    data object MissionDefeat : SimAudioEvent()
    data object VehicleTurretFired : SimAudioEvent()
    data class WeatherPulse(val isRain: Boolean) : SimAudioEvent()
    data object CoreDamaged : SimAudioEvent()
}

enum class MissionRunStatus {
    INTRO,
    ACTIVE,
    PAUSED,
    VICTORY,
    DEFEAT
}

data class MissionSummary(
    val missionNumber: Int,
    val victory: Boolean,
    val kills: Int,
    val headshots: Int,
    val shotsFired: Int,
    val shotsHit: Int,
    val remainingHealth: Float,
    val elapsedSeconds: Float,
    val stars: Int,
    val xpEarned: Int,
    val weaponKills: Map<WeaponId, Int>,
    val gameMode: GameMode = GameMode.CAMPAIGN,
    val cityId: CityId = CityId.RIVER_CITY,
    val waveReached: Int = 1,
    val score: Int = 0,
    val challengeId: String? = null,
    val challengeMedal: ChallengeMedal = ChallengeMedal.NONE
)

data class HudSnapshot(
    val status: MissionRunStatus,
    val missionNumber: Int,
    val health: Float,
    val maxHealth: Float,
    val armor: Float,
    val maxArmor: Float,
    val stamina: Float,
    val weaponId: WeaponId,
    val ammoInMag: Int,
    val magCapacity: Int,
    val ammoReserve: Int,
    val isReloading: Boolean,
    val reloadProgress: Float,
    val isAiming: Boolean,
    val isCrouching: Boolean,
    val isSprinting: Boolean,
    val isInCover: Boolean,
    val canEnterCover: Boolean,
    val canVault: Boolean,
    val isVaulting: Boolean,
    val interactionPrompt: String?,
    val cameraShoulder: CameraShoulder,
    val combatPhase: CombatPhase,
    val respawnProtected: Boolean,
    val animState: PlayerAnimState,
    val lowerBodyState: LowerBodyAnimState,
    val upperBodyState: UpperBodyCombatState,
    val currentObjectiveIndex: Int,
    val totalObjectives: Int,
    val objectiveTitle: String,
    val objectiveProgressText: String,
    val objectiveDistanceMeters: Int,
    val activeEnemiesCount: Int,
    val hitMarkerAlpha: Float,
    val hitMarkerHeadshot: Boolean,
    val damageFlashAlpha: Float,
    val damageDirectionRad: Float,
    val bannerMessage: String?,
    val subtitleText: String?,
    val bossHealthFraction: Float?,
    val playerPos: Vec3,
    val playerYaw: Float,
    val radarEnemies: List<Pair<Float, Float>>, // relative x, z in meters
    val radarObjective: Pair<Float, Float>?,
    val fps: Int,
    // v2.1.0 Multi-Mode, Weather & Vehicle Telemetry
    val gameMode: GameMode = GameMode.CAMPAIGN,
    val weather: WeatherCondition = WeatherCondition.CLEAR_DAY,
    val waveNumber: Int = 1,
    val score: Int = 0,
    val defenseCoreHealth: Float = 0f,
    val defenseCoreMaxHealth: Float = 400f,
    val challengeTimeRemaining: Float = 0f,
    val inVehicle: Boolean = false,
    val vehicleHealth: Float = 280f,
    val vehicleMaxHealth: Float = 280f
)

/**
 * v2.1.0 Total Battlefield Simulation Engine.
 * Supports all 4 playable modes (Campaign, Survival, Defense, Challenges),
 * Dynamic Weather with AI vision scaling, Playable Tactical Armored Rover,
 * PerformanceManager adaptive budgets, Enemy AI 2.0, and Interactive Worlds.
 */
class CombatSimulation(
    val mission: MissionDefinition,
    saveData: PlayerSaveData,
    resumeCheckpoint: SavedCheckpoint? = null,
    seed: Long = 2026L,
    val mode: GameMode = GameMode.CAMPAIGN,
    val weatherCondition: WeatherCondition = GameModeCatalog.weatherForCampaignMission(mission),
    val challenge: ChallengeDefinition? = null
) {
    val city: CityMap = CityBuilder.buildCity(mission.cityId)
    val difficulty: Difficulty = saveData.settings.difficulty
    val appearance: CharacterAppearance = CustomizationCatalog.resolveAppearance(saveData.equippedCharacterGear)
    val reduceShake: Boolean = saveData.settings.reduceScreenShake
    val reduceEffects: Boolean = saveData.settings.reduceIntenseEffects
    val aimAssistEnabled: Boolean = saveData.settings.aimAssist
    val maxParticles: Int = saveData.settings.graphicsQuality.maxParticles
    val language: GameLanguage = saveData.settings.language

    // Unified Authoritative Subsystems
    val eventBus = GameplayEventBus()
    val damageSystem = DamageSystem(eventBus)
    val interactionManager = WorldInteractionManager(city.interactiveObjects.toMutableList(), eventBus)
    val squadCoordinator = SquadCoordinator(difficulty)
    val combatDirector = CombatDirector(eventBus)
    val cameraDirector = CameraDirector(saveData.settings.cameraShoulder)
    val weatherSystem = WeatherSystem(weatherCondition, reduceEffects, seed + 17L)
    val performanceManager = PerformanceManager(saveData.settings.graphicsQuality)
    val vehicle = TacticalVehicle("rover_${mission.cityId.id}", city.vehicleSpawnPos, initialYaw = 0f)
    private val vehicleInteractObj: InteractiveObject

    val simLock = Any()
    private val rng = Random(seed)
    val player: PlayerController
    val enemies: MutableList<EnemyUnit> = mutableListOf()
    val tracers: MutableList<BulletTracer> = mutableListOf()
    val particles: MutableList<ImpactParticle> = mutableListOf()
    val pendingAudio: MutableList<SimAudioEvent> = mutableListOf()

    var status: MissionRunStatus = MissionRunStatus.INTRO
    var introTimer: Float = 2.4f
    var elapsedSeconds: Float = 0f
    var currentObjectiveIndex: Int = 0
    private var objectiveZoneTimer: Float = 0f
    private var objectiveDeviceActivatedThisPhase: Boolean = false
    private var reinforcementWaveSpawned: Boolean = false
    private var nextEnemyId: Int = 1

    // v2.1.0 Multi-Mode Runtime State
    var waveNumber: Int = 1
    var score: Int = 0
    val defenseCorePosition: Vec3 = Vec3(0f, 0f, 18f)
    val defenseCoreMaxHealth: Float = 400f
    var defenseCoreHealth: Float = defenseCoreMaxHealth
    val defenseTotalWaves: Int = 5
    var challengeTimeRemaining: Float = challenge?.timeLimitSeconds ?: 0f
    private var nextWaveDelayTimer: Float = 0f
    private var coreAlertCooldown: Float = 0f

    var missionKills: Int = 0
    var missionHeadshots: Int = 0
    var missionShotsFired: Int = 0
    var missionShotsHit: Int = 0
    val missionWeaponKills: MutableMap<WeaponId, Int> = mutableMapOf()

    var hitMarkerTimer: Float = 0f
    var hitMarkerHeadshot: Boolean = false
    var bannerText: String? = null
    var bannerTimer: Float = 0f
    var subtitleText: String? = null
    var subtitleTimer: Float = 0f
    var latestCheckpoint: SavedCheckpoint
    var smoothedFps: Int = 60

    init {
        val loadout = listOf(
            saveData.equippedPrimary,
            saveData.equippedSecondary,
            saveData.equippedSidearm
        ).distinct().map { wId ->
            WeaponCatalog.computeEffectiveStats(wId, saveData.attachmentsFor(wId))
        }
        val spawn = Vec3(mission.playerSpawnX, 0f, mission.playerSpawnZ)
        player = PlayerController(spawn, loadout)

        // Register parked Tactical Armored Rover as interactive world object
        vehicleInteractObj = InteractiveObject(
            id = vehicle.id,
            kind = InteractiveKind.VEHICLE,
            position = vehicle.position,
            interactRadius = 3.5f,
            label = LocalizedText("المدرّعة التكتيكية", "Tactical Rover")
        )
        interactionManager.objects.add(vehicleInteractObj)

        latestCheckpoint = SavedCheckpoint(
            missionNumber = mission.number.coerceIn(1, 12),
            objectiveIndex = 0,
            playerX = spawn.x,
            playerZ = spawn.z,
            health = 100f,
            armor = 100f,
            ammoInMag = player.activeWeapon.ammoInMag,
            ammoReserve = player.activeWeapon.ammoReserve
        )

        when (mode) {
            GameMode.CAMPAIGN -> {
                spawnEnemyWave(0, enforceSafeDistance = false)
                eventBus.publish(GameplayEvent.ObjectiveStarted(mission.number, 0))
            }
            GameMode.SURVIVAL -> spawnDynamicModeWave(wave = 1, isDefense = false)
            GameMode.DEFENSE -> spawnDynamicModeWave(wave = 1, isDefense = true)
            GameMode.CHALLENGES -> spawnChallengeWave()
        }

        if (mode == GameMode.CAMPAIGN && resumeCheckpoint != null && resumeCheckpoint.missionNumber == mission.number) {
            restoreFromCheckpoint(resumeCheckpoint)
        } else {
            showBanner(mission.introAnnouncement.get(language), 2.8f)
            showSubtitle(mission.briefing.get(language), 4.2f)
        }
    }

    val currentObjective: MissionObjectiveDef?
        get() = mission.objectives.getOrNull(currentObjectiveIndex)

    fun skipIntro() {
        if (status == MissionRunStatus.INTRO) {
            status = MissionRunStatus.ACTIVE
            introTimer = 0f
        }
    }

    fun exitVehicleNow() {
        if (vehicle.isOccupied) {
            vehicle.exit(player, city)
            vehicleInteractObj.isActivated = false
            vehicleInteractObj.position = vehicle.position
        }
    }

    fun restoreFromCheckpoint(cp: SavedCheckpoint = latestCheckpoint) {
        if (vehicle.isOccupied) exitVehicleNow()
        currentObjectiveIndex = cp.objectiveIndex.coerceIn(0, mission.objectives.lastIndex.coerceAtLeast(0))
        player.resetToCheckpoint(Vec3(cp.playerX, 0f, cp.playerZ), cp.health, cp.armor, cp.ammoInMag, cp.ammoReserve)
        squadCoordinator.clear()
        status = MissionRunStatus.ACTIVE
        showBanner(GameStrings.hudCheckpointReached(language), 2.0f)
    }

    private fun spawnEnemyWave(waveIndex: Int, enforceSafeDistance: Boolean) {
        var spawned = 0
        val specs = mission.spawns.filter { it.waveIndex == waveIndex }
        for (spec in specs) {
            val aliveCount = enemies.count { it.isAlive }
            if (aliveCount >= combatDirector.maxActiveEnemies && !spec.archetype.isBoss) continue
            val rawPos = Vec3(spec.x, 0f, spec.z)
            val safePos = combatDirector.resolveSafeSpawnPosition(rawPos, player.position, city, enforceSafeDistance)
            val pB = Vec3(
                (safePos.x + spec.patrolOffsetX).coerceIn(city.minBoundX + 1f, city.maxBoundX - 1f),
                0f,
                (safePos.z + spec.patrolOffsetZ).coerceIn(city.minBoundZ + 1f, city.maxBoundZ - 1f)
            )
            enemies.add(EnemyUnit(nextEnemyId++, spec.archetype, safePos, safePos, pB, difficulty))
            spawned++
        }
        combatDirector.recordWaveSpawned(spawned)
    }

    /** Spawns escalating tactical squads for Survival & Defense modes. */
    private fun spawnDynamicModeWave(wave: Int, isDefense: Boolean) {
        val count = (3 + wave).coerceAtMost(8)
        val spawnAnchors = listOf(
            Vec3(-10f, 0f, -26f), Vec3(10f, 0f, -26f), Vec3(0f, 0f, -30f),
            Vec3(-12f, 0f, -10f), Vec3(12f, 0f, -10f), Vec3(-8f, 0f, 2f),
            Vec3(8f, 0f, 2f), Vec3(0f, 0f, -18f)
        )
        var spawned = 0
        for (i in 0 until count) {
            val archetype = when {
                wave >= 5 && i == 0 -> EnemyArchetype.ARMORED_HEAVY
                wave >= 4 && i == 1 -> EnemyArchetype.SQUAD_COORDINATOR
                wave >= 3 && i % 4 == 2 -> EnemyArchetype.SHIELD_DEFENDER
                wave >= 2 && i % 3 == 1 -> EnemyArchetype.FAST_ASSAULT
                i % 5 == 4 -> EnemyArchetype.PRECISION_MARKSMAN
                else -> EnemyArchetype.STANDARD_INFANTRY
            }
            val raw = spawnAnchors[(i + wave) % spawnAnchors.size]
            val safePos = combatDirector.resolveSafeSpawnPosition(raw, player.position, city, enforceSafeDistance = true)
            val targetPos = if (isDefense) defenseCorePosition else player.position
            val unit = EnemyUnit(nextEnemyId++, archetype, safePos, safePos, targetPos, difficulty)
            unit.alertTo(targetPos, highPriority = true)
            enemies.add(unit)
            spawned++
        }
        combatDirector.recordWaveSpawned(spawned)
    }

    /** Spawns enemies for the active Challenge definition. */
    private fun spawnChallengeWave() {
        val ch = challenge ?: return
        val totalToSpawn = ch.targetKills.coerceIn(6, 12)
        val anchors = listOf(
            Vec3(-8f, 0f, 4f), Vec3(8f, 0f, 4f), Vec3(-10f, 0f, -6f), Vec3(10f, 0f, -6f),
            Vec3(0f, 0f, -14f), Vec3(-9f, 0f, -20f), Vec3(9f, 0f, -20f), Vec3(0f, 0f, -28f)
        )
        for (i in 0 until totalToSpawn) {
            val archetype = when (ch.rule) {
                com.example.game.data.ChallengeRule.HEADSHOT_PRECISION ->
                    if (i % 3 == 0) EnemyArchetype.PRECISION_MARKSMAN else EnemyArchetype.STANDARD_INFANTRY
                com.example.game.data.ChallengeRule.TIMED_BLITZ ->
                    if (i % 2 == 0) EnemyArchetype.FAST_ASSAULT else EnemyArchetype.CLOSE_AGGRESSOR
                com.example.game.data.ChallengeRule.IRONCLAD_SURVIVAL ->
                    if (i % 3 == 0) EnemyArchetype.ARMORED_HEAVY else EnemyArchetype.SHIELD_DEFENDER
                com.example.game.data.ChallengeRule.ELITE_GAUNTLET ->
                    when (i % 4) {
                        0 -> EnemyArchetype.SQUAD_COORDINATOR
                        1 -> EnemyArchetype.ARMORED_HEAVY
                        2 -> EnemyArchetype.FAST_ASSAULT
                        else -> EnemyArchetype.PRECISION_MARKSMAN
                    }
            }
            val raw = anchors[i % anchors.size] + Vec3((i % 3 - 1) * 1.4f, 0f, 0f)
            val safePos = combatDirector.resolveSafeSpawnPosition(raw, player.position, city, enforceSafeDistance = true)
            val unit = EnemyUnit(nextEnemyId++, archetype, safePos, safePos, player.position, difficulty)
            unit.alertTo(player.position, highPriority = false)
            enemies.add(unit)
        }
    }

    fun showBanner(text: String, duration: Float = 2.4f) {
        bannerText = text
        bannerTimer = duration
    }

    fun showSubtitle(text: String, duration: Float = 3.5f) {
        subtitleText = text
        subtitleTimer = duration
    }

    fun drainAudioEvents(): List<SimAudioEvent> {
        if (pendingAudio.isEmpty()) return emptyList()
        val copy = pendingAudio.toList()
        pendingAudio.clear()
        return copy
    }

    /** Advances unified simulation by dt seconds (clamped for physics stability). */
    fun step(rawDt: Float, input: PlayerInputState) {
        val dt = rawDt.coerceIn(0.001f, 0.05f)
        if (dt > 0f) {
            val instFps = (1f / dt).toInt().coerceIn(10, 120)
            smoothedFps = ((smoothedFps * 9) + instFps) / 10
        }

        if (bannerTimer > 0f) {
            bannerTimer -= dt
            if (bannerTimer <= 0f) bannerText = null
        }
        if (subtitleTimer > 0f) {
            subtitleTimer -= dt
            if (subtitleTimer <= 0f) subtitleText = null
        }
        if (hitMarkerTimer > 0f) hitMarkerTimer = max(0f, hitMarkerTimer - dt)
        if (coreAlertCooldown > 0f) coreAlertCooldown = max(0f, coreAlertCooldown - dt)

        // Update weather particles even during intro for immediate visual atmosphere
        val weatherAudioPulse = weatherSystem.step(
            dt = dt,
            anchorPos = player.position,
            maxWeatherParticles = performanceManager.effectiveMaxWeatherParticles
        )
        if (weatherAudioPulse && status == MissionRunStatus.ACTIVE) {
            pendingAudio.add(SimAudioEvent.WeatherPulse(isRain = weatherCondition.isPrecipitation))
        }

        if (status == MissionRunStatus.INTRO) {
            introTimer -= dt
            if (introTimer <= 0f) status = MissionRunStatus.ACTIVE
            cameraDirector.step(dt, player, inVehicle = vehicle.isOccupied)
            return
        }
        if (status != MissionRunStatus.ACTIVE) return

        elapsedSeconds += dt
        interactionManager.tick(dt)

        if (input.toggleShoulderPressed && !vehicle.isOccupied) {
            cameraDirector.toggleShoulder()
        }

        // 1. Player or Vehicle Control Step
        if (vehicle.isOccupied) {
            if (input.interactPressed) {
                exitVehicleNow()
            } else {
                vehicle.step(dt, input, player, city)
                vehicleInteractObj.position = vehicle.position
                if (input.fireHeld && vehicle.tryFireTurret()) {
                    fireVehicleTurret()
                }
            }
        } else {
            val wasReloading = player.isReloading
            val emitFootstep = player.step(dt, input, city, reduceShake)
            if (emitFootstep) pendingAudio.add(SimAudioEvent.Footstep(city.groundMaterial))
            if (!wasReloading && player.isReloading) pendingAudio.add(SimAudioEvent.Reload)
            player.justCompletedReloadWeapon?.let { wId ->
                eventBus.publish(
                    GameplayEvent.WeaponReloaded(wId, player.activeWeapon.ammoInMag, player.activeWeapon.ammoReserve)
                )
            }
            if (player.pendingFallDamage > 0f) {
                damageSystem.applyDamageToPlayer(
                    player = player,
                    rawAmount = player.pendingFallDamage,
                    source = DamageSource.FALL_DAMAGE,
                    sourcePos = player.position,
                    reduceShake = reduceShake
                )
                pendingAudio.add(SimAudioEvent.PlayerDamaged)
            }

            // Handle Interactive World Objects (including boarding Tactical Rover)
            if (input.interactPressed) {
                val outcome = interactionManager.tryInteract(player, city)
                outcome.message?.let { showBanner(it.get(language), 2.3f) }
                if (outcome.resupplied) pendingAudio.add(SimAudioEvent.Reload)
                if (outcome.objectiveDeviceActivated) {
                    objectiveDeviceActivatedThisPhase = true
                    pendingAudio.add(SimAudioEvent.ObjectiveComplete)
                }
                if (outcome.triggeredBarrelBlast && outcome.obj != null) {
                    triggerBarrelDetonation(outcome.obj.position)
                }
                if (outcome.boardedVehicle && vehicle.canBoard) {
                    vehicle.board(player)
                    vehicleInteractObj.isActivated = true
                    showBanner(GameStrings.hudVehicleBoarded(language), 2.2f)
                }
            }

            // Legacy proximity resupply stations
            for (station in city.resupplyStations) {
                if (!station.used && player.position.distanceXZ(station.position) < 2.6f) {
                    station.used = true
                    player.resupplyAll()
                    eventBus.publish(GameplayEvent.PlayerHealed(35f, player.maxArmor))
                    pendingAudio.add(SimAudioEvent.Reload)
                    showBanner(GameStrings.hudAmmoResupplied(language), 2.2f)
                }
            }

            // Handle Player Firing
            if (input.fireHeld) {
                val hadZeroAmmo = player.activeWeapon.ammoInMag == 0 && player.activeWeapon.ammoReserve == 0
                if (player.tryFire(reduceShake)) {
                    firePlayerWeapon()
                } else if (hadZeroAmmo) {
                    eventBus.publish(GameplayEvent.AmmoDepleted(player.activeWeaponId))
                }
            }
        }

        cameraDirector.step(dt, player, inVehicle = vehicle.isOccupied)

        // 2. Step Enemies (AI 2.0 + Weather Vision Multiplier + Defense Core Target)
        val playerEye = Vec3(player.position.x, player.position.y + player.eyeHeight, player.position.z)
        val corePosForAi = if (mode == GameMode.DEFENSE) defenseCorePosition else null
        for (i in enemies.indices) {
            val enemy = enemies[i]
            val fired = enemy.step(
                dt = dt,
                playerPos = player.position,
                playerEyePos = playerEye,
                playerAlive = player.isAlive,
                city = city,
                difficulty = difficulty,
                allies = enemies,
                squadCoordinator = squadCoordinator,
                eventBus = eventBus,
                simTime = elapsedSeconds,
                rng = rng,
                weatherVisionMult = weatherSystem.aiVisionMultiplier,
                defenseCorePos = corePosForAi
            )
            if (fired) {
                if (enemy.lastShotTargetedCore && mode == GameMode.DEFENSE) {
                    resolveEnemyShotAtCore(enemy)
                } else {
                    resolveEnemyShot(enemy, playerEye)
                }
            }
        }

        // 3. Update CombatDirector Pacing
        combatDirector.updatePacing(
            dt = dt,
            enemies = enemies,
            player = player,
            isFinalObjective = currentObjectiveIndex == mission.objectives.lastIndex
        )

        // 4. Update Tracers & Impact Particles (bounded by PerformanceManager)
        val tIt = tracers.iterator()
        while (tIt.hasNext()) {
            val tr = tIt.next()
            tr.life -= dt
            if (tr.life <= 0f) tIt.remove()
        }
        val pIt = particles.iterator()
        while (pIt.hasNext()) {
            val p = pIt.next()
            p.life -= dt
            if (p.life <= 0f) {
                pIt.remove()
            } else {
                p.position = p.position + p.velocity * dt
                p.velocity = Vec3(p.velocity.x * 0.94f, p.velocity.y - 9.8f * dt, p.velocity.z * 0.94f)
                if (p.position.y < 0.02f) {
                    p.position = p.position.copy(y = 0.02f)
                    p.velocity = Vec3(p.velocity.x * 0.4f, -p.velocity.y * 0.2f, p.velocity.z * 0.4f)
                }
            }
        }

        // 5. Evaluate Defeat & Mode-Specific Progression
        if (!player.isAlive) {
            if (vehicle.isOccupied) exitVehicleNow()
            status = MissionRunStatus.DEFEAT
            pendingAudio.add(SimAudioEvent.MissionDefeat)
            eventBus.publish(GameplayEvent.MissionFailed(mission.number, "player_eliminated"))
            return
        }

        when (mode) {
            GameMode.CAMPAIGN -> evaluateObjectives(dt)
            GameMode.SURVIVAL -> evaluateSurvivalMode(dt)
            GameMode.DEFENSE -> evaluateDefenseMode(dt)
            GameMode.CHALLENGES -> evaluateChallengeMode(dt)
        }
    }

    private fun evaluateSurvivalMode(dt: Float) {
        val alive = enemies.count { it.isAlive }
        if (alive == 0) {
            if (nextWaveDelayTimer <= 0f) {
                nextWaveDelayTimer = 2.6f
                score += 200 * waveNumber
                player.resupplyAll()
                pendingAudio.add(SimAudioEvent.Checkpoint)
                showBanner(
                    if (language == GameLanguage.ARABIC) "انتهت الموجة $waveNumber — إمدادات جديدة!"
                    else "WAVE $waveNumber CLEARED — RESUPPLY RECEIVED!",
                    2.4f
                )
            } else {
                nextWaveDelayTimer -= dt
                if (nextWaveDelayTimer <= 0f) {
                    waveNumber += 1
                    spawnDynamicModeWave(wave = waveNumber, isDefense = false)
                    pendingAudio.add(SimAudioEvent.ReinforcementsAlert)
                    showBanner(GameStrings.hudWave(language, waveNumber), 2.2f)
                }
            }
        }
    }

    private fun evaluateDefenseMode(dt: Float) {
        if (defenseCoreHealth <= 0f) {
            status = MissionRunStatus.DEFEAT
            pendingAudio.add(SimAudioEvent.MissionDefeat)
            eventBus.publish(GameplayEvent.MissionFailed(mission.number, "defense_core_destroyed"))
            return
        }
        val alive = enemies.count { it.isAlive }
        if (alive == 0) {
            if (waveNumber >= defenseTotalWaves) {
                score += 450 + defenseCoreHealth.toInt()
                status = MissionRunStatus.VICTORY
                pendingAudio.add(SimAudioEvent.MissionVictory)
                eventBus.publish(GameplayEvent.MissionCompleted(mission.number))
                showBanner(GameStrings.victoryTitle(language), 3.5f)
            } else if (nextWaveDelayTimer <= 0f) {
                nextWaveDelayTimer = 2.5f
                score += 250 * waveNumber
                player.resupplyAll()
                pendingAudio.add(SimAudioEvent.Checkpoint)
                showBanner(
                    if (language == GameLanguage.ARABIC) "تم صد الموجة $waveNumber/$defenseTotalWaves!"
                    else "WAVE $waveNumber/$defenseTotalWaves REPELLED!",
                    2.2f
                )
            } else {
                nextWaveDelayTimer -= dt
                if (nextWaveDelayTimer <= 0f) {
                    waveNumber += 1
                    spawnDynamicModeWave(wave = waveNumber, isDefense = true)
                    pendingAudio.add(SimAudioEvent.ReinforcementsAlert)
                    showBanner(GameStrings.hudWave(language, waveNumber), 2.2f)
                }
            }
        }
    }

    private fun evaluateChallengeMode(dt: Float) {
        val ch = challenge ?: return
        challengeTimeRemaining = max(0f, challengeTimeRemaining - dt)
        val alive = enemies.count { it.isAlive }
        val killsReached = missionKills >= ch.targetKills || alive == 0
        val headshotsOk = missionHeadshots >= ch.requiredHeadshots
        if (killsReached && headshotsOk) {
            score += (challengeTimeRemaining * 8f).toInt() + missionHeadshots * 75
            status = MissionRunStatus.VICTORY
            pendingAudio.add(SimAudioEvent.MissionVictory)
            eventBus.publish(GameplayEvent.MissionCompleted(mission.number))
            showBanner(GameStrings.victoryTitle(language), 3.5f)
            return
        }
        if (alive == 0 && !headshotsOk && challengeTimeRemaining > 2f) {
            // Prevent soft-lock when all initial targets were defeated without enough headshots
            spawnDynamicModeWave(wave = 1, isDefense = false)
            showBanner(
                if (language == GameLanguage.ARABIC)
                    "تعزيزات إضافية — أكمِل إصابات الرأس المطلوبة (${missionHeadshots}/${ch.requiredHeadshots})"
                else
                    "BONUS TARGETS INBOUND — COMPLETE HEADSHOTS (${missionHeadshots}/${ch.requiredHeadshots})",
                2.8f
            )
            return
        }
        if (challengeTimeRemaining <= 0f) {
            status = MissionRunStatus.DEFEAT
            pendingAudio.add(SimAudioEvent.MissionDefeat)
            eventBus.publish(GameplayEvent.MissionFailed(mission.number, "challenge_timeout"))
        }
    }

    private fun fireVehicleTurret() {
        pendingAudio.add(SimAudioEvent.VehicleTurretFired)
        val muzzle = vehicle.turretMuzzleWorldPos()
        val camPose = cameraDirector.computePose(player, city, reduceShake = true, inVehicle = true)
        val camDir = (camPose.lookTarget - camPose.position).normalized()
        val maxRange = 72f
        val camWorldHit = city.raycastWorld(camPose.position, camDir, maxRange)
        val aimPoint = camPose.position + camDir * (camWorldHit?.first ?: maxRange)
        var rawDir = (aimPoint - muzzle).normalized()
        if (aimAssistEnabled) {
            var bestDot = 0.968f
            var assisted: Vec3? = null
            for (e in enemies) {
                if (!e.isAlive) continue
                val tc = Vec3(e.position.x, e.position.y + e.height * 0.65f, e.position.z)
                val toE = tc - muzzle
                val d = toE.length()
                if (d < 1.5f || d > maxRange) continue
                val dNorm = toE * (1f / d)
                val dot = rawDir.dot(dNorm)
                if (dot > bestDot && city.hasLineOfSight(muzzle, tc)) {
                    bestDot = dot
                    assisted = dNorm
                }
            }
            if (assisted != null) {
                rawDir = Vec3(
                    Math3D.lerp(rawDir.x, assisted.x, 0.55f),
                    Math3D.lerp(rawDir.y, assisted.y, 0.55f),
                    Math3D.lerp(rawDir.z, assisted.z, 0.55f)
                ).normalized()
            }
        }
        val worldHit = city.raycastWorld(muzzle, rawDir, maxRange)
        var bestDist = worldHit?.first ?: maxRange
        var hitEnemy: EnemyUnit? = null
        var hitZone = HitZone.TORSO
        var hitBarrel: InteractiveObject? = null

        interactionManager.raycastBarrel(muzzle, rawDir, bestDist)?.let {
            if (it.second < bestDist) {
                bestDist = it.second
                hitBarrel = it.first
            }
        }
        for (i in enemies.indices) {
            val e = enemies[i]
            val res = e.intersectRay(muzzle, rawDir, bestDist)
            if (res != null && res.second < bestDist) {
                bestDist = res.second
                hitEnemy = e
                hitZone = res.first
                hitBarrel = null
            }
        }

        missionShotsFired += 1
        val hitPoint = muzzle + rawDir * bestDist
        if (!reduceEffects && tracers.size < performanceManager.maxActiveTracers) {
            tracers.add(BulletTracer(muzzle, hitPoint, floatArrayOf(1.0f, 0.72f, 0.20f), life = 0.11f))
        }

        if (hitEnemy != null) {
            missionShotsHit += 1
            hitMarkerTimer = 0.24f
            hitMarkerHeadshot = hitZone == HitZone.HEAD
            pendingAudio.add(SimAudioEvent.HitConfirm(hitMarkerHeadshot))
            val dmgRes = damageSystem.applyDamageToEnemy(
                enemy = hitEnemy,
                rawDamage = 44f,
                hitZone = hitZone,
                attackerPos = vehicle.position,
                weaponId = WeaponId.MG48_LMG,
                source = DamageSource.PLAYER_WEAPON
            )
            spawnImpactParticles(hitPoint, SurfaceMaterial.METAL)
            if (dmgRes.eliminated) {
                missionKills += 1
                score += 110 + if (hitZone == HitZone.HEAD) 50 else 0
                if (hitZone == HitZone.HEAD) missionHeadshots += 1
                squadCoordinator.onAllyDefeated(hitEnemy, vehicle.position, enemies)
            }
        } else if (hitBarrel != null) {
            missionShotsHit += 1
            if (interactionManager.damageBarrel(hitBarrel, 44f)) {
                triggerBarrelDetonation(hitBarrel.position)
                showBanner(GameStrings.hudBarrelDetonated(language), 2.0f)
            }
        } else if (worldHit != null) {
            spawnImpactParticles(hitPoint, worldHit.second)
            pendingAudio.add(SimAudioEvent.Impact(worldHit.second))
        }
    }

    private fun resolveEnemyShotAtCore(enemy: EnemyUnit) {
        val origin = Vec3(enemy.position.x, enemy.position.y + enemy.height * 0.78f, enemy.position.z)
        val coreTop = Vec3(defenseCorePosition.x, 1.5f, defenseCorePosition.z)
        val dmg = enemy.archetype.damage * difficulty.enemyDamageMult * 0.85f
        defenseCoreHealth = max(0f, defenseCoreHealth - dmg)
        if (coreAlertCooldown <= 0f) {
            coreAlertCooldown = 4.0f
            pendingAudio.add(SimAudioEvent.CoreDamaged)
            showBanner(GameStrings.hudCoreUnderAttack(language), 2.0f)
        }
        if (!reduceEffects && tracers.size < performanceManager.maxActiveTracers) {
            tracers.add(BulletTracer(origin, coreTop, floatArrayOf(1.0f, 0.35f, 0.24f), life = 0.12f))
        }
        spawnImpactParticles(coreTop, SurfaceMaterial.METAL)
    }

    private fun triggerBarrelDetonation(center: Vec3) {
        val killedCount = damageSystem.applyRadialHazardBlast(
            center = center,
            radius = 7.5f,
            maxDamage = 135f,
            player = player,
            enemies = enemies,
            reduceShake = reduceShake
        )
        missionKills += killedCount
        score += killedCount * 120
        pendingAudio.add(SimAudioEvent.Impact(SurfaceMaterial.METAL))
        val cap = performanceManager.effectiveMaxParticles
        repeat(if (reduceEffects) 4 else 14) {
            if (particles.size < cap) {
                val vel = Vec3(
                    (rng.nextFloat() - 0.5f) * 9.5f,
                    2.5f + rng.nextFloat() * 5.5f,
                    (rng.nextFloat() - 0.5f) * 9.5f
                )
                val col = if (it % 2 == 0) floatArrayOf(1.0f, 0.72f, 0.22f) else floatArrayOf(0.45f, 0.45f, 0.48f)
                particles.add(ImpactParticle(center + Vec3(0f, 0.8f, 0f), vel, col, 0.16f, 0.65f, 0.65f))
            }
        }
        for (e in enemies) {
            if (e.isAlive && e.position.distanceXZ(center) <= 34f) {
                e.alertTo(center, highPriority = true)
            }
        }
    }

    private fun firePlayerWeapon() {
        val stats: EffectiveWeaponStats = player.activeWeapon.stats
        val weaponId = stats.weapon.id
        pendingAudio.add(SimAudioEvent.WeaponFired(weaponId))

        val soundRadius = if (stats.hasLongBarrel) 30f else 36f
        eventBus.publish(GameplayEvent.WeaponFired(weaponId, player.position, soundRadius))

        for (e in enemies) {
            if (e.isAlive && e.position.distanceXZ(player.position) <= soundRadius) {
                e.alertTo(player.position, highPriority = false)
            }
        }

        val (origin, rawDir) = player.getAimOriginAndDirection(city, cameraDirector)
        var aimDir = rawDir

        if (aimAssistEnabled) {
            var bestDot = if (player.isAiming) 0.985f else 0.972f
            var assistedDir: Vec3? = null
            for (e in enemies) {
                if (!e.isAlive) continue
                val targetCenter = Vec3(e.position.x, e.position.y + e.height * 0.68f, e.position.z)
                val toEnemy = targetCenter - origin
                val dist = toEnemy.length()
                if (dist < 1f || dist > stats.effectiveRange * 1.2f) continue
                val dNorm = toEnemy * (1f / dist)
                val dot = rawDir.dot(dNorm)
                if (dot > bestDot && city.hasLineOfSight(origin, targetCenter)) {
                    bestDot = dot
                    assistedDir = dNorm
                }
            }
            if (assistedDir != null) {
                aimDir = Vec3(
                    Math3D.lerp(rawDir.x, assistedDir.x, 0.55f),
                    Math3D.lerp(rawDir.y, assistedDir.y, 0.55f),
                    Math3D.lerp(rawDir.z, assistedDir.z, 0.55f)
                ).normalized()
            }
        }

        val pellets = stats.weapon.pelletsPerShot
        missionShotsFired += 1
        var anyHitThisShot = false
        var anyHeadshot = false

        val shoulderSign = cameraDirector.shoulder.sign
        val muzzleStart = Vec3(
            player.position.x + cos(player.yaw) * 0.34f * shoulderSign - sin(player.yaw) * 0.55f,
            player.position.y + player.eyeHeight - 0.16f,
            player.position.z - sin(player.yaw) * 0.34f * shoulderSign - cos(player.yaw) * 0.55f
        )

        repeat(pellets) {
            val spread = player.currentSpread
            val sx = (rng.nextFloat() * 2f - 1f) * spread
            val sy = (rng.nextFloat() * 2f - 1f) * spread
            val sz = (rng.nextFloat() * 2f - 1f) * spread
            val dir = (aimDir + Vec3(sx, sy, sz)).normalized()
            val maxRange = stats.effectiveRange * 1.6f

            val worldHit = city.raycastWorld(origin, dir, maxRange)
            var bestDist = worldHit?.first ?: maxRange
            var hitEnemy: EnemyUnit? = null
            var hitZone: HitZone = HitZone.TORSO
            var hitBarrel: InteractiveObject? = null

            val barrelHit = interactionManager.raycastBarrel(origin, dir, bestDist)
            if (barrelHit != null && barrelHit.second < bestDist) {
                bestDist = barrelHit.second
                hitBarrel = barrelHit.first
            }

            for (i in enemies.indices) {
                val e = enemies[i]
                val res = e.intersectRay(origin, dir, bestDist)
                if (res != null && res.second < bestDist) {
                    bestDist = res.second
                    hitEnemy = e
                    hitZone = res.first
                    hitBarrel = null
                }
            }

            val hitPoint = origin + dir * bestDist
            val tracerColor = if (weaponId == WeaponId.RAIL_X1_HEAVY)
                floatArrayOf(0.30f, 0.85f, 1.0f) else floatArrayOf(1.0f, 0.85f, 0.45f)
            if (!reduceEffects && tracers.size < performanceManager.maxActiveTracers) {
                tracers.add(BulletTracer(muzzleStart, hitPoint, tracerColor))
            }

            if (hitEnemy != null) {
                anyHitThisShot = true
                if (hitZone == HitZone.HEAD) anyHeadshot = true
                val falloff = if (bestDist <= stats.effectiveRange) 1f else max(0.45f, 1f - (bestDist - stats.effectiveRange) / stats.effectiveRange)
                val dmgRes = damageSystem.applyDamageToEnemy(
                    enemy = hitEnemy,
                    rawDamage = stats.damage * falloff,
                    hitZone = hitZone,
                    attackerPos = player.position,
                    weaponId = weaponId,
                    source = DamageSource.PLAYER_WEAPON
                )
                spawnImpactParticles(hitPoint, if (hitZone == HitZone.SHIELD) SurfaceMaterial.METAL else SurfaceMaterial.CONCRETE)
                if (dmgRes.eliminated) {
                    missionKills += 1
                    score += 100 + if (hitZone == HitZone.HEAD) 50 else 0
                    if (hitZone == HitZone.HEAD) missionHeadshots += 1
                    missionWeaponKills[weaponId] = (missionWeaponKills[weaponId] ?: 0) + 1
                    squadCoordinator.onAllyDefeated(hitEnemy, player.position, enemies)
                }
            } else if (hitBarrel != null) {
                anyHitThisShot = true
                spawnImpactParticles(hitPoint, SurfaceMaterial.METAL)
                if (interactionManager.damageBarrel(hitBarrel, stats.damage)) {
                    triggerBarrelDetonation(hitBarrel.position)
                    showBanner(GameStrings.hudBarrelDetonated(language), 2.0f)
                }
            } else if (worldHit != null) {
                spawnImpactParticles(hitPoint, worldHit.second)
                pendingAudio.add(SimAudioEvent.Impact(worldHit.second))
            }
        }

        if (anyHitThisShot) {
            missionShotsHit += 1
            hitMarkerTimer = 0.24f
            hitMarkerHeadshot = anyHeadshot
            pendingAudio.add(SimAudioEvent.HitConfirm(anyHeadshot))
        }
    }

    private fun resolveEnemyShot(enemy: EnemyUnit, playerEye: Vec3) {
        val origin = Vec3(enemy.position.x, enemy.position.y + enemy.height * 0.78f, enemy.position.z)
        if (!city.hasLineOfSight(origin, playerEye)) return

        val toPlayer = playerEye - origin
        val dist = toPlayer.length()
        val coverPenalty = if (player.isInCover && !player.isLeaningFromCover) 0.42f else if (player.isInCover || player.isCrouching) 0.72f else 1f
        val distFactor = (1f - (dist / 65f)).coerceIn(0.28f, 1f)
        val hitChance = (difficulty.enemyAccuracy * coverPenalty * distFactor).coerceIn(0.10f, 0.85f)

        val endPos = if (rng.nextFloat() < hitChance) {
            val rawDmg = enemy.archetype.damage * difficulty.enemyDamageMult
            if (vehicle.isOccupied) {
                val disabled = vehicle.applyHullDamage(rawDmg)
                pendingAudio.add(SimAudioEvent.Impact(SurfaceMaterial.VEHICLE))
                if (disabled) {
                    exitVehicleNow()
                    vehicleInteractObj.isDestroyed = true
                    showBanner(GameStrings.hudVehicleDisabled(language), 2.5f)
                }
            } else {
                val res = damageSystem.applyDamageToPlayer(
                    player = player,
                    rawAmount = rawDmg,
                    source = DamageSource.ENEMY_WEAPON,
                    sourcePos = enemy.position,
                    reduceShake = reduceShake
                )
                if (res.applied) pendingAudio.add(SimAudioEvent.PlayerDamaged)
            }
            playerEye
        } else {
            playerEye + Vec3((rng.nextFloat() - 0.5f) * 2.4f, (rng.nextFloat() - 0.5f) * 1.4f, (rng.nextFloat() - 0.5f) * 2.4f)
        }
        if (!reduceEffects && tracers.size < performanceManager.maxActiveTracers) {
            tracers.add(BulletTracer(origin, endPos, floatArrayOf(1.0f, 0.42f, 0.28f), life = 0.11f))
        }
    }

    private fun spawnImpactParticles(pos: Vec3, mat: SurfaceMaterial) {
        if (reduceEffects) return
        val cap = performanceManager.effectiveMaxParticles
        val count = if (particles.size > cap - 6) 2 else 5
        val rgb = when (mat) {
            SurfaceMaterial.METAL, SurfaceMaterial.VEHICLE -> floatArrayOf(1.0f, 0.78f, 0.28f)
            SurfaceMaterial.SAND, SurfaceMaterial.SANDBAG -> floatArrayOf(0.82f, 0.70f, 0.48f)
            SurfaceMaterial.WATER -> floatArrayOf(0.65f, 0.85f, 0.96f)
            else -> floatArrayOf(0.72f, 0.70f, 0.66f)
        }
        repeat(count) {
            if (particles.size >= cap) return
            val vel = Vec3(
                (rng.nextFloat() - 0.5f) * 4.5f,
                1.5f + rng.nextFloat() * 3.5f,
                (rng.nextFloat() - 0.5f) * 4.5f
            )
            particles.add(ImpactParticle(pos, vel, rgb, 0.08f + rng.nextFloat() * 0.08f, 0.38f, 0.38f))
        }
    }

    private fun evaluateObjectives(dt: Float) {
        val obj = currentObjective ?: return
        val targetPos = Vec3(obj.targetX, 0f, obj.targetZ)
        val distToTarget = player.position.distanceXZ(targetPos)

        val completed = when (obj.kind) {
            ObjectiveKind.MOVE_TUTORIAL -> player.totalDistanceMoved >= 4.5f || distToTarget <= obj.radius
            ObjectiveKind.REACH_CHECKPOINT -> distToTarget <= obj.radius
            ObjectiveKind.ACQUIRE_AMMO -> city.resupplyStations.any { it.used } ||
                interactionManager.objects.any { (it.kind == InteractiveKind.AMMO_CONTAINER || it.kind == InteractiveKind.SUPPLY_BOX) && it.isActivated } ||
                distToTarget <= obj.radius
            ObjectiveKind.COLLECT_INTEL -> distToTarget <= obj.radius || objectiveDeviceActivatedThisPhase
            ObjectiveKind.SECURE_ZONE -> {
                if (distToTarget <= obj.radius) {
                    objectiveZoneTimer += dt
                }
                objectiveZoneTimer >= 2.2f || objectiveDeviceActivatedThisPhase
            }
            ObjectiveKind.ELIMINATE_HOSTILES, ObjectiveKind.SURVIVE_REINFORCEMENTS -> {
                val aliveCount = enemies.count { it.isAlive }
                missionKills >= obj.requiredCount || aliveCount == 0
            }
            ObjectiveKind.DEFEND_CORE -> defenseCoreHealth > 0f && waveNumber >= defenseTotalWaves && enemies.none { it.isAlive }
            ObjectiveKind.DEFEAT_BOSS -> enemies.none { it.isAlive && it.archetype.isBoss }
            ObjectiveKind.EXTRACT -> distToTarget <= obj.radius
        }

        if (completed) {
            eventBus.publish(GameplayEvent.ObjectiveCompleted(mission.number, currentObjectiveIndex))
            objectiveZoneTimer = 0f
            objectiveDeviceActivatedThisPhase = false
            if (obj.triggerReinforcementWave && !reinforcementWaveSpawned) {
                reinforcementWaveSpawned = true
                spawnEnemyWave(1, enforceSafeDistance = true)
                pendingAudio.add(SimAudioEvent.ReinforcementsAlert)
                showBanner(GameStrings.hudReinforcements(language), 2.8f)
            }

            currentObjectiveIndex += 1
            if (currentObjectiveIndex >= mission.objectives.size) {
                status = MissionRunStatus.VICTORY
                pendingAudio.add(SimAudioEvent.MissionVictory)
                eventBus.publish(GameplayEvent.MissionCompleted(mission.number))
                showBanner(GameStrings.victoryTitle(language), 3.5f)
            } else {
                latestCheckpoint = SavedCheckpoint(
                    missionNumber = mission.number,
                    objectiveIndex = currentObjectiveIndex,
                    playerX = player.position.x,
                    playerZ = player.position.z,
                    health = player.health,
                    armor = player.armor,
                    ammoInMag = player.activeWeapon.ammoInMag,
                    ammoReserve = player.activeWeapon.ammoReserve
                )
                eventBus.publish(GameplayEvent.CheckpointReached(mission.number, currentObjectiveIndex))
                eventBus.publish(GameplayEvent.ObjectiveStarted(mission.number, currentObjectiveIndex))
                pendingAudio.add(SimAudioEvent.Checkpoint)
                pendingAudio.add(SimAudioEvent.ObjectiveComplete)
                val nextObj = currentObjective
                if (nextObj != null) {
                    showBanner(GameStrings.hudCheckpointReached(language), 2.2f)
                    showSubtitle(nextObj.title.get(language), 3.2f)
                }
            }
        }
    }

    fun buildSummary(): MissionSummary {
        val accuracy = if (missionShotsFired > 0) missionShotsHit.toFloat() / missionShotsFired else 0f
        val stars = when {
            status != MissionRunStatus.VICTORY -> 0
            player.health >= 60f && accuracy >= 0.35f -> 3
            player.health >= 35f -> 2
            else -> 1
        }
        val killXp = enemies.filter { !it.isAlive }.sumOf { it.archetype.xpValue } / 2
        val modeBonusXp = when (mode) {
            GameMode.SURVIVAL -> (waveNumber - 1) * 65 + (score / 25)
            GameMode.DEFENSE -> if (status == MissionRunStatus.VICTORY) mission.xpReward else waveNumber * 45
            GameMode.CHALLENGES -> if (status == MissionRunStatus.VICTORY) mission.xpReward else killXp / 2
            GameMode.CAMPAIGN -> if (status == MissionRunStatus.VICTORY) mission.xpReward else 0
        }
        val baseXp = modeBonusXp + killXp
        val totalXp = (baseXp * difficulty.xpMultiplier).toInt().coerceAtLeast(15)
        val medal = challenge?.evaluateMedal(score, status == MissionRunStatus.VICTORY) ?: ChallengeMedal.NONE

        return MissionSummary(
            missionNumber = mission.number,
            victory = status == MissionRunStatus.VICTORY,
            kills = missionKills,
            headshots = missionHeadshots,
            shotsFired = missionShotsFired,
            shotsHit = missionShotsHit,
            remainingHealth = player.health,
            elapsedSeconds = elapsedSeconds,
            stars = stars,
            xpEarned = totalXp,
            weaponKills = missionWeaponKills.toMap(),
            gameMode = mode,
            cityId = mission.cityId,
            waveReached = waveNumber,
            score = score,
            challengeId = challenge?.id,
            challengeMedal = medal
        )
    }

    fun buildHudSnapshot(): HudSnapshot {
        val obj = currentObjective
        val distMeters = obj?.let { player.position.distanceXZ(Vec3(it.targetX, 0f, it.targetZ)).toInt() } ?: 0
        val progressText = when (mode) {
            GameMode.SURVIVAL -> "${GameStrings.hudWave(language, waveNumber)} • $score"
            GameMode.DEFENSE -> "${GameStrings.hudWave(language, waveNumber)}/$defenseTotalWaves • ${defenseCoreHealth.toInt()} HP"
            GameMode.CHALLENGES -> "${min(missionKills, challenge?.targetKills ?: 10)}/${challenge?.targetKills ?: 10} • ${challengeTimeRemaining.toInt()}s"
            GameMode.CAMPAIGN -> when (obj?.kind) {
                ObjectiveKind.ELIMINATE_HOSTILES, ObjectiveKind.SURVIVE_REINFORCEMENTS ->
                    "${min(missionKills, obj.requiredCount)} / ${obj.requiredCount}"
                ObjectiveKind.SECURE_ZONE -> "${((objectiveZoneTimer / 2.2f) * 100f).toInt().coerceIn(0, 100)}%"
                else -> "${distMeters}m"
            }
        }

        val activeBoss = enemies.firstOrNull { it.isAlive && it.archetype.isBoss }
        val bossFrac = activeBoss?.let { (it.health + it.armor) / (it.maxHealth + it.maxArmor) }

        val cosY = cos(player.yaw)
        val sinY = sin(player.yaw)
        fun toRadar(worldX: Float, worldZ: Float): Pair<Float, Float> {
            val dx = worldX - player.position.x
            val dz = worldZ - player.position.z
            val rx = dx * cosY - dz * sinY
            val rz = dx * sinY + dz * cosY
            return rx to rz
        }

        val radarEnemies = enemies.filter { it.isAlive }.map { toRadar(it.position.x, it.position.z) }
        val radarObj = if (mode == GameMode.DEFENSE) {
            toRadar(defenseCorePosition.x, defenseCorePosition.z)
        } else {
            obj?.let { toRadar(it.targetX, it.targetZ) }
        }
        val promptTarget = if (vehicle.isOccupied) null else interactionManager.findPromptTarget(player, city)
        val promptText = if (vehicle.isOccupied) {
            null
        } else {
            interactionManager.buildPromptText(promptTarget, language)
        }

        return HudSnapshot(
            status = status,
            missionNumber = mission.number,
            health = player.health,
            maxHealth = player.maxHealth,
            armor = player.armor,
            maxArmor = player.maxArmor,
            stamina = player.stamina,
            weaponId = player.activeWeaponId,
            ammoInMag = player.activeWeapon.ammoInMag,
            magCapacity = player.activeWeapon.stats.magCapacity,
            ammoReserve = player.activeWeapon.ammoReserve,
            isReloading = player.isReloading,
            reloadProgress = if (player.isReloading) {
                (1f - player.reloadTimer / player.activeWeapon.stats.reloadDuration).coerceIn(0f, 1f)
            } else 0f,
            isAiming = player.isAiming,
            isCrouching = player.isCrouching,
            isSprinting = player.isSprinting,
            isInCover = player.isInCover,
            canEnterCover = player.canEnterCover && !vehicle.isOccupied,
            canVault = player.canVault && !vehicle.isOccupied,
            isVaulting = player.isVaulting,
            interactionPrompt = promptText,
            cameraShoulder = cameraDirector.shoulder,
            combatPhase = combatDirector.currentPhase,
            respawnProtected = player.respawnProtectionTimer > 0f,
            animState = player.animState,
            lowerBodyState = player.lowerBodyState,
            upperBodyState = player.upperBodyState,
            currentObjectiveIndex = currentObjectiveIndex,
            totalObjectives = mission.objectives.size.coerceAtLeast(1),
            objectiveTitle = obj?.title?.get(language) ?: GameStrings.victoryTitle(language),
            objectiveProgressText = progressText,
            objectiveDistanceMeters = distMeters,
            activeEnemiesCount = enemies.count { it.isAlive },
            hitMarkerAlpha = (hitMarkerTimer / 0.24f).coerceIn(0f, 1f),
            hitMarkerHeadshot = hitMarkerHeadshot,
            damageFlashAlpha = (player.damageFlashTimer / 0.42f).coerceIn(0f, 1f),
            damageDirectionRad = player.lastDamageAngleRad,
            bannerMessage = bannerText,
            subtitleText = subtitleText,
            bossHealthFraction = bossFrac,
            playerPos = player.position,
            playerYaw = player.yaw,
            radarEnemies = radarEnemies,
            radarObjective = radarObj,
            fps = smoothedFps,
            gameMode = mode,
            weather = weatherCondition,
            waveNumber = waveNumber,
            score = score,
            defenseCoreHealth = defenseCoreHealth,
            defenseCoreMaxHealth = defenseCoreMaxHealth,
            challengeTimeRemaining = challengeTimeRemaining,
            inVehicle = vehicle.isOccupied,
            vehicleHealth = vehicle.health,
            vehicleMaxHealth = vehicle.maxHealth
        )
    }
}
