package com.gytxtx.openjbd

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

internal object AppSettings {
    const val PREFS = "openjbd_settings"
    const val PREF_THEME = "theme"
    const val PREF_LANGUAGE = "language"
    const val PREF_TEMP_UNIT = "temp_unit"
    const val PREF_REFRESH_INTERVAL_MS = "refresh_interval_ms"
    const val PREF_AUTO_CONNECT = "auto_connect"
    const val PREF_LAST_DEVICE_ADDRESS = "last_device_address"
    const val PREF_LAST_DEVICE_NAME = "last_device_name"
    const val VALUE_AUTO = "auto"
    const val VALUE_SYSTEM = "system"
    private const val VALUE_ON = "on"
    const val VALUE_LIGHT = "light"
    const val VALUE_DARK = "dark"
    const val VALUE_ZH = "zh"
    const val VALUE_EN = "en"
    const val VALUE_C = "c"
    const val VALUE_F = "f"
    const val VALUE_REFRESH_1S = "1000"
    const val VALUE_REFRESH_2S = "2000"
    const val VALUE_REFRESH_5S = "5000"
    const val VALUE_REFRESH_10S = "10000"
    private const val DEFAULT_REFRESH_INTERVAL_MS = 2000L

    @JvmStatic
    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @JvmStatic
    fun preferredContext(context: Context): Context {
        val configuration = Configuration(context.resources.configuration)
        applyLanguageConfiguration(context, configuration)
        applyNightConfiguration(context, configuration)
        return context.createConfigurationContext(configuration)
    }

    @JvmStatic
    fun applyThemePreference(context: Context) {
        when (themeValue(context)) {
            VALUE_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            VALUE_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    @JvmStatic
    fun refreshIntervalMs(context: Context): Long {
        val prefs = prefs(context)
        val value = prefs.all[PREF_REFRESH_INTERVAL_MS]
        return when (value) {
            is Number -> value.toLong()
            is String -> {
                val parsed = value.toLongOrNull() ?: run {
                    prefs.edit().putLong(PREF_REFRESH_INTERVAL_MS, DEFAULT_REFRESH_INTERVAL_MS).apply()
                    return DEFAULT_REFRESH_INTERVAL_MS
                }
                prefs.edit().putLong(PREF_REFRESH_INTERVAL_MS, parsed).apply()
                parsed
            }
            else -> DEFAULT_REFRESH_INTERVAL_MS
        }
    }

    @JvmStatic
    fun refreshIntervalValue(context: Context): String =
        java.lang.Long.toString(refreshIntervalMs(context))

    @JvmStatic
    fun setRefreshIntervalMs(context: Context, value: Long) {
        prefs(context).edit().putLong(PREF_REFRESH_INTERVAL_MS, value).apply()
    }

    @JvmStatic
    fun autoConnectEnabled(context: Context): Boolean {
        val prefs = prefs(context)
        val value = prefs.all[PREF_AUTO_CONNECT]
        return when (value) {
            is Boolean -> value
            is String -> {
                val enabled = VALUE_ON == value || value.toBoolean()
                prefs.edit().putBoolean(PREF_AUTO_CONNECT, enabled).apply()
                enabled
            }
            else -> false
        }
    }

    @JvmStatic
    fun setAutoConnectEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(PREF_AUTO_CONNECT, enabled).apply()
    }

    @JvmStatic
    fun themeValue(context: Context): String {
        val prefs = prefs(context)
        val theme = prefs.getString(PREF_THEME, VALUE_SYSTEM) ?: VALUE_SYSTEM
        if (VALUE_AUTO == theme) {
            prefs.edit().putString(PREF_THEME, VALUE_SYSTEM).apply()
            return VALUE_SYSTEM
        }
        return theme
    }

    @JvmStatic
    fun languageValue(context: Context): String {
        val prefs = prefs(context)
        val language = prefs.getString(PREF_LANGUAGE, VALUE_SYSTEM) ?: VALUE_SYSTEM
        if (VALUE_AUTO == language) {
            prefs.edit().putString(PREF_LANGUAGE, VALUE_SYSTEM).apply()
            return VALUE_SYSTEM
        }
        return language
    }

    @JvmStatic
    fun displayTemperature(context: Context, celsius: Float): Float =
        if (VALUE_F == prefs(context).getString(PREF_TEMP_UNIT, VALUE_C)) {
            celsius * 9.0f / 5.0f + 32.0f
        } else {
            celsius
        }

    @JvmStatic
    fun temperatureUnitLabel(context: Context): String =
        if (VALUE_F == prefs(context).getString(PREF_TEMP_UNIT, VALUE_C))
            context.getString(R.string.unit_fahrenheit)
        else
            context.getString(R.string.unit_celsius)

    private fun applyLanguageConfiguration(context: Context, configuration: Configuration) {
        val language = languageValue(context)
        if (VALUE_SYSTEM == language) {
            return
        }
        val locale = if (VALUE_ZH == language) Locale.SIMPLIFIED_CHINESE else Locale.ENGLISH
        Locale.setDefault(locale)
        if (Build.VERSION.SDK_INT >= 24) {
            configuration.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            configuration.setLocale(locale)
        }
    }

    private fun applyNightConfiguration(context: Context, configuration: Configuration) {
        val theme = themeValue(context)
        if (VALUE_DARK == theme) {
            configuration.uiMode =
                (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
        } else if (VALUE_LIGHT == theme) {
            configuration.uiMode =
                (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_NO
        }
    }
}
