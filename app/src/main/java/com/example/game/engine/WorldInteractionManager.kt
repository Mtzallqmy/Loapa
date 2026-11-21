package com.example.game.engine

import com.example.game.data.GameLanguage
import com.example.game.data.InteractiveKind
import com.example.game.data.LocalizedText

data class InteractiveObject(
    val id: String,
    val kind: InteractiveKind,
    var position: Vec3,
    val interactRadius: Float = 2.8f,
    val label: LocalizedText,
    var isActivated: Boolean = false,
    var isLocked: Boolean = false,
    val linkedTargetId: String? = null,
    val linkedBlockId: String? = null,
    var health: Float = 35f,
    var isDestroyed: Boolean = false
) {
    val isAvailableForPrompt: Boolean
        get() = when (kind) {
            InteractiveKind.DOOR, InteractiveKind.GATE -> !isDestroyed
            InteractiveKind.SWITCH -> !isActivated
            InteractiveKind.AMMO_CONTAINER, InteractiveKind.SUPPLY_BOX -> !isActivated
            InteractiveKind.DESTRUCTIBLE_BARREL -> !isDestroyed
            InteractiveKind.OBJECTIVE_DEVICE -> !isActivated
            InteractiveKind.VEHICLE -> !isDestroyed && !isActivated
            InteractiveKind.CHECKPOINT_MARKER, InteractiveKind.EXTRACTION_ZONE -> false
        }
}

data class InteractionOutcome(
    val interacted: Boolean,
    val obj: InteractiveObject? = null,
    val message: LocalizedText? = null,
    val triggeredBarrelBlast: Boolean = false,
    val unlockedTargetId: String? = null,
    val resupplied: Boolean = false,
    val objectiveDeviceActivated: Boolean = false,
    val boardedVehicle: Boolean = false
)

/**
 * v1.1.0 Interactive World Manager.
 * Validates distance, line-of-sight, debouncing, and physical state changes for doors,
 * gates, switches, ammo/supply crates, destructible barrels, and objective terminals.
 */
