package com.example.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.game.data.ChallengeMedal
import com.example.game.data.CityId
import com.example.game.data.Difficulty
import com.example.game.data.GameLanguage
import com.example.game.data.GameMode
import com.example.game.data.GameModeCatalog
import com.example.game.data.MissionCatalog
import com.example.game.data.WeatherCondition
import com.example.game.localization.GameStrings
import com.example.game.save.PlayerSaveData
import com.example.ui.theme.ArmorCyan
import com.example.ui.theme.DesertGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SignalCrimson
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalEmerald
import com.example.ui.theme.TacticalSlateDark
import com.example.ui.theme.TacticalSlateSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GameModesScreen(
    saveData: PlayerSaveData,
    onSelectDifficulty: (Difficulty) -> Unit,
    onOpenCampaignMissions: () -> Unit,
    onQuickDeployCampaign: () -> Unit,
    onLaunchSurvival: (CityId, WeatherCondition) -> Unit,
    onLaunchDefense: (CityId, WeatherCondition) -> Unit,
    onLaunchChallenge: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val lang = saveData.settings.language
    var selectedMode by remember { mutableStateOf(GameMode.CAMPAIGN) }
    var selectedCity by remember { mutableStateOf(CityId.RIVER_CITY) }
    var selectedWeather by remember { mutableStateOf(WeatherCondition.CLEAR_DAY) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Header Bar + Difficulty Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("modes_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = GameStrings.back(lang), tint = DesertGold)
                }
                Text(
                    text = if (lang == GameLanguage.ARABIC) "أوضاع اللعب — جبهات القتال" else "GAME MODES — COMBAT FRONTS",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DesertGold,
                    modifier = Modifier.testTag("modes_screen_title")
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Difficulty.entries.forEach { diff ->
                    val active = saveData.settings.difficulty == diff
                    FilterChip(
                        selected = active,
                        onClick = { onSelectDifficulty(diff) },
                        label = { Text(GameStrings.difficultyName(diff, lang)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DesertGold,
                            selectedLabelColor = ObsidianBlack
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: 4 Playable Mode Tabs
            Column(
                modifier = Modifier
                    .weight(0.75f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeCardButton(
                    mode = GameMode.CAMPAIGN,
                    icon = Icons.Default.Map,
                    accent = DesertGold,
                    selected = selectedMode == GameMode.CAMPAIGN,
                    lang = lang,
                    testTag = "mode_tab_campaign",
                    onClick = { selectedMode = GameMode.CAMPAIGN },
                    modifier = Modifier.weight(1f)
                )
                ModeCardButton(
                    mode = GameMode.SURVIVAL,
                    icon = Icons.Default.LocalFireDepartment,
                    accent = SignalCrimson,
                    selected = selectedMode == GameMode.SURVIVAL,
                    lang = lang,
                    testTag = "mode_tab_survival",
                    onClick = { selectedMode = GameMode.SURVIVAL },
                    modifier = Modifier.weight(1f)
                )
                ModeCardButton(
                    mode = GameMode.DEFENSE,
                    icon = Icons.Default.Shield,
                    accent = ArmorCyan,
                    selected = selectedMode == GameMode.DEFENSE,
                    lang = lang,
                    testTag = "mode_tab_defense",
                    onClick = { selectedMode = GameMode.DEFENSE },
                    modifier = Modifier.weight(1f)
                )
                ModeCardButton(
                    mode = GameMode.CHALLENGES,
                    icon = Icons.Default.EmojiEvents,
                    accent = TacticalEmerald,
                    selected = selectedMode == GameMode.CHALLENGES,
                    lang = lang,
                    testTag = "mode_tab_challenges",
                    onClick = { selectedMode = GameMode.CHALLENGES },
                    modifier = Modifier.weight(1f)
                )
            }

            // Right: Active Mode Configuration & Launch Panel
            Surface(
                color = TacticalSlateDark,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, TacticalBorder),
                modifier = Modifier
                    .weight(1.35f)
                    .fillMaxHeight()
            ) {
                when (selectedMode) {
                    GameMode.CAMPAIGN -> CampaignModeDetailPane(
                        saveData = saveData,
                        lang = lang,
                        onQuickDeploy = onQuickDeployCampaign,
                        onOpenMissions = onOpenCampaignMissions
                    )
                    GameMode.SURVIVAL -> ArenaWeatherConfigPane(
                        mode = GameMode.SURVIVAL,
                        saveData = saveData,
                        selectedCity = selectedCity,
                        selectedWeather = selectedWeather,
                        onSelectCity = { selectedCity = it },
                        onSelectWeather = { selectedWeather = it },
                        onDeploy = { onLaunchSurvival(selectedCity, selectedWeather) },
                        lang = lang
                    )
                    GameMode.DEFENSE -> ArenaWeatherConfigPane(
                        mode = GameMode.DEFENSE,
                        saveData = saveData,
                        selectedCity = selectedCity,
                        selectedWeather = selectedWeather,
                        onSelectCity = { selectedCity = it },
                        onSelectWeather = { selectedWeather = it },
                        onDeploy = { onLaunchDefense(selectedCity, selectedWeather) },
                        lang = lang
                    )
                    GameMode.CHALLENGES -> ChallengesModePane(
                        saveData = saveData,
                        lang = lang,
                        onLaunchChallenge = onLaunchChallenge
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeCardButton(
    mode: GameMode,
    icon: ImageVector,
    accent: Color,
    selected: Boolean,
    lang: GameLanguage,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (selected) accent.copy(alpha = 0.20f) else TacticalSlateDark,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) accent else TacticalBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
            Column {
                Text(
                    text = GameStrings.gameModeName(mode, lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) accent else TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = GameStrings.gameModeSubtitle(mode, lang),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CampaignModeDetailPane(
    saveData: PlayerSaveData,
    lang: GameLanguage,
    onQuickDeploy: () -> Unit,
    onOpenMissions: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(110.dp)) {
            CityArtworkCanvas(cityId = CityId.RIVER_CITY, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, TacticalSlateDark)))
            )
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text(
                    GameStrings.gameModeName(GameMode.CAMPAIGN, lang),
                    style = MaterialTheme.typography.headlineSmall,
                    color = DesertGold
                )
                Text(
                    if (lang == GameLanguage.ARABIC)
                        "التقدم: ${saveData.completedMissions.size}/12 مهمة مكتملة"
                    else
                        "Progress: ${saveData.completedMissions.size}/12 Missions Completed",
                    style = MaterialTheme.typography.labelMedium,
                    color = TacticalEmerald
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f).padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (lang == GameLanguage.ARABIC)
                    "خُض 12 عملية عسكرية متدرجة عبر 4 مدن افتراضية بتقنيات الذكاء الاصطناعي 2.0، والطقس الديناميكي، والمدرّعة التكتيكية القابلة للقيادة."
                else
                    "Deploy across 12 escalating operations in 4 fictional cities featuring Enemy AI 2.0, dynamic weather, interactive worlds, and a drivable Tactical Rover.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onOpenMissions,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("mode_campaign_list_btn")
                ) {
                    Text(GameStrings.menuMissions(lang))
                }
                Button(
                    onClick = onQuickDeploy,
                    colors = ButtonDefaults.buttonColors(containerColor = DesertGold, contentColor = ObsidianBlack),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("mode_campaign_deploy_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(GameStrings.startMission(lang), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ArenaWeatherConfigPane(
    mode: GameMode,
    saveData: PlayerSaveData,
    selectedCity: CityId,
    selectedWeather: WeatherCondition,
    onSelectCity: (CityId) -> Unit,
    onSelectWeather: (WeatherCondition) -> Unit,
    onDeploy: () -> Unit,
    lang: GameLanguage
) {
    val bestWave = if (mode == GameMode.SURVIVAL) {
        saveData.survivalBestWaves[selectedCity] ?: 0
    } else {
        saveData.defenseBestWaves[selectedCity] ?: 0
    }
    val recordSummary = if (mode == GameMode.SURVIVAL) {
        if (lang == GameLanguage.ARABIC)
            "أفضل موجة: $bestWave  •  أعلى نتيجة: ${saveData.survivalBestScore}"
        else
            "Best Wave: $bestWave  •  High Score: ${saveData.survivalBestScore}"
    } else {
        if (lang == GameLanguage.ARABIC)
            "أفضل موجة: $bestWave/5  •  الانتصارات: ${saveData.defenseVictories}"
        else
            "Best Wave: $bestWave/5  •  Defense Victories: ${saveData.defenseVictories}"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    GameStrings.gameModeName(mode, lang),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (mode == GameMode.SURVIVAL) SignalCrimson else ArmorCyan
                )
                Text(recordSummary, style = MaterialTheme.typography.labelMedium, color = DesertGold)
            }

            // 4-City Selector
            Text(
                if (lang == GameLanguage.ARABIC) "اختر المدينة:" else "Select City:",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MissionCatalog.cities.forEach { city ->
                    val active = city.id == selectedCity
                    Surface(
                        onClick = { onSelectCity(city.id) },
                        color = if (active) DesertGold.copy(alpha = 0.22f) else TacticalSlateSurface,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (active) DesertGold else TacticalBorder),
                        modifier = Modifier.weight(1f).height(42.dp).testTag("arena_city_${city.id.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                            Text(
                                city.name.get(lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) DesertGold else TextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 7-Condition Dynamic Weather Selector
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.WbSunny, contentDescription = null, tint = DesertGold, modifier = Modifier.size(16.dp))
                Text(
                    if (lang == GameLanguage.ARABIC)
                        "الطقس الديناميكي (يؤثر على مدى رؤية العدو: ${(selectedWeather.aiVisionMultiplier * 100).toInt()}%):"
                    else
                        "Dynamic Weather (Enemy Sight Range: ${(selectedWeather.aiVisionMultiplier * 100).toInt()}%):",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WeatherCondition.entries.forEach { w ->
                    val active = w == selectedWeather
                    FilterChip(
                        selected = active,
                        onClick = { onSelectWeather(w) },
                        label = { Text(GameStrings.weatherName(w, lang)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ArmorCyan,
                            selectedLabelColor = ObsidianBlack
                        ),
                        modifier = Modifier.testTag("weather_chip_${w.id}")
                    )
                }
            }
        }

        Button(
            onClick = onDeploy,
            colors = ButtonDefaults.buttonColors(containerColor = DesertGold, contentColor = ObsidianBlack),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag(if (mode == GameMode.SURVIVAL) "deploy_survival_btn" else "deploy_defense_btn")
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(GameStrings.startMission(lang), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ChallengesModePane(
    saveData: PlayerSaveData,
    lang: GameLanguage,
    onLaunchChallenge: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(GameModeCatalog.challenges) { ch ->
            val bestScore = saveData.challengeBestScores[ch.id] ?: 0
            val medal = saveData.challengeMedals[ch.id] ?: ChallengeMedal.NONE
            val cityName = MissionCatalog.cityById(ch.cityId).name.get(lang)
            val medalColor = when (medal) {
                ChallengeMedal.GOLD -> DesertGold
                ChallengeMedal.SILVER -> Color(0xFFD0D8E2)
                ChallengeMedal.BRONZE -> Color(0xFFCD8B52)
                ChallengeMedal.NONE -> TextSecondary
            }

            Surface(
                color = TacticalSlateSurface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (medal != ChallengeMedal.NONE) medalColor else TacticalBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(ch.title.get(lang), style = MaterialTheme.typography.titleMedium, color = DesertGold)
                            if (medal != ChallengeMedal.NONE) {
                                Text(
                                    GameStrings.medalName(medal, lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = medalColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(ch.description.get(lang), style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = ArmorCyan, modifier = Modifier.size(14.dp))
                            Text(
                                "${ch.timeLimitSeconds.toInt()}s • $cityName • ${GameStrings.weatherName(ch.weather, lang)} • ${ch.xpReward} XP" +
                                    if (bestScore > 0) " • Best: $bestScore" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = { onLaunchChallenge(ch.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = DesertGold, contentColor = ObsidianBlack),
                        modifier = Modifier.height(48.dp).testTag("deploy_challenge_${ch.id}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                    }
                }
            }
        }
    }
}
