package cn.razesoldier.rift

import org.jetbrains.exposed.sql.Database

data object StaticDatabase {
    val enDb = Database.connect("jdbc:sqlite:src/main/resources/static.db", "org.sqlite.JDBC")
    val zhDb = Database.connect("jdbc:sqlite:src/main/resources/static-zh.db", "org.sqlite.JDBC")
}