class WorldInteractionManager(
    val objects: MutableList<InteractiveObject>,
    private val eventBus: GameplayEventBus
) {
    private var debounceTimer: Float = 0f

    fun tick(dt: Float) {
        if (debounceTimer > 0f) {
            debounceTimer = (debounceTimer - dt).coerceAtLeast(0f)
        }
    }

    /** Finds the nearest valid interactive object within reach and unobstructed line-of-sight. */
    fun findPromptTarget(player: PlayerController, city: CityMap): InteractiveObject? {
        if (!player.isAlive) return null
        val eye = Vec3(player.position.x, player.position.y + player.eyeHeight, player.position.z)
        var best: InteractiveObject? = null
        var bestDist = Float.MAX_VALUE
        for (i in objects.indices) {
            val obj = objects[i]
            if (!obj.isAvailableForPrompt) continue
            val d = player.position.distanceXZ(obj.position)
            if (d <= obj.interactRadius && d < bestDist) {
                val targetPos = Vec3(obj.position.x, 1.0f, obj.position.z)
                // Doors/gates themselves may be colliders, so skip LoS block for doors/gates when within 3m
                val hasLos = obj.kind == InteractiveKind.DOOR ||
                    obj.kind == InteractiveKind.GATE ||
                    city.hasLineOfSight(eye, targetPos)
                if (hasLos) {
                    bestDist = d
                    best = obj
                }
            }
        }
        return best
    }

    fun buildPromptText(obj: InteractiveObject?, language: GameLanguage): String? {
        if (obj == null) return null
        val action = when (obj.kind) {
            InteractiveKind.DOOR -> if (obj.isActivated) LocalizedText("إغلاق", "CLOSE") else LocalizedText("فتح", "OPEN")
            InteractiveKind.GATE -> when {
                obj.isLocked -> LocalizedText("مغلق بقفل", "LOCKED")
                obj.isActivated -> LocalizedText("إغلاق البوابة", "CLOSE GATE")
                else -> LocalizedText("فتح البوابة", "OPEN GATE")
            }
            InteractiveKind.SWITCH -> LocalizedText("تفعيل", "ACTIVATE")
            InteractiveKind.AMMO_CONTAINER -> LocalizedText("تزود بالذخيرة", "RESUPPLY AMMO")
            InteractiveKind.SUPPLY_BOX -> LocalizedText("صندوق إمداد", "SUPPLY CRATE")
            InteractiveKind.DESTRUCTIBLE_BARREL -> LocalizedText("تفجير حذر", "TRIGGER CHARGE")
            InteractiveKind.OBJECTIVE_DEVICE -> LocalizedText("تشغيل الجهاز", "OPERATE TERMINAL")
            InteractiveKind.VEHICLE -> LocalizedText("ركوب المدرّعة", "BOARD ROVER")
            InteractiveKind.CHECKPOINT_MARKER, InteractiveKind.EXTRACTION_ZONE -> LocalizedText("", "")
        }
        return "${action.get(language)} • ${obj.label.get(language)}"
    }

    /** Executes debounced interaction against the nearest valid interactive object. */
    fun tryInteract(player: PlayerController, city: CityMap): InteractionOutcome {
        if (debounceTimer > 0f || !player.isAlive) return InteractionOutcome(false)
        val obj = findPromptTarget(player, city) ?: return InteractionOutcome(false)
        debounceTimer = 0.35f

        return when (obj.kind) {
            InteractiveKind.DOOR -> {
                obj.isActivated = !obj.isActivated
                setLinkedBlockActive(city, obj.linkedBlockId, active = !obj.isActivated)
                eventBus.publish(GameplayEvent.WorldInteracted(obj.id, obj.kind, obj.position))
                InteractionOutcome(
                    interacted = true,
                    obj = obj,
                    message = if (obj.isActivated) LocalizedText("تم فتح الباب التكتيكي", "Tactical door opened")
                    else LocalizedText("تم إغلاق الباب التكتيكي", "Tactical door closed")
                )
            }
            InteractiveKind.GATE -> {
                if (obj.isLocked) {
                    InteractionOutcome(
                        interacted = false,
                        obj = obj,
                        message = LocalizedText("البوابة مقفلة — فعّل مفتاح التحكم القريب", "Gate locked — activate nearby control switch")
                    )
                } else {
                    obj.isActivated = !obj.isActivated
                    setLinkedBlockActive(city, obj.linkedBlockId, active = !obj.isActivated)
                    eventBus.publish(GameplayEvent.WorldInteracted(obj.id, obj.kind, obj.position))
                    InteractionOutcome(
                        interacted = true,
                        obj = obj,
                        message = if (obj.isActivated) LocalizedText("تم فتح البوابة الأمنية", "Security gate opened")
                        else LocalizedText("تم إغلاق البوابة الأمنية", "Security gate closed")
                    )
                }
            }
            InteractiveKind.SWITCH -> {
                obj.isActivated = true
                val linked = obj.linkedTargetId?.let { id -> objects.firstOrNull { it.id == id } }
                if (linked != null) {
                    linked.isLocked = false
                    linked.isActivated = true
                    setLinkedBlockActive(city, linked.linkedBlockId, active = false)
                }
                eventBus.publish(GameplayEvent.WorldInteracted(obj.id, obj.kind, obj.position))
                InteractionOutcome(
                    interacted = true,
                    obj = obj,
                    unlockedTargetId = linked?.id,
                    message = LocalizedText("تم تفعيل المفتاح وفتح البوابة", "Switch activated — gate unlocked")
                )
            }
            InteractiveKind.AMMO_CONTAINER, InteractiveKind.SUPPLY_BOX -> {
                obj.isActivated = true
                player.resupplyAll()
                eventBus.publish(GameplayEvent.PlayerHealed(35f, player.maxArmor))
                eventBus.publish(GameplayEvent.WorldInteracted(obj.id, obj.kind, obj.position))
                InteractionOutcome(
                    interacted = true,
                    obj = obj,
                    resupplied = true,
                    message = LocalizedText("تم التزود بالذخيرة والدرع", "Ammo & armor resupplied")
                )
            }
            InteractiveKind.DESTRUCTIBLE_BARREL -> {
                obj.isDestroyed = true
                obj.health = 0f
                eventBus.publish(GameplayEvent.WorldInteracted(obj.id, obj.kind, obj.position))
                InteractionOutcome(
                    interacted = true,
                    obj = obj,
                    triggeredBarrelBlast = true,
                    message = LocalizedText("انفجار برميل وقود!", "Fuel barrel detonated!")
                )
            }
            InteractiveKind.OBJECTIVE_DEVICE -> {
                obj.isActivated = true
                eventBus.publish(GameplayEvent.WorldInteracted(obj.id, obj.kind, obj.position))
                InteractionOutcome(
                    interacted = true,
                    obj = obj,
                    objectiveDeviceActivated = true,
                    message = LocalizedText("تم تأمين جهاز الاتصالات التكتيكي", "Tactical terminal secured")
                )
            }
            InteractiveKind.VEHICLE -> {
                eventBus.publish(GameplayEvent.WorldInteracted(obj.id, obj.kind, obj.position))
                InteractionOutcome(
                    interacted = true,
                    obj = obj,
                    boardedVehicle = true,
                    message = LocalizedText("تم ركوب المدرّعة التكتيكية", "Tactical Rover boarded")
                )
            }
            InteractiveKind.CHECKPOINT_MARKER, InteractiveKind.EXTRACTION_ZONE -> InteractionOutcome(false)
        }
    }

    /** Ray-tests destructible explosive barrels against weapon shots. */
    fun raycastBarrel(origin: Vec3, dir: Vec3, maxDist: Float): Pair<InteractiveObject, Float>? {
        var best: InteractiveObject? = null
        var bestDist = maxDist
        for (i in objects.indices) {
            val obj = objects[i]
            if (obj.kind != InteractiveKind.DESTRUCTIBLE_BARREL || obj.isDestroyed) continue
            val box = AABB(
                obj.position.x - 0.55f, 0f, obj.position.z - 0.55f,
                obj.position.x + 0.55f, 1.35f, obj.position.z + 0.55f
            )
            val t = box.intersectRay(origin, dir, bestDist)
            if (t != null && t < bestDist) {
                bestDist = t
                best = obj
            }
        }
        return best?.let { it to bestDist }
    }

    /** Damages a destructible barrel; returns true when it detonates. */
    fun damageBarrel(barrel: InteractiveObject, damage: Float): Boolean {
        if (barrel.isDestroyed || barrel.kind != InteractiveKind.DESTRUCTIBLE_BARREL) return false
        barrel.health -= damage
        if (barrel.health <= 0f) {
            barrel.health = 0f
            barrel.isDestroyed = true
            eventBus.publish(GameplayEvent.WorldInteracted(barrel.id, barrel.kind, barrel.position))
            return true
        }
        return false
    }

    private fun setLinkedBlockActive(city: CityMap, blockId: String?, active: Boolean) {
        if (blockId == null) return
        city.blocks.firstOrNull { it.id == blockId }?.let { block ->
            block.isActive = active
        }
    }
}
