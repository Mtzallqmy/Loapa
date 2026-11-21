package com.example.game.data

data class WeaponData(
    val id: WeaponId,
    val name: LocalizedText,
    val category: LocalizedText,
    val description: LocalizedText,
    val damage: Float,
    val fireRate: Float,          // rounds per second
    val magCapacity: Int,
    val reserveMax: Int,
    val reloadDuration: Float,    // seconds
    val baseSpread: Float,        // radians hipfire
    val adsSpread: Float,         // radians aiming
    val recoilKick: Float,        // vertical pitch kick radians
    val effectiveRange: Float,    // meters before falloff
    val pelletsPerShot: Int = 1,
    val isAutomatic: Boolean = true,
    val isSidearm: Boolean = false,
    val unlockMission: Int = 1,
    val xpCost: Int = 0,
    val bodyLength: Float = 0.78f,
    val barrelLength: Float = 0.34f,
    val stockLength: Float = 0.24f,
    val baseColorRgb: FloatArray = floatArrayOf(0.20f, 0.22f, 0.24f),
    val accentColorRgb: FloatArray = floatArrayOf(0.38f, 0.36f, 0.32f)
)

data class AttachmentItem(
    val id: String,
    val slot: AttachmentSlot,
    val name: LocalizedText,
    val xpCost: Int,
    val accuracyBonus: Float = 0f,       // multiplier reduction on spread (0.15 = 15% tighter)
    val recoilControlBonus: Float = 0f,  // multiplier reduction on recoil
    val magBonus: Int = 0,
    val reloadSpeedBonus: Float = 0f,    // multiplier reduction on reload duration
    val rangeBonus: Float = 0f,          // meters added
    val handlingBonus: Float = 0f,       // ADS speed boost
    val skinColorRgb: FloatArray? = null,
    val skinAccentRgb: FloatArray? = null
)

data class EffectiveWeaponStats(
    val weapon: WeaponData,
    val damage: Float,
    val fireRate: Float,
    val magCapacity: Int,
    val reserveMax: Int,
    val reloadDuration: Float,
    val hipSpread: Float,
    val adsSpread: Float,
    val recoilKick: Float,
    val effectiveRange: Float,
    val adsZoomFov: Float,
    val bodyRgb: FloatArray,
    val accentRgb: FloatArray,
    val hasOptic: Boolean,
    val hasGrip: Boolean,
    val hasExtendedMag: Boolean,
    val hasLongBarrel: Boolean,
    val hasStabilizer: Boolean
)

