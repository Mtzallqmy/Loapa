package com.example.game.ui

import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import com.example.game.audio.TacticalAudioEngine
import com.example.game.data.AchievementCatalog
import com.example.game.data.AttachmentSlot
import com.example.game.data.CharacterSlot
import com.example.game.data.CustomizationCatalog
import com.example.game.data.GameLanguage
import com.example.game.data.MissionCatalog
import com.example.game.data.WeaponCatalog
import com.example.game.data.WeaponId
import com.example.game.engine.CombatSimulation
import com.example.game.engine.HudSnapshot
import com.example.game.engine.MissionRunStatus
import com.example.game.engine.MissionSummary
import com.example.game.engine.PlayerInputState
import com.example.game.engine.SimAudioEvent
import com.example.game.save.ControlLayoutSettings
import com.example.game.save.GameSettings
import com.example.game.save.PlayerSaveData
import com.example.game.save.SaveManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

import com.example.game.data.ChallengeMedal
import com.example.game.data.CityId
import com.example.game.data.GameMode
import com.example.game.data.GameModeCatalog
import com.example.game.data.WeatherCondition

enum class GameScreen {
    SPLASH,
    MAIN_MENU,
    GAME_MODES,
    CAMPAIGN_INTRO,
    MISSIONS,
    GAMEPLAY,
    MISSION_RESULT,
    ARMORY,
    CUSTOMIZATION,
    ACHIEVEMENTS,
    SETTINGS,
    CONTROL_EDITOR,
    ABOUT
}

/** Thread-safe bridge carrying multi-touch HUD input from Compose UI thread to OpenGL render thread. */
class MobileInputBridge {
    @Volatile var moveX: Float = 0f
    @Volatile var moveY: Float = 0f
    @Volatile var fireHeld: Boolean = false
    @Volatile var aimHeld: Boolean = false
    @Volatile var sprintHeld: Boolean = false
    @Volatile var crouchToggled: Boolean = false

    private var pendingLookX: Float = 0f
    private var pendingLookY: Float = 0f
    private var pendingJump: Boolean = false
    private var pendingReload: Boolean = false
    private var pendingCover: Boolean = false
    private var pendingSwitch: Boolean = false
    private var pendingInteract: Boolean = false
    private var pendingVault: Boolean = false
    private var pendingShoulderToggle: Boolean = false
    private var pendingGrenade: Boolean = false
    private var pendingMedkit: Boolean = false

    @Synchronized
    fun addLookDelta(dxRad: Float, dyRad: Float) {
        pendingLookX += dxRad
        pendingLookY += dyRad
    }

    @Synchronized fun triggerJump() { pendingJump = true }
    @Synchronized fun triggerReload() { pendingReload = true }
    @Synchronized fun triggerCover() { pendingCover = true }
    @Synchronized fun triggerSwitchWeapon() { pendingSwitch = true }
    @Synchronized fun triggerInteract() { pendingInteract = true }
    @Synchronized fun triggerVault() { pendingVault = true }
    @Synchronized fun triggerToggleShoulder() { pendingShoulderToggle = true }
    @Synchronized fun triggerGrenade() { pendingGrenade = true }
    @Synchronized fun triggerMedkit() { pendingMedkit = true }

    @Synchronized
    fun reset() {
        moveX = 0f; moveY = 0f; fireHeld = false; aimHeld = false
        sprintHeld = false; crouchToggled = false
        pendingLookX = 0f; pendingLookY = 0f
        pendingJump = false; pendingReload = false; pendingCover = false; pendingSwitch = false
        pendingInteract = false; pendingVault = false; pendingShoulderToggle = false
        pendingGrenade = false; pendingMedkit = false
    }

