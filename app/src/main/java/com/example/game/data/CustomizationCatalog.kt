package com.example.game.data

data class CharacterGearItem(
    val id: String,
    val slot: CharacterSlot,
    val name: LocalizedText,
    val xpCost: Int,
    val colorRgb: FloatArray,
    val secondaryRgb: FloatArray,
    val bulkScale: Float = 1.0f
)

data class CharacterPreset(
    val id: String,
    val name: LocalizedText,
    val gear: Map<CharacterSlot, String>
)

data class CharacterAppearance(
    val outfitRgb: FloatArray,
    val helmetRgb: FloatArray,
    val visorRgb: FloatArray,
    val vestRgb: FloatArray,
    val glovesRgb: FloatArray,
    val bootsRgb: FloatArray,
    val backpackRgb: FloatArray,
    val vestBulk: Float,
    val hasBackpack: Boolean
)

object CustomizationCatalog {
    val items: List<CharacterGearItem> = listOf(
        // Outfits
        CharacterGearItem("outfit_ranger", CharacterSlot.OUTFIT, LocalizedText("زي الحرس الحضري", "Urban Ranger BDU"), 0, floatArrayOf(0.27f, 0.33f, 0.25f), floatArrayOf(0.18f, 0.21f, 0.18f)),
        CharacterGearItem("outfit_desert", CharacterSlot.OUTFIT, LocalizedText("زي رمال السراب", "Mirage Desert Camo"), 150, floatArrayOf(0.66f, 0.56f, 0.40f), floatArrayOf(0.45f, 0.38f, 0.27f)),
        CharacterGearItem("outfit_night", CharacterSlot.OUTFIT, LocalizedText("زي العمليات الليلية", "Night Ops Obsidian"), 280, floatArrayOf(0.14f, 0.16f, 0.20f), floatArrayOf(0.22f, 0.25f, 0.30f)),
        CharacterGearItem("outfit_alpine", CharacterSlot.OUTFIT, LocalizedText("زي المرتفعات الجبلية", "Mountain Ridge Gear"), 400, floatArrayOf(0.45f, 0.48f, 0.50f), floatArrayOf(0.26f, 0.29f, 0.31f)),
        // Helmets
        CharacterGearItem("helmet_tac", CharacterSlot.HELMET, LocalizedText("خوذة تكتيكية قياسية", "FAST Tactical Helmet"), 0, floatArrayOf(0.23f, 0.27f, 0.22f), floatArrayOf(0.10f, 0.12f, 0.15f)),
        CharacterGearItem("helmet_desert", CharacterSlot.HELMET, LocalizedText("خوذة صحراوية مع نظارات", "Desert Goggle Helmet"), 120, floatArrayOf(0.62f, 0.53f, 0.38f), floatArrayOf(0.85f, 0.62f, 0.20f)),
        CharacterGearItem("helmet_nvg", CharacterSlot.HELMET, LocalizedText("خوذة الرؤية الليلية", "NVG Spec-Ops Helmet"), 320, floatArrayOf(0.15f, 0.17f, 0.19f), floatArrayOf(0.20f, 0.86f, 0.55f)),
        // Vests
        CharacterGearItem("vest_light", CharacterSlot.VEST, LocalizedText("درع خفيف سريع الحركة", "Recon Plate Carrier"), 0, floatArrayOf(0.22f, 0.24f, 0.20f), floatArrayOf(0.32f, 0.34f, 0.28f), bulkScale = 1.0f),
        CharacterGearItem("vest_heavy", CharacterSlot.VEST, LocalizedText("درع اقتحام مدرّع", "Heavy Assault Rig"), 260, floatArrayOf(0.17f, 0.19f, 0.22f), floatArrayOf(0.75f, 0.56f, 0.22f), bulkScale = 1.15f),
        CharacterGearItem("vest_sand", CharacterSlot.VEST, LocalizedText("درع الصحراء التكتيكي", "Sandstorm Carrier"), 180, floatArrayOf(0.58f, 0.49f, 0.34f), floatArrayOf(0.36f, 0.30f, 0.22f), bulkScale = 1.06f),
        // Gloves
        CharacterGearItem("gloves_black", CharacterSlot.GLOVES, LocalizedText("قفازات قتال سوداء", "Tactical Black Gloves"), 0, floatArrayOf(0.14f, 0.15f, 0.16f), floatArrayOf(0.25f, 0.25f, 0.27f)),
        CharacterGearItem("gloves_tan", CharacterSlot.GLOVES, LocalizedText("قفازات صحراوية مقوّاة", "Reinforced Tan Gloves"), 90, floatArrayOf(0.56f, 0.46f, 0.31f), floatArrayOf(0.30f, 0.24f, 0.18f)),
        // Boots
        CharacterGearItem("boots_combat", CharacterSlot.BOOTS, LocalizedText("أحذية قتال ميدانية", "All-Terrain Boots"), 0, floatArrayOf(0.18f, 0.16f, 0.14f), floatArrayOf(0.12f, 0.12f, 0.12f)),
        CharacterGearItem("boots_desert", CharacterSlot.BOOTS, LocalizedText("أحذية صحراوية عالية", "Desert Storm Boots"), 90, floatArrayOf(0.54f, 0.45f, 0.32f), floatArrayOf(0.28f, 0.24f, 0.18f)),
        // Backpacks
        CharacterGearItem("pack_recon", CharacterSlot.BACKPACK, LocalizedText("حقيبة استطلاع مدمجة", "Compact Recon Pack"), 0, floatArrayOf(0.24f, 0.27f, 0.21f), floatArrayOf(0.15f, 0.17f, 0.14f), bulkScale = 1.0f),
        CharacterGearItem("pack_comm", CharacterSlot.BACKPACK, LocalizedText("حقيبة اتصالات ميدانية", "Tactical Radio Pack"), 220, floatArrayOf(0.20f, 0.23f, 0.27f), floatArrayOf(0.78f, 0.58f, 0.22f), bulkScale = 1.15f),
        CharacterGearItem("pack_none", CharacterSlot.BACKPACK, LocalizedText("بدون حقيبة ظهر", "Minimalist (No Pack)"), 0, floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f), bulkScale = 0f)
    )

    val presets: List<CharacterPreset> = listOf(
        CharacterPreset(
            id = "preset_river",
            name = LocalizedText("طقم مدينة النهر", "River Urban Spec"),
            gear = defaultGear()
        ),
        CharacterPreset(
            id = "preset_mirage",
            name = LocalizedText("طقم صحراء السراب", "Mirage Desert Squad"),
            gear = mapOf(
                CharacterSlot.OUTFIT to "outfit_desert",
                CharacterSlot.HELMET to "helmet_desert",
                CharacterSlot.VEST to "vest_sand",
                CharacterSlot.GLOVES to "gloves_tan",
                CharacterSlot.BOOTS to "boots_desert",
                CharacterSlot.BACKPACK to "pack_recon"
            )
        ),
        CharacterPreset(
            id = "preset_night",
            name = LocalizedText("طقم الاقتحام الليلي", "Night Strike Force"),
            gear = mapOf(
                CharacterSlot.OUTFIT to "outfit_night",
                CharacterSlot.HELMET to "helmet_nvg",
                CharacterSlot.VEST to "vest_heavy",
                CharacterSlot.GLOVES to "gloves_black",
                CharacterSlot.BOOTS to "boots_combat",
                CharacterSlot.BACKPACK to "pack_comm"
            )
        )
    )

    fun defaultGear(): Map<CharacterSlot, String> = mapOf(
        CharacterSlot.OUTFIT to "outfit_ranger",
        CharacterSlot.HELMET to "helmet_tac",
        CharacterSlot.VEST to "vest_light",
        CharacterSlot.GLOVES to "gloves_black",
        CharacterSlot.BOOTS to "boots_combat",
        CharacterSlot.BACKPACK to "pack_recon"
    )

    fun itemsForSlot(slot: CharacterSlot): List<CharacterGearItem> = items.filter { it.slot == slot }

    fun itemById(id: String): CharacterGearItem? = items.firstOrNull { it.id == id }

    fun resolveAppearance(gear: Map<CharacterSlot, String>): CharacterAppearance {
        val outfit = itemById(gear[CharacterSlot.OUTFIT] ?: "outfit_ranger") ?: items.first()
        val helmet = itemById(gear[CharacterSlot.HELMET] ?: "helmet_tac") ?: items.first { it.slot == CharacterSlot.HELMET }
        val vest = itemById(gear[CharacterSlot.VEST] ?: "vest_light") ?: items.first { it.slot == CharacterSlot.VEST }
        val gloves = itemById(gear[CharacterSlot.GLOVES] ?: "gloves_black") ?: items.first { it.slot == CharacterSlot.GLOVES }
        val boots = itemById(gear[CharacterSlot.BOOTS] ?: "boots_combat") ?: items.first { it.slot == CharacterSlot.BOOTS }
        val pack = itemById(gear[CharacterSlot.BACKPACK] ?: "pack_recon") ?: items.first { it.slot == CharacterSlot.BACKPACK }

        return CharacterAppearance(
            outfitRgb = outfit.colorRgb,
            helmetRgb = helmet.colorRgb,
            visorRgb = helmet.secondaryRgb,
            vestRgb = vest.colorRgb,
            glovesRgb = gloves.colorRgb,
            bootsRgb = boots.colorRgb,
            backpackRgb = pack.colorRgb,
            vestBulk = vest.bulkScale,
            hasBackpack = pack.id != "pack_none"
        )
    }
}
