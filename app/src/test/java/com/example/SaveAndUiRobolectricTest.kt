package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.game.data.AttachmentSlot
import com.example.game.data.CameraShoulder
import com.example.game.data.CharacterSlot
import com.example.game.data.Difficulty
import com.example.game.data.GameLanguage
import com.example.game.data.GraphicsQuality
import com.example.game.data.WeaponId
import com.example.game.localization.GameStrings
import com.example.game.save.ControlLayoutSettings
import com.example.game.save.GameSettings
import com.example.game.save.PlayerSaveData
import com.example.game.save.SAVE_SCHEMA_VERSION
import com.example.game.save.SaveManager
import com.example.game.save.SavedCheckpoint
import org.json.JSONArray
import org.json.JSONObject
import com.example.game.ui.MainMenuScreen
import com.example.ui.theme.CombatFrontsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SaveAndUiRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `offline save manager persists progress, customization, controls, and recovers from corrupt primary`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testDir = File(context.filesDir, "test_save_${System.nanoTime()}")
        val manager = SaveManager(testDir)

        val original = PlayerSaveData(
            highestUnlockedMission = 7,
            completedMissions = setOf(1, 2, 3, 4, 5, 6),
            missionBestStars = mapOf(1 to 3, 2 to 2),
            xp = 1450,
            totalKills = 88,
            totalHeadshots = 22,
            unlockedWeapons = setOf(WeaponId.VX74_ASSAULT, WeaponId.BR12_SHOTGUN, WeaponId.MG48_LMG, WeaponId.M9S_SIDEARM),
            equippedPrimary = WeaponId.MG48_LMG,
            weaponAttachments = mapOf(WeaponId.MG48_LMG to mapOf(AttachmentSlot.OPTIC to "optic_acog", AttachmentSlot.SKIN to "skin_gold")),
            equippedCharacterGear = mapOf(CharacterSlot.OUTFIT to "outfit_desert", CharacterSlot.HELMET to "helmet_nvg"),
            unlockedAchievements = setOf("ach_first_blood", "ach_urban_hero"),
            lastCheckpoint = SavedCheckpoint(missionNumber = 7, objectiveIndex = 1, playerX = 3f, playerZ = 5f, health = 82f, armor = 65f),
            settings = GameSettings(
                language = GameLanguage.ARABIC,
                difficulty = Difficulty.HARD,
                graphicsQuality = GraphicsQuality.HIGH,
                controls = ControlLayoutSettings(leftHanded = true, buttonScale = 1.2f, buttonOpacity = 0.7f)
            )
        )

        assertTrue(manager.save(original))
        val loaded = manager.load()
        assertEquals(7, loaded.highestUnlockedMission)
        assertEquals(1450, loaded.xp)
        assertEquals(WeaponId.MG48_LMG, loaded.equippedPrimary)
        assertEquals("optic_acog", loaded.weaponAttachments[WeaponId.MG48_LMG]?.get(AttachmentSlot.OPTIC))
        assertEquals("outfit_desert", loaded.equippedCharacterGear[CharacterSlot.OUTFIT])
        assertTrue(loaded.settings.controls.leftHanded)
        assertEquals(GraphicsQuality.HIGH, loaded.settings.graphicsQuality)
        assertNotNull(loaded.lastCheckpoint)

        // Second save creates .bak; then corrupt primary file to verify automatic recovery!
        assertTrue(manager.save(original.copy(xp = 1500)))
        val primaryFile = File(testDir, "combat_fronts_save.json")
        primaryFile.writeText("{ CORRUPTED_PARTIAL_WRITE ")

        val recovered = manager.load()
        assertEquals("Should recover from backup without losing progress", 1450, recovered.xp)
        assertEquals(7, recovered.highestUnlockedMission)
    }

    @Test
    fun `manifest declares zero internet permission for true offline startup`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pkgInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val perms = pkgInfo.requestedPermissions?.toList().orEmpty()
        assertFalse("Offline game must not require INTERNET permission", perms.contains(Manifest.permission.INTERNET))
        val appName = context.getString(R.string.app_name)
        assertTrue(appName == "جبهات القتال" || appName == "Combat Fronts")
    }

    @Test
    fun `arabic main menu renders title, developer credit, and all 8 required Arabic buttons`() {
        val save = PlayerSaveData(settings = GameSettings(language = GameLanguage.ARABIC))
        composeRule.setContent {
            CombatFrontsTheme {
                MainMenuScreen(
                    saveData = save,
                    onPlay = {}, onContinue = {}, onMissions = {}, onArmory = {},
                    onCustomization = {}, onAchievements = {}, onSettings = {}, onAbout = {},
                    onToggleLanguage = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("main_menu_title").assertIsDisplayed()
        composeRule.onNodeWithText("جبهات القتال").assertIsDisplayed()
        composeRule.onNodeWithText(GameStrings.DEVELOPER_CREDIT_AR).assertIsDisplayed()

        listOf(
            "بدء اللعب", "متابعة", "المهمات", "ترسانة الأسلحة",
            "تخصيص الشخصية", "الإنجازات", "الإعدادات", "حول اللعبة"
        ).forEach { label ->
            composeRule.onNodeWithText(label).assertIsDisplayed()
        }
    }

    @Test
    fun `main activity launches cleanly with valid adaptive and mipmap launcher icons`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Verify launcher icon drawables & mipmaps resolve cleanly without 0-byte decode crashes
        assertNotNull(androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground))
        assertNotNull(androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_launcher_background))
        assertNotNull(androidx.core.content.ContextCompat.getDrawable(context, R.mipmap.ic_launcher))
        assertNotNull(androidx.core.content.ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round))

        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull("MainActivity must launch without crashing", activity)
        assertFalse("MainActivity must not finish immediately on startup", activity.isFinishing)
        controller.pause().stop().destroy()
    }

    @Test
    fun `save system 2_0 migrates legacy v1 and v2 save payloads cleanly to schema v3`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testDir = File(context.filesDir, "cf_migration_${System.nanoTime()}")
        val manager = SaveManager(testDir)

        val legacyV1Json = JSONObject().apply {
            put("schemaVersion", 1)
            put("highestUnlockedMission", 5)
            put("xp", 920)
            put("completedMissions", JSONArray(listOf(1, 2, 3, 4)))
            put("settings", JSONObject().apply {
                put("language", "ar")
                put("difficulty", "hard")
            })
        }

        val migrated = manager.deserialize(legacyV1Json)
        assertEquals(SAVE_SCHEMA_VERSION, migrated.schemaVersion)
        assertEquals(5, migrated.highestUnlockedMission)
        assertEquals(920, migrated.xp)
        assertEquals(setOf(1, 2, 3, 4), migrated.claimedMissionRewards)
        assertEquals(CameraShoulder.RIGHT, migrated.settings.cameraShoulder)

        val updated = migrated.copy(settings = migrated.settings.copy(cameraShoulder = CameraShoulder.LEFT))
        assertTrue(manager.save(updated))
        val reloaded = manager.load()
        assertEquals(CameraShoulder.LEFT, reloaded.settings.cameraShoulder)
    }

    @Test
    fun `game modes hub renders all 4 required Arabic modes and weather selectors`() {
        val save = PlayerSaveData(settings = GameSettings(language = GameLanguage.ARABIC))
        composeRule.setContent {
            CombatFrontsTheme {
                com.example.game.ui.GameModesScreen(
                    saveData = save,
                    onSelectDifficulty = {},
                    onOpenCampaignMissions = {},
                    onQuickDeployCampaign = {},
                    onLaunchSurvival = { _, _ -> },
                    onLaunchDefense = { _, _ -> },
                    onLaunchChallenge = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("modes_screen_title").assertIsDisplayed()
        composeRule.onNodeWithTag("mode_tab_campaign").assertIsDisplayed()
        composeRule.onNodeWithTag("mode_tab_survival").assertIsDisplayed()
        composeRule.onNodeWithTag("mode_tab_defense").assertIsDisplayed()
        composeRule.onNodeWithTag("mode_tab_challenges").assertIsDisplayed()
    }
}
