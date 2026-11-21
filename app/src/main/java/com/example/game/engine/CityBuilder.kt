package com.example.game.engine

import com.example.game.data.CityId
import com.example.game.data.InteractiveKind
import com.example.game.data.LocalizedText
import com.example.game.data.SurfaceMaterial

enum class BlockShape {
    BOX,
    CYLINDER,
    WEDGE_SLOPE
}

data class WorldBlock(
    val id: String,
    val bounds: AABB,
    val material: SurfaceMaterial,
    val colorRgb: FloatArray,
    val roughness: Float = 0.78f,
    val metallic: Float = 0.08f,
    val emissive: Float = 0f,
    val patternType: Int = 0, // 0=solid, 1=brick/windows, 2=asphalt, 3=sand, 4=steel ribs, 5=rock
    val shape: BlockShape = BlockShape.BOX,
    val solid: Boolean = true,
    val isCover: Boolean = false,
    val detailTier: Int = 1, // 1=always, 2=medium+, 3=high only
    val isVaultable: Boolean = false,
    var isActive: Boolean = true
)

data class CoverPoint(
    val position: Vec3,
    val normal: Vec3, // direction facing the cover barrier
    val height: Float
)

data class ResupplyStation(
    val id: String,
    val position: Vec3,
    var used: Boolean = false
)

data class CityMap(
    val cityId: CityId,
    val minBoundX: Float,
    val maxBoundX: Float,
    val minBoundZ: Float,
    val maxBoundZ: Float,
    val groundMaterial: SurfaceMaterial,
    val groundColorRgb: FloatArray,
    val blocks: List<WorldBlock>,
    val colliders: List<WorldBlock>,
    val coverPoints: List<CoverPoint>,
    val resupplyStations: List<ResupplyStation>,
    val interactiveObjects: List<InteractiveObject> = emptyList(),
    val vehicleSpawnPos: Vec3 = Vec3(0f, 0f, 14f)
) {
    fun hasLineOfSight(from: Vec3, to: Vec3): Boolean {
        val delta = to - from
        val dist = delta.length()
        if (dist < 0.25f) return true
        val dir = delta * (1f / dist)
        val maxCheck = dist - 0.15f
        for (i in colliders.indices) {
            val b = colliders[i]
            if (!b.isActive) continue
            val t = b.bounds.intersectRay(from, dir, maxCheck)
            if (t != null && t > 0.08f) return false
        }
        return true
    }

    fun raycastWorld(origin: Vec3, dir: Vec3, maxDist: Float): Pair<Float, SurfaceMaterial>? {
        var bestDist = maxDist
        var bestMat: SurfaceMaterial? = null
        // Check ground plane y = 0
        if (dir.y < -1e-4f && origin.y > 0f) {
            val tGround = -origin.y / dir.y
            if (tGround in 0.05f..bestDist) {
                bestDist = tGround
                bestMat = groundMaterial
            }
        }
        for (i in colliders.indices) {
            val block = colliders[i]
            if (!block.isActive) continue
            val t = block.bounds.intersectRay(origin, dir, bestDist)
            if (t != null && t < bestDist) {
                bestDist = t
                bestMat = block.material
            }
        }
        return bestMat?.let { bestDist to it }
    }
}

object CityBuilder {
    fun buildCity(cityId: CityId): CityMap = when (cityId) {
        CityId.RIVER_CITY -> buildRiverCity()
        CityId.MIRAGE_CITY -> buildMirageCity()
        CityId.HARBOR_CITY -> buildHarborCity()
        CityId.MOUNTAIN_CITY -> buildMountainCity()
    }

