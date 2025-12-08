package dev.nohus.rift.i18n

fun execIfLocaleNotInZh(block: () -> Unit) {
    if (java.util.Locale.getDefault().language != "zh") {
        block()
    }
}