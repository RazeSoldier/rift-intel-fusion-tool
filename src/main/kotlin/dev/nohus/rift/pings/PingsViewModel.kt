package dev.nohus.rift.pings

import dev.nohus.rift.ViewModel
import dev.nohus.rift.compose.RiftOpportunityCardCategory
import dev.nohus.rift.compose.RiftOpportunityCardTopRight.RiftOpportunityCardCharacter
import dev.nohus.rift.jabber.GetPapsUseCase
import dev.nohus.rift.jabber.GetPapsUseCase.Paps
import dev.nohus.rift.jabber.client.JabberClient
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.GetSolarSystemChipStateUseCase
import dev.nohus.rift.repositories.SolarSystemChipLocation
import dev.nohus.rift.repositories.SolarSystemChipState
import dev.nohus.rift.repositories.character.CharacterDetailsRepository
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.windowing.WindowManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration.Companion.minutes

@Factory
class PingsViewModel(
    private val settings: Settings,
    private val jabberClient: JabberClient,
    private val windowManager: WindowManager,
    private val pingsRepository: PingsRepository,
    private val openMumbleUseCase: OpenMumbleUseCase,
    private val getSolarSystemChipStateUseCase: GetSolarSystemChipStateUseCase,
    private val getPapsUseCase: GetPapsUseCase,
    private val characterDetailsRepository: CharacterDetailsRepository,
) : ViewModel() {

    data class UiState(
        val isPapsDialogOpen: Boolean = false,
        val isShowingPaps: Boolean?,
        val paps: Paps? = null,
        var papsLastChecked: Instant = Instant.EPOCH,
        val pings: List<PingUiModel> = emptyList(),
        val displayTimezone: ZoneId,
        val isJabberConnected: Boolean,
    )

    private val _state = MutableStateFlow(
        UiState(
            isShowingPaps = settings.isShowingPaps,
            displayTimezone = settings.displayTimeZone,
            isJabberConnected = jabberClient.state.value.isConnected,
        ),
    )
    val state = _state.asStateFlow()

    private val pingUiModels = mutableMapOf<PingModel, PingUiModel>()

    init {
        viewModelScope.launch {
            settings.updateFlow.collect {
                _state.update {
                    it.copy(
                        isShowingPaps = settings.isShowingPaps,
                        displayTimezone = settings.displayTimeZone,
                    )
                }
            }
        }
        viewModelScope.launch {
            jabberClient.state.map { it.isConnected }.collect { isConnected ->
                _state.update { it.copy(isJabberConnected = isConnected) }
            }
        }
        viewModelScope.launch {
            pingsRepository.pings.collect { pings ->
                _state.update { it.copy(pings = pings.map { it.toUiModel() }) }
            }
        }
        viewModelScope.launch {
            state.map { it.isShowingPaps to it.papsLastChecked }.distinctUntilChanged().collectLatest { (isShowingPaps, papsLastChecked) ->
                if (isShowingPaps == true) {
                    while (true) {
                        val paps = getPapsUseCase()
                        if (paps != null) {
                            _state.update { it.copy(paps = paps) }
                        }
                        delay(60.minutes)
                    }
                }
            }
        }
    }

    fun isShowingPapsChanged(enabled: Boolean) {
        _state.update { it.copy(isShowingPaps = enabled, isPapsDialogOpen = false) }
        settings.isShowingPaps = enabled
    }

    fun onPapsDialogClose() {
        _state.update { it.copy(isPapsDialogOpen = false) }
    }

    fun onPapsDialogOpen() {
        _state.update { it.copy(isPapsDialogOpen = true) }
    }

    fun onCheckPapsClick() {
        _state.update { it.copy(papsLastChecked = Instant.now()) }
    }

    fun onOpenJabberClick() {
        windowManager.onWindowOpen(WindowManager.RiftWindow.Jabber)
    }

    fun onMumbleClick(link: String) {
        viewModelScope.launch {
            openMumbleUseCase(link)
        }
    }

    private suspend fun PingModel.toUiModel(): PingUiModel {
        pingUiModels[this]?.let { return it }
        return when (this) {
            is PingModel.PlainText -> PingUiModel.PlainText(
                timestamp = timestamp,
                sourceText = sourceText,
                text = text,
                sender = sender,
                target = target,
            )

            is PingModel.FleetPing -> PingUiModel.FleetPing(
                timestamp = timestamp,
                sourceText = sourceText,
                opportunityCategory = getOpportunityCategory(),
                description = description,
                fleetCommander = fleetCommander.toUiModel(),
                fleet = fleet,
                formupLocations = formupLocations.toUiModel(),
                papType = papType,
                comms = comms,
                doctrine = doctrine,
                target = target,
            )
        }.also { pingUiModels[this] = it }
    }

    private fun List<FormupLocation>.toUiModel(): SolarSystemChipState {
        val locations = map {
            when (it) {
                is FormupLocation.System -> SolarSystemChipLocation.SolarSystem(it.id)
                is FormupLocation.Text -> SolarSystemChipLocation.Text(it.text)
            }
        }
        return getSolarSystemChipStateUseCase(locations)
    }

    private suspend fun FleetCommander.toUiModel(): RiftOpportunityCardCharacter {
        return RiftOpportunityCardCharacter(
            name = name,
            id = id,
            details = id?.let { characterDetailsRepository.getCharacterDetails(Originator.Pings, id) },
        )
    }

    private fun PingModel.getOpportunityCategory(): RiftOpportunityCardCategory {
        return when (this) {
            is PingModel.PlainText -> RiftOpportunityCardCategory.Unclassified
            is PingModel.FleetPing -> {
                if (listOf("wormhole", "[^a-z]wh[^a-z]").any { it.toRegex() in description.lowercase() }) {
                    RiftOpportunityCardCategory.Explorer
                } else if (listOf("ess defense", "ess hack ", "hostiles in Delve", "structure defense").any { it in description.lowercase() }) {
                    RiftOpportunityCardCategory.Enforcer
                } else if (listOf("pirating", "hunting").any { it in description.lowercase() }) {
                    RiftOpportunityCardCategory.SoldierOfFortune
                } else {
                    if (papType == PapType.Strategic) {
                        RiftOpportunityCardCategory.Enforcer
                    } else {
                        if (broadcastSource == "skirmishbot") {
                            RiftOpportunityCardCategory.SoldierOfFortune
                        } else {
                            RiftOpportunityCardCategory.Unclassified
                        }
                    }
                }
            }
        }
    }
}
