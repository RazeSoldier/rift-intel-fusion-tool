package dev.nohus.rift.compose

import dev.nohus.rift.clipboard.Clipboard
import dev.nohus.rift.contacts.ContactsExternalControl
import dev.nohus.rift.contacts.ContactsRepository
import dev.nohus.rift.contacts.ContactsRepository.EntityType
import dev.nohus.rift.game.AutopilotController
import dev.nohus.rift.game.GameUiController
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.*
import dev.nohus.rift.i18n.getStringSync
import dev.nohus.rift.map.MapExternalControl
import dev.nohus.rift.map.MapViewModel.MapType
import dev.nohus.rift.map.markers.MapMarkersInputModel
import dev.nohus.rift.repositories.ExternalServiceRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.windowing.WindowManager
import org.koin.core.annotation.Single

@Single
class EntityInteractionProvider(
    val externalServiceRepository: ExternalServiceRepository,
    val gameUiController: GameUiController,
    val contactsRepository: ContactsRepository,
    val contactsExternalControl: ContactsExternalControl,
    val autopilotController: AutopilotController,
    val mapExternalControl: MapExternalControl,
    val solarSystemsRepository: SolarSystemsRepository,
    val windowManager: WindowManager,
    val settings: Settings,
) {

    data class Interaction(
        val onClick: () -> Unit,
        val contextMenuItems: List<ContextMenuItem>,
    )

    fun getCharacter(
        characterId: Int,
    ): Interaction {
        val contextMenuItems = buildList {
            add(
                ContextMenuItem.TextItem(
                    text = getStringSync(Res.string.clickable_entity_show_info),
                    iconContent = { RiftMulticolorIcon(MulticolorIconType.Info, it) },
                    onClick = { gameUiController.openInfoWindow(characterId) },
                ),
            )
            add(ContextMenuItem.DividerItem)
            addAll(externalServiceRepository.getCharacterMenuItems(characterId))
            add(ContextMenuItem.DividerItem)
            add(getContactMenuItem(characterId, EntityType.Character))
        }
        return Interaction(
            onClick = {
                externalServiceRepository.openCharacterPreferredService(characterId)
            },
            contextMenuItems = contextMenuItems,
        )
    }

    fun getLocation(
        systemId: Int,
        locationId: Long? = null,
        locationTypeId: Int? = null,
        locationName: String? = null,
        mapType: MapType? = null,
    ): Interaction {
        val isKnownSpace = solarSystemsRepository.isKnownSpace(systemId)
        return Interaction(
            onClick = {
                if (isKnownSpace) {
                    mapExternalControl.showSystemOnMap(systemId)
                }
            },
            contextMenuItems = getSystemContextMenuItems(systemId, locationId, locationTypeId, locationName, mapType),
        )
    }

    fun getCorporation(
        corporationId: Int,
    ): Interaction {
        val contextMenuItems = buildList {
            add(
                ContextMenuItem.TextItem(
                    text = getStringSync(Res.string.clickable_entity_show_info),
                    iconContent = { RiftMulticolorIcon(MulticolorIconType.Info, it) },
                    onClick = { gameUiController.openInfoWindow(corporationId) },
                ),
            )
            add(ContextMenuItem.DividerItem)
            addAll(externalServiceRepository.getCorporationMenuItems(corporationId))
            add(ContextMenuItem.DividerItem)
            add(getContactMenuItem(corporationId, EntityType.Corporation))
        }
        return Interaction(
            onClick = {
                externalServiceRepository.openCorporationPreferredService(corporationId)
            },
            contextMenuItems = contextMenuItems,
        )
    }

    fun getAlliance(
        allianceId: Int,
    ): Interaction {
        val contextMenuItems = buildList {
            add(
                ContextMenuItem.TextItem(
                    text = getStringSync(Res.string.clickable_entity_show_info),
                    iconContent = { RiftMulticolorIcon(MulticolorIconType.Info, it) },
                    onClick = { gameUiController.openInfoWindow(allianceId) },
                ),
            )
            add(ContextMenuItem.DividerItem)
            addAll(externalServiceRepository.getAllianceMenuItems(allianceId))
            add(ContextMenuItem.DividerItem)
            add(getContactMenuItem(allianceId, EntityType.Alliance))
        }
        return Interaction(
            onClick = {
                externalServiceRepository.openAlliancePreferredService(allianceId)
            },
            contextMenuItems = contextMenuItems,
        )
    }

    fun getShip(
        type: Type,
    ): Interaction {
        val contextMenuItems = buildList {
            add(
                ContextMenuItem.TextItem(
                    text = getStringSync(Res.string.clickable_entity_show_info),
                    iconContent = { RiftMulticolorIcon(MulticolorIconType.Info, it) },
                    onClick = { gameUiController.pushType(type, "ship") },
                ),
            )
            add(ContextMenuItem.DividerItem)
            addAll(externalServiceRepository.getShipMenuItems(type))
        }
        return Interaction(
            onClick = {
                externalServiceRepository.openShipPreferredService(type)
            },
            contextMenuItems = contextMenuItems,
        )
    }

    fun getType(
        type: Type,
        singletonId: Long? = null,
    ): Interaction {
        val contextMenuItems = buildList {
            add(
                ContextMenuItem.TextItem(
                    text = getStringSync(Res.string.clickable_entity_show_info),
                    iconContent = { RiftMulticolorIcon(MulticolorIconType.Info, it) },
                    onClick = { gameUiController.pushType(type, "type", singletonId) },
                ),
            )
            add(ContextMenuItem.DividerItem)
            addAll(externalServiceRepository.getTypeMenuItems(type))
        }
        return Interaction(
            onClick = {
                externalServiceRepository.openTypePreferredService(type)
            },
            contextMenuItems = contextMenuItems,
        )
    }

    private fun getSystemContextMenuItems(
        systemId: Int?,
        locationId: Long?,
        locationTypeId: Int?,
        locationName: String?,
        mapType: MapType?,
    ): List<ContextMenuItem> {
        if (systemId == null) return emptyList()

        val system = solarSystemsRepository.getSystem(systemId) ?: return emptyList()
        val isKnownSpace = solarSystemsRepository.isKnownSpace(systemId)
        val isWormholeSpace = !isKnownSpace && solarSystemsRepository.isWormholeSpace(systemId)
        return buildList {
            add(
                ContextMenuItem.TextItem(
                    text = getStringSync(Res.string.clickable_entity_show_info),
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
                    text = getStringSync(Res.string.clickable_entity_set_destination),
                    iconResource = Res.drawable.menu_set_destination,
                    onClick = {
                        autopilotController.setDestination(locationId ?: systemId.toLong(), systemId)
                    },
                ),
            )
            add(
                ContextMenuItem.TextItem(
                    text = getStringSync(Res.string.clickable_entity_add_waypoint),
                    onClick = {
                        autopilotController.addWaypoint(locationId ?: systemId.toLong(), systemId)
                    },
                ),
            )
            add(
                ContextMenuItem.TextItem(
                    text = getStringSync(Res.string.clickable_entity_clear_autopilot),
                    onClick = {
                        autopilotController.clearRoute()
                    },
                ),
            )
            add(
                ContextMenuItem.CheckboxItemWithInternalState(
                    text = getStringSync(Res.string.clickable_entity_all_characters),
                    isSelected = {
                        settings.isSettingAutopilotToAll
                    },
                    onClick = {
                        settings.isSettingAutopilotToAll = !it
                    },
                ),
            )
            add(ContextMenuItem.DividerItem)
            add(
                ContextMenuItem.TextItem(
                    text = getStringSync(Res.string.clickable_entity_copy_name),
                    onClick = {
                        Clipboard.copy(system.name)
                    },
                ),
            )
            add(
                ContextMenuItem.TextItem(
                    text = getStringSync(Res.string.clickable_entity_add_marker),
                    iconResource = Res.drawable.map_marker_place_bookmark,
                    onClick = {
                        val inputModel = MapMarkersInputModel.AddToSystem(systemId)
                        windowManager.onWindowOpen(WindowManager.RiftWindow.MapMarkers, inputModel)
                    },
                ),
            )
            if (isKnownSpace) {
                if (mapType == null) {
                    add(
                        ContextMenuItem.TextItem(
                            text = getStringSync(Res.string.clickable_entity_show_on_map),
                            onClick = {
                                mapExternalControl.showSystemOnMap(systemId)
                            },
                        ),
                    )
                } else {
                    if (mapType !is MapType.ClusterSystemsMap) {
                        add(
                            ContextMenuItem.TextItem(
                                text = getStringSync(Res.string.clickable_entity_show_in_new_eden),
                                onClick = {
                                    mapExternalControl.showSystemOnNewEdenMap(systemId)
                                },
                            ),
                        )
                    }
                    if (mapType !is MapType.RegionMap) {
                        add(
                            ContextMenuItem.TextItem(
                                text = getStringSync(Res.string.clickable_entity_show_in_region),
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

    private fun getContactMenuItem(id: Int, type: EntityType): ContextMenuItem {
        val onEditContact = {
            contactsExternalControl.editContact(id, type)
        }
        return if (contactsRepository.isCharacterContact(id)) {
            ContextMenuItem.TextItem(getStringSync(Res.string.clickable_entity_edit_contact), null, onClick = onEditContact)
        } else {
            ContextMenuItem.TextItem(getStringSync(Res.string.clickable_entity_add_contact), Res.drawable.menu_add, onClick = onEditContact)
        }
    }
}