    @Synchronized
    fun consumeForFrame(): PlayerInputState {
        val state = PlayerInputState(
            moveX = moveX,
            moveY = moveY,
            lookDeltaX = pendingLookX,
            lookDeltaY = pendingLookY,
            fireHeld = fireHeld,
            aimHeld = aimHeld,
            sprintHeld = sprintHeld,
            crouchToggled = crouchToggled,
            jumpPressed = pendingJump,
            reloadPressed = pendingReload,
            coverPressed = pendingCover,
            switchWeaponPressed = pendingSwitch,
            interactPressed = pendingInteract,
            vaultPressed = pendingVault,
            toggleShoulderPressed = pendingShoulderToggle,
            grenadePressed = pendingGrenade,
            medkitPressed = pendingMedkit
        )
        pendingLookX = 0f
        pendingLookY = 0f
        pendingJump = false
        pendingReload = false
        pendingCover = false
        pendingSwitch = false
        pendingInteract = false
        pendingVault = false
        pendingShoulderToggle = false
        pendingGrenade = false
        pendingMedkit = false
        return state
    }
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val saveManager = SaveManager(application.filesDir)
    val audioEngine = TacticalAudioEngine()
    val inputBridge = MobileInputBridge()
    val activeSimRef = AtomicReference<CombatSimulation?>(null)

    private val _saveData = MutableStateFlow(saveManager.load())
    val saveData: StateFlow<PlayerSaveData> = _saveData.asStateFlow()

    private val _screen = MutableStateFlow(GameScreen.SPLASH)
    val screen: StateFlow<GameScreen> = _screen.asStateFlow()

    private val _hudSnapshot = MutableStateFlow<HudSnapshot?>(null)
    val hudSnapshot: StateFlow<HudSnapshot?> = _hudSnapshot.asStateFlow()

    private val _lastSummary = MutableStateFlow<MissionSummary?>(null)
    val lastSummary: StateFlow<MissionSummary?> = _lastSummary.asStateFlow()

    private val _newlyUnlockedAchievements = MutableStateFlow<List<String>>(emptyList())
    val newlyUnlockedAchievements: StateFlow<List<String>> = _newlyUnlockedAchievements.asStateFlow()

    private val resultProcessedForCurrentRun = AtomicBoolean(false)
    private val saveLock = Any()

    init {
        audioEngine.updateSettings(_saveData.value.settings)
    }

    fun navigate(target: GameScreen) {
        audioEngine.playUiClick()
        audioEngine.setCombatMusicMode(target == GameScreen.GAMEPLAY)
        _screen.value = target
    }

    fun finishSplash() {
        if (_screen.value == GameScreen.SPLASH) {
            _screen.value = GameScreen.MAIN_MENU
        }
    }

    fun startNewCampaignOrMission1() {
        audioEngine.playUiClick()
        val data = _saveData.value
        if (data.completedMissions.isEmpty()) {
            _screen.value = GameScreen.CAMPAIGN_INTRO
        } else {
            launchMission(data.highestUnlockedMission.coerceIn(1, 12), useCheckpoint = false)
        }
    }

    fun continueCampaign() {
        audioEngine.playUiClick()
        val data = _saveData.value
        val cp = data.lastCheckpoint
        if (cp != null) {
            launchMission(cp.missionNumber, useCheckpoint = true)
        } else {
            launchMission(data.highestUnlockedMission, useCheckpoint = false)
        }
    }

    private var lastLaunchedMode: GameMode = GameMode.CAMPAIGN
    private var lastArenaCity: CityId = CityId.RIVER_CITY
    private var lastArenaWeather: WeatherCondition = WeatherCondition.CLEAR_DAY
    private var lastChallengeId: String? = null

    fun launchMission(missionNumber: Int, useCheckpoint: Boolean = false) {
        val data = _saveData.value
        val missionDef = MissionCatalog.missionByNumber(missionNumber.coerceIn(1, 12))
        val cp = if (useCheckpoint && data.lastCheckpoint?.missionNumber == missionDef.number) {
            data.lastCheckpoint
        } else null

        lastLaunchedMode = GameMode.CAMPAIGN
        inputBridge.reset()
        resultProcessedForCurrentRun.set(false)
        _lastSummary.value = null
        _newlyUnlockedAchievements.value = emptyList()

        val sim = CombatSimulation(missionDef, data, resumeCheckpoint = cp, mode = GameMode.CAMPAIGN)
        activeSimRef.set(sim)
        _hudSnapshot.value = sim.buildHudSnapshot()
        updateSave { it.copy(lastPlayedMission = missionDef.number) }
        audioEngine.setCombatMusicMode(true)
        _screen.value = GameScreen.GAMEPLAY
    }

