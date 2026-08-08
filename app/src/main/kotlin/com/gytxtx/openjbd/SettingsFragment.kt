package com.gytxtx.openjbd

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.switchmaterial.SwitchMaterial
import com.gytxtx.openjbd.data.BmsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    @Inject lateinit var connectionManager: BmsConnectionManager
    @Inject lateinit var repository: BmsRepository

    private lateinit var interfaceSettingsList: LinearLayout
    private lateinit var deviceSettingsList: LinearLayout
    private lateinit var otherSettingsList: LinearLayout
    private lateinit var settingsAdapter: SettingsAdapter
    private var connected = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        interfaceSettingsList = view.findViewById(R.id.list_settings_interface)
        deviceSettingsList = view.findViewById(R.id.list_settings_device)
        otherSettingsList = view.findViewById(R.id.list_settings_other)
        settingsAdapter = SettingsAdapter()
        connected = repository.getSnapshot().connected
        renderSettingsRows()
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                repository.uiState.collect { connected = it.connected }
            }
        }
    }

    override fun onStop() = super.onStop()

    private fun renderSettingsRows() {
        interfaceSettingsList.removeAllViews(); deviceSettingsList.removeAllViews(); otherSettingsList.removeAllViews()
        for (i in 0 until settingsAdapter.count) {
            val position = i
            val parent = settingsGroupParent(position)
            val row = settingsAdapter.getView(position, null, parent)
            row.setOnClickListener {
                val settingSwitch = it.findViewById<SwitchMaterial>(R.id.switch_setting_action)
                if (settingSwitch != null && settingSwitch.visibility == View.VISIBLE) settingSwitch.performClick()
                else showSettingMenu(position)
            }
            parent.addView(row)
        }
    }

    private fun settingsGroupParent(position: Int) = when { position <= 3 -> interfaceSettingsList; position == 4 -> deviceSettingsList; else -> otherSettingsList }

    private fun showSettingMenu(position: Int) {
        when (position) {
            0 -> showMenu(settingsRow(position), AppSettings.themeValue(requireContext()),
                arrayOf(getString(R.string.setting_dark_theme_system), getString(R.string.setting_dark_theme_always_on), getString(R.string.setting_dark_theme_always_off)),
                arrayOf(AppSettings.VALUE_SYSTEM, AppSettings.VALUE_DARK, AppSettings.VALUE_LIGHT)) {
                settings().edit().putString(AppSettings.PREF_THEME, it).apply(); AppSettings.applyThemePreference(requireContext()); requireActivity().recreate()
            }
            1 -> showMenu(settingsRow(position), AppSettings.languageValue(requireContext()),
                arrayOf(getString(R.string.setting_language_system), getString(R.string.setting_language_zh), getString(R.string.setting_language_en)),
                arrayOf(AppSettings.VALUE_SYSTEM, AppSettings.VALUE_ZH, AppSettings.VALUE_EN)) {
                settings().edit().putString(AppSettings.PREF_LANGUAGE, it).apply(); connectionManager.refreshLocalizedStatus(); requireActivity().recreate()
            }
            2 -> showMenu(settingsRow(position), settings().getString(AppSettings.PREF_TEMP_UNIT, AppSettings.VALUE_C) ?: AppSettings.VALUE_C,
                arrayOf(getString(R.string.setting_temp_celsius), getString(R.string.setting_temp_fahrenheit)),
                arrayOf(AppSettings.VALUE_C, AppSettings.VALUE_F)) {
                settings().edit().putString(AppSettings.PREF_TEMP_UNIT, it).apply(); renderSettingsRows()
            }
            3 -> showMenu(settingsRow(position), AppSettings.refreshIntervalValue(requireContext()),
                arrayOf(getString(R.string.setting_refresh_1s), getString(R.string.setting_refresh_2s), getString(R.string.setting_refresh_5s), getString(R.string.setting_refresh_10s)),
                arrayOf(AppSettings.VALUE_REFRESH_1S, AppSettings.VALUE_REFRESH_2S, AppSettings.VALUE_REFRESH_5S, AppSettings.VALUE_REFRESH_10S)) {
                AppSettings.setRefreshIntervalMs(requireContext(), it.toLong()); renderSettingsRows()
                if (connected) connectionManager.setRefreshInterval(AppSettings.refreshIntervalMs(requireContext()))
            }
            4 -> setAutoConnectEnabled(!AppSettings.autoConnectEnabled(requireContext()), true)
            5 -> startActivity(Intent(requireContext(), AboutActivity::class.java))
        }
    }

    private fun settingsRow(position: Int): View {
        val parent = settingsGroupParent(position)
        val childIndex = if (position <= 3) position else 0
        return if (childIndex >= parent.childCount) parent else parent.getChildAt(childIndex)
    }

    private fun setAutoConnectEnabled(enabled: Boolean, refreshRows: Boolean) {
        AppSettings.setAutoConnectEnabled(requireContext(), enabled); configureAutoReconnect()
        if (refreshRows) renderSettingsRows()
        if (enabled && !connected) maybeAutoConnect()
    }

    private fun configureAutoReconnect() {
        val prefs = settings(); val enabled = AppSettings.autoConnectEnabled(requireContext())
        val address = prefs.getString(AppSettings.PREF_LAST_DEVICE_ADDRESS, "") ?: ""
        val name = prefs.getString(AppSettings.PREF_LAST_DEVICE_NAME, address)
        connectionManager.setAutoReconnect(enabled, address, name)
    }

    private fun maybeAutoConnect() {
        if (!AppSettings.autoConnectEnabled(requireContext())) return
        val address = settings().getString(AppSettings.PREF_LAST_DEVICE_ADDRESS, "") ?: ""
        if (address.isEmpty()) return
        val name = settings().getString(AppSettings.PREF_LAST_DEVICE_NAME, address) ?: address
        connectionManager.connect(address, if (name.isEmpty()) address else name)
    }

    @SuppressLint("InflateParams")
    private fun showMenu(anchor: View, current: String, labels: Array<String>, values: Array<String>, handler: (String) -> Unit) {
        val ctx = requireContext()
        val menuView = LayoutInflater.from(ctx).inflate(R.layout.popup_setting_menu, null, false) as LinearLayout
        val popupWindow = PopupWindow(menuView, ctx.dp(220), ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindow.setBackgroundDrawable(ColorDrawable(ctx.getColor(R.color.surface_elevation_8)))
        popupWindow.isOutsideTouchable = true; popupWindow.elevation = ctx.dp(8).toFloat()
        var selectedIndex = 0
        for (i in values.indices) {
            val value = values[i]
            val item = LayoutInflater.from(ctx).inflate(R.layout.row_setting_menu_item, menuView, false) as TextView
            item.text = labels[i]
            if (value == current) { selectedIndex = i; item.setBackgroundResource(R.drawable.setting_menu_item_selected); item.setTextColor(ctx.getColor(R.color.primary)) }
            item.setOnClickListener { popupWindow.dismiss(); if (value != current) handler(value) }
            menuView.addView(item)
        }
        val verticalOffset = -anchor.height - ctx.dp(8) - (selectedIndex * ctx.dp(48)) + ((anchor.height - ctx.dp(48)) / 2)
        popupWindow.showAsDropDown(anchor, ctx.dp(24) + ctx.dp(16) + ctx.dp(16), verticalOffset)
    }

    private fun settings(): SharedPreferences = AppSettings.prefs(requireContext())

    private fun themeLabel(value: String) = when (value) { AppSettings.VALUE_DARK -> getString(R.string.setting_dark_theme_always_on); AppSettings.VALUE_LIGHT -> getString(R.string.setting_dark_theme_always_off); else -> getString(R.string.setting_dark_theme_system) }
    private fun languageLabel(value: String) = when (value) { AppSettings.VALUE_ZH -> getString(R.string.setting_language_zh); AppSettings.VALUE_EN -> getString(R.string.setting_language_en); else -> getString(R.string.setting_language_system) }
    private fun tempUnitLabel(value: String) = if (AppSettings.VALUE_F == value) getString(R.string.setting_temp_fahrenheit) else getString(R.string.setting_temp_celsius)
    private fun refreshIntervalLabel(value: String) = when (value) { AppSettings.VALUE_REFRESH_1S -> getString(R.string.setting_refresh_1s); AppSettings.VALUE_REFRESH_5S -> getString(R.string.setting_refresh_5s); AppSettings.VALUE_REFRESH_10S -> getString(R.string.setting_refresh_10s); else -> getString(R.string.setting_refresh_2s) }
    private fun autoConnectLabel(value: Boolean) = if (value) getString(R.string.setting_auto_connect_on) else getString(R.string.setting_auto_connect_off)

    private inner class SettingsAdapter : BaseAdapter() {
        override fun getCount() = 6
        override fun getItem(position: Int) = position
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val row: View = convertView ?: LayoutInflater.from(requireContext()).inflate(R.layout.row_setting_item, parent, false)
            val icon = row.findViewById<ImageView>(R.id.img_setting_icon); val title = row.findViewById<TextView>(R.id.txt_setting_title)
            val subtitle = row.findViewById<TextView>(R.id.txt_setting_subtitle); val settingSwitch = row.findViewById<SwitchMaterial>(R.id.switch_setting_action)
            val chevron = row.findViewById<ImageView>(R.id.img_setting_chevron)
            settingSwitch.setOnCheckedChangeListener(null); settingSwitch.visibility = View.GONE; chevron.visibility = View.GONE
            val prefs = settings()
            when (position) {
                0 -> { icon.setImageResource(R.drawable.ic_brightness_medium_24); icon.contentDescription = getString(R.string.setting_dark_theme); title.setText(R.string.setting_dark_theme); subtitle.text = themeLabel(AppSettings.themeValue(requireContext())) }
                1 -> { icon.setImageResource(R.drawable.ic_language_24); icon.contentDescription = getString(R.string.setting_language); title.setText(R.string.setting_language); subtitle.text = languageLabel(AppSettings.languageValue(requireContext())) }
                2 -> { icon.setImageResource(R.drawable.ic_thermostat_24); icon.contentDescription = getString(R.string.setting_temperature_unit); title.setText(R.string.setting_temperature_unit); subtitle.text = tempUnitLabel(prefs.getString(AppSettings.PREF_TEMP_UNIT, AppSettings.VALUE_C) ?: AppSettings.VALUE_C) }
                3, 4 -> {
                    icon.setImageResource(R.drawable.ic_loop_24); title.setText(R.string.setting_refresh_interval); subtitle.text = refreshIntervalLabel(AppSettings.refreshIntervalValue(requireContext()))
                    if (position == 4) {
                        val autoConnectEnabled = AppSettings.autoConnectEnabled(requireContext())
                        icon.setImageResource(R.drawable.ic_bluetooth_searching_24); icon.contentDescription = getString(R.string.setting_auto_connect)
                        title.setText(R.string.setting_auto_connect); subtitle.text = autoConnectLabel(autoConnectEnabled)
                        settingSwitch.isChecked = autoConnectEnabled
                        settingSwitch.setOnCheckedChangeListener { _, checked -> subtitle.text = autoConnectLabel(checked); setAutoConnectEnabled(checked, false) }
                        settingSwitch.visibility = View.VISIBLE
                    } else { icon.contentDescription = getString(R.string.setting_refresh_interval) }
                }
                else -> { icon.setImageResource(R.drawable.ic_info_24); icon.contentDescription = getString(R.string.setting_about); title.setText(R.string.setting_about); subtitle.setText(R.string.setting_about_subtitle); chevron.setImageResource(R.drawable.ic_chevron_right_24); chevron.setColorFilter(requireContext().getColor(R.color.text_secondary)); chevron.visibility = View.VISIBLE }
            }
            icon.setColorFilter(requireContext().getColor(R.color.icon_default))
            return row
        }
    }
}
