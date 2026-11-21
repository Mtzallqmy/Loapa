package com.example.game.ui

import android.opengl.GLSurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.game.data.CrosshairStyle
import com.example.game.data.GameLanguage
import com.example.game.data.GameMode
import com.example.game.data.WeaponCatalog
import com.example.game.data.WeaponId
import com.example.game.engine.HudSnapshot
import com.example.game.engine.KillFeedEntry
import com.example.game.engine.MissionRunStatus
import com.example.game.engine.ObjectiveEdgeIndicator
import com.example.game.engine.ProjectedEnemyBar
import com.example.game.localization.GameStrings
import com.example.game.render.CombatGLRenderer
import com.example.game.save.GameSettings
import com.example.ui.theme.ArmorCyan
import com.example.ui.theme.DesertGold
import com.example.ui.theme.HudControlBg
import com.example.ui.theme.HudOverlayBg
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SignalCrimson
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalEmerald
import com.example.ui.theme.TacticalSlateDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun GameplayScreen(
    viewModel: GameViewModel,
    hud: HudSnapshot?,
    settings: GameSettings
) {
    BackHandler { viewModel.pauseGame() }
    val lang = settings.language
    val controls = settings.controls
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val glSurfaceView = remember {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            preserveEGLContextOnPause = true
            setRenderer(
                CombatGLRenderer(
                    simRef = viewModel.activeSimRef,
                    inputProvider = { viewModel.inputBridge.consumeForFrame() },
                    qualityProvider = { viewModel.saveData.value.settings.graphicsQuality },
                    onHudUpdate = { snap -> viewModel.onHudSnapshotFromGL(snap) },
                    onAudioEvents = { evs -> viewModel.onAudioEventsFromGL(evs) }
                )
            )
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    DisposableEffect(lifecycleOwner, glSurfaceView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glSurfaceView.onResume()
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.inputBridge.reset()
                    glSurfaceView.onPause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.inputBridge.reset()
            glSurfaceView.onPause()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(ObsidianBlack)) {
        // 1. Live Native OpenGL ES 2.0/3.0 3D Viewport
        AndroidView(
            factory = { glSurfaceView },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Full-Screen Camera Look Touch Zone (supports simultaneous multitouch with joystick & fire!)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(settings.cameraSensitivity, settings.aimSensitivity, settings.invertY) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val aimScale = if (viewModel.inputBridge.aimHeld) settings.aimSensitivity else settings.cameraSensitivity
                        val yawDelta = -dragAmount.x * 0.0046f * aimScale
                        val ySign = if (settings.invertY) 1f else -1f
                        val pitchDelta = dragAmount.y * 0.0036f * aimScale * ySign
                        viewModel.inputBridge.addLookDelta(yawDelta, pitchDelta)
                    }
                }
        )

        // 3. 3D-Projected World Overlays (Floating Enemy Health Bars & Directional Objective Indicator)
        val sniperScopeActive = hud != null &&
            hud.status == MissionRunStatus.ACTIVE &&
            hud.isAiming &&
            !hud.inVehicle &&
            (hud.weaponId == WeaponId.SR308_PRECISION || hud.weaponId == WeaponId.DMR14_MARKSMAN)

        if (hud != null && hud.status == MissionRunStatus.ACTIVE) {
            ProjectedBattlefieldOverlay(
                enemyBars = hud.enemyBars,
                objectiveIndicator = hud.objectiveIndicator
            )
            // Low-Health Crimson Edge Vignette
            if (hud.health < 35f) {
                val severity = ((35f - hud.health) / 35f).coerceIn(0.15f, 1f)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Transparent, SignalCrimson.copy(alpha = 0.48f * severity)),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.maxDimension * 0.62f
                        )
                    )
                }
            }
            if (sniperScopeActive) {
                SniperScopeOverlay(
                    weaponId = hud.weaponId,
                    hasTargetLock = hud.hasTargetLock
                )
            } else {
                CenterTacticalReticle(
                    hud = hud,
                    style = settings.crosshairStyle,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // 4. Safe-Area Mobile Tactical HUD
        if (hud != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(10.dp)
            ) {
                // TOP HUD BAR
                TopTacticalHud(
                    hud = hud,
                    language = lang,
                    onPause = { viewModel.pauseGame() },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Live Kill Feed + +XP Stack (below right weapon readout)
                if (hud.killFeed.isNotEmpty()) {
                    KillFeedOverlay(
                        entries = hud.killFeed,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 72.dp, end = 4.dp)
                    )
                }

                // Banner announcement (Checkpoint / Reinforcements / Ammo / Equipment)
                hud.bannerMessage?.let { msg ->
                    Surface(
                        color = HudOverlayBg,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, DesertGold),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 66.dp)
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.titleMedium,
                            color = DesertGold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }

                // v1.1.0 / v2.1.0 Context-Sensitive World Interaction or Vehicle Exit Button
                val bottomActionLabel = if (hud.inVehicle) {
                    "${GameStrings.hudExitVehicle(lang)} • ${GameStrings.hudVehicleHull(lang)} ${hud.vehicleHealth.roundToInt()}/${hud.vehicleMaxHealth.roundToInt()}"
                } else {
                    hud.interactionPrompt
                }
                bottomActionLabel?.let { prompt ->
                    Surface(
                        onClick = { viewModel.inputBridge.triggerInteract() },
                        color = if (hud.inVehicle) Color(0xDF281A12) else Color(0xDF13241C),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.5.dp, if (hud.inVehicle) DesertGold else TacticalEmerald),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 54.dp)
                            .testTag(if (hud.inVehicle) "hud_exit_vehicle_btn" else "hud_interact_btn")
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (hud.inVehicle) DesertGold else TacticalEmerald,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }

                // Mission 1 Interactive Onboarding Contextual Hint Pill
                hud.tutorialHint?.let { hint ->
                    Surface(
                        color = Color(0xE00B1822),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.2.dp, ArmorCyan),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = if (bottomActionLabel != null) 104.dp else 56.dp)
                            .testTag("hud_tutorial_hint")
                    ) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = ArmorCyan,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }

                // Subtitle Bar (accessibility toggleable)
                if (settings.subtitlesEnabled && hud.subtitleText != null) {
                    Surface(
                        color = Color(0xCC0A0E12),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = hud.subtitleText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }
                }

                // BOTTOM CONTROLS (always physical LTR thumb anchors so Arabic RTL never mirrors Joystick/Fire;
                // Left-Handed setting explicitly swaps physical sides)
                val joyAlign = if (controls.leftHanded) Alignment.BottomEnd else Alignment.BottomStart
                val actionAlign = if (controls.leftHanded) Alignment.BottomStart else Alignment.BottomEnd

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Virtual Analog Movement Joystick
                        VirtualAnalogJoystick(
                            scale = controls.joystickScale,
                            opacity = controls.buttonOpacity,
                            deadzone = controls.joystickDeadzone,
                            onMove = { mx, my, autoSprint ->
                                viewModel.inputBridge.moveX = mx
                                viewModel.inputBridge.moveY = my
                                viewModel.inputBridge.sprintHeld = autoSprint
                            },
                            modifier = Modifier
                                .align(joyAlign)
                                .padding(14.dp)
                                .testTag("virtual_joystick")
                        )

                        // Combat Action Cluster (Fire, Aim, Reload, Jump, Crouch, Sprint, Cover, Swap)
                        CombatActionButtons(
                            hud = hud,
                            language = lang,
                            scale = controls.buttonScale,
                            opacity = controls.buttonOpacity,
                            controls = controls,
                            inputBridge = viewModel.inputBridge,
                            modifier = Modifier
                                .align(actionAlign)
                                .padding(8.dp)
                        )
                    }
                }
            }

            // Intro Cinematic Banner Overlay
            if (hud.status == MissionRunStatus.INTRO) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x88080C10))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                down.consume()
                                viewModel.skipMissionIntro()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = hud.bannerMessage ?: "",
                            style = MaterialTheme.typography.headlineMedium,
                            color = DesertGold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (lang == GameLanguage.ARABIC) "اضغط للبدء الفوري" else "Tap to deploy immediately",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Pause Modal Overlay
            if (hud.status == MissionRunStatus.PAUSED) {
                PauseOverlay(
                    language = lang,
                    showCheckpoint = hud.gameMode == GameMode.CAMPAIGN,
                    onResume = { viewModel.resumeGame() },
                    onLoadCheckpoint = { viewModel.restoreLastCheckpoint() },
                    onRestart = { viewModel.restartCurrentMission() },
                    onExit = { viewModel.exitGameplayToMenu() }
                )
            }
        }
    }
}