object WeaponCatalog {
    val weapons: List<WeaponData> = listOf(
        WeaponData(
            id = WeaponId.VX74_ASSAULT,
            name = LocalizedText("بندقية هجومية VX-74", "VX-74 Assault Rifle"),
            category = LocalizedText("بندقية هجومية", "Assault Rifle"),
            description = LocalizedText(
                "بندقية تكتيكية متوازنة للمسافات المتوسطة والقتال الحضري.",
                "Balanced tactical carbine built for urban and mid-range engagements."
            ),
            damage = 28f, fireRate = 8.5f, magCapacity = 30, reserveMax = 180,
            reloadDuration = 1.95f, baseSpread = 0.032f, adsSpread = 0.008f,
            recoilKick = 0.017f, effectiveRange = 55f, isAutomatic = true,
            unlockMission = 1, xpCost = 0,
            bodyLength = 0.72f, barrelLength = 0.36f, stockLength = 0.25f,
            baseColorRgb = floatArrayOf(0.20f, 0.22f, 0.24f), accentColorRgb = floatArrayOf(0.42f, 0.37f, 0.29f)
        ),
        WeaponData(
            id = WeaponId.KSR9_SMG,
            name = LocalizedText("رشاش خفيف KSR-9", "KSR-9 Compact SMG"),
            category = LocalizedText("رشاش مدمج", "Compact SMG"),
            description = LocalizedText(
                "معدل إطلاق نار مرتفع وحركة سريعة في الأزقة الضيقة.",
                "High fire-rate compact SMG ideal for tight alleys and flanking."
            ),
            damage = 20f, fireRate = 12.2f, magCapacity = 36, reserveMax = 216,
            reloadDuration = 1.55f, baseSpread = 0.044f, adsSpread = 0.016f,
            recoilKick = 0.013f, effectiveRange = 34f, isAutomatic = true,
            unlockMission = 1, xpCost = 0,
            bodyLength = 0.48f, barrelLength = 0.18f, stockLength = 0.18f,
            baseColorRgb = floatArrayOf(0.16f, 0.18f, 0.21f), accentColorRgb = floatArrayOf(0.32f, 0.38f, 0.42f)
        ),
        WeaponData(
            id = WeaponId.SR308_PRECISION,
            name = LocalizedText("بندقية قنص SR-308", "SR-308 Precision Rifle"),
            category = LocalizedText("بندقية قنص", "Precision Rifle"),
            description = LocalizedText(
                "دقة بعيدة المدى وضرر مرتفع لاختراق الدروع من المواقع المرتفعة.",
                "Long-range precision bolt platform capable of high armor penetration."
            ),
            damage = 96f, fireRate = 1.15f, magCapacity = 6, reserveMax = 42,
            reloadDuration = 2.45f, baseSpread = 0.055f, adsSpread = 0.0015f,
            recoilKick = 0.048f, effectiveRange = 130f, isAutomatic = false,
            unlockMission = 1, xpCost = 0,
            bodyLength = 0.88f, barrelLength = 0.56f, stockLength = 0.30f,
            baseColorRgb = floatArrayOf(0.24f, 0.26f, 0.22f), accentColorRgb = floatArrayOf(0.50f, 0.44f, 0.32f)
        ),
        WeaponData(
            id = WeaponId.M9S_SIDEARM,
            name = LocalizedText("مسدس تكتيكي M9-S", "M9-S Tactical Sidearm"),
            category = LocalizedText("سلاح ثانوي", "Sidearm"),
            description = LocalizedText(
                "سلاح احتياطي موثوق بسرعة سحب وإعادة تلقيم عالية.",
                "Reliable backup sidearm with rapid draw speed and quick reload."
            ),
            damage = 25f, fireRate = 5.0f, magCapacity = 15, reserveMax = 90,
            reloadDuration = 1.25f, baseSpread = 0.028f, adsSpread = 0.009f,
            recoilKick = 0.015f, effectiveRange = 30f, isAutomatic = false, isSidearm = true,
            unlockMission = 1, xpCost = 0,
            bodyLength = 0.28f, barrelLength = 0.12f, stockLength = 0.0f,
            baseColorRgb = floatArrayOf(0.18f, 0.19f, 0.21f), accentColorRgb = floatArrayOf(0.55f, 0.55f, 0.58f)
        ),
        WeaponData(
            id = WeaponId.BR12_SHOTGUN,
            name = LocalizedText("بندقية اقتحام BR-12", "BR-12 Tactical Shotgun"),
            category = LocalizedText("بندقية اقتحام", "Shotgun"),
            description = LocalizedText(
                "قوة إيقاف هائلة في المسافات القريبة والمستودعات المغلقة.",
                "Devastating close-quarters scatter blast for warehouses and corridors."
            ),
            damage = 19f, fireRate = 1.65f, magCapacity = 8, reserveMax = 48,
            reloadDuration = 2.20f, baseSpread = 0.085f, adsSpread = 0.055f,
            recoilKick = 0.044f, effectiveRange = 20f, pelletsPerShot = 6, isAutomatic = false,
            unlockMission = 2, xpCost = 250,
            bodyLength = 0.70f, barrelLength = 0.34f, stockLength = 0.24f,
            baseColorRgb = floatArrayOf(0.22f, 0.20f, 0.19f), accentColorRgb = floatArrayOf(0.45f, 0.28f, 0.18f)
        ),
        WeaponData(
            id = WeaponId.DMR14_MARKSMAN,
            name = LocalizedText("بندقية رماية DMR-14", "DMR-14 Marksman Rifle"),
            category = LocalizedText("بندقية رماية", "Marksman Rifle"),
            description = LocalizedText(
                "نصف آلية عالية الدقة للتعامل مع الأهداف المتوسطة والبعيدة.",
                "Semi-auto designated marksman platform bridging assault and sniper roles."
            ),
            damage = 54f, fireRate = 3.4f, magCapacity = 16, reserveMax = 96,
            reloadDuration = 2.15f, baseSpread = 0.036f, adsSpread = 0.003f,
            recoilKick = 0.028f, effectiveRange = 95f, isAutomatic = false,
            unlockMission = 4, xpCost = 450,
            bodyLength = 0.84f, barrelLength = 0.46f, stockLength = 0.27f,
            baseColorRgb = floatArrayOf(0.32f, 0.29f, 0.24f), accentColorRgb = floatArrayOf(0.18f, 0.19f, 0.20f)
        ),
        WeaponData(
            id = WeaponId.MG48_LMG,
            name = LocalizedText("رشاش ثقيل MG-48", "MG-48 Light Machine Gun"),
            category = LocalizedText("رشاش دعم ثقيل", "Light Machine Gun"),
            description = LocalizedText(
                "مخزن ضخم لنيران قمع مستمرة ضد موجات التعزيزات المدرعة.",
                "High-capacity squad support weapon built to suppress reinforcement waves."
            ),
            damage = 31f, fireRate = 9.4f, magCapacity = 75, reserveMax = 300,
            reloadDuration = 3.15f, baseSpread = 0.048f, adsSpread = 0.015f,
            recoilKick = 0.021f, effectiveRange = 68f, isAutomatic = true,
            unlockMission = 6, xpCost = 700,
            bodyLength = 0.86f, barrelLength = 0.44f, stockLength = 0.24f,
            baseColorRgb = floatArrayOf(0.17f, 0.20f, 0.18f), accentColorRgb = floatArrayOf(0.56f, 0.46f, 0.22f)
        ),
        WeaponData(
            id = WeaponId.RAIL_X1_HEAVY,
            name = LocalizedText("مدفع النبض التكتيكي X-1", "X-1 Tactical Pulse Carbine"),
            category = LocalizedText("سلاح خاص ثقيل", "Heavy Special Weapon"),
            description = LocalizedText(
                "نموذج تكتيكي متطور يطلق نبضات طاقة حركية عالية الاختراق للدروع الثقيلة.",
                "Experimental kinetic pulse platform engineered to breach heavy boss armor."
            ),
            damage = 84f, fireRate = 2.6f, magCapacity = 14, reserveMax = 84,
            reloadDuration = 2.35f, baseSpread = 0.024f, adsSpread = 0.002f,
            recoilKick = 0.032f, effectiveRange = 110f, isAutomatic = true,
            unlockMission = 9, xpCost = 1100,
            bodyLength = 0.88f, barrelLength = 0.48f, stockLength = 0.25f,
            baseColorRgb = floatArrayOf(0.15f, 0.21f, 0.28f), accentColorRgb = floatArrayOf(0.28f, 0.78f, 0.94f)
        )
    )

