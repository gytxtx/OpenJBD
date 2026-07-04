package com.gytxtx.openjbd;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.gytxtx.openjbd.ble.BleConstants;

public final class DeviceListActivity extends AppCompatActivity {
    public static final String EXTRA_DEVICE_ADDRESS = "com.gytxtx.openjbd.DEVICE_ADDRESS";
    public static final String EXTRA_DEVICE_NAME = "com.gytxtx.openjbd.DEVICE_NAME";

    private static final int REQUEST_BLE_PERMISSIONS = 200;

    static boolean shouldOpenAppSettings(boolean canShowAnyPermissionRationale) {
        return !canShowAnyPermissionRationale;
    }

    private static final long SCAN_MS = 8000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, ScanResult> scanResults = new LinkedHashMap<>();

    private BluetoothAdapter adapter;
    private BluetoothLeScanner scanner;
    private boolean scanning;
    private boolean waitingForAppSettings;
    private int scanCallbackCount;
    private LinearLayout deviceList;
    private LinearLayout placeholderDevices;
    private ImageView placeholderIcon;
    private TextView placeholderTitle;
    private TextView placeholderSubtitle;
    private TextView statusText;
    private MaterialButton permissionActionButton;
    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;

    private final Runnable stopScanRunnable = new Runnable() {
        @Override
        public void run() {
            stopScan();
        }
    };

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                addScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            scanning = false;
            setRefreshing(false);
            setStatus(getString(R.string.status_scan_failed, errorCode));
        }
    };

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppSettings.preferredContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyThemePreference(this);
        super.onCreate(savedInstanceState);
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = manager == null ? null : manager.getAdapter();
        scanner = adapter == null ? null : adapter.getBluetoothLeScanner();
        setContentView(R.layout.activity_device_list);
        SystemBars.applyAppBars(this);
        toolbar = findViewById(R.id.device_top_app_bar);
        toolbar.setNavigationIconTint(getColorCompat(R.color.on_primary));
        swipeRefresh = findViewById(R.id.device_swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.surface);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                startScanWithPermissions();
            }
        });
        statusText = findViewById(R.id.txt_device_status);
        deviceList = findViewById(R.id.list_devices);
        placeholderDevices = findViewById(R.id.placeholder_devices);
        placeholderIcon = findViewById(R.id.img_device_placeholder);
        placeholderTitle = findViewById(R.id.txt_device_placeholder_title);
        placeholderSubtitle = findViewById(R.id.txt_device_placeholder_subtitle);
        permissionActionButton = findViewById(R.id.btn_device_permission_action);
        permissionActionButton.setOnClickListener(view -> recoverBlePermission());
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        toolbar.setOnMenuItemClickListener(new MaterialToolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_refresh) {
                    startScanWithPermissions();
                    return true;
                }
                return false;
            }
        });
        MenuItem refreshIcon = toolbar.getMenu().findItem(R.id.action_refresh);
        if (refreshIcon != null && refreshIcon.getIcon() != null) {
            refreshIcon.getIcon().setTint(getColorCompat(R.color.on_primary));
        }
        showPlaceholder(getString(R.string.device_placeholder_scanning_title), scanningSubtitle());
        startScanWithPermissions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScan();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!waitingForAppSettings) {
            return;
        }
        waitingForAppSettings = false;
        if (hasBlePermissions()) {
            startScanWithPermissions();
        } else {
            setStatus(getString(R.string.status_ble_permission_denied));
            showPermissionDeniedPlaceholder();
        }
    }

    private void startScanWithPermissions() {
        if (adapter == null || !adapter.isEnabled()) {
            setRefreshing(false);
            toast(getString(R.string.toast_bluetooth_off));
            setStatus(getString(R.string.status_bluetooth_off));
            showPlaceholder(getString(R.string.device_placeholder_bluetooth_off_title), getString(R.string.device_placeholder_bluetooth_off_subtitle));
            return;
        }
        if (!hasBlePermissions()) {
            setRefreshing(false);
            setStatus(getString(R.string.status_requesting_permissions));
            requestPermissions(requiredPermissions(), REQUEST_BLE_PERMISSIONS);
            return;
        }
        if (Build.VERSION.SDK_INT < 31 && !isLocationEnabled()) {
            setRefreshing(false);
            setStatus(getString(R.string.status_location_off));
            showPlaceholder(getString(R.string.device_placeholder_location_off_title), getString(R.string.device_placeholder_location_off_subtitle), R.drawable.ic_location_off_outline_24);
            return;
        }
        startScan();
    }

    @SuppressLint("MissingPermission")
    private void startScan() {
        if (scanner == null) {
            scanner = adapter.getBluetoothLeScanner();
        }
        if (scanner == null) {
            setRefreshing(false);
            setStatus(getString(R.string.status_ble_scanner_unavailable));
            showPlaceholder(getString(R.string.device_placeholder_scanner_unavailable_title), getString(R.string.device_placeholder_scanner_unavailable_subtitle));
            return;
        }
        stopScan();
        scanResults.clear();
        scanCallbackCount = 0;
        showPlaceholder(getString(R.string.device_placeholder_scanning_title), scanningSubtitle());
        scanning = true;
        setRefreshing(true);
        setStatus(getString(R.string.status_scanning));
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0L)
                .build();
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(BleConstants.SERVICE_UUID))
                .build());
        scanner.startScan(filters, settings, scanCallback);
        handler.postDelayed(stopScanRunnable, SCAN_MS);
    }

    @SuppressLint("MissingPermission")
    private void stopScan() {
        handler.removeCallbacks(stopScanRunnable);
        if (scanning && scanner != null) {
            scanner.stopScan(scanCallback);
        }
        if (scanning) {
            setStatus(getString(R.string.status_found_devices, scanResults.size(), scanCallbackCount));
            if (scanResults.isEmpty()) {
                showPlaceholder(getString(R.string.device_placeholder_no_devices_title), getString(R.string.device_placeholder_no_devices_subtitle));
            }
        }
        scanning = false;
        setRefreshing(false);
    }

    @SuppressLint("MissingPermission")
    private void addScanResult(final ScanResult result) {
        scanCallbackCount++;
        BluetoothDevice device = result.getDevice();
        String address = device.getAddress();
        if (scanResults.containsKey(address)) {
            return;
        }
        scanResults.put(address, result);
        if (scanResults.size() == 1) {
            deviceList.removeAllViews();
            placeholderDevices.setVisibility(View.GONE);
            deviceList.setVisibility(View.VISIBLE);
        }
        final String name = deviceName(result);
        final boolean display = isDisplayDevice(result);
        View row = LayoutInflater.from(this).inflate(R.layout.row_device_list_item, deviceList, false);
        ImageView icon = row.findViewById(R.id.img_device_type);
        TextView title = row.findViewById(R.id.txt_device_title);
        TextView subtitle = row.findViewById(R.id.txt_device_subtitle);
        icon.setImageResource(display ? R.drawable.ic_monitor_24 : R.drawable.ic_battery_4_bar_24);
        icon.setColorFilter(getColorCompat(R.color.icon_default));
        title.setText(name);
        subtitle.setText(getString(R.string.device_row_subtitle, display ? getString(R.string.device_type_display) : getString(R.string.device_type_battery), address, result.getRssi()));
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent data = new Intent();
                data.putExtra(EXTRA_DEVICE_ADDRESS, result.getDevice().getAddress());
                data.putExtra(EXTRA_DEVICE_NAME, name);
                setResult(RESULT_OK, data);
                finish();
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(72));
        deviceList.addView(row, params);
    }

    private void showPlaceholder(String title, String subtitle) {
        showPlaceholder(title, subtitle, R.drawable.ic_bluetooth_searching_24);
    }

    private void showPlaceholder(String title, String subtitle, int iconRes) {
        placeholderIcon.setImageResource(iconRes);
        placeholderTitle.setText(title);
        placeholderSubtitle.setText(subtitle);
        permissionActionButton.setVisibility(View.GONE);
        placeholderDevices.setVisibility(View.VISIBLE);
        deviceList.setVisibility(View.GONE);
        deviceList.removeAllViews();
    }

    private void showPermissionDeniedPlaceholder() {
        showPlaceholder(
                getString(R.string.device_placeholder_permission_denied_title),
                getString(R.string.device_placeholder_permission_denied_subtitle),
                R.drawable.ic_bluetooth_disabled_24);
        boolean canShowAnyRationale = canShowAnyPermissionRationale();
        boolean openSettings = shouldOpenAppSettings(canShowAnyRationale);
        permissionActionButton.setText(openSettings
                ? R.string.action_open_app_settings
                : R.string.action_grant_permission);
        permissionActionButton.setTag(Boolean.valueOf(openSettings));
        permissionActionButton.setVisibility(View.VISIBLE);
    }

    private boolean canShowAnyPermissionRationale() {
        for (String permission : requiredPermissions()) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
                    && shouldShowRequestPermissionRationale(permission)) {
                return true;
            }
        }
        return false;
    }

    private void recoverBlePermission() {
        boolean openSettings = Boolean.TRUE.equals(permissionActionButton.getTag());
        if (openSettings) {
            waitingForAppSettings = true;
            Intent intent = new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            return;
        }
        setStatus(getString(R.string.status_requesting_permissions));
        requestPermissions(requiredPermissions(), REQUEST_BLE_PERMISSIONS);
    }

    private String scanningSubtitle() {
        if (Build.VERSION.SDK_INT < 31) {
            return getString(R.string.device_placeholder_scanning_subtitle_legacy_location);
        }
        return getString(R.string.device_placeholder_scanning_subtitle);
    }

    private boolean isDisplayDevice(ScanResult result) {
        byte[] record = result.getScanRecord() == null ? null : result.getScanRecord().getBytes();
        if (record == null) {
            return false;
        }
        String scanHex = toHex(record);
        String mac = result.getDevice().getAddress();
        String forward = mac.replace(":", "").toUpperCase(Locale.US);
        String reversed = reverseMacBytes(mac);
        return hasScreenType(scanHex, forward) || hasScreenType(scanHex, reversed);
    }

    private boolean hasScreenType(String scanHex, String macHex) {
        int index = scanHex.indexOf(macHex);
        if (index < 0 || index + 16 > scanHex.length()) {
            return false;
        }
        return "0003".equals(scanHex.substring(index + 12, index + 16));
    }

    private String reverseMacBytes(String mac) {
        String[] parts = mac.split(":");
        StringBuilder builder = new StringBuilder();
        for (int i = parts.length - 1; i >= 0; i--) {
            builder.append(parts[i]);
        }
        return builder.toString().toUpperCase(Locale.US);
    }

    private String toHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        char[] table = "0123456789ABCDEF".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            chars[i * 2] = table[value >>> 4];
            chars[i * 2 + 1] = table[value & 0x0F];
        }
        return new String(chars);
    }

    @SuppressLint("MissingPermission")
    private String deviceName(ScanResult result) {
        String name = null;
        if (result.getScanRecord() != null) {
            name = result.getScanRecord().getDeviceName();
        }
        if (name == null || name.length() == 0) {
            name = result.getDevice().getName();
        }
        return name == null || name.length() == 0 ? getString(R.string.device_unnamed) : name;
    }

    private boolean hasBlePermissions() {
        for (String permission : requiredPermissions()) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private String[] requiredPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 31) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        return permissions.toArray(new String[0]);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_BLE_PERMISSIONS) {
            return;
        }
        if (hasBlePermissions()) {
            startScan();
        } else {
            setRefreshing(false);
            toast(getString(R.string.toast_ble_permission_denied));
            setStatus(getString(R.string.status_ble_permission_denied));
            showPermissionDeniedPlaceholder();
        }
    }

    private boolean isLocationEnabled() {
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (manager == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= 28) {
            return manager.isLocationEnabled();
        }
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void setStatus(String status) {
        statusText.setText(status);
    }

    private void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void setRefreshing(boolean refreshing) {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(refreshing);
        }
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
