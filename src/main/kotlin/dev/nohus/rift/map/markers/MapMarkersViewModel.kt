package dev.nohus.rift.map.markers

import dev.nohus.rift.ViewModel
import dev.nohus.rift.alerts.creategroup.CreateGroupInputModel
import dev.nohus.rift.compose.DialogMessage
import dev.nohus.rift.compose.MessageDialogType
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.allDrawableResources
import dev.nohus.rift.generated.resources.map_marker_place_bookmark
import dev.nohus.rift.map.MapExternalControl
import dev.nohus.rift.map.markers.MapMarkersInputModel.AddToSystem
import dev.nohus.rift.map.markers.MapMarkersInputModel.New
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.settings.persistence.MapMarker
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.toggle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import java.util.UUID

@Factory
class MapMarkersViewModel(
    @InjectedParam private val inputModel: MapMarkersInputModel,
    private val settings: Settings,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val mapExternalControl: MapExternalControl,
) : ViewModel() {

    data class UiState(
        val systemText: String = "",
        var editingMarker: MapMarkerItem? = null,
        val markers: List<MapMarkerItem> = emptyList(),
        val groups: Set<String> = emptySet(),
        val collapsedGroups: Set<String?> = emptySet(),
        val expandedMarker: UUID? = null,
        val isCreateGroupDialogOpen: CreateGroupInputModel? = null,
        val dialog: DialogMessage? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        when (inputModel) {
            New -> {}
            is AddToSystem -> _state.update {
                it.copy(systemText = solarSystemsRepository.getSystemName(inputModel.systemId) ?: "")
            }
        }
        loadMarkers()
        viewModelScope.launch {
            settings.updateFlow.map { it.mapMarkers to it.mapMarkerGroups }.collect {
                loadMarkers()
            }
        }
    }

    private fun loadMarkers() {
        val markers = settings.mapMarkers.map {
            MapMarkerItem(
                id = it.id,
                systemId = it.systemId,
                systemName = solarSystemsRepository.getSystemName(it.systemId) ?: "Unknown",
                regionName = solarSystemsRepository.getRegionBySystemId(it.systemId)?.name ?: "Unknown",
                label = it.label,
                color = it.color,
                iconName = it.icon,
                icon = Res.allDrawableResources[it.icon] ?: Res.drawable.map_marker_place_bookmark,
                isEnabled = it.isEnabled,
                group = it.group,
            )
        }.sortedWith(compareBy({ it.regionName }, { it.systemName }))
        _state.update { it.copy(markers = markers, groups = settings.mapMarkerGroups) }
    }

    fun onSystemTextChange(text: String) {
        _state.update { it.copy(systemText = text) }
    }

    fun onAddMarkerClick(input: NewMarkerInput): Boolean {
        if (input.system.isNotBlank()) {
            val system = solarSystemsRepository.getSystem(input.system)
            if (system != null) {
                addMarker(system, input)
                return true
            } else {
                showError("System \"${input.system}\" does not exist")
            }
        } else {
            showError("You need to enter a system name")
        }
        return false
    }

    fun onDeleteMarkerClick(id: UUID) {
        deleteMarker(id)
    }

    fun onMarkerClick(id: UUID) {
        val marker = _state.value.markers.firstOrNull { it.id == id } ?: return
        val expandedMarker = if (_state.value.expandedMarker != marker.id) marker.id else null
        _state.update { it.copy(expandedMarker = expandedMarker) }
    }

    fun onGroupClick(name: String?) {
        _state.update { it.copy(collapsedGroups = it.collapsedGroups.toggle(name)) }
    }

    fun onToggleMarker(id: UUID, isEnabled: Boolean) {
        val marker = _state.value.markers.firstOrNull { it.id == id } ?: return
        settings.mapMarkers = settings.mapMarkers.map {
            if (it.id == marker.id) it.copy(isEnabled = isEnabled) else it
        }
    }

    fun onGroupChange(id: UUID, group: String?) {
        val marker = _state.value.markers.firstOrNull { it.id == id } ?: return
        settings.mapMarkers = settings.mapMarkers.map {
            if (it.id == marker.id) it.copy(group = group) else it
        }
    }

    fun onCreateGroupClick() {
        _state.update { it.copy(isCreateGroupDialogOpen = CreateGroupInputModel.New) }
    }

    fun onCloseCreateGroup() {
        _state.update { it.copy(isCreateGroupDialogOpen = null) }
    }

    fun onCreateGroupConfirm(name: String) {
        when (val inputModel = _state.value.isCreateGroupDialogOpen) {
            CreateGroupInputModel.New -> {
                if (name.isNotBlank()) {
                    settings.mapMarkerGroups = (settings.mapMarkerGroups + name).toSet()
                }
            }
            is CreateGroupInputModel.Rename -> {
                if (name.isNotBlank()) {
                    _state.update { it.copy(collapsedGroups = it.collapsedGroups - inputModel.name) }
                    settings.mapMarkerGroups = settings.mapMarkerGroups.map {
                        if (it == inputModel.name) name else it
                    }.toSet()
                    settings.mapMarkers = settings.mapMarkers.map { marker ->
                        if (marker.group == inputModel.name) marker.copy(group = name) else marker
                    }
                }
            }
            null -> {}
        }
        _state.update { it.copy(isCreateGroupDialogOpen = null) }
    }

    fun onGroupRenameClick(group: String) {
        _state.update { it.copy(isCreateGroupDialogOpen = CreateGroupInputModel.Rename(group)) }
    }

    fun onGroupDeleteClick(group: String) {
        _state.update { it.copy(collapsedGroups = it.collapsedGroups - group) }
        settings.mapMarkers = settings.mapMarkers.map { marker ->
            if (marker.group == group) marker.copy(group = null) else marker
        }
        settings.mapMarkerGroups = settings.mapMarkerGroups.filterNot { it == group }.toSet()
    }

    fun onGroupToggleMarkers(group: String?) {
        val hasEnabledMarkers = settings.mapMarkers.any { it.group == group && it.isEnabled }
        settings.mapMarkers = settings.mapMarkers.map {
            if (it.group == group) it.copy(isEnabled = !hasEnabledMarkers) else it
        }
    }

    private fun addMarker(system: MapSolarSystem, input: NewMarkerInput) {
        val id = _state.value.editingMarker?.id ?: UUID.randomUUID()
        val existingMarker = settings.mapMarkers.firstOrNull { it.id == id }
        deleteMarker(id)
        val newMarker = MapMarker(
            id = id,
            systemId = system.id,
            label = input.label,
            color = input.color,
            icon = input.icon,
            isEnabled = existingMarker?.isEnabled ?: true,
            group = existingMarker?.group,
        )
        _state.update { it.copy(editingMarker = null) }
        settings.mapMarkers += newMarker
    }

    private fun deleteMarker(id: UUID) {
        settings.mapMarkers -= settings.mapMarkers.firstOrNull { it.id == id } ?: return
    }

    fun onCancelEditClick() {
        _state.update {
            it.copy(
                systemText = "",
                editingMarker = null,
            )
        }
    }

    fun onShowMarkerClick(id: UUID) {
        val marker = _state.value.markers.firstOrNull { it.id == id } ?: return
        mapExternalControl.showSystemOnMap(marker.systemId)
    }

    fun onEditMarkerClick(id: UUID) {
        val marker = _state.value.markers.firstOrNull { it.id == id } ?: return
        _state.update {
            it.copy(
                systemText = marker.systemName,
                editingMarker = marker,
            )
        }
    }

    private fun showError(text: String) {
        _state.update {
            it.copy(
                dialog = DialogMessage(
                    title = "Cannot create marker",
                    message = text,
                    type = MessageDialogType.Info,
                ),
            )
        }
    }

    fun onCloseDialogMessage() {
        _state.update { it.copy(dialog = null) }
    }
}
