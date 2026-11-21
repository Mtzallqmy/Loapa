package com.example.game.ui

import android.opengl.GLSurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.game.data.AttachmentSlot
import com.example.game.data.CustomizationCatalog
import com.example.game.data.GameLanguage
import com.example.game.data.WeaponCatalog
import com.example.game.data.WeaponId
import com.example.game.localization.GameStrings
import com.example.game.render.PreviewGLRenderer
import com.example.game.render.StudioPreviewMode
import com.example.game.render.StudioPreviewState
import com.example.game.save.PlayerSaveData
import com.example.ui.theme.ArmorCyan
import com.example.ui.theme.DesertGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalEmerald
import com.example.ui.theme.TacticalSlateDark
import com.example.ui.theme.TacticalSlateSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ArmoryScreen(
    saveData: PlayerSaveData,
    onUnlockOrEquipWeapon: (WeaponId, Boolean) -> Unit,
    onUnlockOrEquipAttachment: (WeaponId, AttachmentSlot, String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val lang = saveData.settings.language
    var selectedWeaponId by remember { mutableStateOf(saveData.equippedPrimary) }
    var selectedSlot by remember { mutableStateOf(AttachmentSlot.OPTIC) }
    var rotYaw by remember { mutableFloatStateOf(30f) }
    var zoom by remember { mutableFloatStateOf(1.0f) }

    val weaponData = remember(selectedWeaponId) { WeaponCatalog.byId(selectedWeaponId) }
    val equippedSlots = saveData.attachmentsFor(selectedWeaponId)
    val effectiveStats = remember(selectedWeaponId, equippedSlots) {
        WeaponCatalog.computeEffectiveStats(selectedWeaponId, equippedSlots)
    }
    val appearance = remember(saveData.equippedCharacterGear) {
        CustomizationCatalog.resolveAppearance(saveData.equippedCharacterGear)
    }

    val previewStateRef = remember {
        java.util.concurrent.atomic.AtomicReference(
            StudioPreviewState(StudioPreviewMode.WEAPON, effectiveStats, appearance, rotYaw, zoom)
        )
    }
    previewStateRef.set(StudioPreviewState(StudioPreviewMode.WEAPON, effectiveStats, appearance, rotYaw, zoom))

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val glView = remember {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            preserveEGLContextOnPause = true
            setRenderer(PreviewGLRenderer { previewStateRef.get() })
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }
    DisposableEffect(lifecycleOwner, glView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glView.onResume()
                Lifecycle.Event.ON_PAUSE -> glView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glView.onPause()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("armory_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = GameStrings.back(lang), tint = DesertGold)
                }
                Text(GameStrings.menuArmory(lang), style = MaterialTheme.typography.headlineMedium, color = DesertGold)
            }
            Text(
                "${saveData.xp} ${GameStrings.xpLabel(lang)}",
                style = MaterialTheme.typography.titleMedium,
                color = DesertGold
            )
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left: All 8 Weapons List
            LazyColumn(
                modifier = Modifier.weight(0.75f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(WeaponCatalog.weapons) { w ->
                    val unlocked = w.id in saveData.unlockedWeapons
                    val isEquipped = w.id == saveData.equippedPrimary || w.id == saveData.equippedSecondary || w.id == saveData.equippedSidearm
                    val isSelected = w.id == selectedWeaponId
                    Surface(
                        onClick = { selectedWeaponId = w.id },
                        color = if (isSelected) TacticalSlateSurface else TacticalSlateDark,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) DesertGold else TacticalBorder),
                        modifier = Modifier.fillMaxWidth().testTag("weapon_item_${w.id.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(w.name.get(lang), style = MaterialTheme.typography.labelLarge, color = TextPrimary, maxLines = 1)
                                Text(w.category.get(lang), style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
                            }
                            when {
                                isEquipped -> Icon(Icons.Default.Check, contentDescription = null, tint = TacticalEmerald, modifier = Modifier.size(18.dp))
                                !unlocked -> Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Center: 360-Degree 3D Interactive Weapon Inspection Viewport + Stat Bars
            Column(
                modifier = Modifier.weight(1.1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                rotYaw += drag.x * 0.55f
                            }
                        }
                ) {
                    AndroidView(factory = { glView }, modifier = Modifier.fillMaxSize())
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Autorenew, contentDescription = "360", tint = DesertGold, modifier = Modifier.size(18.dp))
                        Surface(
                            onClick = { zoom = (zoom - 0.18f).coerceAtLeast(0.7f) },
                            color = Color(0xAA11181F),
                            shape = RoundedCornerShape(4.dp)
                        ) { Icon(Icons.Default.Remove, contentDescription = "Zoom out", tint = TextPrimary, modifier = Modifier.padding(4.dp).size(18.dp)) }
                        Surface(
                            onClick = { zoom = (zoom + 0.18f).coerceAtMost(1.5f) },
                            color = Color(0xAA11181F),
                            shape = RoundedCornerShape(4.dp)
                        ) { Icon(Icons.Default.Add, contentDescription = "Zoom in", tint = TextPrimary, modifier = Modifier.padding(4.dp).size(18.dp)) }
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    ) {
                        Text(weaponData.name.get(lang), style = MaterialTheme.typography.titleMedium, color = DesertGold)
                        Text(weaponData.description.get(lang), style = MaterialTheme.typography.labelSmall, color = TextPrimary, maxLines = 2)
                    }
                }

                // Live Effective Weapon Stats Card
                Surface(
                    color = TacticalSlateDark,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, TacticalBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatBarRow(if (lang == GameLanguage.ARABIC) "الضرر" else "Damage", (effectiveStats.damage / 100f).coerceIn(0f, 1f), "${effectiveStats.damage.toInt()}")
                        StatBarRow(if (lang == GameLanguage.ARABIC) "الدقة" else "Accuracy", (1f - effectiveStats.adsSpread * 16f).coerceIn(0.15f, 1f), "${((1f - effectiveStats.adsSpread * 16f) * 100).toInt().coerceIn(15, 99)}")
                        StatBarRow(if (lang == GameLanguage.ARABIC) "التحكم بالارتداد" else "Control", (1f - effectiveStats.recoilKick * 16f).coerceIn(0.15f, 1f), "${((1f - effectiveStats.recoilKick * 16f) * 100).toInt().coerceIn(15, 99)}")
                        StatBarRow(if (lang == GameLanguage.ARABIC) "المخزن والمدى" else "Mag / Range", (effectiveStats.effectiveRange / 130f).coerceIn(0.15f, 1f), "${effectiveStats.magCapacity} / ${effectiveStats.effectiveRange.toInt()}m")
                    }
                }
            }

            // Right: Attachments & Equip Controls
            Column(
                modifier = Modifier.weight(0.95f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(AttachmentSlot.entries) { slot ->
                            FilterChip(
                                selected = selectedSlot == slot,
                                onClick = { selectedSlot = slot },
                                label = { Text(GameStrings.attachmentSlotName(slot, lang), style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DesertGold,
                                    selectedLabelColor = ObsidianBlack
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    val slotItems = remember(selectedSlot) { WeaponCatalog.attachmentsForSlot(selectedSlot) }
                    val currentEquippedId = equippedSlots[selectedSlot]
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(slotItems) { att ->
                            val isOwned = att.id in saveData.unlockedAttachments || att.xpCost == 0
                            val isEq = currentEquippedId == att.id
                            val canAfford = saveData.xp >= att.xpCost
                            Surface(
                                onClick = { onUnlockOrEquipAttachment(selectedWeaponId, selectedSlot, att.id) },
                                color = if (isEq) DesertGold.copy(alpha = 0.18f) else TacticalSlateDark,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isEq) DesertGold else TacticalBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(att.name.get(lang), style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                                    Text(
                                        text = when {
                                            isEq -> GameStrings.equipped(lang)
                                            isOwned -> GameStrings.equip(lang)
                                            else -> "${att.xpCost} XP"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when {
                                            isEq -> TacticalEmerald
                                            isOwned -> DesertGold
                                            canAfford -> ArmorCyan
                                            else -> TextSecondary
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Equip Primary / Secondary or Unlock Button
                val isWeaponUnlocked = selectedWeaponId in saveData.unlockedWeapons
                if (!isWeaponUnlocked) {
                    Button(
                        onClick = { onUnlockOrEquipWeapon(selectedWeaponId, false) },
                        enabled = saveData.xp >= weaponData.xpCost,
                        colors = ButtonDefaults.buttonColors(containerColor = DesertGold, contentColor = ObsidianBlack),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(GameStrings.unlockWithXp(lang, weaponData.xpCost), fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { onUnlockOrEquipWeapon(selectedWeaponId, false) },
                            colors = ButtonDefaults.buttonColors(containerColor = DesertGold, contentColor = ObsidianBlack),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text(
                                if (weaponData.isSidearm) GameStrings.equipped(lang)
                                else if (lang == GameLanguage.ARABIC) "سلاح أساسي" else "Primary",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (!weaponData.isSidearm) {
                            OutlinedButton(
                                onClick = { onUnlockOrEquipWeapon(selectedWeaponId, true) },
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text(if (lang == GameLanguage.ARABIC) "سلاح ثانوي" else "Secondary")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBarRow(label: String, progress: Float, valueText: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.width(86.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)),
            color = DesertGold,
            trackColor = TacticalSlateSurface
        )
        Text(valueText, style = MaterialTheme.typography.labelSmall, color = TextPrimary, modifier = Modifier.width(52.dp))
    }
}
