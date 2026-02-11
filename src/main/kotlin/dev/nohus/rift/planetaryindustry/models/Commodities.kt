package dev.nohus.rift.planetaryindustry.models

import dev.nohus.rift.i18n.ApplicationLocale
import java.util.Locale

object Commodities {
    private val p0Ids = listOf(
        2073,
        2267,
        2268,
        2270,
        2272,
        2286,
        2287,
        2288,
        2305,
        2306,
        2307,
        2308,
        2309,
        2310,
        2311,
    )
    private val p1Ids = listOf(
        2389,
        2390,
        2392,
        2393,
        2395,
        2396,
        2397,
        2398,
        2399,
        2400,
        2401,
        3645,
        3683,
        3779,
        9828,
    )
    private val p2Ids = listOf(
        44,
        2312,
        2317,
        2319,
        2321,
        2327,
        2328,
        2329,
        2463,
        3689,
        3691,
        3693,
        3695,
        3697,
        3725,
        3775,
        3828,
        9830,
        9832,
        9836,
        9838,
        9840,
        9842,
        15317,
    )
    private val p3Ids = listOf(
        2344,
        2345,
        2346,
        2348,
        2349,
        2351,
        2352,
        2354,
        2358,
        2360,
        2361,
        2366,
        2367,
        9834,
        9846,
        9848,
        12836,
        17136,
        17392,
        17898,
        28974,
    )
    private val p4Ids = listOf(
        2867,
        2868,
        2869,
        2870,
        2871,
        2872,
        2875,
        2876,
    )

    fun getTierName(commodity: Int): String {
        if (ApplicationLocale == Locale.CHINESE) {
            return when (commodity) {
                in p0Ids -> "P0"
                in p1Ids -> "P1"
                in p2Ids -> "P2"
                in p3Ids -> "P3"
                in p4Ids -> "P4"
                else -> "未知"
            }
        }
        return when (commodity) {
            in p0Ids -> "Raw Resource"
            in p1Ids -> "Tier 1"
            in p2Ids -> "Tier 2"
            in p3Ids -> "Tier 3"
            in p4Ids -> "Tier 4"
            else -> "Unknown"
        }
    }
}
