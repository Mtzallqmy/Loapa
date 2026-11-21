package com.example.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.game.data.AchievementCatalog
import com.example.game.data.CameraShoulder
import com.example.game.data.CrosshairStyle
import com.example.game.data.Difficulty
import com.example.game.data.GameLanguage
import com.example.game.data.GraphicsQuality
import com.example.game.localization.GameStrings
import com.example.game.save.ControlLayoutSettings
import com.example.game.save.ControlOffset
import com.example.game.save.GameSettings
import com.example.game.save.PlayerSaveData
import com.example.ui.theme.ArmorCyan
import com.example.ui.theme.DesertGold
import com.example.ui.theme.HudControlBg
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SignalCrimson
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalEmerald
import com.example.ui.theme.TacticalSlateDark
import com.example.ui.theme.TacticalSlateSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun AchievementsScreen(
    saveData: PlayerSaveData,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val lang = saveData.settings.language

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("ach_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = GameStrings.back(lang), tint = DesertGold)
                }
                Text(GameStrings.menuAchievements(lang), style = MaterialTheme.typography.headlineMedium, color = DesertGold)
            }
            Text(
                "${saveData.unlockedAchievements.size} / ${AchievementCatalog.all.size}",
                style = MaterialTheme.typography.titleMedium,
                color = TacticalEmerald
            )
        }

        Spacer(Modifier.height(8.dp))

        // Career Stats Summary Strip
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CareerStatTile(if (lang == GameLanguage.ARABIC) "الإقصاءات" else "Kills", "${saveData.totalKills}", Modifier.weight(1f))
            CareerStatTile(if (lang == GameLanguage.ARABIC) "إصابات الرأس" else "Headshots", "${saveData.totalHeadshots}", Modifier.weight(1f))
            CareerStatTile(if (lang == GameLanguage.ARABIC) "الدقة" else "Accuracy", "${saveData.accuracyPercent}%", Modifier.weight(1f))
            CareerStatTile(if (lang == GameLanguage.ARABIC) "المهمات" else "Missions", "${saveData.completedMissions.size}/12", Modifier.weight(1f))
            CareerStatTile(GameStrings.xpLabel(lang), "${saveData.xp}", Modifier.weight(1f), DesertGold)
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AchievementCatalog.all) { ach ->
                val done = ach.id in saveData.unlockedAchievements
                Surface(
                    color = if (done) Color(0xFF14241E) else TacticalSlateDark,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (done) TacticalEmerald else TacticalBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = if (done) TacticalEmerald else TextSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ach.title.get(lang), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(ach.description.get(lang), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Text("+${ach.xpBonus} XP", style = MaterialTheme.typography.labelMedium, color = DesertGold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CareerStatTile(label: String, value: String, modifier: Modifier, color: Color = ArmorCyan) {
    Surface(
        color = TacticalSlateDark,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, TacticalBorder),
        modifier = modifier
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
fun SettingsScreen(
    settings: GameSettings,
    onUpdateSettings: ((GameSettings) -> GameSettings) -> Unit,
    onOpenControlEditor: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val lang = settings.language

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = GameStrings.back(lang), tint = DesertGold)
                }
                Text(GameStrings.menuSettings(lang), style = MaterialTheme.typography.headlineMedium, color = DesertGold)
            }
            Button(
                onClick = onOpenControlEditor,
                colors = ButtonDefaults.buttonColors(containerColor = DesertGold, contentColor = ObsidianBlack),
                modifier = Modifier.testTag("open_control_editor_btn")
            ) {
                Icon(Icons.Default.Gamepad, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(GameStrings.settingsControls(lang), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Column: Graphics, Language & Audio
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsCard(GameStrings.settingsLanguage(lang)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GameLanguage.entries.forEach { l ->
                            FilterChip(
                                selected = settings.language == l,
                                onClick = { onUpdateSettings { it.copy(language = l) } },
                                label = { Text(if (l == GameLanguage.ARABIC) "العربية" else "English") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DesertGold, selectedLabelColor = ObsidianBlack)
                            )
                        }
                    }
                }

                SettingsCard(GameStrings.settingsGraphics(lang)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GraphicsQuality.entries.forEach { q ->
                            FilterChip(
                                selected = settings.graphicsQuality == q,
                                onClick = { onUpdateSettings { it.copy(graphicsQuality = q) } },
                                label = { Text("${GameStrings.graphicsName(q, lang)} (${q.targetFps} FPS)") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DesertGold, selectedLabelColor = ObsidianBlack)
                            )
                        }
                    }
                }

                SettingsCard(GameStrings.settingsAudio(lang)) {
                    SliderRow(if (lang == GameLanguage.ARABIC) "الصوت الرئيسي" else "Master Volume", settings.masterVolume) { v ->
                        onUpdateSettings { it.copy(masterVolume = v) }
                    }
                    SliderRow(if (lang == GameLanguage.ARABIC) "المؤثرات القتالية" else "Effects Volume", settings.sfxVolume) { v ->
                        onUpdateSettings { it.copy(sfxVolume = v) }
                    }
                    SliderRow(if (lang == GameLanguage.ARABIC) "الموسيقى" else "Music Volume", settings.musicVolume) { v ->
                        onUpdateSettings { it.copy(musicVolume = v) }
                    }
                    SliderRow(if (lang == GameLanguage.ARABIC) "أصوات البيئة" else "Ambient Volume", settings.ambientVolume) { v ->
                        onUpdateSettings { it.copy(ambientVolume = v) }
                    }
                }
            }

            // Right Column: Gameplay & Accessibility
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsCard(GameStrings.settingsGameplay(lang)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Difficulty.entries.forEach { d ->
                            FilterChip(
                                selected = settings.difficulty == d,
                                onClick = { onUpdateSettings { it.copy(difficulty = d) } },
                                label = { Text(GameStrings.difficultyName(d, lang)) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DesertGold, selectedLabelColor = ObsidianBlack)
                            )
                        }
                    }
                    SliderRow(if (lang == GameLanguage.ARABIC) "حساسية الكاميرا" else "Camera Sensitivity", settings.cameraSensitivity / 2.2f) { v ->
                        onUpdateSettings { it.copy(cameraSensitivity = (v * 2.2f).coerceIn(0.4f, 2.2f)) }
                    }
                    SliderRow(if (lang == GameLanguage.ARABIC) "حساسية التصويب" else "Aim Sensitivity", settings.aimSensitivity / 1.5f) { v ->
                        onUpdateSettings { it.copy(aimSensitivity = (v * 1.5f).coerceIn(0.25f, 1.5f)) }
                    }
                    ToggleRow(if (lang == GameLanguage.ARABIC) "مساعدة التصويب للجوال" else "Mobile Aim Assist", settings.aimAssist) { v ->
                        onUpdateSettings { it.copy(aimAssist = v) }
                    }
                    ToggleRow(GameStrings.hudAutoFireSetting(lang), settings.autoFireEnabled) { v ->
                        onUpdateSettings { it.copy(autoFireEnabled = v) }
                    }
                    ToggleRow(if (lang == GameLanguage.ARABIC) "عكس محور الكاميرا العمودي" else "Invert Camera Y Axis", settings.invertY) { v ->
                        onUpdateSettings { it.copy(invertY = v) }
                    }
                    ToggleRow(if (lang == GameLanguage.ARABIC) "الاهتزاز التكتيكي" else "Haptic Vibration", settings.vibrationEnabled) { v ->
                        onUpdateSettings { it.copy(vibrationEnabled = v) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CrosshairStyle.entries.forEach { c ->
                            FilterChip(
                                selected = settings.crosshairStyle == c,
                                onClick = { onUpdateSettings { it.copy(crosshairStyle = c) } },
                                label = { Text(GameStrings.crosshairName(c, lang), style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DesertGold, selectedLabelColor = ObsidianBlack)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (lang == GameLanguage.ARABIC) "كتف الكاميرا الافتراضي" else "Default Camera Shoulder",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CameraShoulder.entries.forEach { sh ->
                                FilterChip(
                                    selected = settings.cameraShoulder == sh,
                                    onClick = { onUpdateSettings { it.copy(cameraShoulder = sh) } },
                                    label = {
                                        Text(
                                            when (sh) {
                                                CameraShoulder.RIGHT -> if (lang == GameLanguage.ARABIC) "يمين" else "Right"
                                                CameraShoulder.LEFT -> if (lang == GameLanguage.ARABIC) "يسار" else "Left"
                                            },
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DesertGold, selectedLabelColor = ObsidianBlack)
                                )
                            }
                        }
                    }
                }

                SettingsCard(GameStrings.settingsAccessibility(lang)) {
                    ToggleRow(if (lang == GameLanguage.ARABIC) "إظهار الترجمة النصية" else "Subtitles", settings.subtitlesEnabled) { v ->
                        onUpdateSettings { it.copy(subtitlesEnabled = v) }
                    }
                    SliderRow(if (lang == GameLanguage.ARABIC) "حجم الخط المقروء" else "Readable Text Scale", (settings.textScale - 0.85f) / 0.4f) { v ->
                        onUpdateSettings { it.copy(textScale = (0.85f + v * 0.4f).coerceIn(0.85f, 1.25f)) }
                    }
                    ToggleRow(if (lang == GameLanguage.ARABIC) "تقليل اهتزاز الكاميرا" else "Reduce Screen Shake", settings.reduceScreenShake) { v ->
                        onUpdateSettings { it.copy(reduceScreenShake = v) }
                    }
                    ToggleRow(if (lang == GameLanguage.ARABIC) "تقليل المؤثرات البصرية الحادة" else "Reduce Intense Visual Effects", settings.reduceIntenseEffects) { v ->
                        onUpdateSettings { it.copy(reduceIntenseEffects = v) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Surface(
        color = TacticalSlateDark,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, TacticalBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = DesertGold)
            content()
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, onChanged: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text("${(value.coerceIn(0f, 1f) * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = DesertGold)
        }
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onChanged,
            colors = SliderDefaults.colors(thumbColor = DesertGold, activeTrackColor = DesertGold)
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        Switch(checked = checked, onCheckedChange = onChanged)
    }
}

@Composable
fun ControlLayoutEditorScreen(
    initialLayout: ControlLayoutSettings,
    language: GameLanguage,
    onSave: (ControlLayoutSettings) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var layout by remember { mutableStateOf(initialLayout) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = GameStrings.back(language), tint = DesertGold)
                }
                Text(GameStrings.settingsControls(language), style = MaterialTheme.typography.titleLarge, color = DesertGold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { layout = ControlLayoutSettings() }) {
                    Text(GameStrings.resetDefaults(language))
                }
                Button(
                    onClick = { onSave(layout); onBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = DesertGold, contentColor = ObsidianBlack),
                    modifier = Modifier.testTag("save_controls_btn")
                ) {
                    Text(GameStrings.saveChanges(language), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Quick Tuning Controls Strip
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = layout.leftHanded,
                onClick = { layout = layout.copy(leftHanded = !layout.leftHanded) },
                label = { Text(if (language == GameLanguage.ARABIC) "وضع اليد اليسرى" else "Left-Handed") }
            )
            Column(Modifier.weight(1f)) {
                Text(if (language == GameLanguage.ARABIC) "حجم الأزرار" else "Button Size", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Slider(value = (layout.buttonScale - 0.75f) / 0.6f, onValueChange = {
                    layout = layout.copy(buttonScale = 0.75f + it * 0.6f)
                })
            }
            Column(Modifier.weight(1f)) {
                Text(if (language == GameLanguage.ARABIC) "الشفافية" else "Opacity", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Slider(value = (layout.buttonOpacity - 0.35f) / 0.65f, onValueChange = {
                    layout = layout.copy(buttonOpacity = 0.35f + it * 0.65f)
                })
            }
            Column(Modifier.weight(1f)) {
                Text(if (language == GameLanguage.ARABIC) "حجم عصا الحركة" else "Joystick Size", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Slider(value = (layout.joystickScale - 0.75f) / 0.6f, onValueChange = {
                    layout = layout.copy(joystickScale = 0.75f + it * 0.6f)
                })
            }
        }

        // Interactive Drag Canvas (always LTR physical coordinates matching GameplayScreen)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TacticalSlateDark)
                    .border(1.dp, TacticalBorder, RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = if (language == GameLanguage.ARABIC) "اسحب أزرار القتال لإعادة تموضعها بحرية" else "Drag combat buttons freely to reposition",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Joystick Preview
                Box(
                    modifier = Modifier
                        .align(if (layout.leftHanded) Alignment.BottomEnd else Alignment.BottomStart)
                        .size((110 * layout.joystickScale).dp)
                        .alpha(layout.buttonOpacity)
                        .clip(CircleShape)
                        .background(HudControlBg)
                        .border(2.dp, DesertGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("MOVE", style = MaterialTheme.typography.labelSmall, color = DesertGold)
                }

                // Draggable Combat Buttons
                val actionAlign = if (layout.leftHanded) Alignment.BottomStart else Alignment.BottomEnd
                Row(
                    modifier = Modifier.align(actionAlign),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DraggableHudChip(GameStrings.hudReload(language), layout.reloadOffset, (52 * layout.buttonScale).dp, layout.buttonOpacity, DesertGold) {
                        layout = layout.copy(reloadOffset = it)
                    }
                    DraggableHudChip(GameStrings.hudAim(language), layout.aimOffset, (60 * layout.buttonScale).dp, layout.buttonOpacity, ArmorCyan) {
                        layout = layout.copy(aimOffset = it)
                    }
                    DraggableHudChip(GameStrings.hudFire(language), layout.fireOffset, (74 * layout.buttonScale).dp, layout.buttonOpacity, SignalCrimson) {
                        layout = layout.copy(fireOffset = it)
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggableHudChip(
    label: String,
    offset: ControlOffset,
    sizeDp: androidx.compose.ui.unit.Dp,
    opacity: Float,
    color: Color,
    onOffsetChange: (ControlOffset) -> Unit
) {
    Box(
        modifier = Modifier
            .offset { IntOffset((offset.dx * 90).roundToInt(), (offset.dy * 90).roundToInt()) }
            .size(sizeDp)
            .alpha(opacity)
            .clip(CircleShape)
            .background(HudControlBg)
            .border(2.dp, color, CircleShape)
            .pointerInput(offset) {
                detectDragGestures { change, drag ->
                    change.consume()
                    onOffsetChange(
                        ControlOffset(
                            dx = (offset.dx + drag.x / 90f).coerceIn(-1.2f, 0.4f),
                            dy = (offset.dy + drag.y / 90f).coerceIn(-1.2f, 0.25f)
                        )
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
    }
}

@Composable
fun AboutCreditsScreen(
    language: GameLanguage,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(
                modifier = Modifier.weight(0.8f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TacticalCrestEmblem(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(2.dp, DesertGold, RoundedCornerShape(18.dp))
                )
                Spacer(Modifier.height(12.dp))
                Text("جبهات القتال", style = MaterialTheme.typography.headlineMedium, color = DesertGold)
                Text("COMBAT FRONTS ${GameStrings.GAME_VERSION}", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(14.dp))
                OutlinedButton(onClick = onBack, modifier = Modifier.testTag("about_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(GameStrings.back(language))
                }
            }

            Surface(
                color = TacticalSlateDark,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DesertGold.copy(alpha = 0.5f)),
                modifier = Modifier.weight(1.3f).fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = GameStrings.DEVELOPER_CREDIT_AR,
                        style = MaterialTheme.typography.titleLarge,
                        color = DesertGold
                    )
                    Text(
                        text = GameStrings.DEVELOPER_CREDIT_EN,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = if (language == GameLanguage.ARABIC)
                            "لعبة أكشن عسكرية تكتيكية ثلاثية الأبعاد تعمل دون اتصال بالإنترنت بالكامل (Offline 3D) مع أربع مدن افتراضية (مدينة النهر، مدينة السراب، مدينة المرسى، مدينة الجبل)، واثنتي عشرة مهمة كاملة، وثمانية أسلحة قابلة للتخصيص، وذكاء اصطناعي تكتيكي متقدم للأعداء."
                        else
                            "A 100% offline native 3D third-person military tactical shooter featuring four original fictional cities (River, Mirage, Harbor, Mountain), twelve playable campaign missions, eight customizable weapon platforms, 360-degree 3D character & armory studios, procedural military PCM audio, and tactical squad AI.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = if (language == GameLanguage.ARABIC)
                            "الأصول والتراخيص: جميع النماذج ثلاثية الأبعاد والمظللات (OpenGL ES 2.0/3.0) والمؤثرات الصوتية الإجرائية أصلية ومولّدة داخل المحرك بدون أي أصول محمية بحقوق تجارية. خطوط Cairo و JetBrains Mono مرخّصة تحت OFL 1.1."
                        else
                            "Asset Licenses: All 3D procedural geometry, OpenGL ES 2.0/3.0 shaders, and synthesized PCM military sound effects are original engine-generated assets. Cairo & JetBrains Mono fonts are licensed under SIL OFL 1.1.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