    val attachments: List<AttachmentItem> = listOf(
        // Optics
        AttachmentItem("optic_iron", AttachmentSlot.OPTIC, LocalizedText("تصويب حديدي قياسي", "Standard Iron Sights"), 0),
        AttachmentItem("optic_holo", AttachmentSlot.OPTIC, LocalizedText("منظار هولوجرافي 1.5x", "Holo Sight 1.5x"), 120, accuracyBonus = 0.18f, handlingBonus = 0.10f),
        AttachmentItem("optic_acog", AttachmentSlot.OPTIC, LocalizedText("منظار تكتيكي 3x", "Tactical Optic 3x"), 260, accuracyBonus = 0.30f, rangeBonus = 14f),
        // Grips
        AttachmentItem("grip_std", AttachmentSlot.GRIP, LocalizedText("مقبض قياسي", "Standard Handguard"), 0),
        AttachmentItem("grip_angled", AttachmentSlot.GRIP, LocalizedText("مقبض مائل سريع", "Angled Speed Grip"), 110, recoilControlBonus = 0.14f, handlingBonus = 0.18f),
        AttachmentItem("grip_vertical", AttachmentSlot.GRIP, LocalizedText("مقبض عمودي ثابت", "Vertical Control Grip"), 220, recoilControlBonus = 0.28f, accuracyBonus = 0.10f),
        // Magazines
        AttachmentItem("mag_std", AttachmentSlot.MAGAZINE, LocalizedText("مخزن قياسي", "Standard Magazine"), 0),
        AttachmentItem("mag_fast", AttachmentSlot.MAGAZINE, LocalizedText("مخزن تلقيم سريع", "Quick-Swap Mag"), 140, reloadSpeedBonus = 0.22f),
        AttachmentItem("mag_ext", AttachmentSlot.MAGAZINE, LocalizedText("مخزن موسّع عالي السعة", "Extended High-Cap Mag"), 240, magBonus = 10, reloadSpeedBonus = -0.05f),
        // Barrels
        AttachmentItem("barrel_std", AttachmentSlot.BARREL, LocalizedText("سبطانة قياسية", "Standard Barrel"), 0),
        AttachmentItem("barrel_tac", AttachmentSlot.BARREL, LocalizedText("سبطانة تكتيكية طويلة", "Tactical Extended Barrel"), 190, rangeBonus = 16f, accuracyBonus = 0.12f),
        AttachmentItem("barrel_shroud", AttachmentSlot.BARREL, LocalizedText("غطاء سبطانة مدمج", "Shrouded Combat Barrel"), 260, rangeBonus = 10f, recoilControlBonus = 0.16f),
        // Stabilizers
        AttachmentItem("stab_none", AttachmentSlot.STABILIZER, LocalizedText("بدون مثبّت إضافي", "No Stabilizer"), 0),
        AttachmentItem("stab_comp", AttachmentSlot.STABILIZER, LocalizedText("مثبّت ارتداد متوازن", "Balanced Stabilizer"), 180, recoilControlBonus = 0.22f),
        AttachmentItem("stab_pro", AttachmentSlot.STABILIZER, LocalizedText("مثبّت تكتيكي احترافي", "Pro Tactical Counterweight"), 320, recoilControlBonus = 0.32f, accuracyBonus = 0.12f),
        // Skins
        AttachmentItem("skin_factory", AttachmentSlot.SKIN, LocalizedText("طلاء المصنع الأسود", "Factory Matte Black"), 0),
        AttachmentItem(
            "skin_desert", AttachmentSlot.SKIN, LocalizedText("تمويه رمال السراب", "Mirage Desert Tan"), 150,
            skinColorRgb = floatArrayOf(0.68f, 0.57f, 0.41f), skinAccentRgb = floatArrayOf(0.32f, 0.27f, 0.20f)
        ),
        AttachmentItem(
            "skin_urban", AttachmentSlot.SKIN, LocalizedText("تمويه النهر الرمادي", "River Urban Slate"), 180,
            skinColorRgb = floatArrayOf(0.34f, 0.39f, 0.44f), skinAccentRgb = floatArrayOf(0.18f, 0.21f, 0.24f)
        ),
        AttachmentItem(
            "skin_gold", AttachmentSlot.SKIN, LocalizedText("تشطيب ذهبي تكتيكي", "Desert Gold Sovereign"), 450,
            skinColorRgb = floatArrayOf(0.76f, 0.58f, 0.22f), skinAccentRgb = floatArrayOf(0.16f, 0.16f, 0.18f)
        )
    )

