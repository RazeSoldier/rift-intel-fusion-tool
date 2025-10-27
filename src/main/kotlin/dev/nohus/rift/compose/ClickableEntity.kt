package dev.nohus.rift.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import dev.nohus.rift.clipboard.Clipboard
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.contacts.ContactsExternalControl
import dev.nohus.rift.contacts.ContactsRepository
import dev.nohus.rift.contacts.ContactsRepository.EntityType
import dev.nohus.rift.di.koin
import dev.nohus.rift.game.AutopilotController
import dev.nohus.rift.game.GameUiController
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.map_marker_place_bookmark
import dev.nohus.rift.generated.resources.menu_add
import dev.nohus.rift.generated.resources.menu_set_destination
import dev.nohus.rift.map.MapExternalControl
import dev.nohus.rift.map.MapViewModel.MapType
import dev.nohus.rift.map.markers.MapMarkersInputModel
import dev.nohus.rift.repositories.ExternalServiceRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.windowing.WindowManager.RiftWindow

@Composable
fun ClickableLocation(
    systemId: Int?,
    locationId: Long?,
    locationTypeId: Int?,
    locationName: String?,
    content: @Composable () -> Unit,
) {
    if (systemId == null) {
        content()
        return
    }
    val repository: SolarSystemsRepository = remember { koin.get() }
    val isKnownSpace = repository.isKnownSpace(systemId)
    RiftContextMenuArea(
        items = GetSystemContextMenuItems(systemId, locationId, locationTypeId, locationName),
    ) {
        val mapExternalControl: MapExternalControl = remember { koin.get() }
        ClickableEntity(
            onClick = {
                if (isKnownSpace) {
                    mapExternalControl.showSystemOnMap(systemId)
                }
            },
            content = content,
        )
    }
}

@Composable
fun ClickableSystem(
    system: String,
    content: @Composable () -> Unit,
) {
    val repository: SolarSystemsRepository = remember { koin.get() }
    val systemId = repository.getSystemId(system) ?: run {
        content()
        return
    }
    ClickableSystem(systemId, content)
}

@Composable
fun ClickableSystem(
    systemId: Int?,
    content: @Composable () -> Unit,
) {
    if (systemId == null) {
        content()
        return
    }
    val repository: SolarSystemsRepository = remember { koin.get() }
    val isKnownSpace = repository.isKnownSpace(systemId)
    RiftContextMenuArea(
        items = GetSystemContextMenuItems(systemId),
    ) {
        val mapExternalControl: MapExternalControl = remember { koin.get() }
        ClickableEntity(
            onClick = {
                if (isKnownSpace) {
                    mapExternalControl.showSystemOnMap(systemId)
                }
            },
            content = content,
        )
    }
}

