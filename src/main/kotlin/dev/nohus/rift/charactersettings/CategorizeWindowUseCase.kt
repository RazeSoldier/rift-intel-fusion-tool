package dev.nohus.rift.charactersettings

import dev.nohus.rift.BuildConfig
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.layout_preview_icon_assets
import dev.nohus.rift.generated.resources.layout_preview_icon_charactersheet
import dev.nohus.rift.generated.resources.layout_preview_icon_chatchannels
import dev.nohus.rift.generated.resources.layout_preview_icon_contacts
import dev.nohus.rift.generated.resources.layout_preview_icon_directional_scanner_64px
import dev.nohus.rift.generated.resources.layout_preview_icon_drones
import dev.nohus.rift.generated.resources.layout_preview_icon_evemail
import dev.nohus.rift.generated.resources.layout_preview_icon_fleet
import dev.nohus.rift.generated.resources.layout_preview_icon_grouplist
import dev.nohus.rift.generated.resources.layout_preview_icon_items
import dev.nohus.rift.generated.resources.layout_preview_icon_journal
import dev.nohus.rift.generated.resources.layout_preview_icon_locations
import dev.nohus.rift.generated.resources.layout_preview_icon_log
import dev.nohus.rift.generated.resources.layout_preview_icon_map
import dev.nohus.rift.generated.resources.layout_preview_icon_market
import dev.nohus.rift.generated.resources.layout_preview_icon_moondrill
import dev.nohus.rift.generated.resources.layout_preview_icon_notepad
import dev.nohus.rift.generated.resources.layout_preview_icon_overview
import dev.nohus.rift.generated.resources.layout_preview_icon_probe_scan
import dev.nohus.rift.generated.resources.layout_preview_icon_selected_object
import dev.nohus.rift.generated.resources.layout_preview_icon_station
import dev.nohus.rift.generated.resources.layout_preview_icon_wallet
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.compose.resources.DrawableResource
import org.koin.core.annotation.Single


private val logger = KotlinLogging.logger {}

@Single
class CategorizeWindowUseCase {

    data class EveWindow(
        val name: String,
        val inSpace: Boolean = true,
        val inStation: Boolean = true,
        val inStructure: Boolean = true,
        val isUnknown: Boolean = false,
        /**
         * Windows that EVE recreates on startup or session change, either from openWindows or another system.
         */
        val isPersistent: Boolean = false,
        /**
         * Whether EVE uses the openWindows setting to decide if this window should be opened.
         */
        val isOpenStateControlled: Boolean = true,
        /**
         * Whether the window can be closed. Some windows are always created by EVE for the current context.
         */
        val isCloseable: Boolean = true,
        /**
         * Whether RIFT can offer to add the window to openWindows.
         */
        val isAddable: Boolean = true,
        /**
         * EVE only restores the saved minimized state for chat windows and chat window stacks.
         */
        val isMinimizedStatePersistent: Boolean = false,
        val icon: DrawableResource? = null,
    )