    fun byId(id: WeaponId): WeaponData = weapons.first { it.id == id }

    fun attachmentById(id: String): AttachmentItem? = attachments.firstOrNull { it.id == id }

    fun attachmentsForSlot(slot: AttachmentSlot): List<AttachmentItem> = attachments.filter { it.slot == slot }

    fun defaultAttachments(): Map<AttachmentSlot, String> = mapOf(
        AttachmentSlot.OPTIC to "optic_iron",
        AttachmentSlot.GRIP to "grip_std",
        AttachmentSlot.MAGAZINE to "mag_std",
        AttachmentSlot.BARREL to "barrel_std",
        AttachmentSlot.STABILIZER to "stab_none",
        AttachmentSlot.SKIN to "skin_factory"
    )

    fun computeEffectiveStats(weaponId: WeaponId, equippedSlots: Map<AttachmentSlot, String>): EffectiveWeaponStats {
        val w = byId(weaponId)
        val items = equippedSlots.values.mapNotNull { attachmentById(it) }
        val accBonus = items.sumOf { it.accuracyBonus.toDouble() }.toFloat().coerceAtMost(0.65f)
        val recBonus = items.sumOf { it.recoilControlBonus.toDouble() }.toFloat().coerceAtMost(0.65f)
        val magExtra = items.sumOf { it.magBonus }
        val relBonus = items.sumOf { it.reloadSpeedBonus.toDouble() }.toFloat().coerceIn(-0.15f, 0.45f)
        val rangeExtra = items.sumOf { it.rangeBonus.toDouble() }.toFloat()

        val scaledMagBonus = if (w.magCapacity <= 10) (magExtra / 3) else magExtra
        val skinItem = items.firstOrNull { it.slot == AttachmentSlot.SKIN && it.skinColorRgb != null }
        val opticId = equippedSlots[AttachmentSlot.OPTIC] ?: "optic_iron"
        val adsFov = when {
            w.id == WeaponId.SR308_PRECISION -> 28f
            opticId == "optic_acog" -> 38f
            opticId == "optic_holo" -> 45f
            else -> 50f
        }

        return EffectiveWeaponStats(
            weapon = w,
            damage = w.damage,
            fireRate = w.fireRate,
            magCapacity = (w.magCapacity + scaledMagBonus).coerceAtLeast(4),
            reserveMax = w.reserveMax + scaledMagBonus * 4,
            reloadDuration = (w.reloadDuration * (1f - relBonus)).coerceAtLeast(0.75f),
            hipSpread = (w.baseSpread * (1f - accBonus * 0.6f)).coerceAtLeast(0.006f),
            adsSpread = (w.adsSpread * (1f - accBonus)).coerceAtLeast(0.001f),
            recoilKick = (w.recoilKick * (1f - recBonus)).coerceAtLeast(0.005f),
            effectiveRange = w.effectiveRange + rangeExtra,
            adsZoomFov = adsFov,
            bodyRgb = skinItem?.skinColorRgb ?: w.baseColorRgb,
            accentRgb = skinItem?.skinAccentRgb ?: w.accentColorRgb,
            hasOptic = opticId != "optic_iron",
            hasGrip = (equippedSlots[AttachmentSlot.GRIP] ?: "grip_std") != "grip_std",
            hasExtendedMag = (equippedSlots[AttachmentSlot.MAGAZINE] ?: "mag_std") == "mag_ext",
            hasLongBarrel = (equippedSlots[AttachmentSlot.BARREL] ?: "barrel_std") != "barrel_std",
            hasStabilizer = (equippedSlots[AttachmentSlot.STABILIZER] ?: "stab_none") != "stab_none"
        )
    }
}
