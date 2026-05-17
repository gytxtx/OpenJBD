package com.gytxtx.openjbd;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

final class AppSettings {
    static final String PREFS = "openjbd_settings";
    static final String PREF_THEME = "theme";
    static final String PREF_LANGUAGE = "language";
    static final String PREF_TEMP_UNIT = "temp_unit";
    static final String PREF_REFRESH_INTERVAL_MS = "refresh_interval_ms";
    static final String PREF_AUTO_CONNECT = "auto_connect";
    static final String PREF_LAST_DEVICE_ADDRESS = "last_device_address";
    static final String PREF_LAST_DEVICE_NAME = "last_device_name";
    static final String VALUE_AUTO = "auto";
    static final String VALUE_SYSTEM = "system";
    private static final String VALUE_ON = "on";
    static final String VALUE_LIGHT = "light";
    static final String VALUE_DARK = "dark";
    static final String VALUE_ZH = "zh";
    static final String VALUE_EN = "en";
    static final String VALUE_C = "c";
    static final String VALUE_F = "f";
    static final String VALUE_REFRESH_1S = "1000";
    static final String VALUE_REFRESH_2S = "2000";
    static final String VALUE_REFRESH_5S = "5000";
    static final String VALUE_REFRESH_10S = "10000";
    private static final long DEFAULT_REFRESH_INTERVAL_MS = 2000L;

    private AppSettings() {
    }

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static Context preferredContext(Context context) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        applyLanguageConfiguration(context, configuration);
        applyNightConfiguration(context, configuration);
        return context.createConfigurationContext(configuration);
    }

    static void applyThemePreference(Context context) {
        String theme = themeValue(context);
        if (VALUE_DARK.equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if (VALUE_LIGHT.equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    static long refreshIntervalMs(Context context) {
        SharedPreferences prefs = prefs(context);
        Object value = prefs.getAll().get(PREF_REFRESH_INTERVAL_MS);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                long parsed = Long.parseLong((String) value);
                prefs.edit().putLong(PREF_REFRESH_INTERVAL_MS, parsed).apply();
                return parsed;
            } catch (NumberFormatException ignored) {
                prefs.edit().putLong(PREF_REFRESH_INTERVAL_MS, DEFAULT_REFRESH_INTERVAL_MS).apply();
                return DEFAULT_REFRESH_INTERVAL_MS;
            }
        }
        return DEFAULT_REFRESH_INTERVAL_MS;
    }

    static String refreshIntervalValue(Context context) {
        return Long.toString(refreshIntervalMs(context));
    }

    static void setRefreshIntervalMs(Context context, long value) {
        prefs(context).edit().putLong(PREF_REFRESH_INTERVAL_MS, value).apply();
    }

    static boolean autoConnectEnabled(Context context) {
        SharedPreferences prefs = prefs(context);
        Object value = prefs.getAll().get(PREF_AUTO_CONNECT);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            boolean enabled = VALUE_ON.equals(value) || Boolean.parseBoolean((String) value);
            prefs.edit().putBoolean(PREF_AUTO_CONNECT, enabled).apply();
            return enabled;
        }
        return false;
    }

    static void setAutoConnectEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(PREF_AUTO_CONNECT, enabled).apply();
    }

    static String themeValue(Context context) {
        SharedPreferences prefs = prefs(context);
        String theme = prefs.getString(PREF_THEME, VALUE_SYSTEM);
        if (VALUE_AUTO.equals(theme)) {
            prefs.edit().putString(PREF_THEME, VALUE_SYSTEM).apply();
            return VALUE_SYSTEM;
        }
        return theme;
    }

    static String languageValue(Context context) {
        SharedPreferences prefs = prefs(context);
        String language = prefs.getString(PREF_LANGUAGE, VALUE_SYSTEM);
        if (VALUE_AUTO.equals(language)) {
            prefs.edit().putString(PREF_LANGUAGE, VALUE_SYSTEM).apply();
            return VALUE_SYSTEM;
        }
        return language;
    }

    static float displayTemperature(Context context, float celsius) {
        if (VALUE_F.equals(prefs(context).getString(PREF_TEMP_UNIT, VALUE_C))) {
            return (celsius * 9.0f / 5.0f) + 32.0f;
        }
        return celsius;
    }

    static String temperatureUnitLabel(Context context) {
        return VALUE_F.equals(prefs(context).getString(PREF_TEMP_UNIT, VALUE_C))
                ? context.getString(R.string.unit_fahrenheit)
                : context.getString(R.string.unit_celsius);
    }

    private static void applyLanguageConfiguration(Context context, Configuration configuration) {
        String language = languageValue(context);
        if (VALUE_SYSTEM.equals(language)) {
            return;
        }
        Locale locale = VALUE_ZH.equals(language) ? Locale.SIMPLIFIED_CHINESE : Locale.ENGLISH;
        Locale.setDefault(locale);
        if (Build.VERSION.SDK_INT >= 24) {
            configuration.setLocales(new LocaleList(locale));
        } else {
            configuration.setLocale(locale);
        }
    }

    private static void applyNightConfiguration(Context context, Configuration configuration) {
        String theme = themeValue(context);
        if (VALUE_DARK.equals(theme)) {
            configuration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | Configuration.UI_MODE_NIGHT_YES;
        } else if (VALUE_LIGHT.equals(theme)) {
            configuration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | Configuration.UI_MODE_NIGHT_NO;
        }
    }
}
