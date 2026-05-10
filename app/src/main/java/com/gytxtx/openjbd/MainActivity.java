package com.gytxtx.openjbd;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;
import java.util.Locale;

import com.gytxtx.openjbd.protocol.JbdBasicInfo;
import com.gytxtx.openjbd.protocol.JbdCellVoltages;

public final class MainActivity extends AppCompatActivity implements BmsStateStore.Listener {
    private static final int REQUEST_SELECT_DEVICE = 101;
    private static final int PAGE_OVERVIEW = 0;
    private static final int PAGE_PARAMETERS = 1;
    private static final int PAGE_SETTINGS = 2;
    private static final String PREFS = "openjbd_settings";
    private static final String PREF_THEME = "theme";
    private static final String PREF_LANGUAGE = "language";
    private static final String PREF_TEMP_UNIT = "temp_unit";
    private static final String PREF_REFRESH_INTERVAL_MS = "refresh_interval_ms";
    private static final String PREF_AUTO_CONNECT = "auto_connect";
    private static final String PREF_LAST_DEVICE_ADDRESS = "last_device_address";
    private static final String PREF_LAST_DEVICE_NAME = "last_device_name";
    private static final String VALUE_AUTO = "auto";
    private static final String VALUE_ON = "on";
    private static final String VALUE_OFF = "off";
    private static final String VALUE_LIGHT = "light";
    private static final String VALUE_DARK = "dark";
    private static final String VALUE_ZH = "zh";
    private static final String VALUE_EN = "en";
    private static final String VALUE_C = "c";
    private static final String VALUE_F = "f";
    private static final String VALUE_REFRESH_1S = "1000";
    private static final String VALUE_REFRESH_2S = "2000";
    private static final String VALUE_REFRESH_5S = "5000";
    private static final String VALUE_REFRESH_10S = "10000";

    private boolean connected;
    private String connectedDeviceName;

    private LinearLayout cellList;
    private TextView statusText;
    private TextView voltageText;
    private TextView currentText;
    private TextView powerText;
    private TextView socText;
    private TextView capacityText;
    private TextView cyclesText;
    private TextView mosText;
    private TextView statsText;
    private TextView temperaturesText;
    private ChipGroup temperaturesChipGroup;
    private TextView cellMinText;
    private TextView cellMaxText;
    private TextView cellDeltaText;
    private TextView cellAverageText;
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigationView;
    private ScrollView mainScroll;
    private LinearProgressIndicator socProgress;
    private LinearLayout overviewPage;
    private LinearLayout parametersPage;
    private LinearLayout settingsPage;
    private LinearLayout placeholderConnect;
    private LinearLayout placeholderParameters;
    private LinearLayout connectedOverviewContent;
    private LinearLayout cellStatsGrid;
    private MaterialCardView parametersCard;
    private ListView parametersList;
    private ParametersAdapter parametersAdapter;
    private ListView settingsList;
    private SettingsAdapter settingsAdapter;
    private JbdBasicInfo lastBasicInfo;
    private BmsConnectionManager connectionManager;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(preferredContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyThemePreference(this);
        super.onCreate(savedInstanceState);
        connectionManager = BmsConnectionManager.getInstance(this);
        connectionManager.setRefreshInterval(refreshIntervalMs());
        buildUi();
        updateToolbar();
        clearDeviceData();
        maybeAutoConnect();
    }

    @Override
    protected void onStart() {
        super.onStart();
        BmsStateStore.addListener(this);
        renderState(BmsStateStore.getSnapshot());
    }