@Composable
private fun TopTacticalHud(
    hud: HudSnapshot,
    language: GameLanguage,
    onPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Left: Mini Radar + Health / Armor Bars
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            TacticalMiniRadar(
                enemies = hud.radarEnemies,
                objective = hud.radarObjective,
                modifier = Modifier.size(68.dp)
            )
            Surface(
                color = HudOverlayBg,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, TacticalBorder)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).width(142.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(GameStrings.hudHealth(language), style = MaterialTheme.typography.labelSmall, color = TacticalEmerald)
                        Text("${hud.health.roundToInt()}", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                    }
                    LinearProgressIndicator(
                        progress = { (hud.health / hud.maxHealth).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(2.dp)),
                        color = if (hud.health < 30f) SignalCrimson else TacticalEmerald,
                        trackColor = TacticalSlateDark
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(GameStrings.hudArmor(language), style = MaterialTheme.typography.labelSmall, color = ArmorCyan)
                        Text("${hud.armor.roundToInt()}", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                    }
                    LinearProgressIndicator(
                        progress = { (hud.armor / hud.maxArmor).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(2.dp)),
                        color = ArmorCyan,
                        trackColor = TacticalSlateDark
                    )
                }
            }
        }

        // Center: Active Objective Tracker + Optional Boss HP Bar
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                color = HudOverlayBg,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, DesertGold.copy(alpha = 0.65f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${GameStrings.gameModeName(hud.gameMode, language)}  •  ${hud.objectiveProgressText}  •  ${GameStrings.weatherName(hud.weather, language)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = DesertGold
                    )
                    Text(
                        text = hud.objectiveTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    if (hud.respawnProtected) {
                        Text(
                            text = GameStrings.hudRespawnShield(language),
                            style = MaterialTheme.typography.labelSmall,
                            color = ArmorCyan
                        )
                    }
                }
            }
            if (hud.gameMode == com.example.game.data.GameMode.DEFENSE) {
                Spacer(Modifier.height(4.dp))
                Surface(color = Color(0xCC0D1E28), shape = RoundedCornerShape(4.dp)) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 3.dp).width(196.dp)) {
                        Text(
                            "${GameStrings.hudDefenseCore(language)}: ${hud.defenseCoreHealth.roundToInt()}/${hud.defenseCoreMaxHealth.roundToInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hud.defenseCoreHealth < 140f) SignalCrimson else ArmorCyan
                        )
                        LinearProgressIndicator(
                            progress = { (hud.defenseCoreHealth / hud.defenseCoreMaxHealth).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(5.dp),
                            color = if (hud.defenseCoreHealth < 140f) SignalCrimson else ArmorCyan,
                            trackColor = ObsidianBlack
                        )
                    }
                }
            }
            hud.bossHealthFraction?.let { bf ->
                Spacer(Modifier.height(4.dp))
                Surface(color = Color(0xCC240E0E), shape = RoundedCornerShape(4.dp)) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 3.dp).width(190.dp)) {
                        Text(
                            if (language == GameLanguage.ARABIC) "القائد المدرّع" else "ARMORED COMMANDER",
                            style = MaterialTheme.typography.labelSmall,
                            color = SignalCrimson
                        )
                        LinearProgressIndicator(
                            progress = { bf.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(5.dp),
                            color = SignalCrimson,
                            trackColor = ObsidianBlack
                        )
                    }
                }
            }
        }

        // Right: Weapon Ammo Readout (or Armored Rover Turret Readout) + FPS + Pause
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val wName = if (hud.inVehicle) {
                if (language == GameLanguage.ARABIC) "مدفع المدرّعة" else "ROVER TURRET"
            } else {
                WeaponCatalog.byId(hud.weaponId).name.get(language)
            }
            Surface(
                color = HudOverlayBg,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, if (hud.inVehicle) DesertGold else TacticalBorder)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(wName, style = MaterialTheme.typography.labelSmall, color = DesertGold, maxLines = 1)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (hud.inVehicle) {
                            Text("∞", style = MaterialTheme.typography.titleLarge, color = DesertGold)
                            Text("AUTO", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        } else {
                            Text(
                                text = if (hud.isReloading) GameStrings.hudReloading(language) else "${hud.ammoInMag}",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (hud.ammoInMag <= 4) SignalCrimson else TextPrimary
                            )
                            if (!hud.isReloading) {
                                Text("/ ${hud.ammoReserve}", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            }
                        }
                    }
                    Text("${hud.fps} FPS • ${hud.activeEnemiesCount} TGT", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
            IconButton(
                onClick = onPause,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HudOverlayBg)
                    .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp))
                    .testTag("hud_pause_btn")
            ) {
                Icon(Icons.Default.Pause, contentDescription = GameStrings.pauseTitle(language), tint = DesertGold)
            }
        }
    }
}

