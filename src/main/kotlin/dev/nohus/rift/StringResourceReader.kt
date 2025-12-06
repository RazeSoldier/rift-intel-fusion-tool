package dev.nohus.rift

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.Single

@Single
class StringResourceReader {
    fun getStringSync(res: StringResource): String {
        return runBlocking { getString(res) }
    }
}