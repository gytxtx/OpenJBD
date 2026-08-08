package com.gytxtx.openjbd

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.gytxtx.openjbd.data.BmsRepository
import com.gytxtx.openjbd.data.BmsUiState
import com.gytxtx.openjbd.data.ConnectionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var connected = false
    private var currentPage = -1

    private lateinit var toolbar: MaterialToolbar
    private lateinit var bottomNavigationView: BottomNavigationView

    @Inject lateinit var connectionManager: BmsConnectionManager
    @Inject lateinit var repository: BmsRepository

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppSettings.preferredContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettings.applyThemePreference(this)
        super.onCreate(savedInstanceState)
        connectionManager.refreshLocalizedStatus()
        connectionManager.setRefreshInterval(AppSettings.refreshIntervalMs(this))
        configureAutoReconnect()
        buildUi()
        updateToolbar()
        val initialPage = if (savedInstanceState == null) PAGE_OVERVIEW
            else savedInstanceState.getInt(STATE_CURRENT_PAGE, PAGE_OVERVIEW)
        val initialItemId = navItemId(initialPage)
        if (bottomNavigationView.selectedItemId == initialItemId) showPage(initialPage)
        else bottomNavigationView.selectedItemId = initialItemId
        maybeAutoConnect()
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                repository.uiState.collect { renderState(it) }
            }
        }
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_CURRENT_PAGE, if (currentPage < 0) PAGE_OVERVIEW else currentPage)
        super.onSaveInstanceState(outState)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_SELECT_DEVICE || resultCode != RESULT_OK || data == null) return
        val address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS) ?: run { toast(getString(R.string.status_bluetooth_unavailable)); return }
        val name = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_NAME)
        rememberDevice(address, name)
        connectionManager.connect(address, name ?: address)
    }

    private fun buildUi() {
        setContentView(R.layout.activity_main)
        SystemBars.applyAppBars(this)

        toolbar = findViewById(R.id.top_app_bar)
        toolbar.setNavigationIconTint(getColor(R.color.on_primary))
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        toolbar.setNavigationOnClickListener { openDeviceList() }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_disconnect -> { connectionManager.disconnect(); true }
                R.id.action_dashboard -> { startActivity(Intent(this@MainActivity, DashboardActivity::class.java)); true }
                else -> false
            }
        }
        for (i in 0 until toolbar.menu.size()) {
            toolbar.menu.getItem(i)?.icon?.setTint(getColor(R.color.on_primary))
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_overview -> showPage(PAGE_OVERVIEW)
                R.id.nav_parameters -> showPage(PAGE_PARAMETERS)
                R.id.nav_settings -> showPage(PAGE_SETTINGS)
                else -> return@setOnItemSelectedListener false
            }
            true
        }
    }

    private fun showPage(page: Int) {
        if (page == currentPage) return
        val previousPage = currentPage
        currentPage = page
        val transaction = supportFragmentManager.beginTransaction()
        if (systemAnimationsEnabled()) transaction.setCustomAnimations(R.anim.fragment_material_enter, R.anim.fragment_material_exit)
        var target = supportFragmentManager.findFragmentByTag(pageTag(page))
        if (target == null) { target = createPageFragment(page); transaction.add(R.id.main_fragment_container, target, pageTag(page)) }
        hidePage(transaction, PAGE_OVERVIEW, page)
        hidePage(transaction, PAGE_PARAMETERS, page)
        hidePage(transaction, PAGE_SETTINGS, page)
        transaction.show(target).commit()
        updateToolbar()
        if (previousPage >= 0) animateSelectedNavItem(page)
    }

    private fun createPageFragment(page: Int): Fragment = when (page) {
        PAGE_PARAMETERS -> ParametersFragment()
        PAGE_SETTINGS -> SettingsFragment()
        else -> OverviewFragment()
    }

    private fun hidePage(transaction: FragmentTransaction, page: Int, visiblePage: Int) {
        if (page == visiblePage) return
        supportFragmentManager.findFragmentByTag(pageTag(page))?.let { transaction.hide(it) }
    }

    private fun pageTag(page: Int): String = when (page) {
        PAGE_PARAMETERS -> TAG_PARAMETERS
        PAGE_SETTINGS -> TAG_SETTINGS
        else -> TAG_OVERVIEW
    }

    private fun animateSelectedNavItem(page: Int) {
        val itemView = bottomNavigationView.findViewById<View>(navItemId(page)) ?: return
        itemView.animate().cancel()
        if (!systemAnimationsEnabled()) { itemView.scaleX = 1f; itemView.scaleY = 1f; return }
        itemView.scaleX = 0.96f; itemView.scaleY = 0.96f
        itemView.animate().scaleX(1f).scaleY(1f).setDuration(PAGE_TRANSITION_MS).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun systemAnimationsEnabled(): Boolean {
        val animatorDurationScale = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        return animationsEnabled(animatorDurationScale)
    }

    private fun navItemId(page: Int): Int = when (page) {
        PAGE_PARAMETERS -> R.id.nav_parameters
        PAGE_SETTINGS -> R.id.nav_settings
        else -> R.id.nav_overview
    }

    private fun renderState(snapshot: BmsUiState) { connected = snapshot.connected; updateToolbar() }

    private fun updateToolbar() {
        toolbar.title = pageTitle()
        toolbar.setNavigationIcon(R.drawable.ic_list_24)
        toolbar.setNavigationIconTint(getColor(R.color.on_primary))
        toolbar.menu.findItem(R.id.action_disconnect)?.isVisible = currentPage == PAGE_OVERVIEW && connected
        toolbar.menu.findItem(R.id.action_dashboard)?.isVisible = currentPage == PAGE_OVERVIEW && connected
    }

    private fun pageTitle(): String = when (currentPage) {
        PAGE_PARAMETERS -> getString(R.string.parameters_title)
        PAGE_SETTINGS -> getString(R.string.settings_title)
        else -> getString(R.string.overview_title)
    }

    private fun openDeviceList() {
        @Suppress("DEPRECATION")
        startActivityForResult(Intent(this, DeviceListActivity::class.java), REQUEST_SELECT_DEVICE)
    }

    private fun maybeAutoConnect() {
        val prefs = AppSettings.prefs(this)
        if (!AppSettings.autoConnectEnabled(this)) return
        val address = prefs.getString(AppSettings.PREF_LAST_DEVICE_ADDRESS, "") ?: ""
        if (address.isEmpty()) {
            repository.update(BmsUiState.withConnectionState(ConnectionState.INVALID_DEVICE, false, null, null, getString(R.string.status_auto_connect_no_device), null, null))
            return
        }
        val name = prefs.getString(AppSettings.PREF_LAST_DEVICE_NAME, address) ?: address
        connectionManager.connect(address, if (name.isEmpty()) address else name)
    }

    private fun rememberDevice(address: String, name: String?) {
        AppSettings.prefs(this).edit().putString(AppSettings.PREF_LAST_DEVICE_ADDRESS, address).putString(AppSettings.PREF_LAST_DEVICE_NAME, name ?: address).apply()
        configureAutoReconnect()
    }

    private fun configureAutoReconnect() {
        val prefs = AppSettings.prefs(this)
        val enabled = AppSettings.autoConnectEnabled(this)
        val address = prefs.getString(AppSettings.PREF_LAST_DEVICE_ADDRESS, "") ?: ""
        val name = prefs.getString(AppSettings.PREF_LAST_DEVICE_NAME, address)
        connectionManager.setAutoReconnect(enabled, address, name)
    }

    private fun toast(text: String) { Toast.makeText(this, text, Toast.LENGTH_SHORT).show() }

    companion object {
        private const val REQUEST_SELECT_DEVICE = 101
        private const val STATE_CURRENT_PAGE = "current_page"
        private const val PAGE_OVERVIEW = 0
        private const val PAGE_PARAMETERS = 1
        private const val PAGE_SETTINGS = 2
        private const val PAGE_TRANSITION_MS = 160L
        private const val TAG_OVERVIEW = "overview"
        private const val TAG_PARAMETERS = "parameters"
        private const val TAG_SETTINGS = "settings"

        @JvmStatic
        fun animationsEnabled(animatorDurationScale: Float): Boolean = animatorDurationScale > 0f
    }
}
