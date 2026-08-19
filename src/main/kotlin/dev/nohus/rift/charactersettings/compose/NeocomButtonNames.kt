package dev.nohus.rift.charactersettings.compose

data class NeocomButtonDefinition(
    val id: String,
    val name: String,
    val iconPath: String,
    val btnType: Int = 1,
    val isAddable: Boolean = true,
)

val NeocomButtonDefinitions = listOf(
    NeocomButtonDefinition("job_board", "Opportunities", windowIcon("opportunities.png")),
    NeocomButtonDefinition("journal", "Journal", windowIcon("journal.png")),
    NeocomButtonDefinition(
        id = "airCareerProgram",
        name = "AIR Career Program",
        iconPath = windowIcon("airCareerProgram.png"),
        btnType = 21,
    ),
    NeocomButtonDefinition("agency", "The Agency", windowIcon("theAgency.png")),
    NeocomButtonDefinition("map_beta", "Map", windowIcon("map.png")),
    NeocomButtonDefinition(
        id = "ProjectDiscovery",
        name = "Project Discovery",
        iconPath = windowIcon("projectdiscovery.png"),
    ),
    NeocomButtonDefinition("militia", "Factional Warfare", windowIcon("factionalwarfare.png")),
    NeocomButtonDefinition("fwEnlistmentWnd", "Factional Warfare Enlistment", windowIcon("factionalwarfare.png")),
    NeocomButtonDefinition("insurgentsDashboard", "Insurgency Dashboard", windowIcon("insurgencies.png")),
    NeocomButtonDefinition("bountyoffice", "Bounty Office", windowIcon("bountyoffice.png")),
    NeocomButtonDefinition("pvp_filament_event_window", "The Proving Grounds", windowIcon("provingGrounds.png")),
    NeocomButtonDefinition("market", "Regional Market", windowIcon("market.png")),
    NeocomButtonDefinition("marketOrders", "Market Orders", windowIcon("marketOrders.png")),
    NeocomButtonDefinition("wallet", "Wallet", windowIcon("wallet.png")),
    NeocomButtonDefinition("contracts", "Contracts", windowIcon("contracts.png")),
    NeocomButtonDefinition("multibuy", "Multibuy", texture("classes/MultiSell/multiBuy.png")),
    NeocomButtonDefinition("raffle_window", "HyperNet Relay", windowIcon("hypernet.png")),
    NeocomButtonDefinition("industry", "Industry", windowIcon("Industry.png")),
    NeocomButtonDefinition("planets", "Planetary Industry", windowIcon("planets.png")),
    NeocomButtonDefinition(
        id = "mercenary_den",
        name = "My Mercenary Dens",
        iconPath = texture("eveicon/product_icons/mercenary_den_product_icon_64px.png"),
    ),
    NeocomButtonDefinition("ledger", "Mining Ledger", windowIcon("miningLedger.png")),
    NeocomButtonDefinition("inventory", "Inventory", windowIcon("items.png"), btnType = 4),
    NeocomButtonDefinition("assets", "Personal Assets", windowIcon("assets.png")),
    NeocomButtonDefinition("charactersheet", "Character Sheet", windowIcon("charactersheet.png")),
    NeocomButtonDefinition("locations", "Locations", windowIcon("locations.png")),
    NeocomButtonDefinition("fitting", "Fitting", windowIcon("fitting.png")),
    NeocomButtonDefinition("shipTree", "Ship Tree", windowIcon("ISIS.png")),
    NeocomButtonDefinition("fittingMgmt", "Fitting Management", windowIcon("fittingManagement.png")),
    NeocomButtonDefinition("chatchannels", "Chat Channels", windowIcon("chatchannels.png")),
    NeocomButtonDefinition("fleet", "Fleet", windowIcon("fleet.png")),
    NeocomButtonDefinition("mail", "Mail", windowIcon("evemail.png")),
    NeocomButtonDefinition("corporation", "Corporation", windowIcon("corporation.png")),
    NeocomButtonDefinition("calendar", "Calendar", windowIcon("calendar.png")),
    NeocomButtonDefinition("accessgroups", "Access Lists", windowIcon("accessGroups.png")),
    NeocomButtonDefinition("addressbook", "Contacts", windowIcon("peopleandplaces.png")),
    NeocomButtonDefinition("ship_skinr", "Ship SKINR", windowIcon("paint_tool.png")),
    NeocomButtonDefinition("notepad", "Notepad", windowIcon("notepad.png")),
    NeocomButtonDefinition("compareTool", "Compare Tool", windowIcon("comparetool.png")),
    NeocomButtonDefinition("log", "Log and Messages", windowIcon("log.png")),
    NeocomButtonDefinition("structurebrowser", "Structure Browser", windowIcon("structureBrowser.png")),
    NeocomButtonDefinition("calculator", "Calculator", windowIcon("calculator.png")),
    NeocomButtonDefinition("pointerWnd", "Pointer Window", windowIcon("UIHelper.png")),
    NeocomButtonDefinition("aurumStore", "New Eden Store", windowIcon("NES.png")),
    NeocomButtonDefinition("PLEXvault", "PLEX Vault", texture("Plex/plex_128_gradient_white.png")),
    NeocomButtonDefinition("omega_upsell", "Omega Subscription", windowIcon("upsell.png")),
    NeocomButtonDefinition("help", "EVE Help", windowIcon("help.png")),
    NeocomButtonDefinition("auraguidance", "Aura Guidance", texture("classes/careerPortal/aura/aura_neocom_64.png")),
    NeocomButtonDefinition("settings", "Settings", windowIcon("settings.png")),
    NeocomButtonDefinition("logoff", "Log off", windowIcon("logOut.png")),
    NeocomButtonDefinition("quitGame", "Quit Game", windowIcon("quitGame.png")),
    NeocomButtonDefinition(
        id = "chat",
        name = "Chat",
        iconPath = windowIcon("chatchannel.png"),
        btnType = 10,
    ),
    NeocomButtonDefinition(
        id = "theaters_of_war",
        name = "Theaters of War",
        iconPath = texture("eveicon/product_icons/theaters_of_war_product_64px.png"),
        isAddable = false,
    ),
    NeocomButtonDefinition("achievements", "Achievements", windowIcon("achievements.png"), isAddable = false),
)

val NeocomButtonDefinitionsById = NeocomButtonDefinitions.associateBy { it.id }

private fun windowIcon(filename: String): String = texture("WindowIcons/$filename")

private fun texture(path: String): String = "bytes:res:/UI/Texture/$path"
