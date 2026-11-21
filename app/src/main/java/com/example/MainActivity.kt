package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.game.ui.AboutCreditsScreen
import com.example.game.ui.AchievementsScreen
import com.example.game.ui.ArmoryScreen
import com.example.game.ui.CampaignIntroScreen
import com.example.game.ui.ControlLayoutEditorScreen
import com.example.game.ui.CustomizationScreen
import com.example.game.ui.GameModesScreen
import com.example.game.ui.GameScreen
import com.example.game.ui.GameViewModel
import com.example.game.ui.GameplayScreen
import com.example.game.ui.MainMenuScreen
import com.example.game.ui.MissionResultScreen
import com.example.game.ui.MissionsScreen
import com.example.game.ui.SettingsScreen
import com.example.game.ui.SplashScreen
import com.example.ui.theme.CombatFrontsTheme

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureImmersiveLandscape()
        setContent {
            CombatFrontsRoot(viewModel = viewModel)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.pauseGame()
    }

    private fun configureImmersiveLandscape() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
fun CombatFrontsRoot(viewModel: GameViewModel) {
    val saveData by viewModel.saveData.collectAsState()
    val screen by viewModel.screen.collectAsState()
    val hud by viewModel.hudSnapshot.collectAsState()
    val lastSummary by viewModel.lastSummary.collectAsState()
    val newAchievements by viewModel.newlyUnlockedAchievements.collectAsState()

    val layoutDir = if (saveData.settings.language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        CombatFrontsTheme(textScale = saveData.settings.textScale) {
            when (screen) {
                GameScreen.SPLASH -> SplashScreen(
                    language = saveData.settings.language,
                    onComplete = { viewModel.finishSplash() }
                )
                GameScreen.MAIN_MENU -> MainMenuScreen(
                    saveData = saveData,
                    onPlay = { viewModel.navigate(GameScreen.GAME_MODES) },
                    onContinue = { viewModel.continueCampaign() },
                    onMissions = { viewModel.navigate(GameScreen.MISSIONS) },
                    onArmory = { viewModel.navigate(GameScreen.ARMORY) },
                    onCustomization = { viewModel.navigate(GameScreen.CUSTOMIZATION) },
                    onAchievements = { viewModel.navigate(GameScreen.ACHIEVEMENTS) },
                    onSettings = { viewModel.navigate(GameScreen.SETTINGS) },
                    onAbout = { viewModel.navigate(GameScreen.ABOUT) },
                    onToggleLanguage = { viewModel.toggleLanguage() }
                )
                GameScreen.GAME_MODES -> GameModesScreen(
                    saveData = saveData,
                    onSelectDifficulty = { diff -> viewModel.updateSettings { it.copy(difficulty = diff) } },
                    onOpenCampaignMissions = { viewModel.navigate(GameScreen.MISSIONS) },
                    onQuickDeployCampaign = { viewModel.startNewCampaignOrMission1() },
                    onLaunchSurvival = { city, weather -> viewModel.launchSurvival(city, weather) },
                    onLaunchDefense = { city, weather -> viewModel.launchDefense(city, weather) },
                    onLaunchChallenge = { chId -> viewModel.launchChallenge(chId) },
                    onBack = { viewModel.navigate(GameScreen.MAIN_MENU) }
                )
                GameScreen.CAMPAIGN_INTRO -> CampaignIntroScreen(
                    language = saveData.settings.language,
                    onDeployMission1 = { viewModel.launchMission(1, useCheckpoint = false) },
                    onBack = { viewModel.navigate(GameScreen.MAIN_MENU) }
                )
                GameScreen.MISSIONS -> MissionsScreen(
                    saveData = saveData,
                    onSelectDifficulty = { diff -> viewModel.updateSettings { it.copy(difficulty = diff) } },
                    onLaunchMission = { num -> viewModel.launchMission(num, useCheckpoint = false) },
                    onBack = { viewModel.navigate(GameScreen.MAIN_MENU) }
                )
                GameScreen.GAMEPLAY -> GameplayScreen(
                    viewModel = viewModel,
                    hud = hud,
                    settings = saveData.settings
                )
                GameScreen.MISSION_RESULT -> {
                    val summary = lastSummary
                    if (summary != null) {
                        MissionResultScreen(
                            summary = summary,
                            language = saveData.settings.language,
                            newlyUnlockedAchievementIds = newAchievements,
                            onNextMission = { viewModel.launchMission((summary.missionNumber + 1).coerceAtMost(12)) },
                            onRetryCheckpoint = { viewModel.restoreLastCheckpoint() },
                            onRestartMission = { viewModel.restartCurrentMission() },
                            onMainMenu = { viewModel.exitGameplayToMenu() }
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            viewModel.navigate(GameScreen.MAIN_MENU)
                        }
                    }
                }
                GameScreen.ARMORY -> ArmoryScreen(
                    saveData = saveData,
                    onUnlockOrEquipWeapon = { id, secondary -> viewModel.unlockOrEquipWeapon(id, secondary) },
                    onUnlockOrEquipAttachment = { w, s, a -> viewModel.unlockOrEquipAttachment(w, s, a) },
                    onBack = { viewModel.navigate(GameScreen.MAIN_MENU) }
                )
                GameScreen.CUSTOMIZATION -> CustomizationScreen(
                    saveData = saveData,
                    onUnlockOrEquipGear = { slot, id -> viewModel.unlockOrEquipCharacterGear(slot, id) },
                    onApplyPreset = { preset -> viewModel.applyCharacterPreset(preset) },
                    onBack = { viewModel.navigate(GameScreen.MAIN_MENU) }
                )
                GameScreen.ACHIEVEMENTS -> AchievementsScreen(
                    saveData = saveData,
                    onBack = { viewModel.navigate(GameScreen.MAIN_MENU) }
                )
                GameScreen.SETTINGS -> SettingsScreen(
                    settings = saveData.settings,
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    onOpenControlEditor = { viewModel.navigate(GameScreen.CONTROL_EDITOR) },
                    onBack = { viewModel.navigate(GameScreen.MAIN_MENU) }
                )
                GameScreen.CONTROL_EDITOR -> ControlLayoutEditorScreen(
                    initialLayout = saveData.settings.controls,
                    language = saveData.settings.language,
                    onSave = { viewModel.updateControlLayout(it) },
                    onBack = { viewModel.navigate(GameScreen.SETTINGS) }
                )
                GameScreen.ABOUT -> AboutCreditsScreen(
                    language = saveData.settings.language,
                    onBack = { viewModel.navigate(GameScreen.MAIN_MENU) }
                )
            }
        }
    }
}