    private fun buildRiverCity(): CityMap {
        val blocks = mutableListOf<WorldBlock>()
        val covers = mutableListOf<CoverPoint>()
        val interactives = mutableListOf<InteractiveObject>()
        var idx = 0

        fun addBox(
            cx: Float, baseY: Float, cz: Float, sx: Float, sy: Float, sz: Float,
            mat: SurfaceMaterial, rgb: FloatArray, rough: Float = 0.78f, metal: Float = 0.08f,
            emissive: Float = 0f, pattern: Int = 0, solid: Boolean = true, isCover: Boolean = false,
            tier: Int = 1, shape: BlockShape = BlockShape.BOX, customId: String? = null,
            vaultable: Boolean = isCover && sy in 0.85f..1.55f
        ): String {
            val id = customId ?: "rc_${idx++}"
            val aabb = AABB(cx - sx * 0.5f, baseY, cz - sz * 0.5f, cx + sx * 0.5f, baseY + sy, cz + sz * 0.5f)
            blocks.add(WorldBlock(id, aabb, mat, rgb, rough, metal, emissive, pattern, shape, solid, isCover, tier, vaultable))
            if (isCover) {
                covers.add(CoverPoint(Vec3(cx, baseY, cz + sz * 0.5f + 0.85f), Vec3(0f, 0f, -1f), sy))
                covers.add(CoverPoint(Vec3(cx, baseY, cz - sz * 0.5f - 0.85f), Vec3(0f, 0f, 1f), sy))
                covers.add(CoverPoint(Vec3(cx - sx * 0.5f - 0.85f, baseY, cz), Vec3(1f, 0f, 0f), sy))
                covers.add(CoverPoint(Vec3(cx + sx * 0.5f + 0.85f, baseY, cz), Vec3(-1f, 0f, 0f), sy))
            }
            return id
        }

        // Main asphalt avenue & side plaza sidewalks
        addBox(0f, -0.05f, 0f, 18f, 0.08f, 84f, SurfaceMaterial.ASPHALT, floatArrayOf(0.18f, 0.19f, 0.21f), pattern = 2, solid = false)
        addBox(-14f, 0f, 0f, 10f, 0.16f, 82f, SurfaceMaterial.CONCRETE, floatArrayOf(0.46f, 0.45f, 0.42f), solid = false)
        addBox(14f, 0f, 0f, 10f, 0.16f, 82f, SurfaceMaterial.CONCRETE, floatArrayOf(0.46f, 0.45f, 0.42f), solid = false)
        // River canal running along far east edge
        addBox(30f, -0.2f, 0f, 12f, 0.18f, 86f, SurfaceMaterial.WATER, floatArrayOf(0.12f, 0.30f, 0.42f), rough = 0.22f, metal = 0.4f, solid = false)

        // Multi-level modern concrete & brick buildings framing the urban corridor
        val westBuildings = listOf(
            Triple(-23f, 22f, 14f), Triple(-24f, 4f, 19f), Triple(-23f, -14f, 15f), Triple(-24f, -31f, 18f)
        )
        westBuildings.forEachIndexed { i, (x, z, h) ->
            val tone = if (i % 2 == 0) floatArrayOf(0.56f, 0.53f, 0.48f) else floatArrayOf(0.44f, 0.46f, 0.49f)
            addBox(x, 0f, z, 11f, h, 13f, SurfaceMaterial.CONCRETE, tone, pattern = 1)
            addBox(x, h, z, 11.4f, 0.7f, 13.4f, SurfaceMaterial.CONCRETE, floatArrayOf(0.32f, 0.34f, 0.36f), tier = 2)
        }
        val eastBuildings = listOf(
            Triple(22f, 24f, 13f), Triple(22f, 6f, 17f), Triple(22f, -12f, 12f), Triple(22f, -29f, 16f)
        )
        eastBuildings.forEachIndexed { i, (x, z, h) ->
            val tone = if (i % 2 == 0) floatArrayOf(0.52f, 0.48f, 0.43f) else floatArrayOf(0.38f, 0.43f, 0.47f)
            addBox(x, 0f, z, 9f, h, 12f, SurfaceMaterial.BRICK, tone, pattern = 1)
            addBox(x, h, z, 9.4f, 0.6f, 12.4f, SurfaceMaterial.CONCRETE, floatArrayOf(0.28f, 0.30f, 0.32f), tier = 2)
        }

        // Shops & market cover structures
        addBox(-12f, 0f, 14f, 6.5f, 3.8f, 6.5f, SurfaceMaterial.CONCRETE, floatArrayOf(0.54f, 0.50f, 0.44f), pattern = 1)
        addBox(12f, 0f, -2f, 6.5f, 4.2f, 7.5f, SurfaceMaterial.CONCRETE, floatArrayOf(0.48f, 0.46f, 0.43f), pattern = 1)
        addBox(-11f, 0f, -20f, 7f, 4.0f, 6.5f, SurfaceMaterial.CONCRETE, floatArrayOf(0.45f, 0.47f, 0.50f), pattern = 1)

        // Waist-high vaultable sandbag bunkers & concrete Jersey barriers
        val coverBarriers = listOf(
            Triple(-5.5f, 20f, 3.8f), Triple(5.5f, 16f, 4.2f), Triple(-2.5f, 7f, 4.0f),
            Triple(6.5f, 3f, 3.6f), Triple(-7.5f, -4f, 4.2f), Triple(3.5f, -9f, 4.0f),
            Triple(-4.5f, -15f, 3.8f), Triple(8.0f, -19f, 4.0f), Triple(-1.0f, -25f, 4.4f)
        )
        coverBarriers.forEachIndexed { i, (x, z, w) ->
            val isSandbag = i % 2 == 0
            addBox(
                x, 0f, z, w, 1.35f, 1.0f,
                if (isSandbag) SurfaceMaterial.SANDBAG else SurfaceMaterial.CONCRETE,
                if (isSandbag) floatArrayOf(0.60f, 0.52f, 0.38f) else floatArrayOf(0.46f, 0.47f, 0.49f),
                isCover = true,
                vaultable = true
            )
        }

        // Abandoned tactical vehicles
        val vehicles = listOf(Pair(4.8f, 24f), Pair(-6.0f, -10f), Pair(5.8f, -25f))
        vehicles.forEach { (vx, vz) ->
            addBox(vx, 0f, vz, 2.5f, 1.35f, 4.6f, SurfaceMaterial.VEHICLE, floatArrayOf(0.28f, 0.32f, 0.25f), rough = 0.5f, metal = 0.45f, isCover = true, vaultable = false)
            addBox(vx, 1.35f, vz - 0.3f, 2.2f, 0.85f, 2.2f, SurfaceMaterial.VEHICLE, floatArrayOf(0.20f, 0.24f, 0.20f), rough = 0.42f, metal = 0.55f, tier = 2)
        }

        // Street lamps
        listOf(-10f to 26f, 10f to 14f, -10f to 0f, 10f to -14f, -10f to -28f).forEach { (lx, lz) ->
            addBox(lx, 0f, lz, 0.26f, 6.2f, 0.26f, SurfaceMaterial.METAL, floatArrayOf(0.22f, 0.24f, 0.26f), metal = 0.7f, shape = BlockShape.CYLINDER, tier = 2)
            addBox(lx, 6.1f, lz, 0.55f, 0.25f, 0.55f, SurfaceMaterial.GLASS, floatArrayOf(1.0f, 0.85f, 0.55f), emissive = 0.9f, solid = false, tier = 2)
        }

        // v1.1.0 Interactive Tactical Door, Locked Checkpoint Gate + Switch, Barrels, Supply & Terminal
        val doorBlockId = addBox(-9.2f, 0f, 10.2f, 2.4f, 2.6f, 0.35f, SurfaceMaterial.METAL, floatArrayOf(0.32f, 0.42f, 0.48f), metal = 0.65f, customId = "rc_door_block_1")
        val gateBlockId = addBox(10.5f, 0f, -14.5f, 3.4f, 2.8f, 0.45f, SurfaceMaterial.METAL, floatArrayOf(0.68f, 0.38f, 0.16f), metal = 0.72f, customId = "rc_gate_block_1")
        interactives.addAll(
            listOf(
                InteractiveObject("rc_door_1", InteractiveKind.DOOR, Vec3(-9.2f, 0f, 10.2f), 3.0f, LocalizedText("باب الممر التكتيكي", "Tactical Side Door"), linkedBlockId = doorBlockId),
                InteractiveObject("rc_gate_1", InteractiveKind.GATE, Vec3(10.5f, 0f, -14.5f), 3.2f, LocalizedText("بوابة نقطة التفتيش", "Checkpoint Gate"), isLocked = true, linkedBlockId = gateBlockId),
                InteractiveObject("rc_switch_1", InteractiveKind.SWITCH, Vec3(8.2f, 0f, -12.0f), 2.8f, LocalizedText("مفتاح البوابة الأمنية", "Gate Control Switch"), linkedTargetId = "rc_gate_1"),
                InteractiveObject("rc_supply_1", InteractiveKind.SUPPLY_BOX, Vec3(-7.2f, 0f, 24.5f), 2.8f, LocalizedText("صندوق الإمداد الطبي", "Tactical Supply Box")),
                InteractiveObject("rc_ammo_crate_1", InteractiveKind.AMMO_CONTAINER, Vec3(8.0f, 0f, -5.0f), 2.8f, LocalizedText("حاوية ذخيرة", "Ammo Container")),
                InteractiveObject("rc_barrel_1", InteractiveKind.DESTRUCTIBLE_BARREL, Vec3(3.2f, 0f, 4.5f), 2.8f, LocalizedText("برميل وقود قابل للانفجار", "Explosive Fuel Barrel")),
                InteractiveObject("rc_barrel_2", InteractiveKind.DESTRUCTIBLE_BARREL, Vec3(-3.8f, 0f, -12.5f), 2.8f, LocalizedText("برميل وقود قابل للانفجار", "Explosive Fuel Barrel")),
                InteractiveObject("rc_terminal_1", InteractiveKind.OBJECTIVE_DEVICE, Vec3(0f, 0f, -20.5f), 3.0f, LocalizedText("محطة الاتصالات الميدانية", "Field Comms Terminal"))
            )
        )

        // Perimeter northern/southern blockade walls
        addBox(0f, 0f, 41f, 46f, 4.5f, 2f, SurfaceMaterial.CONCRETE, floatArrayOf(0.36f, 0.37f, 0.39f))
        addBox(0f, 0f, -41f, 46f, 4.5f, 2f, SurfaceMaterial.CONCRETE, floatArrayOf(0.36f, 0.37f, 0.39f))

        val resupply = listOf(
            ResupplyStation("rc_ammo_1", Vec3(8f, 0f, 8f)),
            ResupplyStation("rc_ammo_2", Vec3(-7f, 0f, -14f))
        )

        return CityMap(
            cityId = CityId.RIVER_CITY,
            minBoundX = -18.5f, maxBoundX = 18.5f, minBoundZ = -38.5f, maxBoundZ = 38.5f,
            groundMaterial = SurfaceMaterial.ASPHALT,
            groundColorRgb = floatArrayOf(0.24f, 0.25f, 0.27f),
            blocks = blocks,
            colliders = blocks.filter { it.solid },
            coverPoints = covers,
            resupplyStations = resupply,
            interactiveObjects = interactives,
            vehicleSpawnPos = Vec3(-1.5f, 0f, 12.5f)
        )
    }