    fun launchSurvival(cityId: CityId, weather: WeatherCondition) {
        audioEngine.playUiClick()
        val data = _saveData.value
        lastLaunchedMode = GameMode.SURVIVAL
        lastArenaCity = cityId
        lastArenaWeather = weather
        inputBridge.reset()
        resultProcessedForCurrentRun.set(false)
        _lastSummary.value = null
        _newlyUnlockedAchievements.value = emptyList()

        val def = GameModeCatalog.buildSurvivalMissionDef(cityId, weather)
        val sim = CombatSimulation(def, data, mode = GameMode.SURVIVAL, weatherCondition = weather)
        activeSimRef.set(sim)
        _hudSnapshot.value = sim.buildHudSnapshot()
        audioEngine.setCombatMusicMode(true)
        _screen.value = GameScreen.GAMEPLAY
    }

    fun launchDefense(cityId: CityId, weather: WeatherCondition) {
        audioEngine.playUiClick()
        val data = _saveData.value
        lastLaunchedMode = GameMode.DEFENSE
        lastArenaCity = cityId
        lastArenaWeather = weather
        inputBridge.reset()
        resultProcessedForCurrentRun.set(false)
        _lastSummary.value = null
        _newlyUnlockedAchievements.value = emptyList()

        val def = GameModeCatalog.buildDefenseMissionDef(cityId, weather)
        val sim = CombatSimulation(def, data, mode = GameMode.DEFENSE, weatherCondition = weather)
        activeSimRef.set(sim)
        _hudSnapshot.value = sim.buildHudSnapshot()
        audioEngine.setCombatMusicMode(true)
        _screen.value = GameScreen.GAMEPLAY
    }

    fun launchChallenge(challengeId: String) {
        audioEngine.playUiClick()
        val data = _saveData.value
        val ch = GameModeCatalog.challengeById(challengeId)
        lastLaunchedMode = GameMode.CHALLENGES
        lastChallengeId = ch.id
        inputBridge.reset()
        resultProcessedForCurrentRun.set(false)
        _lastSummary.value = null
        _newlyUnlockedAchievements.value = emptyList()

        val def = GameModeCatalog.buildChallengeMissionDef(ch)
        val sim = CombatSimulation(def, data, mode = GameMode.CHALLENGES, weatherCondition = ch.weather, challenge = ch)
        activeSimRef.set(sim)
        _hudSnapshot.value = sim.buildHudSnapshot()
        audioEngine.setCombatMusicMode(true)
        _screen.value = GameScreen.GAMEPLAY
    }

    fun onHudSnapshotFromGL(snapshot: HudSnapshot) {
        _hudSnapshot.value = snapshot
        val sim = activeSimRef.get() ?: return
        if ((snapshot.status == MissionRunStatus.VICTORY || snapshot.status == MissionRunStatus.DEFEAT) &&
            resultProcessedForCurrentRun.compareAndSet(false, true)
        ) {
            val summary = synchronized(sim.simLock) { sim.buildSummary() }
            applyMissionOutcome(summary)
            _lastSummary.value = summary
            audioEngine.setCombatMusicMode(false)
            _screen.value = GameScreen.MISSION_RESULT
        }
    }

    fun onAudioEventsFromGL(events: List<SimAudioEvent>) {
        val vibrate = _saveData.value.settings.vibrationEnabled
        for (ev in events) {
            audioEngine.handleEvent(ev)
            if (vibrate && (ev is SimAudioEvent.WeaponFired || ev is SimAudioEvent.PlayerDamaged || ev is SimAudioEvent.Explosion)) {
                val dur = when (ev) {
                    is SimAudioEvent.Explosion -> 65L
                    is SimAudioEvent.PlayerDamaged -> 45L
                    else -> 16L
                }
                triggerHaptic(dur)
            }
            if (ev is SimAudioEvent.Checkpoint) {
                activeSimRef.get()?.let { sim ->
                    val cp = synchronized(sim.simLock) { sim.latestCheckpoint }
                    updateSave { it.copy(lastCheckpoint = cp) }
                }
            }
        }
    }

