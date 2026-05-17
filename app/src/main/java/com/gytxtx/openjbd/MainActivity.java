package com.gytxtx.openjbd;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public final class MainActivity extends AppCompatActivity implements BmsStateStore.Listener {
    private static final int REQUEST_SELECT_DEVICE = 101;
    private static final String STATE_CURRENT_PAGE = "current_page";
    private static final int PAGE_OVERVIEW = 0;
    private static final int PAGE_PARAMETERS = 1;
    private static final int PAGE_SETTINGS = 2;
    private static final long PAGE_TRANSITION_MS = 160L;
    private static final String TAG_OVERVIEW = "overview";
    private static final String TAG_PARAMETERS = "parameters";
    private static final String TAG_SETTINGS = "settings";

    private boolean connected;
    private String connectedDeviceName;
    private BmsStateStore.ConnectionState connectionState = BmsStateStore.ConnectionState.DISCONNECTED;
    private int currentPage = -1;

    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigationView;
    private BmsConnectionManager connectionManager;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppSettings.preferredContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyThemePreference(this);
        super.onCreate(savedInstanceState);
        connectionManager = BmsConnectionManager.getInstance(this);
        connectionManager.refreshLocalizedStatus();
        connectionManager.setRefreshInterval(AppSettings.refreshIntervalMs(this));
        configureAutoReconnect();
        buildUi();
        updateToolbar();
        int initialPage = savedInstanceState == null ? PAGE_OVERVIEW : savedInstanceState.getInt(STATE_CURRENT_PAGE, PAGE_OVERVIEW);
        int initialItemId = navItemId(initialPage);
        if (bottomNavigationView.getSelectedItemId() == initialItemId) {
            showPage(initialPage);
        } else {
            bottomNavigationView.setSelectedItemId(initialItemId);
        }
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
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_CURRENT_PAGE, currentPage < 0 ? PAGE_OVERVIEW : currentPage);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBmsStateChanged(final BmsStateStore.Snapshot snapshot) {
        renderState(snapshot);
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
        toolbar.setNavigationIconTint(getColor(R.color.text_primary));
        bottomNavigationView = findViewById(R.id.bottom_navigation);

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
                toolbarItem.getIcon().setTint(getColor(R.color.text_primary));
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
    }

    private void showPage(int page) {
        if (page == currentPage) {
            return;
        }
        int previousPage = currentPage;
        currentPage = page;
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fragment_material_enter, R.anim.fragment_material_exit);
        Fragment target = fragmentManager.findFragmentByTag(pageTag(page));
        if (target == null) {
            target = createPageFragment(page);
            transaction.add(R.id.main_fragment_container, target, pageTag(page));
        }
        hidePage(transaction, fragmentManager, PAGE_OVERVIEW, page);
        hidePage(transaction, fragmentManager, PAGE_PARAMETERS, page);
        hidePage(transaction, fragmentManager, PAGE_SETTINGS, page);
        transaction.show(target).commit();
        updateToolbar();
        if (previousPage >= 0) {
            animateSelectedNavItem(page);
        }
    }

    private Fragment createPageFragment(int page) {
        if (page == PAGE_PARAMETERS) {
            return new ParametersFragment();
        }
        if (page == PAGE_SETTINGS) {
            return new SettingsFragment();
        }
        return new OverviewFragment();
    }

    private void hidePage(FragmentTransaction transaction, FragmentManager fragmentManager, int page, int visiblePage) {
        if (page == visiblePage) {
            return;
        }
        Fragment fragment = fragmentManager.findFragmentByTag(pageTag(page));
        if (fragment != null) {
            transaction.hide(fragment);
        }
    }

    private String pageTag(int page) {
        if (page == PAGE_PARAMETERS) {
            return TAG_PARAMETERS;
        }
        if (page == PAGE_SETTINGS) {
            return TAG_SETTINGS;
        }
        return TAG_OVERVIEW;
    }

    private void animateSelectedNavItem(int page) {
        View itemView = bottomNavigationView.findViewById(navItemId(page));
        if (itemView == null) {
            return;
        }
        itemView.animate().cancel();
        itemView.setScaleX(0.96f);
        itemView.setScaleY(0.96f);
        itemView.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(PAGE_TRANSITION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private int navItemId(int page) {
        if (page == PAGE_PARAMETERS) {
            return R.id.nav_parameters;
        }
        if (page == PAGE_SETTINGS) {
            return R.id.nav_settings;
        }
        return R.id.nav_overview;
    }

    private void renderState(BmsStateStore.Snapshot snapshot) {
        connected = snapshot.connected;
        connectedDeviceName = snapshot.deviceName;
        connectionState = snapshot.connectionState;
        updateToolbar();
    }

    private void updateToolbar() {
        if (toolbar == null) {
            return;
        }
        toolbar.setTitle(pageTitle());
        toolbar.setSubtitle(shouldShowDeviceSubtitle() ? connectedDeviceName : getString(R.string.toolbar_subtitle_local));
        toolbar.setNavigationIcon(R.drawable.ic_list_24);
        toolbar.setNavigationIconTint(getColor(R.color.text_primary));
        Menu menu = toolbar.getMenu();
        MenuItem disconnectItem = menu.findItem(R.id.action_disconnect);
        if (disconnectItem != null) {
            disconnectItem.setVisible(currentPage == PAGE_OVERVIEW && connected);
        }
        MenuItem dashboardItem = menu.findItem(R.id.action_dashboard);
        if (dashboardItem != null) {
            dashboardItem.setVisible(currentPage == PAGE_OVERVIEW && connected);
        }
    }

    private boolean shouldShowDeviceSubtitle() {
        if (connectedDeviceName == null || connectedDeviceName.length() == 0) {
            return false;
        }
        return connected
                || connectionState == BmsStateStore.ConnectionState.CONNECTING
                || connectionState == BmsStateStore.ConnectionState.DISCOVERING_SERVICES
                || connectionState == BmsStateStore.ConnectionState.ENABLING_NOTIFICATIONS
                || connectionState == BmsStateStore.ConnectionState.WAITING_RECONNECT
                || isDeviceScopedFailureState();
    }

    private boolean isDeviceScopedFailureState() {
        return connectionState == BmsStateStore.ConnectionState.CONNECTION_FAILED
                || connectionState == BmsStateStore.ConnectionState.SERVICE_DISCOVERY_FAILED
                || connectionState == BmsStateStore.ConnectionState.SERVICE_NOT_FOUND
                || connectionState == BmsStateStore.ConnectionState.CHARACTERISTICS_NOT_FOUND
                || connectionState == BmsStateStore.ConnectionState.NOTIFICATIONS_FAILED
                || connectionState == BmsStateStore.ConnectionState.INVALID_DEVICE
                || connectionState == BmsStateStore.ConnectionState.ERROR
                || connectionState == BmsStateStore.ConnectionState.PARSE_ERROR;
    }

    private String pageTitle() {
        if (currentPage == PAGE_PARAMETERS) {
            return getString(R.string.parameters_title);
        }
        if (currentPage == PAGE_SETTINGS) {
            return getString(R.string.settings_title);
        }
        return getString(R.string.overview_title);
    }

    private void openDeviceList() {
        startActivityForResult(new Intent(this, DeviceListActivity.class), REQUEST_SELECT_DEVICE);
    }

    private void maybeAutoConnect() {
        SharedPreferences prefs = AppSettings.prefs(this);
        if (!AppSettings.autoConnectEnabled(this)) {
            return;
        }
        String address = prefs.getString(AppSettings.PREF_LAST_DEVICE_ADDRESS, "");
        if (address.length() == 0) {
            BmsStateStore.update(BmsStateStore.Snapshot.withConnectionState(BmsStateStore.ConnectionState.INVALID_DEVICE, false, null, null, getString(R.string.status_auto_connect_no_device), null, null));
            return;
        }
        String name = prefs.getString(AppSettings.PREF_LAST_DEVICE_NAME, address);
        connectionManager.connect(address, name.length() == 0 ? address : name);
    }

    private void rememberDevice(String address, String name) {
        AppSettings.prefs(this).edit()
                .putString(AppSettings.PREF_LAST_DEVICE_ADDRESS, address)
                .putString(AppSettings.PREF_LAST_DEVICE_NAME, name == null ? address : name)
                .apply();
        configureAutoReconnect();
    }

    private void configureAutoReconnect() {
        SharedPreferences prefs = AppSettings.prefs(this);
        boolean enabled = AppSettings.autoConnectEnabled(this);
        String address = prefs.getString(AppSettings.PREF_LAST_DEVICE_ADDRESS, "");
        String name = prefs.getString(AppSettings.PREF_LAST_DEVICE_NAME, address);
        connectionManager.setAutoReconnect(enabled, address, name);
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
