package dev.nohus.rift.wallet

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.*
import dev.nohus.rift.i18n.ApplicationLocale
import dev.nohus.rift.i18n.getStringSync
import java.util.Locale

sealed class TransactionGroup(
    val name: String,
    val color: Color,
) {
    data object CorporationAlliance : TransactionGroup(name = getStringSync(Res.string.transaction_group_corp_and_alliance), color = Color(0xFF5c59d8))
    data object AgentsAndMissions : TransactionGroup(name = getStringSync(Res.string.transaction_group_agents_and_missions), color = Color(0xFFe68348))
    data object Trade : TransactionGroup(name = getStringSync(Res.string.transaction_group_trade), color = Color(0xFF31c4a1))
    data object Bounty : TransactionGroup(name = getStringSync(Res.string.transaction_group_bounties), color = Color(0xFFe53a3a))
    data object Industry : TransactionGroup(name = getStringSync(Res.string.transaction_group_industry), color = Color(0xFF3de53a))
    data object Transfer : TransactionGroup(name = getStringSync(Res.string.transaction_group_transfers), color = Color(0xFFe6e048))
    data object Misc : TransactionGroup(name = getStringSync(Res.string.transaction_group_miscellaneous), color = Color(0xFF808080))
    data object HypernetRelay : TransactionGroup(name = getStringSync(Res.string.transaction_group_hypernet), color = Color(0xFF3aa8e5))

    companion object {
        fun byReferenceType(referenceType: String) = when (referenceType) {
            "acceleration_gate_fee" -> Misc
            "advertisement_listing_fee" -> CorporationAlliance
            "agent_donation" -> AgentsAndMissions
            "agent_location_services" -> AgentsAndMissions
            "agent_miscellaneous" -> AgentsAndMissions
            "agent_mission_collateral_paid" -> AgentsAndMissions
            "agent_mission_collateral_refunded" -> AgentsAndMissions
            "agent_mission_reward" -> AgentsAndMissions
            "agent_mission_reward_corporation_tax" -> CorporationAlliance
            "agent_mission_time_bonus_reward" -> AgentsAndMissions
            "agent_mission_time_bonus_reward_corporation_tax" -> CorporationAlliance
            "agent_security_services" -> AgentsAndMissions
            "agent_services_rendered" -> AgentsAndMissions
            "agents_preward" -> AgentsAndMissions
            "air_career_program_reward" -> AgentsAndMissions
            "alliance_maintainance_fee" -> CorporationAlliance
            "alliance_registration_fee" -> CorporationAlliance
            "allignment_based_gate_toll" -> Misc
            "asset_safety_recovery_tax" -> Misc
            "bounty" -> Bounty
            "bounty_prize" -> Bounty
            "bounty_prize_corporation_tax" -> Bounty
            "bounty_prizes" -> Bounty
            "bounty_reimbursement" -> Bounty
            "bounty_surcharge" -> Bounty
            "brokers_fee" -> Trade
            "clone_activation" -> Misc
            "clone_transfer" -> Misc
            "contraband_fine" -> Misc
            "contract_auction_bid" -> Trade
            "contract_auction_bid_corp" -> Trade
            "contract_auction_bid_refund" -> Trade
            "contract_auction_sold" -> Trade
            "contract_brokers_fee" -> Trade
            "contract_brokers_fee_corp" -> Trade
            "contract_collateral" -> Trade
            "contract_collateral_deposited_corp" -> Trade
            "contract_collateral_payout" -> Trade
            "contract_collateral_refund" -> Trade
            "contract_deposit" -> Trade
            "contract_deposit_corp" -> Trade
            "contract_deposit_refund" -> Trade
            "contract_deposit_sales_tax" -> Trade
            "contract_price" -> Trade
            "contract_price_payment_corp" -> Trade
            "contract_reversal" -> Trade
            "contract_reward" -> Trade
            "contract_reward_deposited" -> Trade
            "contract_reward_deposited_corp" -> Trade
            "contract_reward_refund" -> Trade
            "contract_sales_tax" -> Trade
            "copying" -> Industry
            "corporate_reward_payout" -> CorporationAlliance
            "corporate_reward_tax" -> CorporationAlliance
            "corporation_account_withdrawal" -> CorporationAlliance
            "corporation_bulk_payment" -> CorporationAlliance
            "corporation_dividend_payment" -> CorporationAlliance
            "corporation_liquidation" -> CorporationAlliance
            "corporation_logo_change_cost" -> CorporationAlliance
            "corporation_payment" -> CorporationAlliance
            "corporation_registration_fee" -> CorporationAlliance
            "cosmetic_market_component_item_purchase" -> Trade
            "cosmetic_market_skin_purchase" -> Trade
            "cosmetic_market_skin_sale" -> Trade
            "cosmetic_market_skin_sale_broker_fee" -> Trade
            "cosmetic_market_skin_sale_tax" -> Trade
            "cosmetic_market_skin_transaction" -> Trade
            "courier_mission_escrow" -> AgentsAndMissions
            "cspa" -> Misc
            "cspaofflinerefund" -> Misc
            "daily_challenge_reward" -> AgentsAndMissions
            "daily_goal_payouts" -> AgentsAndMissions
            "daily_goal_payouts_tax" -> AgentsAndMissions
            "datacore_fee" -> Industry
            "dna_modification_fee" -> Misc
            "docking_fee" -> Misc
            "duel_wager_escrow" -> Misc
            "duel_wager_payment" -> Misc
            "duel_wager_refund" -> Misc
            "ess_escrow_transfer" -> Bounty
            "external_trade_delivery" -> Trade
            "external_trade_freeze" -> Trade
            "external_trade_thaw" -> Trade
            "factory_slot_rental_fee" -> Industry
            "flux_payout" -> HypernetRelay
            "flux_tax" -> HypernetRelay
            "flux_ticket_repayment" -> HypernetRelay
            "flux_ticket_sale" -> HypernetRelay
            "freelance_jobs_broadcasting_fee" -> Misc
            "freelance_jobs_duration_fee" -> Misc
            "freelance_jobs_escrow_refund" -> Misc
            "freelance_jobs_reward" -> Misc
            "freelance_jobs_reward_corporation_tax" -> Misc
            "freelance_jobs_reward_escrow" -> Misc
            "gm_cash_transfer" -> Misc
            "gm_plex_fee_refund" -> Misc
            "industry_job_tax" -> Industry
            "infrastructure_hub_maintenance" -> CorporationAlliance
            "inheritance" -> Misc
            "insurance" -> Misc
            "insurgency_corruption_contribution_reward" -> Misc
            "insurgency_suppression_contribution_reward" -> Misc
            "item_trader_payment" -> Trade
            "jump_clone_activation_fee" -> Misc
            "jump_clone_installation_fee" -> Misc
            "kill_right_fee" -> Misc
            "lp_store" -> Trade
            "manufacturing" -> Industry
            "market_escrow" -> Trade
            "market_fine_paid" -> Trade
            "market_provider_tax" -> Trade
            "market_transaction" -> Trade
            "medal_creation" -> CorporationAlliance
            "medal_issued" -> CorporationAlliance
            "milestone_reward_payment" -> Misc
            "mission_completion" -> AgentsAndMissions
            "mission_cost" -> AgentsAndMissions
            "mission_expiration" -> AgentsAndMissions
            "mission_reward" -> AgentsAndMissions
            "office_rental_fee" -> CorporationAlliance
            "operation_bonus" -> Misc
            "opportunity_reward" -> Misc
            "planetary_construction" -> Industry
            "planetary_export_tax" -> Industry
            "planetary_import_tax" -> Industry
            "player_donation" -> Transfer
            "player_trading" -> Trade
            "project_discovery_reward" -> Misc
            "project_discovery_tax" -> Misc
            "project_payouts" -> Misc
            "reaction" -> Industry
            "redeemed_isk_token" -> Misc
            "release_of_impounded_property" -> Misc
            "repair_bill" -> Misc
            "reprocessing_tax" -> Misc
            "researching_material_productivity" -> Industry
            "researching_technology" -> Industry
            "researching_time_productivity" -> Industry
            "resource_wars_reward" -> Misc
            "reverse_engineering" -> Industry
            "season_challenge_reward" -> Misc
            "security_processing_fee" -> Misc
            "shares" -> CorporationAlliance
            "skill_purchase" -> Trade
            "skyhook_claim_fee" -> CorporationAlliance
            "sovereignity_bill" -> CorporationAlliance
            "store_purchase" -> Trade
            "store_purchase_refund" -> Trade
            "structure_gate_jump" -> Misc
            "transaction_tax" -> Trade
            "under_construction" -> Misc
            "upkeep_adjustment_fee" -> Misc
            "war_ally_contract" -> CorporationAlliance
            "war_fee" -> CorporationAlliance
            "war_fee_surrender" -> CorporationAlliance
            else -> Misc
        }
    }
}

