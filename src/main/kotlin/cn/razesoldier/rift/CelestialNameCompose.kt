package cn.razesoldier.rift

import dev.nohus.rift.database.static.SolarSystems
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * SDE出于节省文件占用大小的考虑，不再保存天体名词
 * 需要根据约定组合名名称
 */
fun composePlanet(systemId: Int, celestialIndex: Int): String {
    return transaction(StaticDatabase.zhDb) {
        val system = SolarSystems.select(SolarSystems.solarSystemName)
            .where { SolarSystems.solarSystemId eq systemId }
            .single()
        return@transaction system[SolarSystems.solarSystemName] + " " + toRoman(celestialIndex)
    }
}

private fun toRoman(number: Int): String {
    require(number in 1..3999) { "Only support 1~3999" }

    val values = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
    val symbols = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")

    val sb = StringBuilder()
    var n = number
    for (i in values.indices) {
        while (n >= values[i]) {
            sb.append(symbols[i])
            n -= values[i]
        }
    }
    return sb.toString()
}