    private fun buildMirageCity(): CityMap {
        val blocks = mutableListOf<WorldBlock>()
        val covers = mutableListOf<CoverPoint>()
        val interactives = mutableListOf<InteractiveObject>()
        var idx = 0

        fun addBox(
            cx: Float, baseY: Float, cz: Float, sx: Float, sy: Float, sz: Float,
            mat: SurfaceMaterial, rgb: FloatArray, rough: Float = 0.85f, metal: Float = 0.08f,
            pattern: Int = 0, solid: Boolean = true, isCover: Boolean = false,
            tier: Int = 1, shape: BlockShape = BlockShape.BOX, customId: String? = null,
            vaultable: Boolean = isCover && sy in 0.85f..1.55f
        ): String {
            val id = customId ?: "mc_${idx++}"
            val aabb = AABB(cx - sx * 0.5f, baseY, cz - sz * 0.5f, cx + sx * 0.5f, baseY + sy, cz + sz * 0.5f)
            blocks.add(WorldBlock(id, aabb, mat, rgb, rough, metal, 0f, pattern, shape, solid, isCover, tier, vaultable))
            if (isCover) {
                covers.add(CoverPoint(Vec3(cx, baseY, cz + sz * 0.5f + 0.85f), Vec3(0f, 0f, -1f), sy))
                covers.add(CoverPoint(Vec3(cx, baseY, cz - sz * 0.5f - 0.85f), Vec3(0f, 0f, 1f), sy))
            }
            return id
        }

        addBox(0f, -0.04f, 0f, 14f, 0.06f, 84f, SurfaceMaterial.SAND, floatArrayOf(0.68f, 0.56f, 0.40f), pattern = 3, solid = false)
        addBox(-23f, 0f, 18f, 9f, 5.5f, 14f, SurfaceMaterial.CONCRETE, floatArrayOf(0.74f, 0.64f, 0.48f), pattern = 1)
        addBox(23f, 0f, 16f, 9f, 6.2f, 13f, SurfaceMaterial.CONCRETE, floatArrayOf(0.70f, 0.60f, 0.44f), pattern = 1)
        addBox(-19f, 0f, -8f, 10f, 7.5f, 16f, SurfaceMaterial.METAL, floatArrayOf(0.55f, 0.49f, 0.40f), rough = 0.55f, metal = 0.55f, pattern = 4)
        addBox(20f, 0f, -10f, 10f, 7.2f, 16f, SurfaceMaterial.METAL, floatArrayOf(0.49f, 0.45f, 0.39f), rough = 0.55f, metal = 0.55f, pattern = 4)
        addBox(-17f, 0f, -31f, 8f, 10.5f, 8f, SurfaceMaterial.CONCRETE, floatArrayOf(0.66f, 0.57f, 0.44f))
        addBox(17f, 0f, -31f, 8f, 10.5f, 8f, SurfaceMaterial.CONCRETE, floatArrayOf(0.66f, 0.57f, 0.44f))

        listOf(Pair(11f, -20f), Pair(11f, -15f), Pair(-12f, 4f)).forEach { (sx, sz) ->
            addBox(sx, 0f, sz, 3.6f, 6.4f, 3.6f, SurfaceMaterial.METAL, floatArrayOf(0.62f, 0.55f, 0.46f), rough = 0.48f, metal = 0.62f, shape = BlockShape.CYLINDER, isCover = true, vaultable = false)
        }

        val coverItems = listOf(
            Triple(-5f, 22f, 4.0f), Triple(6f, 16f, 3.8f), Triple(-2f, 8f, 4.4f),
            Triple(7f, 0f, 4.0f), Triple(-8f, -6f, 4.2f), Triple(2f, -12f, 4.0f),
            Triple(-7f, -20f, 4.2f), Triple(5f, -24f, 4.0f)
        )
        coverItems.forEachIndexed { i, (x, z, w) ->
            val mat = if (i % 3 == 0) SurfaceMaterial.ROCK else if (i % 3 == 1) SurfaceMaterial.SANDBAG else SurfaceMaterial.WOOD
            val col = when (mat) {
                SurfaceMaterial.ROCK -> floatArrayOf(0.62f, 0.52f, 0.39f)
                SurfaceMaterial.SANDBAG -> floatArrayOf(0.72f, 0.61f, 0.44f)
                else -> floatArrayOf(0.52f, 0.38f, 0.24f)
            }
            addBox(x, 0f, z, w, 1.45f, 1.5f, mat, col, isCover = true, vaultable = true)
        }

        // v1.1.0 Interactive desert compound gate, switch, fuel barrels & supply crates
        val gateId = addBox(-10.5f, 0f, -14f, 3.6f, 2.8f, 0.45f, SurfaceMaterial.METAL, floatArrayOf(0.72f, 0.44f, 0.18f), metal = 0.68f, customId = "mc_gate_block_1")
        interactives.addAll(
            listOf(
                InteractiveObject("mc_gate_1", InteractiveKind.GATE, Vec3(-10.5f, 0f, -14f), 3.2f, LocalizedText("بوابة المستودع الصحراوي", "Depot Security Gate"), isLocked = true, linkedBlockId = gateId),
                InteractiveObject("mc_switch_1", InteractiveKind.SWITCH, Vec3(-8.5f, 0f, -11.5f), 2.8f, LocalizedText("لوحة تشغيل البوابة", "Depot Gate Switch"), linkedTargetId = "mc_gate_1"),
                InteractiveObject("mc_supply_1", InteractiveKind.SUPPLY_BOX, Vec3(7.5f, 0f, 20f), 2.8f, LocalizedText("صندوق إمداد صحراوي", "Desert Supply Box")),
                InteractiveObject("mc_barrel_1", InteractiveKind.DESTRUCTIBLE_BARREL, Vec3(4.5f, 0f, 5.5f), 2.8f, LocalizedText("برميل وقود مشتعل", "Explosive Fuel Barrel")),
                InteractiveObject("mc_barrel_2", InteractiveKind.DESTRUCTIBLE_BARREL, Vec3(-4.5f, 0f, -15.5f), 2.8f, LocalizedText("برميل وقود مشتعل", "Explosive Fuel Barrel")),
                InteractiveObject("mc_terminal_1", InteractiveKind.OBJECTIVE_DEVICE, Vec3(0f, 0f, -28f), 3.0f, LocalizedText("جهاز التشفير الصحراوي", "Desert Relay Terminal"))
            )
        )

        addBox(0f, 0f, 41f, 46f, 4f, 2f, SurfaceMaterial.CONCRETE, floatArrayOf(0.68f, 0.58f, 0.44f))
        addBox(0f, 0f, -41f, 46f, 4.5f, 2f, SurfaceMaterial.CONCRETE, floatArrayOf(0.68f, 0.58f, 0.44f))

        val resupply = listOf(
            ResupplyStation("mc_ammo_1", Vec3(-6f, 0f, 12f)),
            ResupplyStation("mc_ammo_2", Vec3(7f, 0f, -14f))
        )

        return CityMap(
            cityId = CityId.MIRAGE_CITY,
            minBoundX = -18.5f, maxBoundX = 18.5f, minBoundZ = -38.5f, maxBoundZ = 38.5f,
            groundMaterial = SurfaceMaterial.SAND,
            groundColorRgb = floatArrayOf(0.73f, 0.61f, 0.43f),
            blocks = blocks,
            colliders = blocks.filter { it.solid },
            coverPoints = covers,
            resupplyStations = resupply,
            interactiveObjects = interactives,
            vehicleSpawnPos = Vec3(-1.0f, 0f, 12.5f)
        )
    }

