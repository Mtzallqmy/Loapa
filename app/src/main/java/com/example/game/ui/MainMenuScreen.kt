package com.example.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.game.data.CityId
import com.example.game.data.GameLanguage
import com.example.game.localization.GameStrings
import com.example.game.save.PlayerSaveData
import com.example.ui.theme.DesertGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalEmerald
import com.example.ui.theme.TacticalSlateDark
import com.example.ui.theme.TacticalSlateSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    language: GameLanguage,
    onComplete: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700),
        label = "splash_alpha"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(1750)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1A2530), ObsidianBlack)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha)
        ) {
            TacticalCrestEmblem(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, DesertGold, RoundedCornerShape(16.dp))
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "جبهات القتال",
                style = MaterialTheme.typography.displayLarge,
                color = DesertGold
            )
            Text(
                text = "COMBAT FRONTS  •  ${GameStrings.GAME_VERSION}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = GameStrings.DEVELOPER_CREDIT_AR,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun MainMenuScreen(
    saveData: PlayerSaveData,
    onPlay: () -> Unit,
    onContinue: () -> Unit,
    onMissions: () -> Unit,
    onArmory: () -> Unit,
    onCustomization: () -> Unit,
    onAchievements: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onToggleLanguage: () -> Unit
) {
    val lang = saveData.settings.language
    Box(modifier = Modifier.fillMaxSize()) {
        HeroMainMenuBackdrop(modifier = Modifier.fillMaxSize())
        // Rich cinematic military vignette scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xE80A0E12),
                            Color(0xB80B1016),
                            Color(0x700A0E12)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Primary Tactical Command Column (Hero Title + Rank + Primary CTAs)
            Column(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xCC16241D),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, TacticalEmerald.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.WifiOff, contentDescription = null, tint = TacticalEmerald, modifier = Modifier.size(13.dp))
                                Text(GameStrings.offlineBadge(lang), style = MaterialTheme.typography.labelSmall, color = TacticalEmerald)
                            }
                        }
                        Surface(
                            color = Color(0xBB19222C),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, TacticalBorder),
                            onClick = onToggleLanguage,
                            modifier = Modifier.testTag("language_toggle_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Translate, contentDescription = "Language", tint = DesertGold, modifier = Modifier.size(13.dp))
                                Text(
                                    if (lang == GameLanguage.ARABIC) "EN" else "العربية",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DesertGold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = GameStrings.appTitle(lang),
                        style = MaterialTheme.typography.displayLarge,
                        color = DesertGold,
                        modifier = Modifier.testTag("main_menu_title")
                    )
                    Text(
                        text = GameStrings.appTagline(lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                // Operator Rank & Campaign Progress Card
                Surface(
                    color = Color(0xCC11181F),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, TacticalBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = DesertGold, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "${GameStrings.rankLabel(lang)} ${saveData.rankLevel}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                "${saveData.xp} ${GameStrings.xpLabel(lang)}  •  ${saveData.completedMissions.size}/12",
                                style = MaterialTheme.typography.labelMedium,
                                color = DesertGold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { saveData.rankXpProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = DesertGold,
                            trackColor = TacticalSlateSurface
                        )
                    }
                }

                // Developer Credit Footer
                Text(
                    text = if (lang == GameLanguage.ARABIC) GameStrings.DEVELOPER_CREDIT_AR else GameStrings.DEVELOPER_CREDIT_EN,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            // Right Tactical 8-Button Action Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    MenuActionTile(
                        label = GameStrings.menuPlay(lang),
                        icon = Icons.Default.PlayArrow,
                        primary = true,
                        testTag = "menu_play_btn",
                        onClick = onPlay
                    )
                }
                item {
                    MenuActionTile(
                        label = GameStrings.menuContinue(lang),
                        icon = Icons.Default.Replay,
                        primary = false,
                        accent = TacticalEmerald,
                        testTag = "menu_continue_btn",
                        onClick = onContinue
                    )
                }
                item {
                    MenuActionTile(
                        label = GameStrings.menuMissions(lang),
                        icon = Icons.Default.Map,
                        testTag = "menu_missions_btn",
                        onClick = onMissions
                    )
                }
                item {
                    MenuActionTile(
                        label = GameStrings.menuArmory(lang),
                        icon = Icons.Default.GpsFixed,
                        testTag = "menu_armory_btn",
                        onClick = onArmory
                    )
                }
                item {
                    MenuActionTile(
                        label = GameStrings.menuCustomization(lang),
                        icon = Icons.Default.Person,
                        testTag = "menu_customization_btn",
                        onClick = onCustomization
                    )
                }
                item {
                    MenuActionTile(
                        label = GameStrings.menuAchievements(lang),
                        icon = Icons.Default.EmojiEvents,
                        testTag = "menu_achievements_btn",
                        onClick = onAchievements
                    )
                }
                item {
                    MenuActionTile(
                        label = GameStrings.menuSettings(lang),
                        icon = Icons.Default.Settings,
                        testTag = "menu_settings_btn",
                        onClick = onSettings
                    )
                }
                item {
                    MenuActionTile(
                        label = GameStrings.menuAbout(lang),
                        icon = Icons.Default.Info,
                        testTag = "menu_about_btn",
                        onClick = onAbout
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuActionTile(
    label: String,
    icon: ImageVector,
    primary: Boolean = false,
    accent: Color = DesertGold,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (primary) DesertGold else Color(0xD6151E27),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (primary) 0.dp else 1.dp, if (primary) Color.Transparent else accent.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (primary) ObsidianBlack else accent,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (primary) ObsidianBlack else TextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CampaignIntroScreen(
    language: GameLanguage,
    onDeployMission1: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        CityArtworkCanvas(
            cityId = CityId.RIVER_CITY,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.36f)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(onClick = onBack, modifier = Modifier.testTag("intro_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = GameStrings.back(language))
                    Spacer(Modifier.width(6.dp))
                    Text(GameStrings.back(language))
                }
                Text(
                    text = if (language == GameLanguage.ARABIC) "مقدمة الحملة العسكرية — جبهات القتال" else "CAMPAIGN BRIEFING — COMBAT FRONTS",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DesertGold
                )
            }

            Surface(
                color = TacticalSlateDark.copy(alpha = 0.90f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, DesertGold.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (language == GameLanguage.ARABIC)
                            "تمتد عملياتك عبر أربع مدن افتراضية استراتيجية: مدينة النهر الحضرية، صحراء السراب، ميناء المرسى الساحلي، وحصن مدينة الجبل."
                        else
                            "Your 12-mission tactical campaign spans four distinct fictional theaters: urban River City, arid Mirage City, coastal Harbor City, and Mountain City stronghold.",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (language == GameLanguage.ARABIC)
                            "استخدم السواتر، طوّر أسلحتك الثمانية، نسّق ذخيرتك، واقضِ على فرق العدو الذكية للوصول إلى المواجهة الأخيرة فوق القمة."
                        else
                            "Use tactical cover, customize 8 weapon platforms, manage ammunition, and outmaneuver coordinated enemy squads on your path to the summit showdown.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = onDeployMission1,
                    colors = ButtonDefaults.buttonColors(containerColor = DesertGold, contentColor = ObsidianBlack),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("intro_deploy_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(GameStrings.startMission(language), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
