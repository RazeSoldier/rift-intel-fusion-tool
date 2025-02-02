package dev.nohus.rift.killboard

import dev.nohus.rift.repositories.Position
import java.time.Instant

data class Killmail(
    val killboard: Killboard,
    val killmailId: Int,
    val killmailTime: Instant,
    val solarSystemId: Int,
    val url: String,
    val victim: Victim,
    val attackers: List<Attacker>,
    val position: Position?,
)

data class Victim(
    val characterId: Int?,
    val corporationId: Int?,
    val allianceId: Int?,
    val shipTypeId: Int?,
)

data class Attacker(
    val characterId: Int?,
    val shipTypeId: Int?,
)

enum class Killboard {
    Zkillboard, EveKill
}
