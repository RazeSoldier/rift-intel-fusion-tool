package dev.nohus.rift.i18n

import dev.nohus.rift.settings.persistence.Settings
import java.util.Locale

/**
 * Represents the application's locale settings and provides methods to manage the current locale.
 *
 * This object allows setting and getting the current locale, as well as applying the locale from
 * the application's settings. It supports a predefined list of locales.
 *
 * Methods:
 * - useSettingsLocale: Applies the language setting from the application's settings file to the current locale.
 * - setLocale: Sets the current locale to the specified [Locale].
 *
 * Properties:
 * - current: Returns the currently set [Locale] for the application.
 */
object ApplicationLocale {
    private var appLocale = Locale.getDefault()

    val supportLocales = listOf(Locale.ENGLISH, Locale.CHINESE)
    val current : Locale
        get() = appLocale

    /**
     * Applies the language setting from the application's settings to the current locale.
     */
    fun useSettingsLocale() {
        val settings = dev.nohus.rift.di.koin.get<Settings>()
        appLocale = settings.language
    }

    fun setLocale(locale: Locale) {
        appLocale = locale
    }
}