@Composable
private fun TacticalMiniRadar(
    enemies: List<Pair<Float, Float>>,
    objective: Pair<Float, Float>?,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xBB0B1218))
            .border(1.5.dp, DesertGold.copy(alpha = 0.6f), CircleShape)
    ) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f
        val rangeMeters = 36f
        drawCircle(TacticalBorder.copy(alpha = 0.45f), r * 0.55f, c, style = Stroke(1f))
        drawLine(TacticalBorder.copy(alpha = 0.35f), Offset(c.x, 4f), Offset(c.x, size.height - 4f), 1f)
        drawLine(TacticalBorder.copy(alpha = 0.35f), Offset(4f, c.y), Offset(size.width - 4f, c.y), 1f)

        // Objective diamond dot
        objective?.let { (ox, oz) ->
            val sx = (ox / rangeMeters).coerceIn(-0.88f, 0.88f)
            val sz = (oz / rangeMeters).coerceIn(-0.88f, 0.88f)
            drawCircle(DesertGold, 4.dp.toPx(), Offset(c.x + sx * r, c.y + sz * r))
        }
        // Enemy red blips
        enemies.forEach { (ex, ez) ->
            val d = hypot(ex, ez)
            if (d <= rangeMeters) {
                drawCircle(SignalCrimson, 3.dp.toPx(), Offset(c.x + (ex / rangeMeters) * r, c.y + (ez / rangeMeters) * r))
            }
        }
        // Player center chevron
        drawCircle(TacticalEmerald, 3.5.dp.toPx(), c)
    }
}

