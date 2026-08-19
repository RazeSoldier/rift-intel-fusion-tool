package dev.nohus.rift.charactersettings.io

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.annotation.Single
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

@Single
class ReadAccountSettingsUseCase {

    data class AccountSettings(
        val isShipUiOnTop: Boolean,
        val targetOrigin: Pair<Float, Float>?,
        val isTargetsAlignHorizontal: Boolean,
        val probeFormations: List<ProbeFormation>,
        val rawProbeFormations: JsonObject,
        val parsedProbeFormationKeys: Set<String>,
    )

    data class ProbeFormation(
        val name: String,
        val probes: List<Probe>,
    )

    data class Probe(
        val x: Double,
        val y: Double,
        val z: Double,
        val scanRadius: Double,
    )

    operator fun invoke(settingsFile: Path?): AccountSettings? {
        if (settingsFile == null) return null
        return try {
            read(settingsFile)
        } catch (e: Exception) {
            logger.error(e) { "Failed to read the settings file $settingsFile" }
            null
        }
    }

    private fun read(settingsFile: Path): AccountSettings {
        val settings = settingsFile.readEveSettings()

        val ui = settings.section("bytes:ui")
        val isShipUiOnTop = ui?.tupleValue("bytes:shipuialigntop")?.jsonPrimitive?.booleanOrNull ?: false
        val targetOrigin = ui?.tupleValue("bytes:targetOrigin").tupleValues()
            ?.mapNotNull { it.jsonPrimitive.floatOrNull }
            ?.takeIf { it.size == 2 }
            ?.let { it[0] to it[1] }
        val isTargetsAlignHorizontal = ui?.tupleValue("bytes:alignHorizontally")?.jsonPrimitive?.booleanOrNull ?: true
        val rawProbeFormations = ui?.tupleValue("bytes:probescanning.customFormations") as? JsonObject ?: JsonObject(emptyMap())
        val parsedProbeFormations = rawProbeFormations.entries
            .mapNotNull formationMap@{ (key, formation) ->
                val tuple = formation.tupleValues() ?: return@formationMap null
                val encodedName = tuple.getOrNull(0)?.jsonPrimitive?.content ?: return@formationMap null
                if (encodedName == "bytes:tempFormation") return@formationMap null
                val rawProbes = tuple.getOrNull(1)?.jsonArray ?: return@formationMap null
                val probes = rawProbes.mapNotNull probeMap@{ probe ->
                    val probeTuple = probe.tupleValues() ?: return@probeMap null
                    val position = probeTuple.getOrNull(0).tupleValues() ?: return@probeMap null
                    val coordinates = position.mapNotNull { it.jsonPrimitive.doubleOrNull }
                    val scanRadius = probeTuple.getOrNull(1)?.jsonPrimitive?.doubleOrNull ?: return@probeMap null
                    if (coordinates.size != 3) return@probeMap null
                    Probe(coordinates[0], coordinates[1], coordinates[2], scanRadius)
                }
                if (probes.size != rawProbes.size) return@formationMap null
                ProbeFormation(
                    name = encodedName.substringAfter(':'),
                    probes = probes,
                ).takeIf { it.probes.isNotEmpty() }?.let { key to it }
            }
        val probeFormations = parsedProbeFormations.map { it.second }.sortedBy { it.name.lowercase() }

        return AccountSettings(
            isShipUiOnTop = isShipUiOnTop,
            targetOrigin = targetOrigin,
            isTargetsAlignHorizontal = isTargetsAlignHorizontal,
            probeFormations = probeFormations,
            rawProbeFormations = rawProbeFormations,
            parsedProbeFormationKeys = parsedProbeFormations.map { it.first }.toSet(),
        )
    }
}