    @Override
    protected void onStop() {
        BmsStateStore.removeListener(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBmsStateChanged(final BmsStateStore.Snapshot snapshot) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                renderState(snapshot);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_SELECT_DEVICE || resultCode != RESULT_OK || data == null) {
            return;
        }
        String address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        String name = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_NAME);
        if (address == null) {
            toast(getString(R.string.status_bluetooth_unavailable));
            return;
        }
        rememberDevice(address, name);
        connectionManager.connect(address, name == null ? address : name);
    }

    private void buildUi() {
        setContentView(R.layout.activity_main);
        SystemBars.applyAppBars(this);

        toolbar = findViewById(R.id.top_app_bar);
        toolbar.setNavigationIconTint(getColorCompat(R.color.text_primary));
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        mainScroll = findViewById(R.id.main_scroll);
        overviewPage = findViewById(R.id.page_overview);
        parametersPage = findViewById(R.id.page_parameters);
        settingsPage = findViewById(R.id.page_settings);
        placeholderConnect = findViewById(R.id.placeholder_connect);
        placeholderParameters = findViewById(R.id.placeholder_parameters);
        connectedOverviewContent = findViewById(R.id.content_connected_overview);
        parametersCard = findViewById(R.id.card_parameters);
        parametersList = findViewById(R.id.list_parameters);
        statusText = findViewById(R.id.txt_status);
        socText = findViewById(R.id.txt_soc);
        socProgress = findViewById(R.id.progress_soc);
        voltageText = findViewById(R.id.txt_voltage);
        currentText = findViewById(R.id.txt_current);
        powerText = findViewById(R.id.txt_power);
        capacityText = findViewById(R.id.txt_capacity);
        cyclesText = findViewById(R.id.txt_cycles);
        statsText = findViewById(R.id.txt_stats);
        mosText = findViewById(R.id.txt_mos);
        temperaturesText = findViewById(R.id.txt_temperatures);
        temperaturesChipGroup = findViewById(R.id.chips_temperatures);
        cellStatsGrid = findViewById(R.id.cell_stats_grid);
        cellMinText = findViewById(R.id.txt_cell_min);
        cellMaxText = findViewById(R.id.txt_cell_max);
        cellDeltaText = findViewById(R.id.txt_cell_delta);
        cellAverageText = findViewById(R.id.txt_cell_average);
        cellList = findViewById(R.id.list_cells);
        settingsList = findViewById(R.id.list_settings);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDeviceList();
            }
        });
        toolbar.setOnMenuItemClickListener(new MaterialToolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_disconnect) {
                    connectionManager.disconnect();
                    return true;
                }
                if (item.getItemId() == R.id.action_dashboard) {
                    startActivity(new Intent(MainActivity.this, DashboardActivity.class));
                    return true;
                }
                return false;
            }
        });
        for (int i = 0; i < toolbar.getMenu().size(); i++) {
            MenuItem toolbarItem = toolbar.getMenu().getItem(i);
                if (toolbarItem.getIcon() != null) {
                toolbarItem.getIcon().setTint(getColorCompat(R.color.text_primary));
            }
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_overview) {
                showPage(PAGE_OVERVIEW);
                return true;
            }
            if (item.getItemId() == R.id.nav_parameters) {
                showPage(PAGE_PARAMETERS);
                return true;
            }
            if (item.getItemId() == R.id.nav_settings) {
                showPage(PAGE_SETTINGS);
                return true;
            }
            return false;
        });
        bindParameterList();
        bindSettingsControls();
        showPage(PAGE_OVERVIEW);
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        return view;
    }

    private MaterialCardView wrapCard(View child, int bottomMarginDp) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(getColorCompat(R.color.surface));
        card.setRadius(dp(8));
        card.setStrokeColor(getColorCompat(R.color.card_stroke));
        card.setStrokeWidth(1);
        card.setCardElevation(dp(2));
        card.setUseCompatPadding(true);
        card.addView(child);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(bottomMarginDp);
        card.setLayoutParams(params);
        return card;
    }

    private void showEmptyCells() {
        cellList.removeAllViews();
        TextView emptyCells = text(getString(R.string.empty_cells), 15, Color.rgb(92, 101, 112), Typeface.NORMAL);
        emptyCells.setPadding(dp(14), dp(14), dp(14), dp(14));
        cellList.addView(wrapCard(emptyCells, 8));
    }

    private void showConnectedContent() {
        placeholderConnect.setVisibility(View.GONE);
        connectedOverviewContent.setVisibility(View.VISIBLE);
    }

    private void clearDeviceData() {
        lastBasicInfo = null;
        voltageText.setText("--");
        currentText.setText("--");
        powerText.setText("--");
        socText.setText("--%");
        socProgress.setProgress(0);
        capacityText.setText("--");
        cyclesText.setText("--");
        mosText.setText("--");
        statsText.setText("--");
        temperaturesText.setText("--");
        temperaturesText.setVisibility(View.VISIBLE);
        temperaturesChipGroup.removeAllViews();
        temperaturesChipGroup.setVisibility(View.GONE);
        cellMinText.setText("--");
        cellMaxText.setText("--");
        cellDeltaText.setText("--");
        cellAverageText.setText("--");
        cellStatsGrid.setVisibility(View.GONE);
        showEmptyCells();
        updateParametersPage();
        setStatus(getString(R.string.status_select_bms));
        connectedOverviewContent.setVisibility(View.GONE);
        placeholderConnect.setVisibility(View.VISIBLE);
    }

    private void openDeviceList() {
        startActivityForResult(new Intent(this, DeviceListActivity.class), REQUEST_SELECT_DEVICE);
    }

    private void maybeAutoConnect() {
        if (!VALUE_ON.equals(settings().getString(PREF_AUTO_CONNECT, VALUE_OFF))) {
            return;
        }
        String address = settings().getString(PREF_LAST_DEVICE_ADDRESS, "");
        if (address.length() == 0) {
            setStatus(getString(R.string.status_auto_connect_no_device));
            return;
        }
        String name = settings().getString(PREF_LAST_DEVICE_NAME, address);
        connectionManager.connect(address, name.length() == 0 ? address : name);
    }

    private void rememberDevice(String address, String name) {
        settings().edit()
                .putString(PREF_LAST_DEVICE_ADDRESS, address)
                .putString(PREF_LAST_DEVICE_NAME, name == null ? address : name)
                .apply();
    }

    private void renderBasicInfo(JbdBasicInfo info) {
        lastBasicInfo = info;
        voltageText.setText(String.format(Locale.US, "%.2f V", info.totalVoltage));
        currentText.setText(String.format(Locale.US, "%.2f A", info.current));
        powerText.setText(String.format(Locale.US, "%.1f W", info.totalVoltage * info.current));
        socText.setText(info.soc + "%");
        socProgress.setProgress(info.soc);
        capacityText.setText(String.format(Locale.US, "%.2f / %.2f Ah", info.remainingAh, info.nominalAh));
        cyclesText.setText(String.format(Locale.US, "%d", info.cycleCount));
        mosText.setText(getString(R.string.label_charge) + " " + onOff(info.chargeEnabled) + "  /  " + getString(R.string.label_discharge) + " " + onOff(info.dischargeEnabled));
        statsText.setText(getString(R.string.pack_stats, info.cellCount, info.ntcCount, info.softwareVersion));
        updateParametersPage();
        if (!info.temperaturesC.isEmpty()) {
            temperaturesText.setVisibility(View.GONE);
            temperaturesChipGroup.removeAllViews();
            temperaturesChipGroup.setVisibility(View.VISIBLE);
            temperaturesChipGroup.addView(temperatureChip(String.format(Locale.US, getString(R.string.temperature_mos_item), 0, displayTemperature(info.temperaturesC.get(0)), temperatureUnitLabel()), true));
            for (int i = 0; i < info.temperaturesC.size(); i++) {
                temperaturesChipGroup.addView(temperatureChip(String.format(Locale.US, getString(R.string.temperature_probe_item), i + 1, displayTemperature(info.temperaturesC.get(i)), temperatureUnitLabel()), false));
            }
        } else {
            temperaturesChipGroup.removeAllViews();
            temperaturesChipGroup.setVisibility(View.GONE);
            temperaturesText.setVisibility(View.VISIBLE);
            temperaturesText.setText(getString(R.string.temperature_none));
        }
    }

    private Chip temperatureChip(String text, boolean primary) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCheckable(false);
        chip.setClickable(false);
        chip.setTextColor(getColorCompat(primary ? R.color.primary : R.color.text_primary));
        chip.setChipBackgroundColorResource(R.color.surface);
        chip.setChipStrokeColorResource(primary ? R.color.primary : R.color.card_stroke);
        chip.setChipStrokeWidth(dp(1));
        chip.setChipMinHeight(dp(36));
        chip.setTextSize(14);
        chip.setEnsureMinTouchTargetSize(false);
        return chip;
    }

    private void renderCells(JbdCellVoltages voltages) {
        cellList.removeAllViews();
        cellStatsGrid.setVisibility(View.VISIBLE);
        cellMinText.setText(String.format(Locale.US, "%.3f V", voltages.min));
        cellMaxText.setText(String.format(Locale.US, "%.3f V", voltages.max));
        cellDeltaText.setText(String.format(Locale.US, "%.3f V", voltages.delta));
        cellAverageText.setText(String.format(Locale.US, "%.3f V", voltages.average));
        for (int i = 0; i < voltages.cells.size(); i++) {
            cellList.addView(cellVoltageRow(i + 1, voltages.cells.get(i), voltages.min, voltages.max));
        }
    }

    private View cellVoltageRow(int index, float voltage, float min, float max) {
        View row = LayoutInflater.from(this).inflate(R.layout.row_cell_voltage, cellList, false);
        TextView label = row.findViewById(R.id.txt_cell_label);
        TextView value = row.findViewById(R.id.txt_cell_value);
        LinearProgressIndicator progress = row.findViewById(R.id.progress_cell);
        label.setText(String.format(Locale.US, getString(R.string.cell_label), index));
        value.setText(String.format(Locale.US, "%.3f V", voltage));
        float span = Math.max(0.001f, max - min);
        int scaled = Math.max(80, Math.min(1000, (int) (1000f * ((voltage - min) / span))));
        progress.setProgress(scaled);
        int color = voltage == max ? Color.rgb(0, 166, 118) : voltage == min ? Color.rgb(21, 101, 192) : Color.rgb(117, 125, 138);
        progress.setIndicatorColor(color);
        return row;
    }

    private String onOff(boolean value) {
        return value ? getString(R.string.label_on) : getString(R.string.label_off);
    }

    private void showPage(int page) {
        boolean overview = page == PAGE_OVERVIEW;
        boolean parameters = page == PAGE_PARAMETERS;
        overviewPage.setVisibility(overview ? View.VISIBLE : View.GONE);
        parametersPage.setVisibility(parameters ? View.VISIBLE : View.GONE);
        settingsPage.setVisibility(page == PAGE_SETTINGS ? View.VISIBLE : View.GONE);
        if (overview) {
            toolbar.setTitle(getString(R.string.overview_title));
        } else if (parameters) {
            toolbar.setTitle(getString(R.string.parameters_title));
        } else {
            toolbar.setTitle(getString(R.string.settings_title));
        }
        toolbar.setNavigationIcon(R.drawable.ic_view_list_24);
        toolbar.setNavigationIconTint(getColorCompat(R.color.text_primary));
        MenuItem disconnectItem = toolbar.getMenu().findItem(R.id.action_disconnect);
        if (disconnectItem != null) {
            disconnectItem.setVisible(overview && connected);
        }
        MenuItem dashboardItem = toolbar.getMenu().findItem(R.id.action_dashboard);
        if (dashboardItem != null) {
            dashboardItem.setVisible(overview && connected);
        }
        mainScroll.scrollTo(0, 0);
    }

    private void bindParameterList() {
        parametersAdapter = new ParametersAdapter();
        parametersList.setAdapter(parametersAdapter);
    }

    private void updateParametersPage() {
        if (placeholderParameters == null || parametersCard == null || parametersAdapter == null) {
            return;
        }
        boolean hasData = lastBasicInfo != null;
        placeholderParameters.setVisibility(hasData ? View.GONE : View.VISIBLE);
        parametersCard.setVisibility(hasData ? View.VISIBLE : View.GONE);
        parametersAdapter.notifyDataSetChanged();
    }

    private void bindSettingsControls() {
        settingsAdapter = new SettingsAdapter();
        settingsList.setAdapter(settingsAdapter);
        settingsList.setOnItemClickListener((parent, view, position, id) -> showSettingMenu(view, position));
    }

    private void showSettingMenu(View anchor, int position) {
        if (position == 0) {
            showRadioDialog(R.string.setting_theme, PREF_THEME,
                    new String[]{getString(R.string.setting_theme_auto), getString(R.string.setting_theme_light), getString(R.string.setting_theme_dark)},
                    new String[]{VALUE_AUTO, VALUE_LIGHT, VALUE_DARK},
                    value -> {
                        settings().edit().putString(PREF_THEME, value).apply();
                        applyThemePreference(this);
                        recreate();
                    });
        } else if (position == 1) {
            showRadioDialog(R.string.setting_language, PREF_LANGUAGE,
                    new String[]{getString(R.string.setting_language_auto), getString(R.string.setting_language_zh), getString(R.string.setting_language_en)},
                    new String[]{VALUE_AUTO, VALUE_ZH, VALUE_EN},
                    value -> {
                        settings().edit().putString(PREF_LANGUAGE, value).apply();
                        recreate();
                    });
        } else if (position == 2) {
            showRadioDialog(R.string.setting_temperature_unit, PREF_TEMP_UNIT,
                    new String[]{getString(R.string.setting_temp_celsius), getString(R.string.setting_temp_fahrenheit)},
                    new String[]{VALUE_C, VALUE_F},
                    value -> {
                        settings().edit().putString(PREF_TEMP_UNIT, value).apply();
                        settingsAdapter.notifyDataSetChanged();
                        if (lastBasicInfo != null) {
                            renderBasicInfo(lastBasicInfo);
                        }
                    });
        } else if (position == 3) {
            showRadioDialog(R.string.setting_refresh_interval, PREF_REFRESH_INTERVAL_MS,
                    new String[]{getString(R.string.setting_refresh_1s), getString(R.string.setting_refresh_2s), getString(R.string.setting_refresh_5s), getString(R.string.setting_refresh_10s)},
                    new String[]{VALUE_REFRESH_1S, VALUE_REFRESH_2S, VALUE_REFRESH_5S, VALUE_REFRESH_10S},
                    value -> {
                        settings().edit().putString(PREF_REFRESH_INTERVAL_MS, value).apply();
                        settingsAdapter.notifyDataSetChanged();
                        if (connected) {
                            connectionManager.setRefreshInterval(refreshIntervalMs());
                        }
                    });
        } else if (position == 4) {
            showRadioDialog(R.string.setting_auto_connect, PREF_AUTO_CONNECT,
                    new String[]{getString(R.string.setting_auto_connect_on), getString(R.string.setting_auto_connect_off)},
                    new String[]{VALUE_ON, VALUE_OFF},
                    value -> {
                        settings().edit().putString(PREF_AUTO_CONNECT, value).apply();
                        settingsAdapter.notifyDataSetChanged();
                        if (VALUE_ON.equals(value) && !connected) {
                            maybeAutoConnect();
                        }
                    });
        }
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
        new MaterialAlertDialogBuilder(this)
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
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private static Context preferredContext(Context context) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        applyLanguageConfiguration(context, configuration);
        applyNightConfiguration(context, configuration);
        return context.createConfigurationContext(configuration);
    }

    private static void applyLanguageConfiguration(Context context, Configuration configuration) {
        String language = context.getSharedPreferences(PREFS, MODE_PRIVATE).getString(PREF_LANGUAGE, VALUE_AUTO);
        if (VALUE_AUTO.equals(language)) {
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
        String theme = context.getSharedPreferences(PREFS, MODE_PRIVATE).getString(PREF_THEME, VALUE_AUTO);
        if (VALUE_DARK.equals(theme)) {
            configuration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | Configuration.UI_MODE_NIGHT_YES;
        } else if (VALUE_LIGHT.equals(theme)) {
            configuration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | Configuration.UI_MODE_NIGHT_NO;
        }
    }

    private static void applyThemePreference(Context context) {
        String theme = context.getSharedPreferences(PREFS, MODE_PRIVATE).getString(PREF_THEME, VALUE_AUTO);
        if (VALUE_DARK.equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if (VALUE_LIGHT.equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    private float displayTemperature(float celsius) {
        if (VALUE_F.equals(settings().getString(PREF_TEMP_UNIT, VALUE_C))) {
            return (celsius * 9.0f / 5.0f) + 32.0f;
        }
        return celsius;
    }

    private String temperatureUnitLabel() {
        if (VALUE_F.equals(settings().getString(PREF_TEMP_UNIT, VALUE_C))) {
            return getString(R.string.unit_fahrenheit);
        }
        return getString(R.string.unit_celsius);
    }

    private String themeLabel(String value) {
        if (VALUE_LIGHT.equals(value)) {
            return getString(R.string.setting_theme_light);
        }
        if (VALUE_DARK.equals(value)) {
            return getString(R.string.setting_theme_dark);
        }
        return getString(R.string.setting_theme_auto);
    }

    private String languageLabel(String value) {
        if (VALUE_ZH.equals(value)) {
            return getString(R.string.setting_language_zh);
        }
        if (VALUE_EN.equals(value)) {
            return getString(R.string.setting_language_en);
        }
        return getString(R.string.setting_language_auto);
    }

    private String tempUnitLabel(String value) {
        return VALUE_F.equals(value) ? getString(R.string.setting_temp_fahrenheit) : getString(R.string.setting_temp_celsius);
    }

    private long refreshIntervalMs() {
        String value = settings().getString(PREF_REFRESH_INTERVAL_MS, VALUE_REFRESH_2S);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return Long.parseLong(VALUE_REFRESH_2S);
        }
    }

    private String refreshIntervalLabel(String value) {
        if (VALUE_REFRESH_1S.equals(value)) {
            return getString(R.string.setting_refresh_1s);
        }
        if (VALUE_REFRESH_5S.equals(value)) {
            return getString(R.string.setting_refresh_5s);
        }
        if (VALUE_REFRESH_10S.equals(value)) {
            return getString(R.string.setting_refresh_10s);
        }
        return getString(R.string.setting_refresh_2s);
    }

    private String autoConnectLabel(String value) {
        return VALUE_ON.equals(value) ? getString(R.string.setting_auto_connect_on) : getString(R.string.setting_auto_connect_off);
    }

    private void renderState(BmsStateStore.Snapshot snapshot) {
        connected = snapshot.connected;
        connectedDeviceName = snapshot.deviceName;
        if (snapshot.basicInfo == null) {
            if (!snapshot.connected && snapshot.deviceName == null && snapshot.status != null && snapshot.status.length() > 0) {
                clearDeviceData();
                setStatus(snapshot.status);
                updateToolbar();
            } else if (snapshot.status != null && snapshot.status.length() > 0) {
                setStatus(snapshot.status);
                showConnectedContent();
                updateToolbar();
            }
            return;
        }
        showConnectedContent();
        setStatus(snapshot.status);
        renderBasicInfo(snapshot.basicInfo);
        if (snapshot.cellVoltages != null) {
            renderCells(snapshot.cellVoltages);
        }
        updateToolbar();
    }

    private final class ParametersAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return 20;
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
                row = LayoutInflater.from(MainActivity.this).inflate(R.layout.row_setting_item, parent, false);
            }
            ImageView icon = row.findViewById(R.id.img_setting_icon);
            TextView title = row.findViewById(R.id.txt_setting_title);
            TextView subtitle = row.findViewById(R.id.txt_setting_subtitle);
            icon.setImageResource(parameterIcon(position));
            title.setText(parameterTitle(position));
            subtitle.setText(parameterValue(position));
            return row;
        }

        private int parameterIcon(int position) {
            if (position == 0 || position == 1) {
                return R.drawable.ic_bluetooth_searching_24;
            }
            if (position == 8 || position == 9) {
                return R.drawable.ic_battery_5_bar_24;
            }
            if (position == 10 || position == 11) {
                return R.drawable.ic_electric_bolt_24;
            }
            if (position == 12 || position == 13 || position == 14) {
                return R.drawable.ic_dashboard_24;
            }
            return R.drawable.ic_info_24;
        }

        private int parameterTitle(int position) {
            switch (position) {
                case 0:
                    return R.string.param_bluetooth_name;
                case 1:
                    return R.string.param_device_address;
                case 2:
                    return R.string.param_bms_version;
                case 3:
                    return R.string.param_manufacturing_date;
                case 4:
                    return R.string.param_nominal_capacity;
                case 5:
                    return R.string.param_remaining_capacity;
                case 6:
                    return R.string.param_soc;
                case 7:
                    return R.string.param_cycle_count;
                case 8:
                    return R.string.param_cell_count;
                case 9:
                    return R.string.param_ntc_count;
                case 10:
                    return R.string.param_charge_mos;
                case 11:
                    return R.string.param_discharge_mos;
                case 12:
                    return R.string.param_pack_voltage;
                case 13:
                    return R.string.param_pack_current;
                case 14:
                    return R.string.param_pack_power;
                case 15:
                    return R.string.param_serial_number;
                case 16:
                    return R.string.param_barcode;
                case 17:
                    return R.string.param_battery_model;
                case 18:
                    return R.string.param_manufacturer;
                default:
                    return R.string.param_bms_model;
            }
        }

        private String parameterValue(int position) {
            String unread = getString(R.string.param_unread);
            if (position == 0) {
                return connectedDeviceName == null || connectedDeviceName.length() == 0 ? unread : connectedDeviceName;
            }
            if (position == 1) {
                BmsStateStore.Snapshot snapshot = BmsStateStore.getSnapshot();
                return snapshot.deviceAddress == null || snapshot.deviceAddress.length() == 0 ? unread : snapshot.deviceAddress;
            }
            JbdBasicInfo info = lastBasicInfo;
            if (info == null) {
                return unread;
            }
            switch (position) {
                case 2:
                    return info.softwareVersion;
                case 3:
                    return info.productionDate;
                case 4:
                    return String.format(Locale.US, "%.2f Ah", info.nominalAh);
                case 5:
                    return String.format(Locale.US, "%.2f Ah", info.remainingAh);
                case 6:
                    return info.soc + "%";
                case 7:
                    return String.format(Locale.US, "%d", info.cycleCount);
                case 8:
                    return String.format(Locale.US, "%d", info.cellCount);
                case 9:
                    return String.format(Locale.US, "%d", info.ntcCount);
                case 10:
                    return onOff(info.chargeEnabled);
                case 11:
                    return onOff(info.dischargeEnabled);
                case 12:
                    return String.format(Locale.US, "%.2f V", info.totalVoltage);
                case 13:
                    return String.format(Locale.US, "%.2f A", info.current);
                case 14:
                    return String.format(Locale.US, "%.1f W", info.totalVoltage * info.current);
                default:
                    return unread;
            }
        }
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
                row = LayoutInflater.from(MainActivity.this).inflate(R.layout.row_setting_item, parent, false);
            }
            ImageView icon = row.findViewById(R.id.img_setting_icon);
            TextView title = row.findViewById(R.id.txt_setting_title);
            TextView subtitle = row.findViewById(R.id.txt_setting_subtitle);
            SharedPreferences prefs = settings();
            if (position == 0) {
                icon.setImageResource(R.drawable.ic_palette_24);
                title.setText(R.string.setting_theme);
                subtitle.setText(themeLabel(prefs.getString(PREF_THEME, VALUE_AUTO)));
            } else if (position == 1) {
                icon.setImageResource(R.drawable.ic_language_24);
                title.setText(R.string.setting_language);
                subtitle.setText(languageLabel(prefs.getString(PREF_LANGUAGE, VALUE_AUTO)));
            } else if (position == 2) {
                icon.setImageResource(R.drawable.ic_thermostat_24);
                title.setText(R.string.setting_temperature_unit);
                subtitle.setText(tempUnitLabel(prefs.getString(PREF_TEMP_UNIT, VALUE_C)));
            } else {
                icon.setImageResource(R.drawable.ic_loop_24);
                title.setText(R.string.setting_refresh_interval);
                subtitle.setText(refreshIntervalLabel(prefs.getString(PREF_REFRESH_INTERVAL_MS, VALUE_REFRESH_2S)));
                if (position == 4) {
                    icon.setImageResource(R.drawable.ic_bluetooth_searching_24);
                    title.setText(R.string.setting_auto_connect);
                    subtitle.setText(autoConnectLabel(prefs.getString(PREF_AUTO_CONNECT, VALUE_OFF)));
                }
            }
            return row;
        }
    }

    private interface ChoiceHandler {
        void onChoice(String value);
    }

    private void setStatus(String status) {
        statusText.setText(status);
    }

    private void updateToolbar() {
        if (toolbar == null) {
            return;
        }
        toolbar.setSubtitle(connected && connectedDeviceName != null ? connectedDeviceName : getString(R.string.toolbar_subtitle_local));
        Menu menu = toolbar.getMenu();
        MenuItem disconnectItem = menu.findItem(R.id.action_disconnect);
        if (disconnectItem != null) {
            disconnectItem.setVisible(overviewPage.getVisibility() == View.VISIBLE && connected);
        }
        MenuItem dashboardItem = menu.findItem(R.id.action_dashboard);
        if (dashboardItem != null) {
            dashboardItem.setVisible(overviewPage.getVisibility() == View.VISIBLE && connected);
        }
    }

    private void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int getColorCompat(int colorRes) {
        if (Build.VERSION.SDK_INT >= 23) {
            return getColor(colorRes);
        }
        return getResources().getColor(colorRes);
    }
}