    private fun buildHarborCity(): CityMap {
        val blocks = mutableListOf<WorldBlock>()
        val covers = mutableListOf<CoverPoint>()
        val interactives = mutableListOf<InteractiveObject>()
        var idx = 0

        fun addBox(
            cx: Float, baseY: Float, cz: Float, sx: Float, sy: Float, sz: Float,
            mat: SurfaceMaterial, rgb: FloatArray, rough: Float = 0.65f, metal: Float = 0.35f,
            pattern: Int = 0, solid: Boolean = true, isCover: Boolean = false,
            tier: Int = 1, shape: BlockShape = BlockShape.BOX, customId: String? = null,
            vaultable: Boolean = isCover && sy in 0.85f..1.55f
        ): String {
            val id = customId ?: "hc_${idx++}"
            val aabb = AABB(cx - sx * 0.5f, baseY, cz - sz * 0.5f, cx + sx * 0.5f, baseY + sy, cz + sz * 0.5f)
            blocks.add(WorldBlock(id, aabb, mat, rgb, rough, metal, 0f, pattern, shape, solid, isCover, tier, vaultable))
            if (isCover) {
                covers.add(CoverPoint(Vec3(cx, baseY, cz + sz * 0.5f + 0.85f), Vec3(0f, 0f, -1f), sy))
                covers.add(CoverPoint(Vec3(cx, baseY, cz - sz * 0.5f - 0.85f), Vec3(0f, 0f, 1f), sy))
                covers.add(CoverPoint(Vec3(cx - sx * 0.5f - 0.85f, baseY, cz), Vec3(1f, 0f, 0f), sy))
                covers.add(CoverPoint(Vec3(cx + sx * 0.5f + 0.85f, baseY, cz), Vec3(-1f, 0f, 0f), sy))
            }
            return id
        }

        addBox(30f, -0.35f, 0f, 20f, 0.25f, 92f, SurfaceMaterial.WATER, floatArrayOf(0.08f, 0.22f, 0.34f), rough = 0.18f, metal = 0.45f, solid = false)
        addBox(0f, -0.05f, 0f, 38f, 0.08f, 84f, SurfaceMaterial.CONCRETE, floatArrayOf(0.32f, 0.34f, 0.36f), pattern = 2, solid = false)
        addBox(-22f, 0f, 14f, 10f, 8.5f, 18f, SurfaceMaterial.METAL, floatArrayOf(0.28f, 0.33f, 0.38f), pattern = 4)
        addBox(-22f, 0f, -14f, 10f, 9.0f, 20f, SurfaceMaterial.METAL, floatArrayOf(0.32f, 0.35f, 0.38f), pattern = 4)

        val containers = listOf(
            Triple(-8f, 18f, floatArrayOf(0.68f, 0.24f, 0.18f)),
            Triple(9f, 14f, floatArrayOf(0.18f, 0.38f, 0.56f)),
            Triple(-10f, 2f, floatArrayOf(0.22f, 0.48f, 0.32f)),
            Triple(8f, -2f, floatArrayOf(0.76f, 0.48f, 0.16f)),
            Triple(-6f, -14f, floatArrayOf(0.18f, 0.38f, 0.56f)),
            Triple(10f, -16f, floatArrayOf(0.65f, 0.22f, 0.18f)),
            Triple(-11f, -26f, floatArrayOf(0.68f, 0.45f, 0.18f)),
            Triple(9f, -28f, floatArrayOf(0.24f, 0.42f, 0.34f))
        )
        containers.forEachIndexed { i, (cx, cz, rgb) ->
            addBox(cx, 0f, cz, 3.0f, 2.8f, 7.2f, SurfaceMaterial.METAL, rgb, rough = 0.52f, metal = 0.58f, pattern = 4, isCover = true, vaultable = false)
            if (i % 2 == 0) {
                addBox(cx, 2.8f, cz, 3.0f, 2.7f, 7.2f, SurfaceMaterial.METAL, floatArrayOf(rgb[0] * 0.82f, rgb[1] * 0.82f, rgb[2] * 0.82f), rough = 0.58f, metal = 0.52f, pattern = 4, tier = 2)
            }
        }

        listOf(10f, -16f).forEach { cz ->
            addBox(18f, 0f, cz - 4f, 1.2f, 18f, 1.2f, SurfaceMaterial.METAL, floatArrayOf(0.72f, 0.32f, 0.16f), metal = 0.7f, tier = 2)
            addBox(18f, 0f, cz + 4f, 1.2f, 18f, 1.2f, SurfaceMaterial.METAL, floatArrayOf(0.72f, 0.32f, 0.16f), metal = 0.7f, tier = 2)
            addBox(15f, 17f, cz, 12f, 1.3f, 9.5f, SurfaceMaterial.METAL, floatArrayOf(0.72f, 0.32f, 0.16f), metal = 0.7f, solid = false, tier = 2)
        }

        listOf(Pair(0f, 22f), Pair(-1f, 8f), Pair(1f, -8f), Pair(0f, -22f)).forEach { (x, z) ->
            addBox(x, 0f, z, 3.8f, 1.35f, 1.6f, SurfaceMaterial.WOOD, floatArrayOf(0.50f, 0.39f, 0.25f), isCover = true, vaultable = true)
        }

        val doorId = addBox(-14.2f, 0f, 10f, 2.6f, 2.8f, 0.4f, SurfaceMaterial.METAL, floatArrayOf(0.28f, 0.48f, 0.62f), metal = 0.7f, customId = "hc_door_block_1")
        interactives.addAll(
            listOf(
                InteractiveObject("hc_door_1", InteractiveKind.DOOR, Vec3(-14.2f, 0f, 10f), 3.0f, LocalizedText("بوابة رصيف الشحن", "Cargo Dock Door"), linkedBlockId = doorId),
                InteractiveObject("hc_ammo_crate_1", InteractiveKind.AMMO_CONTAINER, Vec3(-4.5f, 0f, 24f), 2.8f, LocalizedText("حاوية ذخيرة بحرية", "Naval Ammo Crate")),
                InteractiveObject("hc_barrel_1", InteractiveKind.DESTRUCTIBLE_BARREL, Vec3(4.5f, 0f, 2.5f), 2.8f, LocalizedText("برميل وقود سفن", "Marine Fuel Barrel")),
                InteractiveObject("hc_barrel_2", InteractiveKind.DESTRUCTIBLE_BARREL, Vec3(-3.5f, 0f, -12.5f), 2.8f, LocalizedText("برميل وقود سفن", "Marine Fuel Barrel")),
                InteractiveObject("hc_terminal_1", InteractiveKind.OBJECTIVE_DEVICE, Vec3(2f, 0f, -26f), 3.0f, LocalizedText("وحدة التحكم بالرافعة", "Crane Control Terminal"))
            )
        )

        addBox(0f, 0f, 41f, 46f, 4f, 2f, SurfaceMaterial.CONCRETE, floatArrayOf(0.32f, 0.36f, 0.40f))
        addBox(0f, 0f, -41f, 46f, 4f, 2f, SurfaceMaterial.CONCRETE, floatArrayOf(0.32f, 0.36f, 0.40f))

        val resupply = listOf(
            ResupplyStation("hc_ammo_1", Vec3(4f, 0f, 16f)),
            ResupplyStation("hc_ammo_2", Vec3(-4f, 0f, -18f))
        )

        return CityMap(
            cityId = CityId.HARBOR_CITY,
            minBoundX = -17.5f, maxBoundX = 17.0f, minBoundZ = -38.5f, maxBoundZ = 38.5f,
            groundMaterial = SurfaceMaterial.CONCRETE,
            groundColorRgb = floatArrayOf(0.30f, 0.33f, 0.36f),
            blocks = blocks,
            colliders = blocks.filter { it.solid },
            coverPoints = covers,
            resupplyStations = resupply,
            interactiveObjects = interactives,
            vehicleSpawnPos = Vec3(3.2f, 0f, 13.0f)
        )
    }