fun getReferenceTypeName(referenceType: String): String {
    if (ApplicationLocale.current == Locale.CHINESE) {
        val name = getReferenceTypeNameL10n(referenceType)
        if (name != null) {
            return name
        }
    }
    return referenceType
        .split('_')
        .joinToString(" ") { it.replaceFirstChar { c -> c.titlecase() } }
        .replace("Ess", "ESS")
}

private fun getReferenceTypeNameL10n(referenceType: String): String? {
    return refTypeZhMap.getOrDefault(referenceType, null)
}

private val refTypeZhMap: Map<String, String> = mapOf(
    "agent_location_services" to "代理人查人服务",
    "agent_mission_reward" to "代理人任务奖励",
    "agent_mission_time_bonus_reward" to "代理人任务时间奖励",
    "asset_safety_recovery_tax" to "资产安全赎回费",
    "bounty_prizes" to "海盗击杀赏金",
    "brokers_fee" to "中介费收入",
    "contract_auction_bid" to "拍卖合同出价",
    "contract_auction_bid_corp" to "拍卖合同军团出价",
    "contract_brokers_fee" to "合同中介费",
    "contract_brokers_fee_corp" to "军团合同中介费",
    "contract_price" to "合同费",
    "contract_price_payment_corp" to "合同费（军团支付）",
    "contract_reward" to "合同酬劳",
    "contract_reward_deposited" to "预付完成合同的酬劳",
    "corporate_reward_payout" to "势力奖励",
    "corporation_account_withdrawal" to "军团账户支出",
    "daily_goal_payouts" to "每日任务奖励",
    "ess_escrow_transfer" to "事件监测收入",
    "industry_job_tax" to "工业税",
    "insurance" to "保险",
    "jump_clone_activation_fee" to "跳跃克隆激活费",
    "jump_clone_installation_fee" to "跳跃克隆安装费",
    "manufacturing" to "制造税",
    "market_escrow" to "市场契约金",
    "market_provider_tax" to "市场商业税",
    "market_transaction" to "市场交易",
    "office_rental_fee" to "军团办公室租金",
    "player_donation" to "玩家转账",
    "player_trading" to "玩家交易",
    "project_discovery_reward" to "探索计划奖励",
    "reaction" to "反应税",
    "reprocessing_tax" to "化矿税",
    "skill_purchase" to "技能购买",
    "structure_gate_jump" to "跳桥费",
    "transaction_tax" to "交易税",
)