    fun onAppPaused() {
        audioEngine.setPaused(true)
        pauseGame()
    }

    fun onAppResumed() {
        audioEngine.setPaused(false)
    }

    fun pauseGame() {
        inputBridge.reset()
        activeSimRef.get()?.let { sim ->
            val snap = synchronized(sim.simLock) {
                if (sim.status == MissionRunStatus.ACTIVE) {
                    sim.status = MissionRunStatus.PAUSED
                    sim.buildHudSnapshot()
                } else null
            }
            if (snap != null) {
                _hudSnapshot.value = snap
            }
        }
    }

    fun resumeGame() {
        activeSimRef.get()?.let { sim ->
            val snap = synchronized(sim.simLock) {
                if (sim.status == MissionRunStatus.PAUSED) {
                    sim.status = MissionRunStatus.ACTIVE
                    sim.buildHudSnapshot()
                } else null
            }
            if (snap != null) {
                _hudSnapshot.value = snap
            }
        }
    }

    fun skipMissionIntro() {
        activeSimRef.get()?.let { sim ->
            val snap = synchronized(sim.simLock) {
                sim.skipIntro()
                sim.buildHudSnapshot()
            }
            _hudSnapshot.value = snap
        }
    }

    fun restoreLastCheckpoint() {
        activeSimRef.get()?.let { sim ->
            inputBridge.reset()
            resultProcessedForCurrentRun.set(false)
            val snap = synchronized(sim.simLock) {
                sim.restoreFromCheckpoint()
                sim.buildHudSnapshot()
            }
            _hudSnapshot.value = snap
            _screen.value = GameScreen.GAMEPLAY
        }
    }

    fun restartCurrentMission() {
        when (lastLaunchedMode) {
            GameMode.CAMPAIGN -> {
                val num = activeSimRef.get()?.mission?.number ?: _saveData.value.lastPlayedMission
                launchMission(num.coerceIn(1, 12), useCheckpoint = false)
            }
            GameMode.SURVIVAL -> launchSurvival(lastArenaCity, lastArenaWeather)
            GameMode.DEFENSE -> launchDefense(lastArenaCity, lastArenaWeather)
            GameMode.CHALLENGES -> launchChallenge(lastChallengeId ?: GameModeCatalog.challenges.first().id)
        }
    }

    fun exitGameplayToMenu() {
        activeSimRef.get()?.let { sim ->
            val cpToSave = synchronized(sim.simLock) {
                if (sim.mode == GameMode.CAMPAIGN &&
                    (sim.status == MissionRunStatus.ACTIVE || sim.status == MissionRunStatus.PAUSED)
                ) {
                    sim.latestCheckpoint
                } else null
            }
            if (cpToSave != null) {
                updateSave { it.copy(lastCheckpoint = cpToSave) }
            }
        }
        activeSimRef.set(null)
        inputBridge.reset()
        audioEngine.setCombatMusicMode(false)
        _screen.value = GameScreen.MAIN_MENU
    }