@Composable
private fun CenterTacticalReticle(
    hud: HudSnapshot,
    style: CrosshairStyle,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(130.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Directional damage red arc
        if (hud.damageFlashAlpha > 0.02f) {
            val angleDeg = Math.toDegrees(hud.damageDirectionRad.toDouble()).toFloat() - 90f
            drawArc(
                color = SignalCrimson.copy(alpha = hud.damageFlashAlpha * 0.85f),
                startAngle = angleDeg - 28f,
                sweepAngle = 56f,
                useCenter = false,
                topLeft = Offset(10f, 10f),
                size = Size(size.width - 20f, size.height - 20f),
                style = Stroke(width = 7f, cap = StrokeCap.Round)
            )
        }

        // Crosshair (bright SignalCrimson highlight on clean enemy target lock)
        val gap = if (hud.isAiming) 6f else if (style == CrosshairStyle.DYNAMIC) 14f else 10f
        val arm = if (hud.isAiming) 9f else 13f
        val col = when {
            hud.hasTargetLock -> SignalCrimson
            hud.isAiming -> DesertGold
            else -> Color.White.copy(alpha = 0.88f)
        }
        if (style == CrosshairStyle.TACTICAL_DOT) {
            drawCircle(col, 3.2f, Offset(cx, cy))
            drawCircle(col.copy(alpha = 0.45f), gap + 4f, Offset(cx, cy), style = Stroke(1.5f))
        } else {
            drawLine(col, Offset(cx - gap - arm, cy), Offset(cx - gap, cy), 2.2f)
            drawLine(col, Offset(cx + gap, cy), Offset(cx + gap + arm, cy), 2.2f)
            drawLine(col, Offset(cx, cy - gap - arm), Offset(cx, cy - gap), 2.2f)
            drawLine(col, Offset(cx, cy + gap), Offset(cx, cy + gap + arm), 2.2f)
            drawCircle(col, 2f, Offset(cx, cy))
        }
        if (hud.hasTargetLock) {
            val r = gap + arm + 5f
            drawCircle(SignalCrimson.copy(alpha = 0.55f), r, Offset(cx, cy), style = Stroke(1.4f))
        }

        // Hit Confirmation X (Gold on Headshot, White on Body)
        if (hud.hitMarkerAlpha > 0.02f) {
            val hmCol = (if (hud.hitMarkerHeadshot) DesertGold else Color.White).copy(alpha = hud.hitMarkerAlpha)
            val d1 = 6f; val d2 = 16f
            drawLine(hmCol, Offset(cx - d2, cy - d2), Offset(cx - d1, cy - d1), 3f)
            drawLine(hmCol, Offset(cx + d1, cy - d1), Offset(cx + d2, cy - d2), 3f)
            drawLine(hmCol, Offset(cx - d2, cy + d2), Offset(cx - d1, cy + d1), 3f)
            drawLine(hmCol, Offset(cx + d1, cy + d1), Offset(cx + d2, cy + d2), 3f)
        }
    }
}

