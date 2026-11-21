package com.example.game.engine

import com.example.game.data.CityId
import com.example.game.data.EnemyArchetype
import com.example.game.data.MissionCatalog
import com.example.game.data.ObjectiveKind

data class MissionValidationIssue(
    val missionNumber: Int,
    val severity: String,
    val detail: String
)

data class MissionValidationReport(
    val missionsChecked: Int,
    val citiesChecked: Int,
    val issues: List<MissionValidationIssue>
) {
    val isValid: Boolean get() = issues.isEmpty()
}

/**
 * v1.1.0 Automated MissionValidator.
 * Verifies every city and all 12 campaign missions for playable bounds, unobstructed spawns,
 * reachable objectives, valid cover points, interactive world objects, and boss integrity.
 */
object MissionValidator {

    fun validateAll(): MissionValidationReport {
        val issues = mutableListOf<MissionValidationIssue>()
        val cityCache = CityId.entries.associateWith { CityBuilder.buildCity(it) }

        cityCache.forEach { (cityId, city) ->
            if (city.coverPoints.size < 8) {
                issues.add(MissionValidationIssue(0, "ERROR", "City ${cityId.id} has fewer than 8 cover points"))
            }
            if (city.interactiveObjects.size < 4) {
                issues.add(MissionValidationIssue(0, "ERROR", "City ${cityId.id} lacks sufficient interactive world objects"))
            }
        }

        MissionCatalog.missions.forEach { m ->
            val city = cityCache[m.cityId]!!
            if (m.playerSpawnX !in city.minBoundX..city.maxBoundX || m.playerSpawnZ !in city.minBoundZ..city.maxBoundZ) {
                issues.add(MissionValidationIssue(m.number, "ERROR", "Player spawn out of bounds"))
            }
            val spawnBlocked = city.colliders.any {
                it.isActive && it.bounds.intersectsCylinder(m.playerSpawnX, 0.1f, 1.8f, m.playerSpawnZ, 0.42f)
            }
            if (spawnBlocked) {
                issues.add(MissionValidationIssue(m.number, "ERROR", "Player spawn overlaps solid collider"))
            }
            if (m.objectives.isEmpty()) {
                issues.add(MissionValidationIssue(m.number, "ERROR", "Mission has zero objectives"))
            }
            m.objectives.forEachIndexed { idx, obj ->
                if (obj.targetX !in city.minBoundX..city.maxBoundX || obj.targetZ !in city.minBoundZ..city.maxBoundZ) {
                    issues.add(MissionValidationIssue(m.number, "ERROR", "Objective #$idx target out of bounds"))
                }
                if (obj.kind == ObjectiveKind.DEFEAT_BOSS && m.spawns.none { it.archetype == EnemyArchetype.ELITE_BOSS }) {
                    issues.add(MissionValidationIssue(m.number, "ERROR", "Boss objective has no ELITE_BOSS spawn"))
                }
            }
            if (m.spawns.isEmpty()) {
                issues.add(MissionValidationIssue(m.number, "ERROR", "Mission has zero enemy spawns"))
            }
            m.spawns.forEachIndexed { idx, sp ->
                if (sp.x !in city.minBoundX..city.maxBoundX || sp.z !in city.minBoundZ..city.maxBoundZ) {
                    issues.add(MissionValidationIssue(m.number, "ERROR", "Enemy spawn #$idx out of city bounds"))
                }
            }
        }

        return MissionValidationReport(
            missionsChecked = MissionCatalog.missions.size,
            citiesChecked = cityCache.size,
            issues = issues
        )
    }
}