    private fun applyMissionOutcome(summary: MissionSummary) {
        val newlyUnlocked = mutableListOf<String>()
        updateSave { current ->
            val isCampaign = summary.gameMode == GameMode.CAMPAIGN
            val newCompleted = if (isCampaign && summary.victory) current.completedMissions + summary.missionNumber else current.completedMissions
            val newHighest = if (isCampaign && summary.victory) maxOf(current.highestUnlockedMission, (summary.missionNumber + 1).coerceAtMost(12)) else current.highestUnlockedMission
            val bestStars = current.missionBestStars.toMutableMap()
            if (isCampaign && summary.victory) {
                bestStars[summary.missionNumber] = maxOf(bestStars[summary.missionNumber] ?: 0, summary.stars)
            }

            val newWeapons = current.unlockedWeapons.toMutableSet()
            val newClaimed = current.claimedMissionRewards.toMutableSet()
            if (isCampaign && summary.victory && summary.missionNumber !in newClaimed) {
                val missionDef = MissionCatalog.missionByNumber(summary.missionNumber)
                newClaimed.add(summary.missionNumber)
                missionDef.unlockWeaponReward?.let { newWeapons.add(it) }
            }

            // Multi-Mode Records (Survival / Defense / Challenges)
            val survWaves = current.survivalBestWaves.toMutableMap()
            var survScore = current.survivalBestScore
            val defWaves = current.defenseBestWaves.toMutableMap()
            var defVictories = current.defenseVictories
            val chalScores = current.challengeBestScores.toMutableMap()
            val chalMedals = current.challengeMedals.toMutableMap()
            val completedChals = current.completedChallenges.toMutableSet()

            when (summary.gameMode) {
                GameMode.SURVIVAL -> {
                    survWaves[summary.cityId] = maxOf(survWaves[summary.cityId] ?: 0, summary.waveReached)
                    survScore = maxOf(survScore, summary.score)
                }
                GameMode.DEFENSE -> {
                    defWaves[summary.cityId] = maxOf(defWaves[summary.cityId] ?: 0, summary.waveReached)
                    if (summary.victory) defVictories += 1
                }
                GameMode.CHALLENGES -> {
                    summary.challengeId?.let { cId ->
                        chalScores[cId] = maxOf(chalScores[cId] ?: 0, summary.score)
                        if (summary.victory) {
                            completedChals.add(cId)
                            val prev = chalMedals[cId] ?: ChallengeMedal.NONE
                            if (summary.challengeMedal.tier > prev.tier) {
                                chalMedals[cId] = summary.challengeMedal
                            }
                        }
                    }
                }
                GameMode.CAMPAIGN -> Unit
            }

            val updatedWeaponKills = current.weaponKills.toMutableMap()
            summary.weaponKills.forEach { (w, k) -> updatedWeaponKills[w] = (updatedWeaponKills[w] ?: 0) + k }

            val totalKills = current.totalKills + summary.kills
            val totalHeadshots = current.totalHeadshots + summary.headshots
            val totalShotsFired = current.totalShotsFired + summary.shotsFired
            val totalShotsHit = current.totalShotsHit + summary.shotsHit
            val runAccuracy = if (summary.shotsFired > 0) (summary.shotsHit * 100) / summary.shotsFired else 0

            val ach = current.unlockedAchievements.toMutableSet()
            var bonusXp = 0
            fun grant(id: String, condition: Boolean) {
                if (condition && id !in ach) {
                    ach.add(id)
                    newlyUnlocked.add(id)
                    bonusXp += AchievementCatalog.all.firstOrNull { it.id == id }?.xpBonus ?: 0
                }
            }
            grant("ach_first_blood", 1 in newCompleted)
            grant("ach_sharpshooter", summary.victory && summary.shotsFired >= 5 && runAccuracy >= 45)
            grant("ach_headhunter", totalHeadshots >= 15)
            grant("ach_survivor", summary.victory && summary.remainingHealth >= 50f)
            grant("ach_urban_hero", setOf(1, 2, 3).all { it in newCompleted })
            grant("ach_desert_storm", setOf(4, 5, 6).all { it in newCompleted })
            grant("ach_harbor_master", setOf(7, 8, 9).all { it in newCompleted })
            grant("ach_weapon_master", newWeapons.size >= 6)
            grant("ach_veteran_50", totalKills >= 50)
            grant("ach_campaign_victory", (1..12).all { it in newCompleted })

            current.copy(
                highestUnlockedMission = newHighest,
                completedMissions = newCompleted,
                missionBestStars = bestStars,
                claimedMissionRewards = newClaimed,
                xp = current.xp + summary.xpEarned + bonusXp,
                totalKills = totalKills,
                totalHeadshots = totalHeadshots,
                totalShotsFired = totalShotsFired,
                totalShotsHit = totalShotsHit,
                weaponKills = updatedWeaponKills,
                unlockedWeapons = newWeapons,
                unlockedAchievements = ach,
                lastCheckpoint = if (isCampaign && summary.victory) null else current.lastCheckpoint,
                survivalBestWaves = survWaves,
                survivalBestScore = survScore,
                defenseBestWaves = defWaves,
                defenseVictories = defVictories,
                challengeBestScores = chalScores,
                challengeMedals = chalMedals,
                completedChallenges = completedChals
            )
        }
        _newlyUnlockedAchievements.value = newlyUnlocked
    }