@Composable
private fun ProjectedBattlefieldOverlay(
    enemyBars: List<ProjectedEnemyBar>,
    objectiveIndicator: ObjectiveEdgeIndicator?
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        // 1. Floating 3D-projected enemy health bars on a single fast Canvas
        if (enemyBars.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (bar in enemyBars) {
                    val bw = if (bar.isBoss) 72.dp.toPx() else 44.dp.toPx()
                    val bh = if (bar.isBoss) 6.dp.toPx() else 4.5.dp.toPx()
                    val cx = bar.screenX * size.width
                    val cy = bar.screenY * size.height
                    val left = cx - bw * 0.5f
                    val top = cy - bh * 0.5f
                    drawRect(Color(0xCC0A0E12), Offset(left - 1.5f, top - 1.5f), Size(bw + 3f, bh + 3f))
                    val fillCol = when {
                        bar.isBoss -> DesertGold
                        bar.isSniper -> ArmorCyan
                        else -> SignalCrimson
                    }
                    drawRect(fillCol, Offset(left, top), Size(bw * bar.healthFrac.coerceIn(0f, 1f), bh))
                }
            }
        }

        // 2. Directional Objective Arrow + Distance Badge
        objectiveIndicator?.let { ind ->
            val pxX = with(density) { (maxWidth.toPx() * ind.screenX).roundToInt() }
            val pxY = with(density) { (maxHeight.toPx() * ind.screenY).roundToInt() }
            Column(
                modifier = Modifier
                    .offset { IntOffset(pxX - 34, pxY - 24) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    rotate(ind.angleDeg) {
                        val path = Path().apply {
                            moveTo(size.width * 0.5f, 2f)
                            lineTo(size.width - 2f, size.height - 3f)
                            lineTo(size.width * 0.5f, size.height * 0.68f)
                            lineTo(2f, size.height - 3f)
                            close()
                        }
                        drawPath(path, DesertGold)
                    }
                }
                Surface(
                    color = Color(0xCC0A0E12),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, DesertGold.copy(alpha = 0.75f))
                ) {
                    Text(
                        text = "${ind.distanceMeters}m",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = DesertGold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SniperScopeOverlay(
    weaponId: WeaponId,
    hasTargetLock: Boolean
) {
    Box(modifier = Modifier.fillMaxSize().testTag("sniper_scope_overlay")) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = (size.minDimension * 0.43f).coerceAtLeast(120f)

            // Even-Odd blackout mask outside circular sniper optic
            val maskPath = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(0f, 0f, size.width, size.height))
                addOval(Rect(cx - radius, cy - radius, cx + radius, cy + radius))
            }
            drawPath(maskPath, Color(0xF5040608))

            // Outer optic bezel ring
            drawCircle(Color(0xFF1A222A), radius, Offset(cx, cy), style = Stroke(width = 8f))
            drawCircle(DesertGold.copy(alpha = 0.55f), radius - 4f, Offset(cx, cy), style = Stroke(width = 1.5f))

            // Fine crosshairs & mil-dot ticks
            val reticleCol = if (hasTargetLock) SignalCrimson else Color(0xFF14181C)
            drawLine(reticleCol, Offset(cx - radius, cy), Offset(cx + radius, cy), 1.8f)
            drawLine(reticleCol, Offset(cx, cy - radius), Offset(cx, cy + radius), 1.8f)
            // Thicker outer stadia bars
            drawLine(Color.Black, Offset(cx - radius, cy), Offset(cx - radius * 0.42f, cy), 5f)
            drawLine(Color.Black, Offset(cx + radius * 0.42f, cy), Offset(cx + radius, cy), 5f)
            drawLine(Color.Black, Offset(cx, cy + radius * 0.42f), Offset(cx, cy + radius), 5f)

            // Mil-dot range ticks
            val step = radius * 0.11f
            for (k in 1..3) {
                val tick = if (k == 2) 7f else 4.5f
                drawLine(reticleCol, Offset(cx - k * step, cy - tick), Offset(cx - k * step, cy + tick), 1.6f)
                drawLine(reticleCol, Offset(cx + k * step, cy - tick), Offset(cx + k * step, cy + tick), 1.6f)
                drawLine(reticleCol, Offset(cx - tick, cy + k * step), Offset(cx + tick, cy + k * step), 1.6f)
                drawLine(reticleCol, Offset(cx - tick, cy - k * step), Offset(cx + tick, cy - k * step), 1.6f)
            }
            // Illuminated center dot
            drawCircle(if (hasTargetLock) SignalCrimson else TacticalEmerald, 3.2f, Offset(cx, cy))
        }

        // Optic magnification badge
        Surface(
            color = Color(0xCC080C10),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, DesertGold.copy(alpha = 0.65f)),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 128.dp, y = (-104).dp)
        ) {
            Text(
                text = if (weaponId == WeaponId.SR308_PRECISION) "6.0x OPTIC" else "4.0x DMR",
                style = MaterialTheme.typography.labelSmall,
                color = DesertGold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun KillFeedOverlay(
    entries: List<KillFeedEntry>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        entries.take(4).forEach { entry ->
            val accent = if (entry.isHeadshot) DesertGold else TacticalEmerald
            Surface(
                color = Color(0xCC0A0E12),
                shape = RoundedCornerShape(5.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.75f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.text,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "+${entry.xpBonus} XP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
            }
        }
    }
}

@Composable
private fun VirtualAnalogJoystick(
    scale: Float,
    opacity: Float,
    deadzone: Float,
    onMove: (Float, Float, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val outerSize = (122 * scale).dp
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(Unit) {
        onDispose {
            knobOffset = Offset.Zero
            onMove(0f, 0f, false)
        }
    }

    Box(
        modifier = modifier
            .size(outerSize)
            .alpha(opacity)
            .pointerInput(deadzone) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val maxR = size.width * 0.42f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    fun emitFrom(pos: Offset) {
                        val dx = pos.x - center.x
                        val dy = pos.y - center.y
                        val dist = hypot(dx, dy).coerceAtLeast(1f)
                        val clamped = dist.coerceAtMost(maxR)
                        val nx = (dx / dist) * (clamped / maxR)
                        val ny = -(dy / dist) * (clamped / maxR)
                        knobOffset = Offset((dx / dist) * clamped, (dy / dist) * clamped)
                        val mag = hypot(nx, ny)
                        if (mag < deadzone) {
                            onMove(0f, 0f, false)
                        } else {
                            onMove(nx, ny, ny > 0.86f)
                        }
                    }

                    try {
                        emitFrom(down.position)
                        while (true) {
                            val ev = awaitPointerEvent()
                            val change = ev.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) {
                                break
                            }
                            change.consume()
                            emitFrom(change.position)
                        }
                    } finally {
                        knobOffset = Offset.Zero
                        onMove(0f, 0f, false)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2f
            drawCircle(HudControlBg, r, c)
            drawCircle(DesertGold.copy(alpha = 0.55f), r - 2f, c, style = Stroke(2.2f))
            drawCircle(DesertGold.copy(alpha = 0.85f), r * 0.34f, c + knobOffset)
        }
    }
}

@Composable
private fun CombatActionButtons(
    hud: HudSnapshot,
    language: GameLanguage,
    scale: Float,
    opacity: Float,
    controls: com.example.game.save.ControlLayoutSettings,
    inputBridge: MobileInputBridge,
    modifier: Modifier = Modifier
) {
    val fireSize = (78 * scale).dp
    val btnSize = (54 * scale).dp

    Column(
        modifier = modifier.alpha(opacity),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top mini row (foot soldier only): Vault, Cover, Shoulder, Swap, Crouch, Jump
        if (!hud.inVehicle) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hud.canVault) {
                    TacticalTapButton(
                        label = GameStrings.hudVault(language),
                        size = btnSize,
                        active = hud.isVaulting,
                        accent = ArmorCyan,
                        testTag = "hud_vault_btn",
                        onTap = { inputBridge.triggerVault() }
                    )
                }
                if (hud.canEnterCover || hud.isInCover) {
                    TacticalTapButton(
                        label = GameStrings.hudCover(language),
                        size = btnSize,
                        active = hud.isInCover,
                        accent = TacticalEmerald,
                        offsetDp = IntOffset((controls.coverOffset.dx * 90).roundToInt(), (controls.coverOffset.dy * 90).roundToInt()),
                        testTag = "hud_cover_btn",
                        onTap = { inputBridge.triggerCover() }
                    )
                }
                TacticalTapButton(
                    label = "${GameStrings.hudShoulder(language)} ${if (hud.cameraShoulder.sign > 0f) "R" else "L"}",
                    size = btnSize,
                    active = hud.cameraShoulder.sign < 0f,
                    accent = ArmorCyan,
                    testTag = "hud_shoulder_btn",
                    onTap = { inputBridge.triggerToggleShoulder() }
                )
                TacticalTapButton(
                    label = GameStrings.hudSwitch(language),
                    size = btnSize,
                    active = false,
                    testTag = "hud_swap_btn",
                    onTap = { inputBridge.triggerSwitchWeapon() }
                )
                TacticalTapButton(
                    label = GameStrings.hudCrouch(language),
                    size = btnSize,
                    active = hud.isCrouching,
                    offsetDp = IntOffset((controls.crouchOffset.dx * 90).roundToInt(), (controls.crouchOffset.dy * 90).roundToInt()),
                    testTag = "hud_crouch_btn",
                    onTap = { inputBridge.crouchToggled = !inputBridge.crouchToggled }
                )
                TacticalTapButton(
                    label = GameStrings.hudJump(language),
                    size = btnSize,
                    active = false,
                    offsetDp = IntOffset((controls.jumpOffset.dx * 90).roundToInt(), (controls.jumpOffset.dy * 90).roundToInt()),
                    testTag = "hud_jump_btn",
                    onTap = { inputBridge.triggerJump() }
                )
            }
        }

        // Tactical Equipment Row: Frag Grenade (on foot) + Field Medkit (always accessible)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TacticalTapButton(
                label = "${GameStrings.hudMedkit(language)} ${hud.medkits}",
                size = btnSize.coerceAtLeast(48.dp),
                active = hud.health < 55f && hud.medkits > 0,
                accent = if (hud.medkits > 0) TacticalEmerald else TextSecondary,
                testTag = "hud_medkit_btn",
                onTap = { inputBridge.triggerMedkit() }
            )
            if (!hud.inVehicle) {
                TacticalTapButton(
                    label = "${GameStrings.hudGrenade(language)} ${hud.grenades}",
                    size = btnSize.coerceAtLeast(48.dp),
                    active = false,
                    accent = if (hud.grenades > 0) DesertGold else TextSecondary,
                    testTag = "hud_grenade_btn",
                    onTap = { inputBridge.triggerGrenade() }
                )
            }
        }

        // Primary row: Reload (on foot), Aim (ADS / Turret Zoom), FIRE (Weapon / Turret)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!hud.inVehicle) {
                TacticalTapButton(
                    label = GameStrings.hudReload(language),
                    size = btnSize,
                    active = hud.isReloading,
                    offsetDp = IntOffset((controls.reloadOffset.dx * 90).roundToInt(), (controls.reloadOffset.dy * 90).roundToInt()),
                    testTag = "hud_reload_btn",
                    onTap = { inputBridge.triggerReload() }
                )
            }
            TacticalHoldButton(
                label = GameStrings.hudAim(language),
                size = (62 * scale).dp,
                active = hud.isAiming,
                accent = ArmorCyan,
                offsetDp = IntOffset((controls.aimOffset.dx * 90).roundToInt(), (controls.aimOffset.dy * 90).roundToInt()),
                testTag = "hud_aim_btn",
                onHoldChanged = { held -> inputBridge.aimHeld = held }
            )
            TacticalHoldButton(
                label = GameStrings.hudFire(language),
                size = fireSize,
                active = inputBridge.fireHeld || (hud.autoFireEnabled && hud.hasTargetLock),
                accent = SignalCrimson,
                offsetDp = IntOffset((controls.fireOffset.dx * 90).roundToInt(), (controls.fireOffset.dy * 90).roundToInt()),
                testTag = "hud_fire_btn",
                onHoldChanged = { held -> inputBridge.fireHeld = held }
            )
        }
    }
}