    val persistentWindows = mapOf(
        "bytes:overview" to EveWindow("Overview", inStation = false, inStructure = false, isPersistent = true, isOpenStateControlled = false, isCloseable = false, icon = Res.drawable.layout_preview_icon_overview),
        "bytes:selecteditemview" to EveWindow("Selected Object", inStation = false, inStructure = false, isPersistent = true, isOpenStateControlled = false, isCloseable = false, icon = Res.drawable.layout_preview_icon_selected_object),
        "bytes:lobbyWnd" to EveWindow("Station Services", inSpace = false, isPersistent = true, isOpenStateControlled = false, isCloseable = false, icon = Res.drawable.layout_preview_icon_station),
        "bytes:InventoryStructure" to EveWindow("Structure Inventory", inSpace = false, inStation = false, isPersistent = true, icon = Res.drawable.layout_preview_icon_items),
        "bytes:InventoryStation" to EveWindow("Station Inventory", inSpace = false, inStructure = false, isPersistent = true, icon = Res.drawable.layout_preview_icon_items),
        "bytes:InventorySpace" to EveWindow("Inventory", inStation = false, inStructure = false, isPersistent = true, icon = Res.drawable.layout_preview_icon_items),
        "bytes:StationItems" to EveWindow("Item Hangar", inSpace = false, isPersistent = true, icon = Res.drawable.layout_preview_icon_items),
        "bytes:StationShips" to EveWindow("Ship Hangar", inSpace = false, isPersistent = true, icon = Res.drawable.layout_preview_icon_items),
        "bytes:StationCorpDeliveries" to EveWindow("Corporation Deliveries", inSpace = false, isPersistent = true, isAddable = false, icon = Res.drawable.layout_preview_icon_items),
        "bytes:CorporationItemDeliveries" to EveWindow("Capsuleer Deliveries", inSpace = false, isPersistent = true, icon = Res.drawable.layout_preview_icon_items),
        "bytes:StationCorpHangars" to EveWindow("Corporation Hangars", inSpace = false, inStructure = false, isPersistent = true, isAddable = false, icon = Res.drawable.layout_preview_icon_items),
        "bytes:StructureCorpHangars" to EveWindow("Corporation Hangars", inSpace = false, inStation = false, isPersistent = true, isAddable = false, icon = Res.drawable.layout_preview_icon_items),
        "bytes:fleetwindow" to EveWindow("Fleet", isPersistent = true, isOpenStateControlled = false, isCloseable = false, icon = Res.drawable.layout_preview_icon_fleet),
        "bytes:watchlistpanel" to EveWindow("Fleet Watchlist", isPersistent = true, isOpenStateControlled = false, isCloseable = false, icon = Res.drawable.layout_preview_icon_grouplist),
        "bytes:chatchannel_alliance" to EveWindow("Alliance Chat", isPersistent = true, isMinimizedStatePersistent = true, icon = Res.drawable.layout_preview_icon_chatchannels),
        "bytes:chatchannel_corp" to EveWindow("Corp Chat", isPersistent = true, isMinimizedStatePersistent = true, icon = Res.drawable.layout_preview_icon_chatchannels),
        "bytes:chatchannel_faction" to EveWindow("Faction Chat", isPersistent = true, isMinimizedStatePersistent = true, icon = Res.drawable.layout_preview_icon_chatchannels),
        "bytes:chatchannel_local" to EveWindow("Local Chat", isPersistent = true, isMinimizedStatePersistent = true, icon = Res.drawable.layout_preview_icon_chatchannels),
        "bytes:XmppChatChannels" to EveWindow("Chat Channels", isPersistent = true, icon = Res.drawable.layout_preview_icon_chatchannels),
        "bytes:droneview" to EveWindow("Drones", inStation = false, inStructure = false, isPersistent = true, isOpenStateControlled = false, isCloseable = false, icon = Res.drawable.layout_preview_icon_drones),
        "bytes:MoonScanner" to EveWindow("Moon Scanner", inStation = false, inStructure = false, isPersistent = true, icon = Res.drawable.layout_preview_icon_moondrill),
        "bytes:addressbook" to EveWindow("Contacts", isPersistent = true, icon = Res.drawable.layout_preview_icon_contacts),
        "bytes:standaloneBookmarkWnd" to EveWindow("System Bookmarks", inStation = false, inStructure = false, isPersistent = true, icon = Res.drawable.layout_preview_icon_locations),
        "bytes:probeScannerWindow" to EveWindow("Probe Scanner", inStation = false, inStructure = false, isPersistent = true, icon = Res.drawable.layout_preview_icon_probe_scan),
        "bytes:directionalScannerWindow" to EveWindow("Directional Scanner", inStation = false, inStructure = false, isPersistent = true, icon = Res.drawable.layout_preview_icon_directional_scanner_64px),
        "bytes:solar_system_map_panel" to EveWindow("Solar System Map", inStation = false, inStructure = false, isPersistent = true, icon = Res.drawable.layout_preview_icon_map),
        "bytes:market" to EveWindow("Market", isPersistent = true, icon = Res.drawable.layout_preview_icon_market),
        "bytes:locations" to EveWindow("Locations", isPersistent = true, icon = Res.drawable.layout_preview_icon_locations),
        "bytes:assets" to EveWindow("Assets", isPersistent = true, icon = Res.drawable.layout_preview_icon_assets),
        "bytes:notepad" to EveWindow("Notepad", isPersistent = true, icon = Res.drawable.layout_preview_icon_notepad),
        "bytes:charactersheet" to EveWindow("Character Sheet", isPersistent = true, icon = Res.drawable.layout_preview_icon_charactersheet),
        "bytes:walletWindow" to EveWindow("Wallet", isPersistent = true, icon = Res.drawable.layout_preview_icon_wallet),
        "bytes:mail" to EveWindow("Mail", isPersistent = true, icon = Res.drawable.layout_preview_icon_evemail),
        "bytes:journal" to EveWindow("Journal", isPersistent = true, icon = Res.drawable.layout_preview_icon_journal),
        "bytes:logger" to EveWindow("Log and Messages", isPersistent = true, icon = Res.drawable.layout_preview_icon_log),
    ) + corporationHangarDivisions.associate { division ->
        "bytes:StationCorpHangar_$division" to EveWindow(
            name = division.corporationHangarName,
            inSpace = false,
            inStructure = false,
            isPersistent = true,
            isAddable = false,
            icon = Res.drawable.layout_preview_icon_items,
        )
    } + corporationHangarDivisions.associate { division ->
        "bytes:StructureCorpHangar_$division" to EveWindow(
            name = division.corporationHangarName,
            inSpace = false,
            inStation = false,
            isPersistent = true,
            isAddable = false,
            icon = Res.drawable.layout_preview_icon_items,
        )
    }

