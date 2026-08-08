package com.gytxtx.openjbd

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.gytxtx.openjbd.ble.BleConstants
import java.util.LinkedHashMap
import java.util.Locale

class DeviceListActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_DEVICE_ADDRESS = "com.gytxtx.openjbd.DEVICE_ADDRESS"
        const val EXTRA_DEVICE_NAME = "com.gytxtx.openjbd.DEVICE_NAME"
        private const val REQUEST_BLE_PERMISSIONS = 200
        private const val SCAN_MS = 8000L

        @JvmStatic
        fun shouldOpenAppSettings(canShowAnyPermissionRationale: Boolean): Boolean =
            !canShowAnyPermissionRationale
    }

    private val handler = Handler(Looper.getMainLooper())
    private val scanResults = LinkedHashMap<String, ScanResult>()

    private var adapter: BluetoothAdapter? = null
    private var scanner: BluetoothLeScanner? = null
    private var scanning = false
    private var waitingForAppSettings = false
    private var scanCallbackCount = 0
    private lateinit var deviceList: LinearLayout
    private lateinit var placeholderDevices: LinearLayout
    private lateinit var placeholderIcon: ImageView
    private lateinit var placeholderTitle: TextView
    private lateinit var placeholderSubtitle: TextView
    private lateinit var statusText: TextView
    private lateinit var permissionActionButton: MaterialButton
    private lateinit var toolbar: MaterialToolbar
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private val stopScanRunnable = Runnable { stopScan() }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            addScanResult(result)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            for (result in results) {
                addScanResult(result)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            scanning = false
            setRefreshing(false)
            setStatus(getString(R.string.status_scan_failed, errorCode))
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppSettings.preferredContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettings.applyThemePreference(this)
        super.onCreate(savedInstanceState)
        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        adapter = manager?.adapter
        scanner = adapter?.bluetoothLeScanner
        setContentView(R.layout.activity_device_list)
        SystemBars.applyAppBars(this)
        toolbar = findViewById(R.id.device_top_app_bar)
        toolbar.setNavigationIconTint(getColorCompat(R.color.on_primary))
        swipeRefresh = findViewById(R.id.device_swipe_refresh)
        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent)
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.surface)
        swipeRefresh.setOnRefreshListener { startScanWithPermissions() }
        statusText = findViewById(R.id.txt_device_status)
        deviceList = findViewById(R.id.list_devices)
        placeholderDevices = findViewById(R.id.placeholder_devices)
        placeholderIcon = findViewById(R.id.img_device_placeholder)
        placeholderTitle = findViewById(R.id.txt_device_placeholder_title)
        placeholderSubtitle = findViewById(R.id.txt_device_placeholder_subtitle)
        permissionActionButton = findViewById(R.id.btn_device_permission_action)
        permissionActionButton.setOnClickListener { recoverBlePermission() }
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_refresh) {
                startScanWithPermissions()
                true
            } else {
                false
            }
        }
        val refreshIcon = toolbar.menu.findItem(R.id.action_refresh)
        refreshIcon?.icon?.setTint(getColorCompat(R.color.on_primary))
        showPlaceholder(getString(R.string.device_placeholder_scanning_title), scanningSubtitle())
        startScanWithPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScan()
    }

    override fun onResume() {
        super.onResume()
        if (!waitingForAppSettings) {
            return
        }
        waitingForAppSettings = false
        if (hasBlePermissions()) {
            startScanWithPermissions()
        } else {
            setStatus(getString(R.string.status_ble_permission_denied))
            showPermissionDeniedPlaceholder()
        }
    }

    private fun startScanWithPermissions() {
        if (adapter == null || adapter!!.isEnabled == false) {
            setRefreshing(false)
            toast(getString(R.string.toast_bluetooth_off))
            setStatus(getString(R.string.status_bluetooth_off))
            showPlaceholder(
                getString(R.string.device_placeholder_bluetooth_off_title),
                getString(R.string.device_placeholder_bluetooth_off_subtitle)
            )
            return
        }
        if (!hasBlePermissions()) {
            setRefreshing(false)
            setStatus(getString(R.string.status_requesting_permissions))
            requestPermissions(requiredPermissions(), REQUEST_BLE_PERMISSIONS)
            return
        }
        if (Build.VERSION.SDK_INT < 31 && !isLocationEnabled()) {
            setRefreshing(false)
            setStatus(getString(R.string.status_location_off))
            showPlaceholder(
                getString(R.string.device_placeholder_location_off_title),
                getString(R.string.device_placeholder_location_off_subtitle),
                R.drawable.ic_location_off_outline_24
            )
            return
        }
        startScan()
    }

    @SuppressLint("MissingPermission")
    private fun startScan() {
        if (scanner == null) {
            scanner = adapter!!.bluetoothLeScanner
        }
        if (scanner == null) {
            setRefreshing(false)
            setStatus(getString(R.string.status_ble_scanner_unavailable))
            showPlaceholder(
                getString(R.string.device_placeholder_scanner_unavailable_title),
                getString(R.string.device_placeholder_scanner_unavailable_subtitle)
            )
            return
        }
        stopScan()
        scanResults.clear()
        scanCallbackCount = 0
        showPlaceholder(getString(R.string.device_placeholder_scanning_title), scanningSubtitle())
        scanning = true
        setRefreshing(true)
        setStatus(getString(R.string.status_scanning))
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0L)
            .build()
        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
                .build()
        )
        scanner!!.startScan(filters, settings, scanCallback)
        handler.postDelayed(stopScanRunnable, SCAN_MS)
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        handler.removeCallbacks(stopScanRunnable)
        if (scanning && scanner != null) {
            scanner!!.stopScan(scanCallback)
        }
        if (scanning) {
            setStatus(getString(R.string.status_found_devices, scanResults.size, scanCallbackCount))
            if (scanResults.isEmpty()) {
                showPlaceholder(
                    getString(R.string.device_placeholder_no_devices_title),
                    getString(R.string.device_placeholder_no_devices_subtitle)
                )
            }
        }
        scanning = false
        setRefreshing(false)
    }

    @SuppressLint("MissingPermission")
    private fun addScanResult(result: ScanResult) {
        scanCallbackCount++
        val device = result.device
        val address = device.address
        if (scanResults.containsKey(address)) {
            return
        }
        scanResults[address] = result
        if (scanResults.size == 1) {
            deviceList.removeAllViews()
            placeholderDevices.visibility = View.GONE
            deviceList.visibility = View.VISIBLE
        }
        val name = deviceName(result)
        val display = isDisplayDevice(result)
        val row = LayoutInflater.from(this).inflate(R.layout.row_device_list_item, deviceList, false)
        val icon = row.findViewById<ImageView>(R.id.img_device_type)
        val title = row.findViewById<TextView>(R.id.txt_device_title)
        val subtitle = row.findViewById<TextView>(R.id.txt_device_subtitle)
        icon.setImageResource(if (display) R.drawable.ic_monitor_24 else R.drawable.ic_battery_4_bar_24)
        icon.contentDescription =
            if (display) getString(R.string.device_type_display) else getString(R.string.device_type_battery)
        icon.setColorFilter(getColorCompat(R.color.icon_default))
        title.text = name
        subtitle.text = getString(
            R.string.device_row_subtitle,
            if (display) getString(R.string.device_type_display) else getString(R.string.device_type_battery),
            address,
            result.rssi
        )
        row.setOnClickListener {
            val data = Intent()
            data.putExtra(EXTRA_DEVICE_ADDRESS, result.device.address)
            data.putExtra(EXTRA_DEVICE_NAME, name)
            setResult(RESULT_OK, data)
            finish()
        }
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(72))
        deviceList.addView(row, params)
    }

    private fun showPlaceholder(title: String, subtitle: String) {
        showPlaceholder(title, subtitle, R.drawable.ic_bluetooth_searching_24)
    }

    private fun showPlaceholder(title: String, subtitle: String, iconRes: Int) {
        placeholderIcon.setImageResource(iconRes)
        placeholderTitle.text = title
        placeholderSubtitle.text = subtitle
        permissionActionButton.visibility = View.GONE
        placeholderDevices.visibility = View.VISIBLE
        deviceList.visibility = View.GONE
        deviceList.removeAllViews()
    }

    private fun showPermissionDeniedPlaceholder() {
        showPlaceholder(
            getString(R.string.device_placeholder_permission_denied_title),
            getString(R.string.device_placeholder_permission_denied_subtitle),
            R.drawable.ic_bluetooth_disabled_24
        )
        val canShowAnyRationale = canShowAnyPermissionRationale()
        val openSettings = shouldOpenAppSettings(canShowAnyRationale)
        permissionActionButton.setText(
            if (openSettings) R.string.action_open_app_settings else R.string.action_grant_permission
        )
        permissionActionButton.tag = java.lang.Boolean.valueOf(openSettings)
        permissionActionButton.visibility = View.VISIBLE
    }

    private fun canShowAnyPermissionRationale(): Boolean {
        for (permission in requiredPermissions()) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
                && shouldShowRequestPermissionRationale(permission)
            ) {
                return true
            }
        }
        return false
    }

    private fun recoverBlePermission() {
        val openSettings = java.lang.Boolean.TRUE == permissionActionButton.tag
        if (openSettings) {
            waitingForAppSettings = true
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }
        setStatus(getString(R.string.status_requesting_permissions))
        requestPermissions(requiredPermissions(), REQUEST_BLE_PERMISSIONS)
    }

    private fun scanningSubtitle(): String {
        if (Build.VERSION.SDK_INT < 31) {
            return getString(R.string.device_placeholder_scanning_subtitle_legacy_location)
        }
        return getString(R.string.device_placeholder_scanning_subtitle)
    }

    private fun isDisplayDevice(result: ScanResult): Boolean {
        val record = result.scanRecord?.bytes ?: return false
        val scanHex = toHex(record)
        val mac = result.device.address
        val forward = mac.replace(":", "").uppercase(Locale.US)
        val reversed = reverseMacBytes(mac)
        return hasScreenType(scanHex, forward) || hasScreenType(scanHex, reversed)
    }

    private fun hasScreenType(scanHex: String, macHex: String): Boolean {
        val index = scanHex.indexOf(macHex)
        if (index < 0 || index + 16 > scanHex.length) {
            return false
        }
        return "0003" == scanHex.substring(index + 12, index + 16)
    }

    private fun reverseMacBytes(mac: String): String {
        val parts = mac.split(":")
        val builder = StringBuilder()
        for (i in parts.indices.reversed()) {
            builder.append(parts[i])
        }
        return builder.toString().uppercase(Locale.US)
    }

    private fun toHex(bytes: ByteArray): String {
        val chars = CharArray(bytes.size * 2)
        val table = "0123456789ABCDEF".toCharArray()
        for (i in bytes.indices) {
            val value = bytes[i].toInt() and 0xFF
            chars[i * 2] = table[value ushr 4]
            chars[i * 2 + 1] = table[value and 0x0F]
        }
        return String(chars)
    }

    @SuppressLint("MissingPermission")
    private fun deviceName(result: ScanResult): String {
        var name: String? = null
        if (result.scanRecord != null) {
            name = result.scanRecord!!.deviceName
        }
        if (name.isNullOrEmpty()) {
            name = result.device.name
        }
        return if (name.isNullOrEmpty()) getString(R.string.device_unnamed) else name!!
    }

    private fun hasBlePermissions(): Boolean {
        for (permission in requiredPermissions()) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= 31) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_BLE_PERMISSIONS) {
            return
        }
        if (hasBlePermissions()) {
            startScan()
        } else {
            setRefreshing(false)
            toast(getString(R.string.toast_ble_permission_denied))
            setStatus(getString(R.string.status_ble_permission_denied))
            showPermissionDeniedPlaceholder()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val manager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return if (Build.VERSION.SDK_INT >= 28) {
            manager.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    private fun setStatus(status: String) {
        statusText.text = status
    }

    private fun toast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun setRefreshing(refreshing: Boolean) {
        swipeRefresh.isRefreshing = refreshing
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun getColorCompat(colorRes: Int): Int =
        if (Build.VERSION.SDK_INT >= 23) getColor(colorRes) else resources.getColor(colorRes)
}