@Composable
fun GetSystemContextMenuItems(
    systemId: Int?,
    locationId: Long? = null,
    locationTypeId: Int? = null,
    locationName: String? = null,
    mapType: MapType? = null,
): List<ContextMenuItem> {
    if (systemId == null) return emptyList()

    val autopilotController: AutopilotController = remember { koin.get() }
    val mapExternalControl: MapExternalControl = remember { koin.get() }
    val solarSystemsRepository: SolarSystemsRepository = remember { koin.get() }
    val gameUiController: GameUiController = remember { koin.get() }
    val windowManager: WindowManager = remember { koin.get() }
    val settings: Settings = remember { koin.get() }
    val externalServiceRepository: ExternalServiceRepository = remember { koin.get() }
    val system = solarSystemsRepository.getSystem(systemId) ?: return emptyList()
    val isKnownSpace = solarSystemsRepository.isKnownSpace(systemId)
    val isWormholeSpace = !isKnownSpace && solarSystemsRepository.isWormholeSpace(systemId)
    var isSettingAutopilotToAll by remember { mutableStateOf(settings.isSettingAutopilotToAll) }
    return buildList {
        add(
            ContextMenuItem.TextItem(
                text = "Show Info",
                iconContent = { RiftMulticolorIcon(MulticolorIconType.Info, it) },
                onClick = {
                    if (locationId != null && locationTypeId != null) {
                        gameUiController.pushLocation(locationId, locationTypeId, locationName ?: "Location")
                    } else {
                        gameUiController.pushSystem(system)
                    }
                },
            ),
        )
        add(ContextMenuItem.DividerItem)
        add(
            ContextMenuItem.TextItem(
                text = "Set Destination",
                iconResource = Res.drawable.menu_set_destination,
                onClick = {
                    autopilotController.setDestination(locationId ?: systemId.toLong(), systemId)
                },
            ),
        )
        add(
            ContextMenuItem.TextItem(
                text = "Add Waypoint",
                onClick = {
                    autopilotController.addWaypoint(locationId ?: systemId.toLong(), systemId)
                },
            ),
        )
        add(
            ContextMenuItem.TextItem(
                text = "Clear Autopilot",
                onClick = {
                    autopilotController.clearRoute()
                },
            ),
        )
        add(
            ContextMenuItem.CheckboxItem(
                text = "All Characters",
                isSelected = isSettingAutopilotToAll,
                onClick = {
                    isSettingAutopilotToAll = !isSettingAutopilotToAll
                    settings.isSettingAutopilotToAll = isSettingAutopilotToAll
                },
            ),
        )
        add(ContextMenuItem.DividerItem)
        add(
            ContextMenuItem.TextItem(
                text = "Copy Name",
                onClick = {
                    Clipboard.copy(system.name)
                },
            ),
        )
        add(
            ContextMenuItem.TextItem(
                text = "Add Marker",
                iconResource = Res.drawable.map_marker_place_bookmark,
                onClick = {
                    val inputModel = MapMarkersInputModel.AddToSystem(systemId)
                    windowManager.onWindowOpen(RiftWindow.MapMarkers, inputModel)
                },
            ),
        )
        if (isKnownSpace) {
            if (mapType == null) {
                add(
                    ContextMenuItem.TextItem(
                        text = "Show on Map",
                        onClick = {
                            mapExternalControl.showSystemOnMap(systemId)
                        },
                    ),
                )
            } else {
                if (mapType !is MapType.ClusterSystemsMap) {
                    add(
                        ContextMenuItem.TextItem(
                            text = "Show in New Eden",
                            onClick = {
                                mapExternalControl.showSystemOnNewEdenMap(systemId)
                            },
                        ),
                    )
                }
                if (mapType !is MapType.RegionMap) {
                    add(
                        ContextMenuItem.TextItem(
                            text = "Show in Region",
                            onClick = {
                                mapExternalControl.showSystemOnRegionMap(systemId)
                            },
                        ),
                    )
                }
            }
        }
        add(ContextMenuItem.DividerItem)
        addAll(externalServiceRepository.getSystemMenuItems(system.name, systemId, isWormholeSpace))
    }
}

@Composable
fun ClickableCharacter(
    characterId: Int?,
    content: @Composable () -> Unit,
) {
    if (characterId == null) {
        content()
        return
    }
    val externalServiceRepository: ExternalServiceRepository = remember { koin.get() }
    val gameUiController: GameUiController = remember { koin.get() }
    RiftContextMenuArea(
        buildList {
            add(
                ContextMenuItem.TextItem(
                    text = "Show Info",
                    iconContent = { RiftMulticolorIcon(MulticolorIconType.Info, it) },
                    onClick = { gameUiController.openInfoWindow(characterId) },
                ),
            )
            add(ContextMenuItem.DividerItem)
            addAll(externalServiceRepository.getCharacterMenuItems(characterId))
            add(ContextMenuItem.DividerItem)
            add(getContactMenuItem(characterId, EntityType.Character))
        },
    ) {
        ClickableEntity(
            onClick = {
                externalServiceRepository.openCharacterPreferredService(characterId)
            },
            content = content,
        )
    }
}