    operator fun invoke(window: String, children: List<String>, joinedChatChannels: Map<String, String>): EveWindow {
        persistentWindows[window]?.let { return it }
        return when (window) {
            "bytes:tradeWnd" -> EveWindow("Trade")
            "bytes:StructureBrowser" -> EveWindow("Structure Browser")
            "bytes:TaskConversationWindow" -> EveWindow("Task Conversation")
            "bytes:CovidWindow" -> EveWindow("Project Discovery")
            "bytes:MultiBuy" -> EveWindow("Multi Buy")
            "bytes:SurveyScanView" -> EveWindow("Survey Scanner")
            "bytes:HackingWindow" -> EveWindow("Hacking")
            "bytes:help" -> EveWindow("Help")
            "bytes:outstandingcalls" -> EveWindow("Outstanding Calls")
            "bytes:BugReportingWindow" -> EveWindow("Bug Report")
            "bytes:bookmarkLocationWindow" -> EveWindow("Bookmark Location")
            "bytes:CrateRedeemedWindow" -> EveWindow("Redeem Crate")
            "bytes:myMercDensWnd" -> EveWindow("Mercenary Dens")
            "bytes:marketmodifyaction" -> EveWindow("Modify Market Order")
            "bytes:SellItemsWindow" -> EveWindow("Sell Items")
            "bytes:PreviewSubSystems" -> EveWindow("Preview Subsystems")
            "bytes:sovHubHackingResultWnd" -> EveWindow("Sov Hub Hacking Result")
            "utf8:TemplatePreviewWindow" -> EveWindow("PI Template Preview")
            "bytes:missingSkillbooksWnd" -> EveWindow("Missing Skillbooks")
            "bytes:RedeemActionMessage" -> EveWindow("Redeem Action")
            "bytes:theme_edit_window" -> EveWindow("Edit Theme")
            "bytes:ship_name_dialog" -> EveWindow("Ship Name Dialog")
            "bytes:enterShipPassword" -> EveWindow("Ship Password Dialog")
            "bytes:PlanetPinWindow" -> EveWindow("PI Building Details")
            "bytes:marketbuyaction" -> EveWindow("Market Buy")
            "bytes:PlanetSurvey" -> EveWindow("Planet Survey")
            "bytes:SkinNameDialogue" -> EveWindow("Skin Name Dialog")
            "bytes:itemTraderDialogPopup" -> EveWindow("Item Trader Popup")
            "bytes:CrateWindow" -> EveWindow("Crate")
            "bytes:DisconnectNotice" -> EveWindow("Disconnect Notice")
            "bytes:comfirmPIexport" -> EveWindow("PI Export Confirmation")
            "bytes:createcontract" -> EveWindow("Create Contract")
            "bytes:contractEndpointSearch" -> EveWindow("Contract Endpoint Search")
            "bytes:skill_requirement_dialog" -> EveWindow("Skill Requirement Dialog")
            "bytes:InsuranceTermsWindow" -> EveWindow("Insurance Terms")
            "bytes:setNewName" -> EveWindow("Set New Name")
            "bytes:DeliverToStructure" -> EveWindow("Deliver to Structure")
            "bytes:SpaceComponentInventory" -> EveWindow("Space Component Inventory")
            "bytes:contractFinishStepSearch" -> EveWindow("Contract Search Finish")
            "bytes:setQuantityPopup" -> EveWindow("Set Quantity Popup")
            "bytes:askLink" -> EveWindow("Ask Link")
            "bytes:message" -> EveWindow("Message")
            "bytes:MonitorWnd" -> EveWindow("Monitor")
            "bytes:select_jobboard_claim_location" -> EveWindow("Select Job Claim Location")
            "bytes:SkillPlanPanelWnd" -> EveWindow("Skill Plan")
            "bytes:PortraitWindow" -> EveWindow("Portrait")
            "bytes:AutopilotSettings" -> EveWindow("Autopilot Settings")
            "bytes:containerContentWindow" -> EveWindow("Container Contents")
            "bytes:NewMessageWindow" -> EveWindow("New Message")
            "bytes:careerPortal" -> EveWindow("Career Portal")
            "bytes:buyAllMessageBox" -> EveWindow("Buy All")
            "bytes:moreParameterValues" -> EveWindow("More Parameter Values")
            "bytes:ChatFilterSettings" -> EveWindow("Chat Filter Settings")
            "bytes:AgencyWndNew" -> EveWindow("Agency")
            "bytes:ApplySkillPointsWindow" -> EveWindow("Apply Skill Points")
            "bytes:mySearch" -> EveWindow("Search")
            "bytes:shipInfoWindow" -> EveWindow("Ship Info")
            "bytes:calendarEventWnd" -> EveWindow("Calendar Event")
            "bytes:contractdetails" -> EveWindow("Contract Details")
            "bytes:AssetSafetyDeliverWindow" -> EveWindow("Asset Safety")
            "bytes:AchievementsWindow" -> EveWindow("Achievements")
            "bytes:probeScannerFilterEditor" -> EveWindow("Probe Scanner Filter")
            "bytes:previewCharacterWnd" -> EveWindow("Character Preview")
            "bytes:ShipDroneBay" -> EveWindow("Ship Drone Bay")
            "bytes:ledger" -> EveWindow("Ledger")
            "bytes:MapCmdWindow" -> EveWindow("Map Commands")
            "bytes:KillReportWnd" -> EveWindow("Kill Report")
            "bytes:telecom" -> EveWindow("Communications")
            "bytes:typecompare" -> EveWindow("Compare Types")
            "bytes:ManageLabelscontact" -> EveWindow("Manage Contact Labels")
            "bytes:industryWnd" -> EveWindow("Industry")
            "bytes:MemoryMonitor" -> EveWindow("Memory Monitor")
            "bytes:RegisterFleetWindow" -> EveWindow("Register Fleet")
            "bytes:dropboxWnd" -> EveWindow("Drop Box")
            "bytes:FleetJoinRequestWindow" -> EveWindow("Fleet Join Request")
            "bytes:HideMessageSettingWindow" -> EveWindow("Hide Message Settings")
            "bytes:EntityWindow" -> EveWindow("Entity")
            "bytes:ExpertSystemFanfareWindow" -> EveWindow("Expert System Fanfare")
            "bytes:warDeclareWnd" -> EveWindow("War Declaration")
            "bytes:fwEnlistmentWnd" -> EveWindow("Factional Warfare Enlistment")
            "bytes:lpstore" -> EveWindow("Loyalty Point Store")
            "bytes:RandomJumpActivationWindow" -> EveWindow("Random Jump")
            "bytes:FleetComposition" -> EveWindow("Fleet Composition")
            "bytes:ViewFitting" -> EveWindow("View Fitting")
            "bytes:PCOwnerPickerDialog" -> EveWindow("Owner Picker Dialog")
            "bytes:ship_restrictions" -> EveWindow("Ship Restrictions")
            "bytes:mapbrowser" -> EveWindow("Map Browser")
            "bytes:mooonminingScheduling" -> EveWindow("Moon Mining Schedule")
            "bytes:OverviewTabColumnConfig" -> EveWindow("Overview Column Configuration")
            "bytes:AuraGuidanceWindow" -> EveWindow("Aura Guidance")
            "bytes:overviewsettings" -> EveWindow("Overview Settings")
            "bytes:SkillPlanner" -> EveWindow("Skill Planner")
            "bytes:contracts" -> EveWindow("Contracts")
            "bytes:ShipSKINRWindow" -> EveWindow("SKINR")
            "bytes:EditMemberDialog" -> EveWindow("Edit Member")
            "bytes:ProjectDiscoveryDialogPopup" -> EveWindow("Project Discovery Popup")
            "bytes:calendar" -> EveWindow("Calendar")
            "bytes:compression_window" -> EveWindow("Compression")
            "bytes:previewWnd" -> EveWindow("Preview")
            "bytes:MultiLoginBlockedWindow" -> EveWindow("Multiple Login Blocked")
            "bytes:rewardsWnd" -> EveWindow("Rewards")
            "bytes:insurance" -> EveWindow("Insurance")
            "bytes:ShipTree" -> EveWindow("Ship Tree")
            "bytes:primary_map_panel" -> EveWindow("Map")
            "bytes:PVPFilamentEventWindow" -> EveWindow("PVP Filament Event")
            "bytes:NewFeatureNotifyWnd" -> EveWindow("New Feature")
            "bytes:fwWarzoneDashboard" -> EveWindow("Factional Warfare Warzone")
            "bytes:repairshop" -> EveWindow("Repair Shop")
            "bytes:mailReadingWnd" -> EveWindow("Mail")
            "bytes:broadcastsettings" -> EveWindow("Broadcast Settings")
            "bytes:RaffleWindow" -> EveWindow("Raffle")
            "bytes:EngineTools" -> EveWindow("Engine Tools")
            "bytes:theatersOfWarDashboardWnd" -> EveWindow("Theaters of War")
            "bytes:VoidSpaceActivationWindow" -> EveWindow("Void Space")
            "bytes:multiFitWnd" -> EveWindow("Multi Fit")
            "bytes:PlanetaryImportExportUI" -> EveWindow("Planetary Import Export")
            "bytes:redeem" -> EveWindow("Redeem")
            "bytes:reprocessingWindow" -> EveWindow("Reprocessing")
            "bytes:corporation" -> EveWindow("Corporation")
            "bytes:GiveSharesDialog" -> EveWindow("Give Shares Dialog")
            "bytes:marketOrders" -> EveWindow("Market Orders")
            "bytes:type_list_window" -> EveWindow("Type List")
            "bytes:networkdatamonitor" -> EveWindow("Network Data Monitor")
            "bytes:FittingMgmt" -> EveWindow("Fitting Management")
            "bytes:moon" -> EveWindow("Moon")
            "bytes:StructureShipHangar" -> EveWindow("Structure Ship Hangar")
            "bytes:ScreenshotEditingWnd" -> EveWindow("Screenshot Editor")
            "bytes:LogViewer" -> EveWindow("Log Viewer")
            "bytes:InsurgentsDashboard" -> EveWindow("Insurgency Dashboard")
            "bytes:planetWindow" -> EveWindow("Planetary Industry")
            "bytes:MiningScanResultsWindow2" -> EveWindow("Mining Scan Results")
            "bytes:pointerToolWnd" -> EveWindow("Pointer Tool")
            "bytes:comparison_window" -> EveWindow("Comparison")
            "bytes:fittingWnd" -> EveWindow("Fitting")
            "bytes:itemTrader" -> EveWindow("Item Trader")
            "bytes:job_board" -> EveWindow("Job Board")
            "bytes:ActivityTracker" -> EveWindow("Activity Tracker")
            "bytes:FilterCreationWindow" -> EveWindow("Filter Creation")
            "bytes:createTransfer" -> EveWindow("Create Transfer")
            "bytes:LinkedBookmarkFolderWindow" -> EveWindow("Linked Bookmark Folder")
            "bytes:TransferMoney" -> EveWindow("Transfer Money")
            "bytes:editcorpdetails" -> EveWindow("Edit Corp Details")
            "bytes:ActiveShipCargo" -> EveWindow("Active Ship Cargo")
            "bytes:infowindow" -> EveWindow("Info")
            "bytes:NotifySettingsWindow" -> EveWindow("Notify Settings")
            "bytes:calculator" -> EveWindow("Calculator")
            "bytes:PlexVault" -> EveWindow("Plex Vault")
            "bytes:ItemWreck" -> EveWindow("Wreck")
            "bytes:ItemFloatingCargo" -> EveWindow("Floating Cargo")
            "bytes:StationContainer" -> EveWindow("Station Container")
            "bytes:ShipGeneralMiningHold" -> EveWindow("Ship Mining Hold")
            "bytes:fpsMonitor2" -> EveWindow("FPS Monitor")
            "bytes:ShipCargo" -> EveWindow("Ship Cargo")
            "bytes:ShipPlanetaryCommoditiesHold" -> EveWindow("Ship Planetary Commodities Hold")
            "bytes:ShipColonyResourcesHold" -> EveWindow("Ship Colony Resources Hold")
            "bytes:GroupsWnd" -> EveWindow("Groups")
            "bytes:cargoscanner" -> EveWindow("Cargo Scanner")
            "bytes:NeocomGroupNamePopup" -> EveWindow("Neocom Group Name Popup")
            "bytes:attributerespecification" -> EveWindow("Character Attributes Remap")
            else -> {
                if (window.startsWith("utf8:chatchannel_")) {
                    val id = window.substringAfter("channel_")
                    if (id in joinedChatChannels) {
                        EveWindow(
                            "${joinedChatChannels[id]} Chat",
                            isPersistent = true,
                            isOpenStateControlled = false,
                            isMinimizedStatePersistent = true,
                            icon = Res.drawable.layout_preview_icon_chatchannels,
                        )
                    } else {
                        EveWindow("Chat $id", isPersistent = false)
                    }
                }
                else if (window.startsWith("bytes:contactmanagement")) EveWindow("Contacts Management")
                else if (window.startsWith("bytes:overview_")) EveWindow("Additional Overview", inStation = false, inStructure = false, isPersistent = true, isOpenStateControlled = false, isCloseable = false, icon = Res.drawable.layout_preview_icon_overview)
                else if (window.isCorporationHangarDivision("bytes:StationCorpHangar_")) EveWindow("Corporation Hangar", inSpace = false, inStructure = false, isPersistent = true, icon = Res.drawable.layout_preview_icon_items)
                else if (window.isCorporationHangarDivision("bytes:StructureCorpHangar_")) EveWindow("Corporation Hangar", inSpace = false, inStation = false, isPersistent = true, icon = Res.drawable.layout_preview_icon_items)
                else if (window.startsWith("bytes:ChatInvitation_")) EveWindow("Conversation Invite")
                else if (window.startsWith("bytes:Save_ViewFitting_")) EveWindow("Save Fitting")
                else if (window.startsWith("bytes:bookmarkSubfolderWindow_newSub_")) EveWindow("Bookmark Subfolder")
                else if (window.startsWith("utf8:('assetslocations_")) EveWindow("Assets Locations")
                else if (window.startsWith("bytes:AgentConversation_")) EveWindow("Agent Conversation")
                else if (window.startsWith("bytes:mail_readingWnd_")) EveWindow("Mail")
                else if (window.startsWith("bytes:KillReport_")) EveWindow("Kill Report")
                else if (window.startsWith("bytes:groupInfoWnd_")) EveWindow("Group Info")
                else if (window.startsWith("utf8:ChannelSettingsDlg_")) EveWindow("Channel Settings")
                else if (window.startsWith("utf8:('contact',")) EveWindow("Contact")
                else if (window.startsWith("utf8:ColonyTemplateWindow")) EveWindow("PI Colony Template")
                else if (window.startsWith("utf8:('TypeSel',")) EveWindow("Type Selection")
                else if (window.startsWith("bytes:calendarNewEventWnd_")) EveWindow("New Calendar Event")
                else if (children.isNotEmpty()) categorizeStack(children, joinedChatChannels)
                else if (window.startsWith("bytes:") && window.substringAfter(":").toIntOrNull() != null) EveWindow("Dynamic")
                else EveWindow(window, isUnknown = true)
            }
        }
    }

