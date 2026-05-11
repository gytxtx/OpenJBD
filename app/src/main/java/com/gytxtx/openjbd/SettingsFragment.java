package com.gytxtx.openjbd;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

public final class SettingsFragment extends Fragment implements BmsStateStore.Listener {
    private LinearLayout settingsList;
    private SettingsAdapter settingsAdapter;
    private BmsConnectionManager connectionManager;
    private boolean connected;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        connectionManager = BmsConnectionManager.getInstance(requireContext());
        settingsList = view.findViewById(R.id.list_settings);
        settingsAdapter = new SettingsAdapter();
        connected = BmsStateStore.getSnapshot().connected;
        renderSettingsRows();
    }

    @Override
    public void onStart() {
        super.onStart();
        BmsStateStore.addListener(this);
        connected = BmsStateStore.getSnapshot().connected;
    }

    @Override
    public void onStop() {
        BmsStateStore.removeListener(this);
        super.onStop();
    }

    @Override
    public void onBmsStateChanged(BmsStateStore.Snapshot snapshot) {
        connected = snapshot.connected;
    }

    private void renderSettingsRows() {
        settingsList.removeAllViews();
        for (int i = 0; i < settingsAdapter.getCount(); i++) {
            final int position = i;
            View row = settingsAdapter.getView(position, null, settingsList);
            row.setOnClickListener(view -> {
                SwitchMaterial settingSwitch = view.findViewById(R.id.switch_setting_action);
                if (settingSwitch != null && settingSwitch.getVisibility() == View.VISIBLE) {
                    settingSwitch.performClick();
                } else {
                    showSettingMenu(position);
                }
            });
            settingsList.addView(row);
        }
    }

    private void showSettingMenu(int position) {
        if (position == 0) {
            showRadioDialog(R.string.setting_theme, AppSettings.PREF_THEME,
                    new String[]{getString(R.string.setting_theme_auto), getString(R.string.setting_theme_light), getString(R.string.setting_theme_dark)},
                    new String[]{AppSettings.VALUE_AUTO, AppSettings.VALUE_LIGHT, AppSettings.VALUE_DARK},
                    value -> {
                        settings().edit().putString(AppSettings.PREF_THEME, value).apply();
                        AppSettings.applyThemePreference(requireContext());
                        requireActivity().recreate();
                    });
        } else if (position == 1) {
            showRadioDialog(R.string.setting_language, AppSettings.PREF_LANGUAGE,
                    new String[]{getString(R.string.setting_language_auto), getString(R.string.setting_language_zh), getString(R.string.setting_language_en)},
                    new String[]{AppSettings.VALUE_AUTO, AppSettings.VALUE_ZH, AppSettings.VALUE_EN},
                    value -> {
                        settings().edit().putString(AppSettings.PREF_LANGUAGE, value).apply();
                        requireActivity().recreate();
                    });
        } else if (position == 2) {
            showRadioDialog(R.string.setting_temperature_unit, AppSettings.PREF_TEMP_UNIT,
                    new String[]{getString(R.string.setting_temp_celsius), getString(R.string.setting_temp_fahrenheit)},
                    new String[]{AppSettings.VALUE_C, AppSettings.VALUE_F},
                    value -> {
                        settings().edit().putString(AppSettings.PREF_TEMP_UNIT, value).apply();
                        renderSettingsRows();
                        BmsStateStore.update(BmsStateStore.getSnapshot());
                    });
        } else if (position == 3) {
            showRadioDialog(R.string.setting_refresh_interval, AppSettings.PREF_REFRESH_INTERVAL_MS,
                    new String[]{getString(R.string.setting_refresh_1s), getString(R.string.setting_refresh_2s), getString(R.string.setting_refresh_5s), getString(R.string.setting_refresh_10s)},
                    new String[]{AppSettings.VALUE_REFRESH_1S, AppSettings.VALUE_REFRESH_2S, AppSettings.VALUE_REFRESH_5S, AppSettings.VALUE_REFRESH_10S},
                    value -> {
                        settings().edit().putString(AppSettings.PREF_REFRESH_INTERVAL_MS, value).apply();
                        renderSettingsRows();
                        if (connected) {
                            connectionManager.setRefreshInterval(AppSettings.refreshIntervalMs(requireContext()));
                        }
                    });
        } else if (position == 4) {
            boolean enabled = AppSettings.VALUE_ON.equals(settings().getString(AppSettings.PREF_AUTO_CONNECT, AppSettings.VALUE_OFF));
            setAutoConnectEnabled(!enabled, true);
        }
    }

    private void setAutoConnectEnabled(boolean enabled, boolean refreshRows) {
        settings().edit().putString(AppSettings.PREF_AUTO_CONNECT, enabled ? AppSettings.VALUE_ON : AppSettings.VALUE_OFF).apply();
        configureAutoReconnect();
        if (refreshRows) {
            renderSettingsRows();
        }
        if (enabled && !connected) {
            maybeAutoConnect();
        }
    }

    private void configureAutoReconnect() {
        SharedPreferences prefs = settings();
        boolean enabled = AppSettings.VALUE_ON.equals(prefs.getString(AppSettings.PREF_AUTO_CONNECT, AppSettings.VALUE_OFF));
        String address = prefs.getString(AppSettings.PREF_LAST_DEVICE_ADDRESS, "");
        String name = prefs.getString(AppSettings.PREF_LAST_DEVICE_NAME, address);
        connectionManager.setAutoReconnect(enabled, address, name);
    }

    private void maybeAutoConnect() {
        if (!AppSettings.VALUE_ON.equals(settings().getString(AppSettings.PREF_AUTO_CONNECT, AppSettings.VALUE_OFF))) {
            return;
        }
        String address = settings().getString(AppSettings.PREF_LAST_DEVICE_ADDRESS, "");
        if (address.length() == 0) {
            return;
        }
        String name = settings().getString(AppSettings.PREF_LAST_DEVICE_NAME, address);
        connectionManager.connect(address, name.length() == 0 ? address : name);
    }

    private void showRadioDialog(int titleRes, String prefKey, String[] labels, String[] values, final ChoiceHandler handler) {
        String current = settings().getString(prefKey, values[0]);
        int checked = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) {
                checked = i;
                break;
            }
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(titleRes)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    String value = values[which];
                    if (!value.equals(settings().getString(prefKey, ""))) {
                        handler.onChoice(value);
                    }
                    dialog.dismiss();
                })
                .show();
    }

    private SharedPreferences settings() {
        return AppSettings.prefs(requireContext());
    }

    private String themeLabel(String value) {
        if (AppSettings.VALUE_LIGHT.equals(value)) {
            return getString(R.string.setting_theme_light);
        }
        if (AppSettings.VALUE_DARK.equals(value)) {
            return getString(R.string.setting_theme_dark);
        }
        return getString(R.string.setting_theme_auto);
    }

    private String languageLabel(String value) {
        if (AppSettings.VALUE_ZH.equals(value)) {
            return getString(R.string.setting_language_zh);
        }
        if (AppSettings.VALUE_EN.equals(value)) {
            return getString(R.string.setting_language_en);
        }
        return getString(R.string.setting_language_auto);
    }

    private String tempUnitLabel(String value) {
        return AppSettings.VALUE_F.equals(value) ? getString(R.string.setting_temp_fahrenheit) : getString(R.string.setting_temp_celsius);
    }

    private String refreshIntervalLabel(String value) {
        if (AppSettings.VALUE_REFRESH_1S.equals(value)) {
            return getString(R.string.setting_refresh_1s);
        }
        if (AppSettings.VALUE_REFRESH_5S.equals(value)) {
            return getString(R.string.setting_refresh_5s);
        }
        if (AppSettings.VALUE_REFRESH_10S.equals(value)) {
            return getString(R.string.setting_refresh_10s);
        }
        return getString(R.string.setting_refresh_2s);
    }

    private String autoConnectLabel(String value) {
        return AppSettings.VALUE_ON.equals(value) ? getString(R.string.setting_auto_connect_on) : getString(R.string.setting_auto_connect_off);
    }

    private final class SettingsAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = LayoutInflater.from(requireContext()).inflate(R.layout.row_setting_item, parent, false);
            }
            ImageView icon = row.findViewById(R.id.img_setting_icon);
            TextView title = row.findViewById(R.id.txt_setting_title);
            TextView subtitle = row.findViewById(R.id.txt_setting_subtitle);
            SwitchMaterial settingSwitch = row.findViewById(R.id.switch_setting_action);
            settingSwitch.setOnCheckedChangeListener(null);
            settingSwitch.setVisibility(View.GONE);
            SharedPreferences prefs = settings();
            if (position == 0) {
                icon.setImageResource(R.drawable.ic_palette_24);
                title.setText(R.string.setting_theme);
                subtitle.setText(themeLabel(prefs.getString(AppSettings.PREF_THEME, AppSettings.VALUE_AUTO)));
            } else if (position == 1) {
                icon.setImageResource(R.drawable.ic_language_24);
                title.setText(R.string.setting_language);
                subtitle.setText(languageLabel(prefs.getString(AppSettings.PREF_LANGUAGE, AppSettings.VALUE_AUTO)));
            } else if (position == 2) {
                icon.setImageResource(R.drawable.ic_thermostat_24);
                title.setText(R.string.setting_temperature_unit);
                subtitle.setText(tempUnitLabel(prefs.getString(AppSettings.PREF_TEMP_UNIT, AppSettings.VALUE_C)));
            } else {
                icon.setImageResource(R.drawable.baseline_loop_24);
                title.setText(R.string.setting_refresh_interval);
                subtitle.setText(refreshIntervalLabel(prefs.getString(AppSettings.PREF_REFRESH_INTERVAL_MS, AppSettings.VALUE_REFRESH_2S)));
                if (position == 4) {
                    boolean autoConnectEnabled = AppSettings.VALUE_ON.equals(prefs.getString(AppSettings.PREF_AUTO_CONNECT, AppSettings.VALUE_OFF));
                    icon.setImageResource(R.drawable.ic_bluetooth_searching_24);
                    title.setText(R.string.setting_auto_connect);
                    subtitle.setText(autoConnectLabel(autoConnectEnabled ? AppSettings.VALUE_ON : AppSettings.VALUE_OFF));
                    settingSwitch.setChecked(autoConnectEnabled);
                    settingSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
                        subtitle.setText(autoConnectLabel(checked ? AppSettings.VALUE_ON : AppSettings.VALUE_OFF));
                        setAutoConnectEnabled(checked, false);
                    });
                    settingSwitch.setVisibility(View.VISIBLE);
                }
            }
            icon.setColorFilter(requireContext().getColor(R.color.text_secondary));
            return row;
        }
    }

    private interface ChoiceHandler {
        void onChoice(String value);
    }
}
