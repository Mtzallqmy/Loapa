package com.example.game.data

enum class AchievementCategory {
    COMBAT,
    MISSION,
    ACCURACY,
    SURVIVAL,
    CAMPAIGN,
    MASTERY
}

data class AchievementDef(
    val id: String,
    val category: AchievementCategory,
    val title: LocalizedText,
    val description: LocalizedText,
    val xpBonus: Int
)

object AchievementCatalog {
    val all: List<AchievementDef> = listOf(
        AchievementDef(
            id = "ach_first_blood",
            category = AchievementCategory.COMBAT,
            title = LocalizedText("الانتشار الناجح", "First Deployment"),
            description = LocalizedText("أكمل المهمة الأولى في مدينة النهر.", "Complete Mission 01 in River City."),
            xpBonus = 100
        ),
        AchievementDef(
            id = "ach_sharpshooter",
            category = AchievementCategory.ACCURACY,
            title = LocalizedText("رامٍ دقيق", "Sharpshooter"),
            description = LocalizedText("أنهِ مهمة بدقة تصويب 45% أو أعلى.", "Finish a mission with 45%+ shot accuracy."),
            xpBonus = 180
        ),
        AchievementDef(
            id = "ach_headhunter",
            category = AchievementCategory.COMBAT,
            title = LocalizedText("إصابات حاسمة", "Precision Marksman"),
            description = LocalizedText("سجّل 15 إصابة رأس إجمالاً.", "Record 15 total headshots."),
            xpBonus = 200
        ),
        AchievementDef(
            id = "ach_survivor",
            category = AchievementCategory.SURVIVAL,
            title = LocalizedText("ناجٍ تكتيكي", "Tactical Survivor"),
            description = LocalizedText("أكمل مهمة دون السقوط مع صحة متبقية فوق 50%.", "Complete a mission without falling and 50%+ HP remaining."),
            xpBonus = 220
        ),
        AchievementDef(
            id = "ach_urban_hero",
            category = AchievementCategory.MISSION,
            title = LocalizedText("بطل مدينة النهر", "River City Liberator"),
            description = LocalizedText("أكمل جميع مهمات مدينة النهر الثلاث.", "Complete all three River City missions."),
            xpBonus = 250
        ),
        AchievementDef(
            id = "ach_desert_storm",
            category = AchievementCategory.MISSION,
            title = LocalizedText("فارس السراب", "Mirage Veteran"),
            description = LocalizedText("أكمل جميع مهمات مدينة السراب.", "Complete all Mirage City desert operations."),
            xpBonus = 300
        ),
        AchievementDef(
            id = "ach_harbor_master",
            category = AchievementCategory.MISSION,
            title = LocalizedText("حامي المرسى", "Harbor Commander"),
            description = LocalizedText("أكمل جميع مهمات مدينة المرسى الساحلية.", "Complete all Harbor City coastal operations."),
            xpBonus = 350
        ),
        AchievementDef(
            id = "ach_weapon_master",
            category = AchievementCategory.MASTERY,
            title = LocalizedText("سيد الترسانة", "Arsenal Master"),
            description = LocalizedText("افتح 6 أسلحة مختلفة على الأقل.", "Unlock at least 6 different weapon platforms."),
            xpBonus = 300
        ),
        AchievementDef(
            id = "ach_veteran_50",
            category = AchievementCategory.COMBAT,
            title = LocalizedText("مقاتل مخضرم", "Combat Veteran"),
            description = LocalizedText("اقضِ على 50 هدفاً معادياً في الحملة.", "Eliminate 50 total hostile units across the campaign."),
            xpBonus = 320
        ),
        AchievementDef(
            id = "ach_campaign_victory",
            category = AchievementCategory.CAMPAIGN,
            title = LocalizedText("قائد جبهات القتال", "Combat Fronts Champion"),
            description = LocalizedText("أكمل جميع المهمات الاثنتي عشرة واهزم القائد الأعلى.", "Complete all 12 campaign missions and defeat the Elite Boss."),
            xpBonus = 600
        )
    )
}
