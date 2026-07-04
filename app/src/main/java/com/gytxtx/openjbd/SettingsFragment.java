package com.gytxtx.openjbd;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

public final class SettingsFragment extends Fragment implements BmsStateStore.Listener {
    private LinearLayout interfaceSettingsList;
    private LinearLayout deviceSettingsList;
    private LinearLayout otherSettingsList;
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
        interfaceSettingsList = view.findViewById(R.id.list_settings_interface);
        deviceSettingsList = view.findViewById(R.id.list_settings_device);
        otherSettingsList = view.findViewById(R.id.list_settings_other);
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
        interfaceSettingsList.removeAllViews();
        deviceSettingsList.removeAllViews();
        otherSettingsList.removeAllViews();
        for (int i = 0; i < settingsAdapter.getCount(); i++) {
            final int position = i;
            LinearLayout parent = settingsGroupParent(position);
            View row = settingsAdapter.getView(position, null, parent);
            row.setOnClickListener(view -> {
                SwitchMaterial settingSwitch = view.findViewById(R.id.switch_setting_action);
                if (settingSwitch != null && settingSwitch.getVisibility() == View.VISIBLE) {
                    settingSwitch.performClick();
                } else {
                    showSettingMenu(position);
                }
            });
            parent.addView(row);
        }
    }

    private LinearLayout settingsGroupParent(int position) {
        if (position <= 3) {
            return interfaceSettingsList;
        }
        if (position == 4) {
            return deviceSettingsList;
        }
        return otherSettingsList;
    }

    private void showSettingMenu(int position) {
        if (position == 0) {
            showMenu(settingsRow(position), AppSettings.themeValue(requireContext()),
                    new String[]{getString(R.string.setting_dark_theme_system), getString(R.string.setting_dark_theme_always_on), getString(R.string.setting_dark_theme_always_off)},
                    new String[]{AppSettings.VALUE_SYSTEM, AppSettings.VALUE_DARK, AppSettings.VALUE_LIGHT},
                    value -> {
                        settings().edit().putString(AppSettings.PREF_THEME, value).apply();
                        AppSettings.applyThemePreference(requireContext());
                        requireActivity().recreate();
                    });
        } else if (position == 1) {
            showMenu(settingsRow(position), AppSettings.languageValue(requireContext()),
                    new String[]{getString(R.string.setting_language_system), getString(R.string.setting_language_zh), getString(R.string.setting_language_en)},
                    new String[]{AppSettings.VALUE_SYSTEM, AppSettings.VALUE_ZH, AppSettings.VALUE_EN},
                    value -> {
                        settings().edit().putString(AppSettings.PREF_LANGUAGE, value).apply();
                        connectionManager.refreshLocalizedStatus();
                        requireActivity().recreate();
                    });
        } else if (position == 2) {
            showMenu(settingsRow(position), settings().getString(AppSettings.PREF_TEMP_UNIT, AppSettings.VALUE_C),
                    new String[]{getString(R.string.setting_temp_celsius), getString(R.string.setting_temp_fahrenheit)},
                    new String[]{AppSettings.VALUE_C, AppSettings.VALUE_F},
                    value -> {
                        settings().edit().putString(AppSettings.PREF_TEMP_UNIT, value).apply();
                        renderSettingsRows();
                        BmsStateStore.update(BmsStateStore.getSnapshot());
                    });
        } else if (position == 3) {
            showMenu(settingsRow(position), AppSettings.refreshIntervalValue(requireContext()),
                    new String[]{getString(R.string.setting_refresh_1s), getString(R.string.setting_refresh_2s), getString(R.string.setting_refresh_5s), getString(R.string.setting_refresh_10s)},
                    new String[]{AppSettings.VALUE_REFRESH_1S, AppSettings.VALUE_REFRESH_2S, AppSettings.VALUE_REFRESH_5S, AppSettings.VALUE_REFRESH_10S},
                    value -> {
                        AppSettings.setRefreshIntervalMs(requireContext(), Long.parseLong(value));
                        renderSettingsRows();
                        if (connected) {
                            connectionManager.setRefreshInterval(AppSettings.refreshIntervalMs(requireContext()));
                        }
                    });
        } else if (position == 4) {
            setAutoConnectEnabled(!AppSettings.autoConnectEnabled(requireContext()), true);
        } else if (position == 5) {
            startActivity(new Intent(requireContext(), AboutActivity.class));
        }
    }

    private View settingsRow(int position) {
        LinearLayout parent = settingsGroupParent(position);
        if (parent == null || position < 0) {
            return requireView();
        }
        int childIndex = position <= 3 ? position : position == 4 ? 0 : 0;
        if (childIndex >= parent.getChildCount()) {
            return parent;
        }
        return parent.getChildAt(childIndex);
    }

    private void setAutoConnectEnabled(boolean enabled, boolean refreshRows) {
        AppSettings.setAutoConnectEnabled(requireContext(), enabled);
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
        boolean enabled = AppSettings.autoConnectEnabled(requireContext());
        String address = prefs.getString(AppSettings.PREF_LAST_DEVICE_ADDRESS, "");
        String name = prefs.getString(AppSettings.PREF_LAST_DEVICE_NAME, address);
        connectionManager.setAutoReconnect(enabled, address, name);
    }

    private void maybeAutoConnect() {
        if (!AppSettings.autoConnectEnabled(requireContext())) {
            return;
        }
        String address = settings().getString(AppSettings.PREF_LAST_DEVICE_ADDRESS, "");
        if (address.length() == 0) {
            return;
        }
        String name = settings().getString(AppSettings.PREF_LAST_DEVICE_NAME, address);
        connectionManager.connect(address, name.length() == 0 ? address : name);
    }

    @SuppressLint("InflateParams")
    private void showMenu(View anchor, String current, String[] labels, String[] values, final ChoiceHandler handler) {
        LinearLayout menuView = (LinearLayout) LayoutInflater.from(requireContext()).inflate(R.layout.popup_setting_menu, null, false);
        PopupWindow popupWindow = new PopupWindow(menuView, dp(220), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(requireContext().getColor(R.color.surface_elevation_8)));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(dp(8));
        int selectedIndex = 0;
        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            TextView item = (TextView) LayoutInflater.from(requireContext()).inflate(R.layout.row_setting_menu_item, menuView, false);
            item.setText(labels[i]);
            if (value.equals(current)) {
                selectedIndex = i;
                item.setBackgroundResource(R.drawable.setting_menu_item_selected);
                item.setTextColor(requireContext().getColor(R.color.primary));
            }
            item.setOnClickListener(view -> {
                popupWindow.dismiss();
                if (!value.equals(current)) {
                    handler.onChoice(value);
                }
            });
            menuView.addView(item);
        }
        int verticalOffset = -anchor.getHeight()
                - dp(8)
                - (selectedIndex * dp(48))
                + ((anchor.getHeight() - dp(48)) / 2);
        popupWindow.showAsDropDown(anchor, 0, verticalOffset);
    }

    private SharedPreferences settings() {
        return AppSettings.prefs(requireContext());
    }

    private String themeLabel(String value) {
        if (AppSettings.VALUE_DARK.equals(value)) {
            return getString(R.string.setting_dark_theme_always_on);
        }
        if (AppSettings.VALUE_LIGHT.equals(value)) {
            return getString(R.string.setting_dark_theme_always_off);
        }
        return getString(R.string.setting_dark_theme_system);
    }

    private String languageLabel(String value) {
        if (AppSettings.VALUE_ZH.equals(value)) {
            return getString(R.string.setting_language_zh);
        }
        if (AppSettings.VALUE_EN.equals(value)) {
            return getString(R.string.setting_language_en);
        }
        return getString(R.string.setting_language_system);
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

    private String autoConnectLabel(boolean value) {
        return value ? getString(R.string.setting_auto_connect_on) : getString(R.string.setting_auto_connect_off);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class SettingsAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return 6;
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
            ImageView chevron = row.findViewById(R.id.img_setting_chevron);
            settingSwitch.setOnCheckedChangeListener(null);
            settingSwitch.setVisibility(View.GONE);
            chevron.setVisibility(View.GONE);
            SharedPreferences prefs = settings();
            if (position == 0) {
                icon.setImageResource(R.drawable.ic_brightness_medium_24);
                icon.setContentDescription(getString(R.string.setting_dark_theme));
                title.setText(R.string.setting_dark_theme);
                subtitle.setText(themeLabel(AppSettings.themeValue(requireContext())));
            } else if (position == 1) {
                icon.setImageResource(R.drawable.ic_language_24);
                icon.setContentDescription(getString(R.string.setting_language));
                title.setText(R.string.setting_language);
                subtitle.setText(languageLabel(AppSettings.languageValue(requireContext())));
            } else if (position == 2) {
                icon.setImageResource(R.drawable.ic_thermostat_24);
                icon.setContentDescription(getString(R.string.setting_temperature_unit));
                title.setText(R.string.setting_temperature_unit);
                subtitle.setText(tempUnitLabel(prefs.getString(AppSettings.PREF_TEMP_UNIT, AppSettings.VALUE_C)));
            } else if (position == 3 || position == 4) {
                icon.setImageResource(R.drawable.ic_loop_24);
                title.setText(R.string.setting_refresh_interval);
                subtitle.setText(refreshIntervalLabel(AppSettings.refreshIntervalValue(requireContext())));
                if (position == 4) {
                    boolean autoConnectEnabled = AppSettings.autoConnectEnabled(requireContext());
                    icon.setImageResource(R.drawable.ic_bluetooth_searching_24);
                    icon.setContentDescription(getString(R.string.setting_auto_connect));
                    title.setText(R.string.setting_auto_connect);
                    subtitle.setText(autoConnectLabel(autoConnectEnabled));
                    settingSwitch.setChecked(autoConnectEnabled);
                    settingSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
                        subtitle.setText(autoConnectLabel(checked));
                        setAutoConnectEnabled(checked, false);
                    });
                    settingSwitch.setVisibility(View.VISIBLE);
                } else {
                    icon.setContentDescription(getString(R.string.setting_refresh_interval));
                }
            } else {
                icon.setImageResource(R.drawable.ic_info_24);
                icon.setContentDescription(getString(R.string.setting_about));
                title.setText(R.string.setting_about);
                subtitle.setText(R.string.setting_about_subtitle);
                chevron.setImageResource(R.drawable.ic_chevron_right_24);
                chevron.setColorFilter(requireContext().getColor(R.color.text_secondary));
                chevron.setVisibility(View.VISIBLE);
            }
            icon.setColorFilter(requireContext().getColor(R.color.icon_default));
            return row;
        }
    }

    private interface ChoiceHandler {
        void onChoice(String value);
    }
}