@Composable
private fun TacticalTapButton(
    label: String,
    size: Dp,
    active: Boolean,
    accent: Color = DesertGold,
    offsetDp: IntOffset = IntOffset.Zero,
    testTag: String,
    onTap: () -> Unit
) {
    Surface(
        onClick = onTap,
        shape = CircleShape,
        color = if (active) accent.copy(alpha = 0.85f) else HudControlBg,
        border = BorderStroke(1.5.dp, if (active) Color.White else accent.copy(alpha = 0.7f)),
        modifier = Modifier
            .offset { offsetDp }
            .size(size)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (active) ObsidianBlack else TextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TacticalHoldButton(
    label: String,
    size: Dp,
    active: Boolean,
    accent: Color,
    offsetDp: IntOffset = IntOffset.Zero,
    testTag: String,
    onHoldChanged: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val isLit = pressed || active
    DisposableEffect(Unit) {
        onDispose {
            pressed = false
            onHoldChanged(false)
        }
    }
    Box(
        modifier = Modifier
            .offset { offsetDp }
            .size(size)
            .clip(CircleShape)
            .background(if (isLit) accent.copy(alpha = 0.88f) else HudControlBg)
            .border(2.dp, if (isLit) Color.White else accent, CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    pressed = true
                    onHoldChanged(true)
                    try {
                        while (true) {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.firstOrNull { it.id == down.id }
                            if (ch == null || !ch.pressed) break
                            ch.consume()
                        }
                    } finally {
                        pressed = false
                        onHoldChanged(false)
                    }
                }
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (isLit) ObsidianBlack else TextPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun PauseOverlay(
    language: GameLanguage,
    showCheckpoint: Boolean,
    onResume: () -> Unit,
    onLoadCheckpoint: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xC8070B0E)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = TacticalSlateDark,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, DesertGold),
            modifier = Modifier.width(340.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(GameStrings.pauseTitle(language), style = MaterialTheme.typography.headlineMedium, color = DesertGold)
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = DesertGold, contentColor = ObsidianBlack),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_resume_btn")
                ) { Text(GameStrings.resume(language), fontWeight = FontWeight.Bold) }
                if (showCheckpoint) {
                    OutlinedButton(onClick = onLoadCheckpoint, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text(GameStrings.loadCheckpoint(language))
                    }
                }
                OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text(GameStrings.restartMission(language))
                }
                OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text(GameStrings.exitToMenu(language), color = SignalCrimson)
                }
            }
        }
    }
}
