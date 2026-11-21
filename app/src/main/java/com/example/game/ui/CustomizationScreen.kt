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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.game.data.CharacterSlot
import com.example.game.data.CustomizationCatalog
import com.example.game.data.GameLanguage
import com.example.game.data.WeaponCatalog
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CustomizationScreen(
    saveData: PlayerSaveData,
    onUnlockOrEquipGear: (CharacterSlot, String) -> Unit,
    onApplyPreset: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val lang = saveData.settings.language
    var selectedSlot by remember { mutableStateOf(CharacterSlot.OUTFIT) }
    var rotYaw by remember { mutableFloatStateOf(205f) }
    var zoom by remember { mutableFloatStateOf(1.0f) }

    val appearance = remember(saveData.equippedCharacterGear) {
        CustomizationCatalog.resolveAppearance(saveData.equippedCharacterGear)
    }
    val weaponStats = remember(saveData.equippedPrimary, saveData.weaponAttachments) {
        WeaponCatalog.computeEffectiveStats(saveData.equippedPrimary, saveData.attachmentsFor(saveData.equippedPrimary))
    }

    val previewRef = remember {
        java.util.concurrent.atomic.AtomicReference(
            StudioPreviewState(StudioPreviewMode.CHARACTER, weaponStats, appearance, rotYaw, zoom)
        )
    }
    previewRef.set(StudioPreviewState(StudioPreviewMode.CHARACTER, weaponStats, appearance, rotYaw, zoom))

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val glView = remember {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            preserveEGLContextOnPause = true
            setRenderer(PreviewGLRenderer { previewRef.get() })
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("custom_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = GameStrings.back(lang), tint = DesertGold)
                }
                Text(GameStrings.menuCustomization(lang), style = MaterialTheme.typography.headlineMedium, color = DesertGold)
            }
            Text("${saveData.xp} ${GameStrings.xpLabel(lang)}", style = MaterialTheme.typography.titleMedium, color = DesertGold)
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: 360-Degree Rotatable & Zoomable 3D Operator Preview
            Box(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            rotYaw += drag.x * 0.6f
                        }
                    }
            ) {
                AndroidView(factory = { glView }, modifier = Modifier.fillMaxSize())
                Row(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Autorenew, contentDescription = "360", tint = DesertGold, modifier = Modifier.size(18.dp))
                    Surface(
                        onClick = { zoom = (zoom - 0.15f).coerceAtLeast(0.7f) },
                        color = Color(0xAA11181F),
                        shape = RoundedCornerShape(4.dp)
                    ) { Icon(Icons.Default.Remove, contentDescription = "Zoom out", tint = TextPrimary, modifier = Modifier.padding(4.dp).size(18.dp)) }
                    Surface(
                        onClick = { zoom = (zoom + 0.15f).coerceAtMost(1.5f) },
                        color = Color(0xAA11181F),
                        shape = RoundedCornerShape(4.dp)
                    ) { Icon(Icons.Default.Add, contentDescription = "Zoom in", tint = TextPrimary, modifier = Modifier.padding(4.dp).size(18.dp)) }
                }

                // Quick Tactical Presets Bar
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CustomizationCatalog.presets.forEach { preset ->
                        Surface(
                            onClick = { onApplyPreset(preset.id) },
                            color = Color(0xCC11181F),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, DesertGold.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = preset.name.get(lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = DesertGold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Right: Gear Categories & Unlockable Items
            Column(
                modifier = Modifier.weight(1.05f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CharacterSlot.entries) { slot ->
                        FilterChip(
                            selected = selectedSlot == slot,
                            onClick = { selectedSlot = slot },
                            label = { Text(GameStrings.characterSlotName(slot, lang)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DesertGold,
                                selectedLabelColor = ObsidianBlack
                            )
                        )
                    }
                }

                val slotItems = remember(selectedSlot) { CustomizationCatalog.itemsForSlot(selectedSlot) }
                val equippedId = saveData.equippedCharacterGear[selectedSlot]
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(slotItems) { item ->
                        val isOwned = item.id in saveData.unlockedCharacterGear || item.xpCost == 0
                        val isEq = equippedId == item.id
                        val canAfford = saveData.xp >= item.xpCost
                        Surface(
                            onClick = { onUnlockOrEquipGear(selectedSlot, item.id) },
                            color = if (isEq) DesertGold.copy(alpha = 0.18f) else TacticalSlateDark,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isEq) DesertGold else TacticalBorder),
                            modifier = Modifier.fillMaxWidth().testTag("gear_${item.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(item.colorRgb[0], item.colorRgb[1], item.colorRgb[2]))
                                )
                                Text(item.name.get(lang), style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                                Text(
                                    text = when {
                                        isEq -> GameStrings.equipped(lang)
                                        isOwned -> GameStrings.equip(lang)
                                        else -> "${item.xpCost} XP"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
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
        }
    }
}