    private fun categorizeStack(children: List<String>, joinedChatChannels: Map<String, String>): EveWindow {
        return if (children.size == 1) {
            invoke(children.single(), emptyList(), joinedChatChannels)
        } else {
            val categorizedChildren = children
                .map { invoke(it, emptyList(), joinedChatChannels) }
                .filter { it.isPersistent }
            val isUnknown = categorizedChildren.any { it.isUnknown }
            val icon = categorizedChildren.firstNotNullOfOrNull { it.icon }
            val names = categorizedChildren
                .groupBy { it.name }
                .map {
                    if (it.value.size > 1) {
                        "${it.value.size}x ${it.key}"
                    } else {
                        it.key
                    }
                }
            if (names.isNotEmpty()) EveWindow(
                name = names.joinToString("\n"),
                isUnknown = isUnknown,
                isPersistent = true,
                isOpenStateControlled = categorizedChildren.all { it.isOpenStateControlled },
                isCloseable = categorizedChildren.all { it.isCloseable },
                isMinimizedStatePersistent = categorizedChildren.all { it.isMinimizedStatePersistent },
                icon = icon,
            )
            else EveWindow("Stack", isUnknown = isUnknown)
        }
    }

    private fun String.isCorporationHangarDivision(prefix: String): Boolean {
        return startsWith(prefix) && removePrefix(prefix).toIntOrNull() in corporationHangarDivisions
    }

    private val Int.corporationHangarName: String
        get() = if (this == 14) "Corporation Goal Deliveries" else "Corporation Hangar ${this + 1}"

    private companion object {
        val corporationHangarDivisions = (0..6) + 14
    }
}
