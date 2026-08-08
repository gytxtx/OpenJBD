package com.gytxtx.openjbd

import android.content.Context
import android.widget.TextView
import com.gytxtx.openjbd.protocol.JbdBasicInfo

fun Context.dp(value: Int): Int = Math.round(value * resources.displayMetrics.density)

fun TextView.setTextIfChanged(value: String) {
    if (value != text.toString()) {
        text = value
    }
}

fun Context.onOff(value: Boolean): String =
    getString(if (value) R.string.label_on else R.string.label_off)

fun Context.balanceSummary(info: JbdBasicInfo): String {
    val builder = StringBuilder()
    for (i in info.balanceStates.indices) {
        if (!info.balanceStates[i]) continue
        if (builder.isNotEmpty()) builder.append(", ")
        builder.append(i + 1)
    }
    return if (builder.isEmpty()) getString(R.string.balance_none)
    else getString(R.string.balance_cells, builder.toString())
}

fun Context.protectionSummary(info: JbdBasicInfo): String {
    val labels = intArrayOf(
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
    val builder = StringBuilder()
    for (i in 0 until minOf(info.protectionStates.size, labels.size)) {
        if (!info.protectionStates[i]) continue
        if (builder.isNotEmpty()) builder.append(" / ")
        builder.append(getString(labels[i]))
    }
    return if (builder.isEmpty()) getString(R.string.protection_none) else builder.toString()
}