    private fun buildMountainCity(): CityMap {
        val blocks = mutableListOf<WorldBlock>()
        val covers = mutableListOf<CoverPoint>()
        val interactives = mutableListOf<InteractiveObject>()
        var idx = 0

        fun addBox(
            cx: Float, baseY: Float, cz: Float, sx: Float, sy: Float, sz: Float,
            mat: SurfaceMaterial, rgb: FloatArray, rough: Float = 0.88f, metal: Float = 0.06f,
            pattern: Int = 5, solid: Boolean = true, isCover: Boolean = false,
            tier: Int = 1, shape: BlockShape = BlockShape.BOX, customId: String? = null,
            vaultable: Boolean = isCover && sy in 0.85f..1.55f
        ): String {
            val id = customId ?: "mt_${idx++}"
            val aabb = AABB(cx - sx * 0.5f, baseY, cz - sz * 0.5f, cx + sx * 0.5f, baseY + sy, cz + sz * 0.5f)
            blocks.add(WorldBlock(id, aabb, mat, rgb, rough, metal, 0f, pattern, shape, solid, isCover, tier, vaultable))
            if (isCover) {
                covers.add(CoverPoint(Vec3(cx, baseY, cz + sz * 0.5f + 0.85f), Vec3(0f, 0f, -1f), sy))
                covers.add(CoverPoint(Vec3(cx, baseY, cz - sz * 0.5f - 0.85f), Vec3(0f, 0f, 1f), sy))
            }
            return id
        }

        addBox(0f, -0.05f, 0f, 16f, 0.08f, 84f, SurfaceMaterial.ASPHALT, floatArrayOf(0.24f, 0.25f, 0.26f), pattern = 2, solid = false)

        listOf(-24f to 20f, -25f to 0f, -24f to -20f, 24f to 22f, 25f to 2f, 24f to -18f).forEachIndexed { i, (rx, rz) ->
            addBox(rx, 0f, rz, 12f, 14f + (i % 3) * 4f, 18f, SurfaceMaterial.ROCK, floatArrayOf(0.38f, 0.36f, 0.35f), pattern = 5)
        }

        addBox(-11f, 0f, 8f, 5f, 6.5f, 7f, SurfaceMaterial.ROCK, floatArrayOf(0.36f, 0.35f, 0.34f))
        addBox(11f, 0f, 8f, 5f, 6.5f, 7f, SurfaceMaterial.ROCK, floatArrayOf(0.36f, 0.35f, 0.34f))
        addBox(0f, 6.2f, 8f, 26f, 2.8f, 7.2f, SurfaceMaterial.ROCK, floatArrayOf(0.34f, 0.33f, 0.32f), solid = false)

        addBox(-13f, 0f, -10f, 7f, 6.5f, 8f, SurfaceMaterial.CONCRETE, floatArrayOf(0.45f, 0.44f, 0.42f), pattern = 1)
        addBox(13f, 0f, -12f, 7f, 7.0f, 8f, SurfaceMaterial.CONCRETE, floatArrayOf(0.42f, 0.43f, 0.45f), pattern = 1)
        addBox(-14f, 0f, -31f, 9f, 10f, 9f, SurfaceMaterial.CONCRETE, floatArrayOf(0.35f, 0.37f, 0.40f), pattern = 1)
        addBox(14f, 0f, -31f, 9f, 10f, 9f, SurfaceMaterial.CONCRETE, floatArrayOf(0.35f, 0.37f, 0.40f), pattern = 1)
        addBox(14f, 10f, -31f, 1.0f, 5f, 1.0f, SurfaceMaterial.METAL, floatArrayOf(0.55f, 0.58f, 0.62f), shape = BlockShape.CYLINDER, solid = false, tier = 2)

        val mountainCover = listOf(
            Triple(-5.5f, 22f, 4.0f), Triple(5.5f, 18f, 3.8f), Triple(-3.5f, 2f, 4.2f),
            Triple(6.0f, -3f, 3.8f), Triple(-6.5f, -15f, 4.2f), Triple(5.5f, -18f, 4.2f),
            Triple(0f, -24f, 5.0f)
        )
        mountainCover.forEachIndexed { i, (x, z, w) ->
            val isRock = i % 2 == 0
            addBox(
                x, 0f, z, w, 1.45f, 1.5f,
                if (isRock) SurfaceMaterial.ROCK else SurfaceMaterial.CONCRETE,
                if (isRock) floatArrayOf(0.42f, 0.40f, 0.38f) else floatArrayOf(0.48f, 0.49f, 0.51f),
                isCover = true,
                vaultable = true
            )
        }

        val gateId = addBox(0f, 0f, -11f, 4.2f, 3.0f, 0.5f, SurfaceMaterial.METAL, floatArrayOf(0.56f, 0.36f, 0.22f), metal = 0.75f, customId = "mt_gate_block_1")
        interactives.addAll(
            listOf(
                InteractiveObject("mt_gate_1", InteractiveKind.GATE, Vec3(0f, 0f, -11f), 3.2f, LocalizedText("بوابة القلعة الجبلية", "Citadel Fortress Gate"), isLocked = true, linkedBlockId = gateId),
                InteractiveObject("mt_switch_1", InteractiveKind.SWITCH, Vec3(-6.8f, 0f, -8.2f), 2.8f, LocalizedText("مفتاح فتح بوابة القلعة", "Citadel Gate Override"), linkedTargetId = "mt_gate_1"),
                InteractiveObject("mt_supply_1", InteractiveKind.SUPPLY_BOX, Vec3(-7.5f, 0f, 20f), 2.8f, LocalizedText("صندوق إمداد جبلي", "Alpine Supply Box")),
                InteractiveObject("mt_barrel_1", InteractiveKind.DESTRUCTIBLE_BARREL, Vec3(3.2f, 0f, 1.5f), 2.8f, LocalizedText("برميل وقود المولد", "Generator Fuel Barrel")),
                InteractiveObject("mt_barrel_2", InteractiveKind.DESTRUCTIBLE_BARREL, Vec3(-4.2f, 0f, -20.5f), 2.8f, LocalizedText("برميل وقود المولد", "Generator Fuel Barrel")),
                InteractiveObject("mt_terminal_1", InteractiveKind.OBJECTIVE_DEVICE, Vec3(8f, 0f, -26f), 3.0f, LocalizedText("لوحة الرادار الجبلي", "Summit Radar Console"))
            )
        )

        addBox(0f, 0f, 41f, 46f, 5f, 2f, SurfaceMaterial.ROCK, floatArrayOf(0.36f, 0.35f, 0.34f))
        addBox(0f, 0f, -41f, 46f, 5f, 2f, SurfaceMaterial.CONCRETE, floatArrayOf(0.32f, 0.34f, 0.37f))

        val resupply = listOf(
            ResupplyStation("mt_ammo_1", Vec3(6f, 0f, 12f)),
            ResupplyStation("mt_ammo_2", Vec3(-6f, 0f, -20f))
        )

        return CityMap(
            cityId = CityId.MOUNTAIN_CITY,
            minBoundX = -17.5f, maxBoundX = 17.5f, minBoundZ = -38.5f, maxBoundZ = 38.5f,
            groundMaterial = SurfaceMaterial.ROCK,
            groundColorRgb = floatArrayOf(0.34f, 0.33f, 0.32f),
            blocks = blocks,
            colliders = blocks.filter { it.solid },
            coverPoints = covers,
            resupplyStations = resupply,
            interactiveObjects = interactives,
            vehicleSpawnPos = Vec3(-2.5f, 0f, 13.5f)
        )
    }
}
