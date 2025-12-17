package dev.nohus.rift.i18n

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

fun execIfLocaleNotInZh(block: () -> Unit) {
    if (java.util.Locale.getDefault().language != "zh") {
        block()
    }
}

fun getStringSync(res: StringResource): String {
    return runBlocking { getString(res) }
}

fun getStringSync(res: StringResource, vararg formatArgs: Any): String {
    return runBlocking { getString(res, *formatArgs) }
}

fun getPluralStringSync(resource: PluralStringResource, quantity: Int): String {
    return runBlocking { getPluralString(resource, quantity) }
}

fun getPluralStringSync(resource: PluralStringResource, quantity: Int, vararg formatArgs: Any): String {
    return runBlocking { getPluralString(resource, quantity, *formatArgs) }
}