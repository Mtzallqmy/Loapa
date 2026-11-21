package com.example.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.game.data.AchievementCatalog
import com.example.game.data.CityId
import com.example.game.data.Difficulty
import com.example.game.data.GameLanguage
import com.example.game.data.MissionCatalog
import com.example.game.data.WeaponCatalog
import com.example.game.engine.MissionSummary
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
fun MissionsScreen(
    saveData: PlayerSaveData,
    onSelectDifficulty: (Difficulty) -> Unit,
    onLaunchMission: (Int) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val lang = saveData.settings.language
    var selectedCity by remember { mutableStateOf(CityId.RIVER_CITY) }
    val cityMissions = remember(selectedCity) { MissionCatalog.missionsForCity(selectedCity) }
    var selectedMissionNum by remember(selectedCity) { mutableStateOf(cityMissions.first().number) }
    val selectedMission = remember(selectedMissionNum) { MissionCatalog.missionByNumber(selectedMissionNum) }
    val selectedCityDef = remember(selectedCity) { MissionCatalog.cityById(selectedCity) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Top Header with Difficulty Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("missions_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = GameStrings.back(lang), tint = DesertGold)
                }
                Text(
                    text = GameStrings.menuMissions(lang),
                    style = MaterialTheme.typography.headlineMedium,
                    color = DesertGold
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

        Spacer(Modifier.height(8.dp))

        // 4-City Selector Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MissionCatalog.cities.forEach { city ->
                val isSelected = city.id == selectedCity
                Surface(
                    onClick = { selectedCity = city.id },
                    color = if (isSelected) DesertGold.copy(alpha = 0.22f) else TacticalSlateDark,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) DesertGold else TacticalBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("city_tab_${city.id.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = city.name.get(lang),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) DesertGold else TextPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = city.subtitle.get(lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Split List-Detail: Missions List + Selected Mission Tactical Briefing Card
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(0.95f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cityMissions) { m ->
                    val unlocked = m.number <= saveData.highestUnlockedMission
                    val completed = m.number in saveData.completedMissions
                    val stars = saveData.missionBestStars[m.number] ?: 0
                    val isSelected = m.number == selectedMissionNum
                    Surface(
                        onClick = { selectedMissionNum = m.number },
                        color = if (isSelected) TacticalSlateSurface else TacticalSlateDark,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) DesertGold else TacticalBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mission_card_${m.number}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "0${m.number}".takeLast(2) + " — " + m.title.get(lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (unlocked) TextPrimary else TextSecondary
                                )
                                Text(
                                    text = "${m.xpReward} ${GameStrings.xpLabel(lang)} • ${m.spawns.size} ${if (lang == GameLanguage.ARABIC) "هدف معادٍ" else "Hostiles"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (unlocked) DesertGold else TextSecondary
                                )
                            }
                            if (!unlocked) {
                                Icon(Icons.Default.Lock, contentDescription = GameStrings.locked(lang), tint = TextSecondary)
                            } else if (completed) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    repeat(3) { idx ->
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (idx < stars) DesertGold else TacticalBorder,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TacticalEmerald, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Selected Mission Briefing + City Artwork
            Surface(
                color = TacticalSlateDark,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, TacticalBorder),
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(106.dp)
                    ) {
                        CityArtworkBanner(selectedCity)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, TacticalSlateDark)
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = selectedMission.title.get(lang),
                                style = MaterialTheme.typography.titleLarge,
                                color = DesertGold
                            )
                            Text(
                                text = selectedCityDef.name.get(lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = selectedMission.briefing.get(lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            selectedMission.unlockWeaponReward?.let { wId ->
                                val wName = WeaponCatalog.byId(wId).name.get(lang)
                                Text(
                                    text = if (lang == GameLanguage.ARABIC) "مكافأة الفتح: $wName" else "Unlock Reward: $wName",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TacticalEmerald
                                )
                            }
                        }

                        val canPlay = selectedMission.number <= saveData.highestUnlockedMission
                        Button(
                            onClick = { onLaunchMission(selectedMission.number) },
                            enabled = canPlay,
                            colors = ButtonDefaults.buttonColors(containerColor = DesertGold, contentColor = ObsidianBlack),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("deploy_mission_btn")
                        ) {
                            Icon(if (canPlay) Icons.Default.PlayArrow else Icons.Default.Lock, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (canPlay) GameStrings.startMission(lang) else GameStrings.locked(lang),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CityArtworkBanner(cityId: CityId) {
    CityArtworkCanvas(cityId = cityId, modifier = Modifier.fillMaxSize())
}

@Composable
fun MissionResultScreen(
    summary: MissionSummary,
    language: GameLanguage,
    newlyUnlockedAchievementIds: List<String>,
    onNextMission: () -> Unit,
    onRetryCheckpoint: () -> Unit,
    onRestartMission: () -> Unit,
    onMainMenu: () -> Unit
) {
    BackHandler(onBack = onMainMenu)
    val isCampaign = summary.gameMode == com.example.game.data.GameMode.CAMPAIGN
    val isCampaignFinished = isCampaign && summary.victory && summary.missionNumber == 12
    val acc = if (summary.shotsFired > 0) (summary.shotsHit * 100) / summary.shotsFired else 0
    val subtitleLabel = if (isCampaign) {
        MissionCatalog.missionByNumber(summary.missionNumber.coerceIn(1, 12)).title.get(language)
    } else {
        val city = MissionCatalog.cityById(summary.cityId).name.get(language)
        "${GameStrings.gameModeName(summary.gameMode, language)} • $city"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (summary.victory) Color(0xFF13221C) else Color(0xFF241315),
                        ObsidianBlack
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = TacticalSlateDark,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, if (summary.victory) DesertGold else SignalCrimson),
            modifier = Modifier.fillMaxWidth(0.86f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when {
                                isCampaignFinished -> GameStrings.campaignCompleteTitle(language)
                                summary.victory -> GameStrings.victoryTitle(language)
                                summary.gameMode == com.example.game.data.GameMode.SURVIVAL ->
                                    if (language == GameLanguage.ARABIC) "انتهت جولة البقاء!" else "SURVIVAL RUN COMPLETE"
                                else -> GameStrings.defeatTitle(language)
                            },
                            style = MaterialTheme.typography.displaySmall,
                            color = if (summary.victory) DesertGold else SignalCrimson
                        )
                        Text(
                            text = subtitleLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                    }
                    if (isCampaign && summary.victory) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(3) { i ->
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (i < summary.stars) DesertGold else TacticalBorder,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    } else if (summary.challengeMedal != com.example.game.data.ChallengeMedal.NONE) {
                        Surface(
                            color = DesertGold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, DesertGold)
                        ) {
                            Text(
                                text = GameStrings.medalName(summary.challengeMedal, language),
                                style = MaterialTheme.typography.titleMedium,
                                color = DesertGold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Combat telemetry stats grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBox(if (language == GameLanguage.ARABIC) "الإقصاءات" else "Eliminations", "${summary.kills}", Modifier.weight(1f))
                    if (!isCampaign) {
                        StatBox(
                            if (summary.gameMode == com.example.game.data.GameMode.CHALLENGES)
                                GameStrings.hudScore(language)
                            else GameStrings.hudWave(language, summary.waveReached),
                            if (summary.gameMode == com.example.game.data.GameMode.CHALLENGES)
                                "${summary.score}"
                            else "${summary.score} PTS",
                            Modifier.weight(1f)
                        )
                    } else {
                        StatBox(if (language == GameLanguage.ARABIC) "إصابات الرأس" else "Headshots", "${summary.headshots}", Modifier.weight(1f))
                    }
                    StatBox(if (language == GameLanguage.ARABIC) "الدقة" else "Accuracy", "$acc%", Modifier.weight(1f))
                    StatBox(GameStrings.xpLabel(language), "+${summary.xpEarned}", Modifier.weight(1f), highlight = DesertGold)
                }

                if (newlyUnlockedAchievementIds.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = TacticalEmerald)
                        val names = newlyUnlockedAchievementIds.mapNotNull { id ->
                            AchievementCatalog.all.firstOrNull { it.id == id }?.title?.get(language)
                        }.joinToString(" • ")
                        Text(names, style = MaterialTheme.typography.labelLarge, color = TacticalEmerald)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                ) {
                    OutlinedButton(onClick = onMainMenu, modifier = Modifier.height(48.dp)) {
                        Text(GameStrings.exitToMenu(language))
                    }
                    if (!isCampaign || !summary.victory) {
                        if (isCampaign && !summary.victory) {
                            OutlinedButton(onClick = onRetryCheckpoint, modifier = Modifier.height(48.dp)) {
                                Icon(Icons.Default.Replay, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(GameStrings.loadCheckpoint(language))
                            }
                        }
                        Button(
                            onClick = onRestartMission,
                            colors = ButtonDefaults.buttonColors(containerColor = DesertGold, contentColor = ObsidianBlack),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(GameStrings.restartMission(language), fontWeight = FontWeight.Bold)
                        }
                    } else if (summary.missionNumber < 12) {
                        Button(
                            onClick = onNextMission,
                            colors = ButtonDefaults.buttonColors(containerColor = DesertGold, contentColor = ObsidianBlack),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("next_mission_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(GameStrings.nextMission(language), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier, highlight: Color = ArmorCyan) {
    Surface(
        color = TacticalSlateSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, TacticalBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = highlight)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}
