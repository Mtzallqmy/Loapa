package com.example.game.save

import com.example.game.data.AttachmentSlot
import com.example.game.data.CameraShoulder
import com.example.game.data.ChallengeMedal
import com.example.game.data.CharacterSlot
import com.example.game.data.CityId
import com.example.game.data.CrosshairStyle
import com.example.game.data.CustomizationCatalog
import com.example.game.data.Difficulty
import com.example.game.data.GameLanguage
import com.example.game.data.GraphicsQuality
import com.example.game.data.WeaponCatalog
import com.example.game.data.WeaponId
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Atomic, versioned offline SaveManager with backup recovery and schema migration.
 * Never silently erases saved progress on partial/interrupted writes.
 */
class SaveManager(private val saveDir: File) {
    private val primaryFile: File get() = File(saveDir, "combat_fronts_save.json")
    private val backupFile: File get() = File(saveDir, "combat_fronts_save.bak")
    private val tempFile: File get() = File(saveDir, "combat_fronts_save.tmp")

    @Synchronized
    fun load(): PlayerSaveData {
        val primary = tryParseFile(primaryFile)
        if (primary != null) return primary
        val backup = tryParseFile(backupFile)
        if (backup != null) {
            // Self-heal primary from verified backup
            save(backup)
            return backup
        }
        return PlayerSaveData()
    }

