package dev.nohus.rift.killboard

import dev.nohus.rift.killboard.Killboard.EveKill
import dev.nohus.rift.killboard.Killboard.Zkillboard
import dev.nohus.rift.network.evekill.EveKillKillmail
import dev.nohus.rift.network.zkillboardqueue.Package
import dev.nohus.rift.repositories.Position
import org.koin.core.annotation.Single

@Single
class KillmailConverter {

    fun convert(zkillboardPackage: Package): Killmail = with(zkillboardPackage) {
        return Killmail(
            killboard = Zkillboard,
            killmailId = killmail.killmailId,
            killmailTime = killmail.killmailTime,
            solarSystemId = killmail.solarSystemId,
            url = zkb.url,
            victim = Victim(
                characterId = killmail.victim.characterId,
                corporationId = killmail.victim.corporationId,
                allianceId = killmail.victim.allianceId,
                shipTypeId = killmail.victim.shipTypeId,
            ),
            attackers = killmail.attackers.map { attacker ->
                Attacker(
                    characterId = attacker.characterId,
                    shipTypeId = attacker.shipTypeId,
                )
            },
            position = killmail.victim.position?.let { Position(it.x, it.y, it.z) },
        )
    }

    fun convert(killmail: EveKillKillmail): Killmail? = with(killmail) {
        return Killmail(
            killboard = EveKill,
            killmailId = killmailId ?: return null,
            killmailTime = killTimeStr ?: return null,
            solarSystemId = systemId ?: return null,
            url = "https://eve-kill.com/kill/$killmailId",
            victim = Victim(
                characterId = victim?.characterId?.takeIf { it > 0 },
                corporationId = victim?.corporationId?.takeIf { it > 0 },
                allianceId = victim?.allianceId?.takeIf { it > 0 },
                shipTypeId = victim?.shipId?.takeIf { it > 0 },
            ),
            attackers = attackers.map { attacker ->
                Attacker(
                    characterId = attacker.characterId?.takeIf { it > 0 },
                    shipTypeId = attacker.shipId?.takeIf { it > 0 },
                )
            },
            position = if (x != null && y != null && z != null) Position(x, y, z) else null,
        )
    }
}
