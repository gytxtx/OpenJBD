package com.gytxtx.openjbd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.gytxtx.openjbd.protocol.JbdBasicInfo
import com.gytxtx.openjbd.protocol.JbdDeviceInfo

class ParametersFragment : Fragment(), BmsStateStore.Listener {
    private val parameterGroups = arrayOf(
        intArrayOf(0, 1),                                  // Connection
        intArrayOf(2, 3, 19, 17, 18, 15, 16),             // Battery Info
        intArrayOf(4, 5, 20, 6, 7),                       // Capacity
        intArrayOf(12, 13, 14, 8, 9, 10, 11),             // Electrical
        intArrayOf(21, 22, 23, 24, 25)                    // Status
    )

    private lateinit var placeholderParameters: LinearLayout
    private lateinit var sectionParametersContent: LinearLayout
    private lateinit var connectionList: LinearLayout
    private lateinit var identityList: LinearLayout
    private lateinit var capacityList: LinearLayout
    private lateinit var electricalList: LinearLayout
    private lateinit var statusList: LinearLayout
    private lateinit var parametersAdapter: ParametersAdapter
    private var snapshot: BmsStateStore.Snapshot? = null
    private var lastRenderedHasData = false
    private var lastRenderedBasicInfo: JbdBasicInfo? = null
    private var lastRenderedDeviceInfo: JbdDeviceInfo? = null
    private var lastRenderedDeviceName: String? = null
    private var lastRenderedDeviceAddress: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_parameters, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        placeholderParameters = view.findViewById(R.id.placeholder_parameters)
        sectionParametersContent = view.findViewById(R.id.section_parameters_content)
        connectionList = view.findViewById(R.id.list_parameters_connection)
        identityList = view.findViewById(R.id.list_parameters_identity)
        capacityList = view.findViewById(R.id.list_parameters_capacity)
        electricalList = view.findViewById(R.id.list_parameters_electrical)
        statusList = view.findViewById(R.id.list_parameters_status)
        parametersAdapter = ParametersAdapter()
        snapshot = BmsStateStore.getSnapshot()
        updateParametersPage()
    }

    override fun onStart() {
        super.onStart()
        BmsStateStore.addListener(this)
        renderState(BmsStateStore.getSnapshot())
    }

    override fun onStop() {
        BmsStateStore.removeListener(this)
        super.onStop()
    }

    override fun onBmsStateChanged(snapshot: BmsStateStore.Snapshot) {
        if (activity == null) {
            return
        }
        requireActivity().runOnUiThread { renderState(snapshot) }
    }

    private fun renderState(snapshot: BmsStateStore.Snapshot?) {
        this.snapshot = snapshot
        updateParametersPage()
    }

    private fun updateParametersPage() {
        if (!::placeholderParameters.isInitialized || !::sectionParametersContent.isInitialized || !::parametersAdapter.isInitialized) {
            return
        }
        val hasData = snapshot != null && snapshot!!.basicInfo != null
        val deviceName = snapshot?.deviceName
        val deviceAddress = snapshot?.deviceAddress
        if (hasData == lastRenderedHasData
            && lastRenderedBasicInfo === (snapshot?.basicInfo)
            && lastRenderedDeviceInfo === (snapshot?.deviceInfo)
            && lastRenderedDeviceName === deviceName
            && lastRenderedDeviceAddress === deviceAddress
        ) {
            return
        }
        lastRenderedHasData = hasData
        lastRenderedBasicInfo = snapshot?.basicInfo
        lastRenderedDeviceInfo = snapshot?.deviceInfo
        lastRenderedDeviceName = deviceName
        lastRenderedDeviceAddress = deviceAddress
        placeholderParameters.visibility = if (hasData) View.GONE else View.VISIBLE
        sectionParametersContent.visibility = if (hasData) View.VISIBLE else View.GONE
        renderParameterRows()
    }

    private fun renderParameterRows() {
        val groupLists = arrayOf(
            connectionList, identityList, capacityList, electricalList, statusList
        )

        for (g in parameterGroups.indices) {
            val positions = parameterGroups[g]
            val groupList = groupLists[g]

            while (groupList.childCount < positions.size) {
                groupList.addView(parametersAdapter.getView(positions[groupList.childCount], null, groupList))
            }
            while (groupList.childCount > positions.size) {
                groupList.removeViewAt(groupList.childCount - 1)
            }
            for (i in positions.indices) {
                parametersAdapter.getView(positions[i], groupList.getChildAt(i), groupList)
            }
        }
    }

    private inner class ParametersAdapter : BaseAdapter() {
        override fun getCount(): Int = 26

        override fun getItem(position: Int): Any = position

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val row: View = convertView
                ?: LayoutInflater.from(requireContext()).inflate(R.layout.row_setting_item, parent, false)
            val icon = row.findViewById<ImageView>(R.id.img_setting_icon)
            val title = row.findViewById<TextView>(R.id.txt_setting_title)
            val subtitle = row.findViewById<TextView>(R.id.txt_setting_subtitle)
            val settingSwitch = row.findViewById<View>(R.id.switch_setting_action)
            settingSwitch.visibility = View.GONE
            icon.setImageResource(parameterIcon(position))
            icon.setColorFilter(requireContext().getColor(R.color.icon_default))
            icon.contentDescription = getString(parameterTitle(position))
            title.setText(parameterTitle(position))
            setTextIfChanged(subtitle, parameterValue(position))
            return row
        }

        private fun parameterIcon(position: Int): Int {
            return when (position) {
                0, 1 -> R.drawable.ic_bluetooth_searching_24
                4, 8, 9, 20, 23, 24 -> R.drawable.ic_battery_4_bar_24
                25 -> R.drawable.ic_widgets_outline_24
                10, 11 -> R.drawable.ic_bolt_24
                12, 13, 14 -> R.drawable.ic_dashboard_24
                else -> R.drawable.ic_info_24
            }
        }

        private fun parameterTitle(position: Int): Int {
            return when (position) {
                0 -> R.string.param_bluetooth_name
                1 -> R.string.param_device_address
                2 -> R.string.param_bms_version
                3 -> R.string.param_manufacturing_date
                4 -> R.string.param_nominal_capacity
                5 -> R.string.param_remaining_capacity
                6 -> R.string.param_soc
                7 -> R.string.param_cycle_count
                8 -> R.string.param_cell_count
                9 -> R.string.param_ntc_count
                10 -> R.string.param_charge_mos
                11 -> R.string.param_discharge_mos
                12 -> R.string.param_pack_voltage
                13 -> R.string.param_pack_current
                14 -> R.string.param_pack_power
                15 -> R.string.param_serial_number
                16 -> R.string.param_barcode
                17 -> R.string.param_battery_model
                18 -> R.string.param_manufacturer
                20 -> R.string.param_learn_capacity
                21 -> R.string.param_extension_marker
                22 -> R.string.param_alter
                23 -> R.string.param_balance_current
                24 -> R.string.param_balance_state
                25 -> R.string.param_protection_state
                else -> R.string.param_bms_model
            }
        }

        private fun parameterValue(position: Int): String {
            val unread = getString(R.string.param_unread)
            if (position == 0) {
                return if (snapshot == null || snapshot!!.deviceName.isNullOrEmpty()) unread else snapshot!!.deviceName
                    ?: unread
            }
            if (position == 1) {
                return if (snapshot == null || snapshot!!.deviceAddress.isNullOrEmpty()) unread else snapshot!!.deviceAddress
                    ?: unread
            }
            val info = snapshot?.basicInfo
            val deviceInfo = snapshot?.deviceInfo
            if (info == null) {
                return unread
            }
            return when (position) {
                2 -> info.softwareVersion
                3 -> info.productionDate
                4 -> getString(R.string.format_value_capacity_2, info.nominalAh)
                5 -> getString(R.string.format_value_capacity_2, info.remainingAh)
                6 -> getString(R.string.format_value_percent, info.soc)
                7 -> getString(R.string.format_value_integer, info.cycleCount)
                8 -> getString(R.string.format_value_integer, info.cellCount)
                9 -> getString(R.string.format_value_integer, info.ntcCount)
                10 -> onOff(info.chargeEnabled)
                11 -> onOff(info.dischargeEnabled)
                12 -> getString(R.string.format_value_voltage_2, info.totalVoltage)
                13 -> getString(R.string.format_value_current_2, info.current)
                14 -> getString(R.string.format_value_power_1, info.totalVoltage * info.current)
                15 -> fieldOrUnread(deviceInfo?.serialNumber, unread)
                16 -> fieldOrUnread(deviceInfo?.barcode, unread)
                17 -> fieldOrUnread(deviceInfo?.batteryModel, unread)
                18 -> fieldOrUnread(deviceInfo?.manufacturer, unread)
                19 -> fieldOrUnread(deviceInfo?.bmsModel, unread)
                20 -> if (info.hasLearnCapacity) getString(R.string.format_value_capacity_2, info.learnCapacityAh) else unread
                21 -> if (info.hasExtendedInfo) getString(R.string.format_value_integer, info.extensionMarker) else unread
                22 -> if (info.hasExtendedInfo) getString(R.string.format_value_integer, info.alter) else unread
                23 -> if (info.hasBalanceCurrent) getString(R.string.format_value_current_2, info.balanceCurrentA) else unread
                24 -> balanceSummary(info)
                25 -> protectionSummary(info)
                else -> unread
            }
        }

        private fun fieldOrUnread(value: String?, unread: String): String =
            if (value.isNullOrEmpty()) unread else value
    }

    private fun onOff(value: Boolean): String =
        if (value) getString(R.string.label_on) else getString(R.string.label_off)

    private fun balanceSummary(info: JbdBasicInfo): String {
        val builder = StringBuilder()
        for (i in info.balanceStates.indices) {
            if (!info.balanceStates[i]) {
                continue
            }
            if (builder.isNotEmpty()) {
                builder.append(", ")
            }
            builder.append(i + 1)
        }
        return if (builder.isEmpty()) getString(R.string.balance_none)
        else getString(R.string.balance_cells, builder.toString())
    }

    private fun protectionSummary(info: JbdBasicInfo): String {
        val labels = protectionLabelIds()
        val builder = StringBuilder()
        var i = 0
        while (i < info.protectionStates.size && i < labels.size) {
            if (!info.protectionStates[i]) {
                i++
                continue
            }
            if (builder.isNotEmpty()) {
                builder.append(" / ")
            }
            builder.append(getString(labels[i]))
            i++
        }
        return if (builder.isEmpty()) getString(R.string.protection_none) else builder.toString()
    }

    private fun protectionLabelIds(): IntArray = intArrayOf(
        R.string.protection_cell_over_voltage,
        R.string.protection_cell_under_voltage,
        R.string.protection_pack_over_voltage,
        R.string.protection_pack_under_voltage,
        R.string.protection_charge_over_temp,
        R.string.protection_charge_low_temp,
        R.string.protection_discharge_over_temp,
        R.string.protection_discharge_low_temp,
        R.string.protection_charge_over_current,
        R.string.protection_discharge_over_current,
        R.string.protection_short_circuit,
        R.string.protection_ic_error,
        R.string.protection_software_lock_mos
    )

    private fun setTextIfChanged(view: TextView, value: String) {
        if (value != view.text.toString()) {
            view.text = value
        }
    }
}
