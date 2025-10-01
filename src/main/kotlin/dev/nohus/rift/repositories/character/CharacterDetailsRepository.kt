package dev.nohus.rift.repositories.character

import dev.nohus.rift.contacts.ContactsRepository
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.models.AlliancesIdAlliance
import dev.nohus.rift.network.esi.models.CorporationsIdCorporation
import dev.nohus.rift.standings.Standing
import dev.nohus.rift.standings.StandingsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.koin.core.annotation.Single

@Single
class CharacterDetailsRepository(
    private val esiApi: EsiApi,
    private val standingsRepository: StandingsRepository,
    private val contactsRepository: ContactsRepository,
) {

    data class CharacterDetails(
        val characterId: Int,
        val name: String,
        val corporationId: Int,
        val corporationName: String?,
        val corporationTicker: String?,
        val allianceId: Int?,
        val allianceName: String?,
        val allianceTicker: String?,
        val standing: Float,
        val standingLevel: Standing,
        val title: String?,
        val characterLabels: List<String>,
        val corporationLabels: List<String>,
        val allianceLabels: List<String>,
    )

    suspend fun getCharacterDetails(characterId: Int): CharacterDetails? = coroutineScope {
        val characterDeferred = async { esiApi.getCharactersId(characterId).success }
        val affiliationDeferred = async { esiApi.getCharactersAffiliation(listOf(characterId)).success?.firstOrNull() }
        val character = characterDeferred.await() ?: return@coroutineScope null
        val affiliation = affiliationDeferred.await()
        val corporationId = affiliation?.corporationId ?: character.corporationId
        val allianceId = affiliation?.allianceId ?: character.allianceId
        val deferredCorporation = async { esiApi.getCorporationsId(corporationId).success }
        val deferredAlliance = async { allianceId?.let { esiApi.getAlliancesId(it).success } }
        val corporation = deferredCorporation.await()
        val alliance = deferredAlliance.await()
        val standing = standingsRepository.getStanding(allianceId, corporationId, characterId) ?: 0f
        val standingLevel = standingsRepository.getStandingLevel(allianceId, corporationId, characterId)
        val characterLabels = contactsRepository.getLabels(listOf(characterId)).map { it.name }.distinct()
        val corporationLabels = contactsRepository.getLabels(listOf(corporationId)).map { it.name }.distinct()
        val allianceLabels = allianceId
            ?.let { contactsRepository.getLabels(listOf(allianceId)).map { it.name }.distinct() }
            ?: emptyList()
        CharacterDetails(
            characterId = characterId,
            name = character.name,
            corporationId = corporationId,
            corporationName = corporation?.name,
            corporationTicker = corporation?.ticker,
            allianceId = allianceId,
            allianceName = alliance?.name,
            allianceTicker = alliance?.ticker,
            standing = standing,
            standingLevel = standingLevel,
            title = character.title,
            characterLabels = characterLabels,
            corporationLabels = corporationLabels,
            allianceLabels = allianceLabels,
        )
    }

    suspend fun getCorporationName(corporationId: Int): Result<CorporationsIdCorporation> {
        return esiApi.getCorporationsId(corporationId)
    }

    suspend fun getAllianceName(allianceId: Int): Result<AlliancesIdAlliance> {
        return esiApi.getAlliancesId(allianceId)
    }
}