    @Synchronized
    fun save(data: PlayerSaveData): Boolean {
        return try {
            if (!saveDir.exists()) saveDir.mkdirs()
            val payload = serialize(data).toString(2).toByteArray(Charsets.UTF_8)
            FileOutputStream(tempFile).use { fos ->
                fos.write(payload)
                fos.flush()
                try {
                    fos.fd.sync()
                } catch (_: Exception) {
                    // Ignore fsync unsupported on some virtual filesystems
                }
            }
            if (primaryFile.exists() && primaryFile.length() > 0) {
                primaryFile.copyTo(backupFile, overwrite = true)
            }
            if (!tempFile.renameTo(primaryFile)) {
                tempFile.copyTo(primaryFile, overwrite = true)
                tempFile.delete()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun tryParseFile(file: File): PlayerSaveData? {
        if (!file.exists() || file.length() <= 2) return null
        return try {
            val raw = file.readText(Charsets.UTF_8)
            deserialize(JSONObject(raw))
        } catch (_: Exception) {
            null
        }
    }

    fun serialize(d: PlayerSaveData): JSONObject {
        val root = JSONObject()
        root.put("schemaVersion", SAVE_SCHEMA_VERSION)
        root.put("highestUnlockedMission", d.highestUnlockedMission)
        root.put("lastPlayedMission", d.lastPlayedMission)
        root.put("completedMissions", JSONArray(d.completedMissions.sorted()))
        root.put("claimedMissionRewards", JSONArray(d.claimedMissionRewards.sorted()))

        val starsObj = JSONObject()
        d.missionBestStars.forEach { (k, v) -> starsObj.put(k.toString(), v) }
        root.put("missionBestStars", starsObj)

        root.put("xp", d.xp)
        root.put("totalKills", d.totalKills)
        root.put("totalHeadshots", d.totalHeadshots)
        root.put("totalShotsFired", d.totalShotsFired)
        root.put("totalShotsHit", d.totalShotsHit)

        val wkObj = JSONObject()
        d.weaponKills.forEach { (w, count) -> wkObj.put(w.id, count) }
        root.put("weaponKills", wkObj)

        root.put("unlockedWeapons", JSONArray(d.unlockedWeapons.map { it.id }))
        root.put("equippedPrimary", d.equippedPrimary.id)
        root.put("equippedSecondary", d.equippedSecondary.id)
        root.put("equippedSidearm", d.equippedSidearm.id)
        root.put("unlockedAttachments", JSONArray(d.unlockedAttachments.sorted()))

        val waObj = JSONObject()
        d.weaponAttachments.forEach { (w, slotMap) ->
            val sObj = JSONObject()
            slotMap.forEach { (slot, attId) -> sObj.put(slot.id, attId) }
            waObj.put(w.id, sObj)
        }
        root.put("weaponAttachments", waObj)

        root.put("unlockedCharacterGear", JSONArray(d.unlockedCharacterGear.sorted()))
        val cgObj = JSONObject()
        d.equippedCharacterGear.forEach { (slot, gearId) -> cgObj.put(slot.id, gearId) }
        root.put("equippedCharacterGear", cgObj)
        root.put("unlockedAchievements", JSONArray(d.unlockedAchievements.sorted()))

        d.lastCheckpoint?.let { cp ->
            val cpObj = JSONObject()
            cpObj.put("missionNumber", cp.missionNumber)
            cpObj.put("objectiveIndex", cp.objectiveIndex)
            cpObj.put("playerX", cp.playerX.toDouble())
            cpObj.put("playerZ", cp.playerZ.toDouble())
            cpObj.put("health", cp.health.toDouble())
            cpObj.put("armor", cp.armor.toDouble())
            cpObj.put("ammoInMag", cp.ammoInMag)
            cpObj.put("ammoReserve", cp.ammoReserve)
            root.put("lastCheckpoint", cpObj)
        }

        // v2.1.0 Schema v4 Multi-Mode Records
        val survWavesObj = JSONObject()
        d.survivalBestWaves.forEach { (city, wave) -> survWavesObj.put(city.id, wave) }
        root.put("survivalBestWaves", survWavesObj)
        root.put("survivalBestScore", d.survivalBestScore)

        val defWavesObj = JSONObject()
        d.defenseBestWaves.forEach { (city, wave) -> defWavesObj.put(city.id, wave) }
        root.put("defenseBestWaves", defWavesObj)
        root.put("defenseVictories", d.defenseVictories)

        val chalScoresObj = JSONObject()
        d.challengeBestScores.forEach { (id, score) -> chalScoresObj.put(id, score) }
        root.put("challengeBestScores", chalScoresObj)

        val chalMedalsObj = JSONObject()
        d.challengeMedals.forEach { (id, medal) -> chalMedalsObj.put(id, medal.id) }
        root.put("challengeMedals", chalMedalsObj)
        root.put("completedChallenges", JSONArray(d.completedChallenges.sorted()))

        val s = d.settings
        val sObj = JSONObject()
        sObj.put("language", s.language.code)
        sObj.put("difficulty", s.difficulty.id)
        sObj.put("graphicsQuality", s.graphicsQuality.id)
        sObj.put("masterVolume", s.masterVolume.toDouble())
        sObj.put("sfxVolume", s.sfxVolume.toDouble())
        sObj.put("musicVolume", s.musicVolume.toDouble())
        sObj.put("ambientVolume", s.ambientVolume.toDouble())
        sObj.put("cameraSensitivity", s.cameraSensitivity.toDouble())
        sObj.put("aimSensitivity", s.aimSensitivity.toDouble())
        sObj.put("invertY", s.invertY)
        sObj.put("aimAssist", s.aimAssist)
        sObj.put("vibrationEnabled", s.vibrationEnabled)
        sObj.put("crosshairStyle", s.crosshairStyle.id)
        sObj.put("subtitlesEnabled", s.subtitlesEnabled)
        sObj.put("textScale", s.textScale.toDouble())
        sObj.put("reduceScreenShake", s.reduceScreenShake)
        sObj.put("reduceIntenseEffects", s.reduceIntenseEffects)
        sObj.put("cameraShoulder", s.cameraShoulder.id)

        val c = s.controls
        val cObj = JSONObject()
        cObj.put("leftHanded", c.leftHanded)
        cObj.put("buttonScale", c.buttonScale.toDouble())
        cObj.put("buttonOpacity", c.buttonOpacity.toDouble())
        cObj.put("joystickScale", c.joystickScale.toDouble())
        cObj.put("joystickDeadzone", c.joystickDeadzone.toDouble())
        cObj.put("fireDx", c.fireOffset.dx.toDouble()); cObj.put("fireDy", c.fireOffset.dy.toDouble())
        cObj.put("aimDx", c.aimOffset.dx.toDouble()); cObj.put("aimDy", c.aimOffset.dy.toDouble())
        cObj.put("reloadDx", c.reloadOffset.dx.toDouble()); cObj.put("reloadDy", c.reloadOffset.dy.toDouble())
        cObj.put("jumpDx", c.jumpOffset.dx.toDouble()); cObj.put("jumpDy", c.jumpOffset.dy.toDouble())
        cObj.put("crouchDx", c.crouchOffset.dx.toDouble()); cObj.put("crouchDy", c.crouchOffset.dy.toDouble())
        cObj.put("coverDx", c.coverOffset.dx.toDouble()); cObj.put("coverDy", c.coverOffset.dy.toDouble())
        sObj.put("controls", cObj)
        root.put("settings", sObj)
        return root
    }

    fun deserialize(root: JSONObject): PlayerSaveData {
        val version = root.optInt("schemaVersion", 1)
        val completed = mutableSetOf<Int>()
        root.optJSONArray("completedMissions")?.let { arr ->
            for (i in 0 until arr.length()) completed.add(arr.optInt(i))
        }
        val claimed = mutableSetOf<Int>()
        root.optJSONArray("claimedMissionRewards")?.let { arr ->
            for (i in 0 until arr.length()) claimed.add(arr.optInt(i))
        }
        if (version < 2 && claimed.isEmpty() && completed.isNotEmpty()) {
            claimed.addAll(completed)
        }

        val stars = mutableMapOf<Int, Int>()
        root.optJSONObject("missionBestStars")?.let { obj ->
            obj.keys().forEach { k -> k.toIntOrNull()?.let { stars[it] = obj.optInt(k, 1) } }
        }

        val unlockedWeapons = mutableSetOf(
            WeaponId.VX74_ASSAULT, WeaponId.KSR9_SMG, WeaponId.SR308_PRECISION, WeaponId.M9S_SIDEARM
        )
        root.optJSONArray("unlockedWeapons")?.let { arr ->
            for (i in 0 until arr.length()) {
                val id = arr.optString(i)
                WeaponId.entries.firstOrNull { it.id == id }?.let { unlockedWeapons.add(it) }
            }
        }

        val weaponKills = mutableMapOf<WeaponId, Int>()
        root.optJSONObject("weaponKills")?.let { obj ->
            obj.keys().forEach { k ->
                WeaponId.entries.firstOrNull { it.id == k }?.let { weaponKills[it] = obj.optInt(k, 0) }
            }
        }

        val unlockedAtts = WeaponCatalog.defaultAttachments().values.toMutableSet()
        root.optJSONArray("unlockedAttachments")?.let { arr ->
            for (i in 0 until arr.length()) unlockedAtts.add(arr.optString(i))
        }

        val weaponAtts = mutableMapOf<WeaponId, Map<AttachmentSlot, String>>()
        root.optJSONObject("weaponAttachments")?.let { waObj ->
            waObj.keys().forEach { wKey ->
                val wId = WeaponId.entries.firstOrNull { it.id == wKey } ?: return@forEach
                val sObj = waObj.optJSONObject(wKey) ?: return@forEach
                val map = mutableMapOf<AttachmentSlot, String>()
                sObj.keys().forEach { sKey ->
                    val slot = AttachmentSlot.entries.firstOrNull { it.id == sKey } ?: return@forEach
                    map[slot] = sObj.optString(sKey)
                }
                weaponAtts[wId] = map
            }
        }

        val unlockedGear = (CustomizationCatalog.defaultGear().values + "pack_none").toMutableSet()
        root.optJSONArray("unlockedCharacterGear")?.let { arr ->
            for (i in 0 until arr.length()) unlockedGear.add(arr.optString(i))
        }

        val equippedGear = CustomizationCatalog.defaultGear().toMutableMap()
        root.optJSONObject("equippedCharacterGear")?.let { cgObj ->
            cgObj.keys().forEach { sKey ->
                val slot = CharacterSlot.entries.firstOrNull { it.id == sKey } ?: return@forEach
                equippedGear[slot] = cgObj.optString(sKey)
            }
        }

        val achievements = mutableSetOf<String>()
        root.optJSONArray("unlockedAchievements")?.let { arr ->
            for (i in 0 until arr.length()) achievements.add(arr.optString(i))
        }

        val checkpoint = root.optJSONObject("lastCheckpoint")?.let { cp ->
            SavedCheckpoint(
                missionNumber = cp.optInt("missionNumber", 1).coerceIn(1, 12),
                objectiveIndex = cp.optInt("objectiveIndex", 0).coerceAtLeast(0),
                playerX = cp.optDouble("playerX", 0.0).toFloat(),
                playerZ = cp.optDouble("playerZ", 32.0).toFloat(),
                health = cp.optDouble("health", 100.0).toFloat(),
                armor = cp.optDouble("armor", 100.0).toFloat(),
                ammoInMag = cp.optInt("ammoInMag", 30),
                ammoReserve = cp.optInt("ammoReserve", 150)
            )
        }

        val sObj = root.optJSONObject("settings")
        val cObj = sObj?.optJSONObject("controls")
        val controls = ControlLayoutSettings(
            leftHanded = cObj?.optBoolean("leftHanded", false) ?: false,
            buttonScale = (cObj?.optDouble("buttonScale", 1.0) ?: 1.0).toFloat().coerceIn(0.75f, 1.35f),
            buttonOpacity = (cObj?.optDouble("buttonOpacity", 0.84) ?: 0.84).toFloat().coerceIn(0.35f, 1.0f),
            joystickScale = (cObj?.optDouble("joystickScale", 1.0) ?: 1.0).toFloat().coerceIn(0.75f, 1.35f),
            joystickDeadzone = (cObj?.optDouble("joystickDeadzone", 0.12) ?: 0.12).toFloat().coerceIn(0.04f, 0.3f),
            fireOffset = ControlOffset(
                (cObj?.optDouble("fireDx", 0.0) ?: 0.0).toFloat(),
                (cObj?.optDouble("fireDy", 0.0) ?: 0.0).toFloat()
            ),
            aimOffset = ControlOffset(
                (cObj?.optDouble("aimDx", 0.0) ?: 0.0).toFloat(),
                (cObj?.optDouble("aimDy", 0.0) ?: 0.0).toFloat()
            ),
            reloadOffset = ControlOffset(
                (cObj?.optDouble("reloadDx", 0.0) ?: 0.0).toFloat(),
                (cObj?.optDouble("reloadDy", 0.0) ?: 0.0).toFloat()
            ),
            jumpOffset = ControlOffset(
                (cObj?.optDouble("jumpDx", 0.0) ?: 0.0).toFloat(),
                (cObj?.optDouble("jumpDy", 0.0) ?: 0.0).toFloat()
            ),
            crouchOffset = ControlOffset(
                (cObj?.optDouble("crouchDx", 0.0) ?: 0.0).toFloat(),
                (cObj?.optDouble("crouchDy", 0.0) ?: 0.0).toFloat()
            ),
            coverOffset = ControlOffset(
                (cObj?.optDouble("coverDx", 0.0) ?: 0.0).toFloat(),
                (cObj?.optDouble("coverDy", 0.0) ?: 0.0).toFloat()
            )
        )

        val settings = GameSettings(
            language = GameLanguage.entries.firstOrNull { it.code == sObj?.optString("language") } ?: GameLanguage.ARABIC,
            difficulty = Difficulty.entries.firstOrNull { it.id == sObj?.optString("difficulty") } ?: Difficulty.MEDIUM,
            graphicsQuality = GraphicsQuality.entries.firstOrNull { it.id == sObj?.optString("graphicsQuality") } ?: GraphicsQuality.MEDIUM,
            masterVolume = (sObj?.optDouble("masterVolume", 0.85) ?: 0.85).toFloat().coerceIn(0f, 1f),
            sfxVolume = (sObj?.optDouble("sfxVolume", 0.90) ?: 0.90).toFloat().coerceIn(0f, 1f),
            musicVolume = (sObj?.optDouble("musicVolume", 0.45) ?: 0.45).toFloat().coerceIn(0f, 1f),
            ambientVolume = (sObj?.optDouble("ambientVolume", 0.65) ?: 0.65).toFloat().coerceIn(0f, 1f),
            cameraSensitivity = (sObj?.optDouble("cameraSensitivity", 1.0) ?: 1.0).toFloat().coerceIn(0.4f, 2.2f),
            aimSensitivity = (sObj?.optDouble("aimSensitivity", 0.65) ?: 0.65).toFloat().coerceIn(0.25f, 1.5f),
            invertY = sObj?.optBoolean("invertY", false) ?: false,
            aimAssist = sObj?.optBoolean("aimAssist", true) ?: true,
            vibrationEnabled = sObj?.optBoolean("vibrationEnabled", true) ?: true,
            crosshairStyle = CrosshairStyle.entries.firstOrNull { it.id == sObj?.optString("crosshairStyle") } ?: CrosshairStyle.DYNAMIC,
            subtitlesEnabled = sObj?.optBoolean("subtitlesEnabled", true) ?: true,
            textScale = (sObj?.optDouble("textScale", 1.0) ?: 1.0).toFloat().coerceIn(0.85f, 1.25f),
            reduceScreenShake = sObj?.optBoolean("reduceScreenShake", false) ?: false,
            reduceIntenseEffects = sObj?.optBoolean("reduceIntenseEffects", false) ?: false,
            cameraShoulder = CameraShoulder.entries.firstOrNull { it.id == sObj?.optString("cameraShoulder") } ?: CameraShoulder.RIGHT,
            controls = controls
        )

        val primary = WeaponId.entries.firstOrNull { it.id == root.optString("equippedPrimary") } ?: WeaponId.VX74_ASSAULT
        val secondary = WeaponId.entries.firstOrNull { it.id == root.optString("equippedSecondary") } ?: WeaponId.KSR9_SMG
        val sidearm = WeaponId.entries.firstOrNull { it.id == root.optString("equippedSidearm") } ?: WeaponId.M9S_SIDEARM

        val survivalBestWaves = mutableMapOf<CityId, Int>()
        root.optJSONObject("survivalBestWaves")?.let { obj ->
            obj.keys().forEach { k ->
                CityId.entries.firstOrNull { it.id == k }?.let { survivalBestWaves[it] = obj.optInt(k, 0).coerceAtLeast(0) }
            }
        }
        val defenseBestWaves = mutableMapOf<CityId, Int>()
        root.optJSONObject("defenseBestWaves")?.let { obj ->
            obj.keys().forEach { k ->
                CityId.entries.firstOrNull { it.id == k }?.let { defenseBestWaves[it] = obj.optInt(k, 0).coerceAtLeast(0) }
            }
        }
        val challengeBestScores = mutableMapOf<String, Int>()
        root.optJSONObject("challengeBestScores")?.let { obj ->
            obj.keys().forEach { k -> challengeBestScores[k] = obj.optInt(k, 0).coerceAtLeast(0) }
        }
        val challengeMedals = mutableMapOf<String, ChallengeMedal>()
        root.optJSONObject("challengeMedals")?.let { obj ->
            obj.keys().forEach { k ->
                val mId = obj.optString(k)
                ChallengeMedal.entries.firstOrNull { it.id == mId }?.let { challengeMedals[k] = it }
            }
        }
        val completedChallenges = mutableSetOf<String>()
        root.optJSONArray("completedChallenges")?.let { arr ->
            for (i in 0 until arr.length()) completedChallenges.add(arr.optString(i))
        }

        return PlayerSaveData(
            schemaVersion = SAVE_SCHEMA_VERSION,
            highestUnlockedMission = root.optInt("highestUnlockedMission", 1).coerceIn(1, 12),
            lastPlayedMission = root.optInt("lastPlayedMission", 1).coerceIn(1, 12),
            completedMissions = completed,
            missionBestStars = stars,
            claimedMissionRewards = claimed,
            xp = root.optInt("xp", 0).coerceAtLeast(0),
            totalKills = root.optInt("totalKills", 0).coerceAtLeast(0),
            totalHeadshots = root.optInt("totalHeadshots", 0).coerceAtLeast(0),
            totalShotsFired = root.optInt("totalShotsFired", 0).coerceAtLeast(0),
            totalShotsHit = root.optInt("totalShotsHit", 0).coerceAtLeast(0),
            weaponKills = weaponKills,
            unlockedWeapons = unlockedWeapons,
            equippedPrimary = if (primary in unlockedWeapons) primary else WeaponId.VX74_ASSAULT,
            equippedSecondary = if (secondary in unlockedWeapons) secondary else WeaponId.KSR9_SMG,
            equippedSidearm = if (sidearm in unlockedWeapons) sidearm else WeaponId.M9S_SIDEARM,
            unlockedAttachments = unlockedAtts,
            weaponAttachments = weaponAtts,
            unlockedCharacterGear = unlockedGear,
            equippedCharacterGear = equippedGear,
            unlockedAchievements = achievements,
            lastCheckpoint = checkpoint,
            survivalBestWaves = survivalBestWaves,
            survivalBestScore = root.optInt("survivalBestScore", 0).coerceAtLeast(0),
            defenseBestWaves = defenseBestWaves,
            defenseVictories = root.optInt("defenseVictories", 0).coerceAtLeast(0),
            challengeBestScores = challengeBestScores,
            challengeMedals = challengeMedals,
            completedChallenges = completedChallenges,
            settings = settings
        )
    }
}
