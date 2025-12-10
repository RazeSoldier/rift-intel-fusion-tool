package dev.nohus.rift.database.static

import dev.nohus.rift.database.SqliteInitializer
import dev.nohus.rift.settings.persistence.Settings
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class StaticDatabase(
    sqliteInitializer: SqliteInitializer,
    settings: Settings,
) {
    private val targetDatabase: Database = (
        if (settings.language.language == "zh") {
            logger.info { "Using Chinese static database" }
            Database.connect("jdbc:sqlite::resource:static-zh.db", "org.sqlite.JDBC")
        } else {
            logger.info { "Using default static database" }
            Database.connect("jdbc:sqlite::resource:static.db", "org.sqlite.JDBC")
    })

    fun <T> transaction(block: Transaction.() -> T): T {
        return transaction(targetDatabase) {
            block()
        }
    }
}