@Composable
fun ClickableCorporation(
    corporationId: Int?,
    content: @Composable () -> Unit,
) {
    if (corporationId == null) {
        content()
        return
    }
    val externalServiceRepository: ExternalServiceRepository = remember { koin.get() }
    val gameUiController: GameUiController = remember { koin.get() }
    RiftContextMenuArea(
        buildList {
            add(
                ContextMenuItem.TextItem(
                    text = "Show Info",
                    iconContent = { RiftMulticolorIcon(MulticolorIconType.Info, it) },
                    onClick = { gameUiController.openInfoWindow(corporationId) },
                ),
            )
            add(ContextMenuItem.DividerItem)
            addAll(externalServiceRepository.getCorporationMenuItems(corporationId))
            add(ContextMenuItem.DividerItem)
            add(getContactMenuItem(corporationId, EntityType.Corporation))
        },
    ) {
        ClickableEntity(
            onClick = {
                externalServiceRepository.openCorporationPreferredService(corporationId)
            },
            content = content,
        )
    }
}

@Composable
fun ClickableAlliance(
    allianceId: Int?,
    content: @Composable () -> Unit,
) {
    if (allianceId == null) {
        content()
        return
    }
    val externalServiceRepository: ExternalServiceRepository = remember { koin.get() }
    val gameUiController: GameUiController = remember { koin.get() }
    RiftContextMenuArea(
        buildList {
            add(
                ContextMenuItem.TextItem(
                    text = "Show Info",
                    iconContent = { RiftMulticolorIcon(MulticolorIconType.Info, it) },
                    onClick = { gameUiController.openInfoWindow(allianceId) },
                ),
            )
            add(ContextMenuItem.DividerItem)
            addAll(externalServiceRepository.getAllianceMenuItems(allianceId))
            add(ContextMenuItem.DividerItem)
            add(getContactMenuItem(allianceId, EntityType.Alliance))
        },
    ) {
        ClickableEntity(
            onClick = {
                externalServiceRepository.openAlliancePreferredService(allianceId)
            },
            content = content,
        )
    }
}

@Composable
private fun getContactMenuItem(id: Int, type: EntityType): ContextMenuItem {
    val contactsRepository: ContactsRepository = remember { koin.get() }
    val contactsExternalControl: ContactsExternalControl = remember { koin.get() }
    val onEditContact = {
        contactsExternalControl.editContact(id, type)
    }
    return if (contactsRepository.isCharacterContact(id)) {
        ContextMenuItem.TextItem("Edit Contact", null, onClick = onEditContact)
    } else {
        ContextMenuItem.TextItem("Add Contact", Res.drawable.menu_add, onClick = onEditContact)
    }
}

@Composable
fun ClickableShip(
    type: Type,
    content: @Composable () -> Unit,
) {
    val externalServiceRepository: ExternalServiceRepository = remember { koin.get() }
    val gameUiController: GameUiController = remember { koin.get() }
    RiftContextMenuArea(
        buildList {
            add(
                ContextMenuItem.TextItem(
                    text = "Show Info",
                    iconContent = { RiftMulticolorIcon(MulticolorIconType.Info, it) },
                    onClick = { gameUiController.pushType(type, "ship") },
                ),
            )
            add(ContextMenuItem.DividerItem)
            addAll(externalServiceRepository.getShipMenuItems(type))
        },
    ) {
        ClickableEntity(
            onClick = {
                externalServiceRepository.openShipPreferredService(type)
            },
            content = content,
        )
    }
}

@Composable
fun ClickableType(
    type: Type,
    content: @Composable () -> Unit,
) {
    val externalServiceRepository: ExternalServiceRepository = remember { koin.get() }
    val gameUiController: GameUiController = remember { koin.get() }
    RiftContextMenuArea(
        buildList {
            add(
                ContextMenuItem.TextItem(
                    text = "Show Info",
                    iconContent = { RiftMulticolorIcon(MulticolorIconType.Info, it) },
                    onClick = { gameUiController.pushType(type, "type") },
                ),
            )
            add(ContextMenuItem.DividerItem)
            addAll(externalServiceRepository.getTypeMenuItems(type))
        },
    ) {
        ClickableEntity(
            onClick = {
                externalServiceRepository.openTypePreferredService(type)
            },
            content = content,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClickableEntity(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .pointerHoverIcon(PointerIcon(Cursors.pointerDropdown))
            .onClick { onClick() },
    ) {
        content()
    }
}
