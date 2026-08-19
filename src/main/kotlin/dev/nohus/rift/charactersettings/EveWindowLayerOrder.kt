package dev.nohus.rift.charactersettings

import dev.nohus.rift.charactersettings.io.ReadCharacterSettingsUseCase.CharacterSettings

internal fun getEveReopenedWindowLayerOrder(settings: CharacterSettings): List<String> {
    val settingsOrder = settings.windowSizesAndPositions.keys
        .withIndex()
        .associate { (index, window) -> window to index }

    return settings.windowSizesAndPositions.keys.sortedWith(
        compareBy<String> { window ->
            val children = settings.windowStacks
                .filter { it.second == window }
                .map { it.first }
            (children + window).maxOfOrNull { eveSessionChangeWindowOpeningIndex[it] ?: -1 } ?: -1
        }.thenBy { window ->
            settingsOrder.getValue(window)
        }
    )
}

private val eveSessionChangeWindowOpeningOrder = listOf(
    // All states
    "bytes:mail",
    "bytes:walletWindow",
    "bytes:assets",
    "bytes:XmppChatChannels",
    "bytes:journal",
    "bytes:logger",
    "bytes:charactersheet",
    "bytes:addressbook",
    "bytes:locations",
    "bytes:market",
    "bytes:notepad",
    "bytes:standaloneBookmarkWnd",

    // Docked
    "bytes:InventoryStation",
    "bytes:InventoryStructure",
    "bytes:StationItems",
    "bytes:StationShips",
    "bytes:StationCorpDeliveries",
    "bytes:CorporationItemDeliveries",
    "bytes:StationCorpHangars",
    "bytes:StationCorpHangar_0",
    "bytes:StationCorpHangar_1",
    "bytes:StationCorpHangar_2",
    "bytes:StationCorpHangar_3",
    "bytes:StationCorpHangar_4",
    "bytes:StationCorpHangar_5",
    "bytes:StationCorpHangar_6",
    "bytes:StationCorpHangar_14",
    "bytes:StructureCorpHangars",
    "bytes:StructureCorpHangar_0",
    "bytes:StructureCorpHangar_1",
    "bytes:StructureCorpHangar_2",
    "bytes:StructureCorpHangar_3",
    "bytes:StructureCorpHangar_4",
    "bytes:StructureCorpHangar_5",
    "bytes:StructureCorpHangar_6",
    "bytes:StructureCorpHangar_14",

    // In-space
    "bytes:InventorySpace",
    "bytes:MoonScanner",
    "bytes:directionalScannerWindow",
    "bytes:probeScannerWindow",
    "bytes:solar_system_map_panel",

    // Last
    "bytes:lobbyWnd",
)

private val eveSessionChangeWindowOpeningIndex = eveSessionChangeWindowOpeningOrder
    .withIndex()
    .associate { (index, window) -> window to index }