    // --- Armory & Customization Actions ---

    fun unlockOrEquipWeapon(weaponId: WeaponId, asSecondary: Boolean = false) {
        audioEngine.playUiClick()
        val weapon = WeaponCatalog.byId(weaponId)
        updateSave { cur ->
            val unlocked = cur.unlockedWeapons.toMutableSet()
            var xp = cur.xp
            if (weaponId !in unlocked) {
                if (xp < weapon.xpCost) return@updateSave cur
                xp -= weapon.xpCost
                unlocked.add(weaponId)
            }
            when {
                weapon.isSidearm -> cur.copy(xp = xp, unlockedWeapons = unlocked, equippedSidearm = weaponId)
                asSecondary -> cur.copy(xp = xp, unlockedWeapons = unlocked, equippedSecondary = weaponId)
                else -> cur.copy(xp = xp, unlockedWeapons = unlocked, equippedPrimary = weaponId)
            }
        }
    }

    fun unlockOrEquipAttachment(weaponId: WeaponId, slot: AttachmentSlot, attachmentId: String) {
        audioEngine.playUiClick()
        val item = WeaponCatalog.attachmentById(attachmentId) ?: return
        updateSave { cur ->
            val unlocked = cur.unlockedAttachments.toMutableSet()
            var xp = cur.xp
            if (attachmentId !in unlocked) {
                if (xp < item.xpCost) return@updateSave cur
                xp -= item.xpCost
                unlocked.add(attachmentId)
            }
            val map = cur.weaponAttachments.toMutableMap()
            val wSlots = (map[weaponId] ?: WeaponCatalog.defaultAttachments()).toMutableMap()
            wSlots[slot] = attachmentId
            map[weaponId] = wSlots
            cur.copy(xp = xp, unlockedAttachments = unlocked, weaponAttachments = map)
        }
    }

    fun unlockOrEquipCharacterGear(slot: CharacterSlot, gearId: String) {
        audioEngine.playUiClick()
        val item = CustomizationCatalog.itemById(gearId) ?: return
        updateSave { cur ->
            val unlocked = cur.unlockedCharacterGear.toMutableSet()
            var xp = cur.xp
            if (gearId !in unlocked) {
                if (xp < item.xpCost) return@updateSave cur
                xp -= item.xpCost
                unlocked.add(gearId)
            }
            val equipped = cur.equippedCharacterGear.toMutableMap()
            equipped[slot] = gearId
            cur.copy(xp = xp, unlockedCharacterGear = unlocked, equippedCharacterGear = equipped)
        }
    }

    fun applyCharacterPreset(presetId: String) {
        audioEngine.playUiClick()
        val preset = CustomizationCatalog.presets.firstOrNull { it.id == presetId } ?: return
        updateSave { cur ->
            val unlocked = cur.unlockedCharacterGear.toMutableSet()
            unlocked.addAll(preset.gear.values)
            cur.copy(unlockedCharacterGear = unlocked, equippedCharacterGear = preset.gear)
        }
    }

    fun updateSettings(transform: (GameSettings) -> GameSettings) {
        updateSave { cur ->
            val next = transform(cur.settings)
            audioEngine.updateSettings(next)
            activeSimRef.get()?.autoFireEnabled = next.autoFireEnabled
            cur.copy(settings = next)
        }
    }

    fun updateControlLayout(layout: ControlLayoutSettings) {
        updateSettings { it.copy(controls = layout) }
    }

    fun toggleLanguage() {
        audioEngine.playUiClick()
        updateSettings {
            val next = if (it.language == GameLanguage.ARABIC) GameLanguage.ENGLISH else GameLanguage.ARABIC
            it.copy(language = next)
        }
    }

    private fun updateSave(transform: (PlayerSaveData) -> PlayerSaveData) {
        synchronized(saveLock) {
            val next = transform(_saveData.value)
            _saveData.value = next
            saveManager.save(next)
        }
    }

    private fun triggerHaptic(durationMs: Long) {
        try {
            val ctx = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Vibrator::class.java)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Throwable) {
            // Safe ignore in tests or devices without vibrator
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
