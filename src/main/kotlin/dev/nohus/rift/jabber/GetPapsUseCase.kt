package dev.nohus.rift.jabber

import dev.nohus.rift.jabber.client.JabberClient
import dev.nohus.rift.utils.get
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.Single
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

@Single
class GetPapsUseCase(
    private val jabberClient: JabberClient,
) {

    private val papMonthRegex = """This Month: (?<str>\d+) STR.*(?<pct>\d+) PCT""".toRegex()
    private val pap30daysRegex = """Last 30 Days: (?<str>\d+) STR.*(?<pct>\d+) PCT""".toRegex()
    private val pap90daysRegex = """Last 90 Days: (?<str>\d+) STR.*(?<pct>\d+) PCT""".toRegex()
    private val lastStratRegex = """Last strat-op: (?<date>.*) with (?<character>.*)""".toRegex()

    data class Paps(
        val timestamp: Instant,
        val strategic: PapStats,
        val peacetime: PapStats,
        val lastStrat: LastStrat?,
    )

    data class PapStats(
        val month: Int?,
        val days30: Int?,
        val days90: Int?,
    )

    data class LastStrat(
        val date: String,
        val character: String,
    )

    suspend operator fun invoke(): Paps? {
        withTimeoutOrNull(30.seconds) {
            jabberClient.state.map { it.isConnected }.first { it }
        }
        if (jabberClient.state.value.isConnected) {
            val sentTimestamp = Instant.now()
            jabberClient.sendMessage("directorbot@goonfleet.com", "!me")
            val response = withTimeoutOrNull(5.seconds) {
                jabberClient.state.mapNotNull { state ->
                    state.userChatMessages.entries
                        .firstOrNull { it.key.xmppAddressOfChatPartner.localpartOrNull?.toString() == "directorbot" }
                        ?.value
                        ?.filterNot { it.isOutgoing }
                        ?.filter { it.timestamp > sentTimestamp }
                        ?.lastOrNull { "Account Info:" in it.text }
                }.first().text
            }
            if (response != null) {
                val month = getPaps(response, papMonthRegex)
                val days30 = getPaps(response, pap30daysRegex)
                val days90 = getPaps(response, pap90daysRegex)
                val lastStrat = getLastStrat(response)

                val strategic = PapStats(month?.first, days30?.first, days90?.first)
                val peacetime = PapStats(month?.second, days30?.second, days90?.second)
                return Paps(Instant.now(), strategic, peacetime, lastStrat)
            } else {
                logger.warn { "No reply from directorbot, not getting paps" }
            }
        } else {
            logger.warn { "Not connected, not getting paps" }
        }
        return null
    }

    private fun getPaps(text: String, regex: Regex): Pair<Int, Int>? {
        val match = text.lines().firstNotNullOfOrNull { regex.find(it) }
        if (match == null) return null
        val str = match["str"].toIntOrNull() ?: 0
        val pct = match["pct"].toIntOrNull() ?: 0
        return str to pct
    }

    private fun getLastStrat(text: String): LastStrat? {
        val match = text.lines().firstNotNullOfOrNull { lastStratRegex.find(it) }
        if (match == null) return null
        val date = match["date"]
        val character = match["character"]
        return LastStrat(date, character)
    }
}
