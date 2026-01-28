package dev.nohus.rift.configurationpack

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.settings.persistence.ConfigurationPack
import dev.nohus.rift.settings.persistence.ConfigurationPack.Imperium
import dev.nohus.rift.settings.persistence.ConfigurationPack.PhoenixCoalition
import dev.nohus.rift.settings.persistence.ConfigurationPack.TheInitiative
import dev.nohus.rift.settings.persistence.IntelChannel
import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single
import java.time.LocalDate

@Single
class ConfigurationPackRepository(
    private val settings: Settings,
    private val localCharactersRepository: LocalCharactersRepository,
) {
    data class SuggestedIntelChannels(
        val promptTitleText: String,
        val promptButtonText: String,
        val channels: List<IntelChannel>,
    )

    fun set(configurationPack: ConfigurationPack?) {
        if (settings.configurationPack != configurationPack) {
            settings.configurationPack = configurationPack
            settings.isConfigurationPackReminderDismissed = false
        }
    }

    fun getSuggestedPack(): ConfigurationPack? {
        val characterAlliances = localCharactersRepository.characters.value
            .mapNotNull { it.info?.allianceId }
            .toSet()
        return ConfigurationPack.entries.firstOrNull { pack ->
            getPackMemberAllianceIds(pack).any { it in characterAlliances }
        }
    }

    private fun getPackMemberAllianceIds(pack: ConfigurationPack?): List<Int> {
        return when (pack) {
            Imperium -> listOf(
                1354830081, // Goonswarm Federation
                99003214, // Brave Collective
                99010079, // Brave United
                99013363, // Dracarys Wing
                99009163, // Dracarys.
                99012042, // Fanatic Legion.
                150097440, // Get Off My Lawn
                99003995, // Invidia Gloriae Comes
                99011239, // Ligma Grindset
                99013568, // S0B Citizens Alliance
                99001969, // SONS of BANE
                99009331, // Scumlords
                99011162, // Shadow Ultimatum
                99011223, // Sigma Grindset
                99010140, // Stribog Clade
                131511956, // Tactical Narcotics Team
                99010877, // Out of the Blue.
                99010931, // WE FORM BL0B
            )

            TheInitiative -> listOf(
                1900696668, // The Initiative.
            )

            PhoenixCoalition -> listOf(
                99002685, // Synergy of Steel
                741557221, // Razor Alliance
                99001317, // Banderlogs Alliance
                99010281, // GameTheory
                99012770, // Black Rose.
                99005274, // La Ligue des mondes libres
                99012040, // Regnum Astera
                99013231, // Blood Drive
                99013216, // Nomad Alliance
                154104258, // Apocalypse Now.
                99010896, // Caldari Alliance
                99013539, // The Disciples of Space Piracy
                99013456, // Northern Frontier Group
                99013759, // Imurukka Conglomerate
                99012410, // DECOY
            )

            null -> emptyList()
        }
    }

    fun getSuggestedIntelChannels(): SuggestedIntelChannels? {
        return when (settings.configurationPack) {
            Imperium -> SuggestedIntelChannels(
                promptTitleText = "Would you like intel channels of the Imperium to be configured automatically?",
                promptButtonText = "Add Imperium channels",
                channels = listOf(
                    IntelChannel("east.imperium", "Catch"),
                    IntelChannel("east.imperium", "Immensea"),
                    IntelChannel("east.imperium", "Impass"),
                    IntelChannel("east.imperium", "Tenerifis"),
                    IntelChannel("fareast.imperium", "Detorid"),
                    IntelChannel("fareast.imperium", "Wicked Creek"),
                    IntelChannel("fareast.imperium", "Insmother"),
                    IntelChannel("fareast.imperium", "Cache"),
                    IntelChannel("fareast.imperium", "Scalding Pass"),
                    IntelChannel("west.imperium", "Delve"),
                    IntelChannel("west.imperium", "Querious"),
                    IntelChannel("west.imperium", "Period Basis"),
                    IntelChannel("southeast.imperium", "Esoteria"),
                    IntelChannel("southeast.imperium", "Feythabolis"),
                    IntelChannel("southeast.imperium", "Paragon Soul"),
                    IntelChannel("aridia.imperium", "Aridia"),
                    IntelChannel("curse.imperium", "Curse"),
                    IntelChannel("ftn.imperium", "Fountain"),
                    IntelChannel("khanid.imperium", "Khanid"),
                    IntelChannel("triangle.imperium", "Pochven"),
                ),
            )

            TheInitiative -> SuggestedIntelChannels(
                promptTitleText = "Would you like intel channels of The Initiative. to be configured automatically?",
                promptButtonText = "Add Init channels",
                channels = listOf(
                    IntelChannel("I. Ftn Intel", "Fountain"),
                    IntelChannel("I. OR Intel", "Outer Ring"),
                    IntelChannel("I. Aridia Intel", "Aridia"),
                    IntelChannel("I. Curse Intel", "Curse"),
                    IntelChannel("I. Poch Intel", "Pochven"),
                    IntelChannel("I. C Ring Intel", "Cloud Ring"),
                ),
            )

            PhoenixCoalition -> SuggestedIntelChannels(
                promptTitleText = "Would you like intel channels of the Phoenix Coalition to be configured automatically?",
                promptButtonText = "Add Phoenix Coalition channels",
                channels = listOf(
                    IntelChannel("Phoenix_Intel", "Delve"),
                    IntelChannel("Phoenix_Intel", "Querious"),
                ),
            )

            null -> null
        }
    }

    fun isJabberEnabled(): Boolean {
        return when (settings.configurationPack) {
            Imperium -> true
            TheInitiative -> false
            PhoenixCoalition -> false
            null -> false
        }
    }

    sealed class JumpBridgesReference(open val packName: String) {
        data class Url(override val packName: String, val url: String) : JumpBridgesReference(packName)
        data class Text(override val packName: String, val text: String, val date: LocalDate) :
            JumpBridgesReference(packName)
    }

    fun getJumpBridges(): JumpBridgesReference? {
        return when (settings.configurationPack) {
            Imperium -> JumpBridgesReference.Url(
                packName = "The Imperium",
                url = "https://wiki.goonswarm.org/w/Alliance:Stargate",
            )
            TheInitiative -> null
            PhoenixCoalition -> JumpBridgesReference.Text(
                packName = "Phoenix Coalition",
                text = """
                W-KQPI -> F2OY-X
                E3OI-U -> 4O-239
                D-3GIQ -> RF-K9W
                K-6K16 -> 5V-BJI
                QY6-RK -> 6Z-CKS
                F-TE1T -> CX8-6K
                AJI-MA -> A-BO4V
                """.trimIndent(),
                date = LocalDate.of(2026, 1, 28),
            )

            null -> null
        }
    }

    fun getSovereigntyUpgradesUrl(): String? {
        return when (settings.configurationPack) {
            Imperium -> "https://goonfleet.com/index.php/topic/371770-equinox-upgrade-information-station/"
            TheInitiative -> null
            PhoenixCoalition -> null
            null -> null
        }
    }
}
