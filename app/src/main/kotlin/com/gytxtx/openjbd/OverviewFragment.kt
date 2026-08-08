package com.gytxtx.openjbd

import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.gytxtx.openjbd.data.BmsRepository
import com.gytxtx.openjbd.data.BmsUiState
import com.gytxtx.openjbd.data.ConnectionState
import com.gytxtx.openjbd.protocol.JbdBasicInfo
import com.gytxtx.openjbd.protocol.JbdCellVoltages
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class OverviewFragment : Fragment() {
    @Inject lateinit var repository: BmsRepository
    @Inject lateinit var connectionManager: BmsConnectionManager
    private lateinit var placeholderConnect: LinearLayout
    private lateinit var connectedOverviewContent: LinearLayout
    private lateinit var cellStatsGrid: LinearLayout
    private lateinit var cellList: LinearLayout
    private lateinit var reconnectBanner: View
    private lateinit var reconnectBannerBody: TextView
    private lateinit var statusText: TextView
    private var reconnectBannerAnimator: ValueAnimator? = null
    private var reconnectBannerVisible = false
    private var hideOverviewAfterReconnectBanner = false
    private lateinit var voltageText: TextView
    private lateinit var currentText: TextView
    private lateinit var powerText: TextView
    private lateinit var socText: TextView
    private lateinit var capacityText: TextView
    private lateinit var cyclesText: TextView
    private lateinit var mosText: TextView
    private lateinit var socNoteText: TextView
    private lateinit var statsText: TextView
    private lateinit var temperaturesText: TextView
    private lateinit var protectionText: TextView
    private lateinit var balanceText: TextView
    private lateinit var temperaturesChipGroup: ChipGroup
    private lateinit var cellMinText: TextView
    private lateinit var cellMaxText: TextView
    private lateinit var cellDeltaText: TextView
    private lateinit var cellAverageText: TextView
    private lateinit var socProgress: LinearProgressIndicator
    private var lastRenderedBasicInfo: JbdBasicInfo? = null
    private var lastRenderedTemps: List<Float>? = null
    private var lastUnitLabel: String? = null
    private val cellColorAccent by lazy { requireContext().getColor(R.color.accent) }
    private val cellColorPrimary by lazy { requireContext().getColor(R.color.primary) }
    private val cellColorNormal by lazy { requireContext().getColor(R.color.cell_voltage_normal) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_overview, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        placeholderConnect = view.findViewById(R.id.placeholder_connect)
        connectedOverviewContent = view.findViewById(R.id.content_connected_overview)
        reconnectBanner = view.findViewById(R.id.banner_reconnect)
        reconnectBannerBody = view.findViewById(R.id.txt_reconnect_banner_body)
        statusText = view.findViewById(R.id.txt_status)
        val cancelReconnectButton = view.findViewById<View>(R.id.btn_cancel_reconnect)
        cancelReconnectButton.setOnClickListener {
            connectionManager.cancelReconnect()
        }
        socText = view.findViewById(R.id.txt_soc)
        socProgress = view.findViewById(R.id.progress_soc)
        voltageText = view.findViewById(R.id.txt_voltage)
        currentText = view.findViewById(R.id.txt_current)
        powerText = view.findViewById(R.id.txt_power)
        capacityText = view.findViewById(R.id.txt_capacity)
        cyclesText = view.findViewById(R.id.txt_cycles)
        statsText = view.findViewById(R.id.txt_stats)
        mosText = view.findViewById(R.id.txt_mos)
        socNoteText = view.findViewById(R.id.txt_soc_note)
        temperaturesText = view.findViewById(R.id.txt_temperatures)
        protectionText = view.findViewById(R.id.txt_protection)
        balanceText = view.findViewById(R.id.txt_balance)
        temperaturesChipGroup = view.findViewById(R.id.chips_temperatures)
        cellStatsGrid = view.findViewById(R.id.cell_stats_grid)
        cellMinText = view.findViewById(R.id.txt_cell_min)
        cellMaxText = view.findViewById(R.id.txt_cell_max)
        cellDeltaText = view.findViewById(R.id.txt_cell_delta)
        cellAverageText = view.findViewById(R.id.txt_cell_average)
        cellList = view.findViewById(R.id.list_cells)
        clearDeviceData()
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

    private fun renderState(snapshot: BmsUiState) {
        if (!::statusText.isInitialized) return
        if (snapshot.basicInfo == null) {
            val showReconnectBanner =
                snapshot.connectionState == ConnectionState.WAITING_RECONNECT
            val showOverview =
                showReconnectBanner || snapshot.connected || snapshot.deviceName != null
            val bannerVisibleOrExiting =
                reconnectBannerVisible || reconnectBanner.visibility == View.VISIBLE
            val waitForBannerExit = bannerVisibleOrExiting && !showOverview
            clearDeviceData()
            if (showOverview || waitForBannerExit) {
                showConnectedContent()
            } else {
                showEmptyContent()
            }
            hideOverviewAfterReconnectBanner = waitForBannerExit
            renderReconnectAction(snapshot)
            if (!snapshot.status.isNullOrEmpty()) {
                setStatus(snapshot.status!!)
            }
            return
        }
        hideOverviewAfterReconnectBanner = false
        showConnectedContent()
        renderReconnectAction(snapshot)
        setStatus(snapshot.status ?: "")
        if (snapshot.basicInfo !== lastRenderedBasicInfo) {
            renderBasicInfo(snapshot.basicInfo!!)
            lastRenderedBasicInfo = snapshot.basicInfo
        }
        if (snapshot.cellVoltages != null) {
            renderCells(snapshot.cellVoltages!!)
        }
    }

    private fun renderBasicInfo(info: JbdBasicInfo) {
        voltageText.setTextIfChanged(getString(R.string.format_value_voltage_2, info.totalVoltage))
        currentText.setTextIfChanged(getString(R.string.format_value_current_2, info.current))
        powerText.setTextIfChanged(getString(R.string.format_value_power_1, info.totalVoltage * info.current))
        socText.setTextIfChanged(getString(R.string.format_value_percent, info.soc))
        socProgress.setProgressCompat(info.soc, false)
        capacityText.setTextIfChanged(
            getString(R.string.format_value_capacity_pair, info.remainingAh, info.learnedOrNominalAh)
        )
        cyclesText.setTextIfChanged(getString(R.string.format_value_integer, info.cycleCount))
        mosText.setTextIfChanged(
            getString(
                R.string.format_mos_state,
                getString(R.string.label_charge),
                requireContext().onOff(info.chargeEnabled),
                getString(R.string.label_discharge),
                requireContext().onOff(info.dischargeEnabled)
            )
        )
        socNoteText.setTextIfChanged(socNote(info))
        statsText.setTextIfChanged(
            getString(R.string.pack_stats, info.cellCount, info.ntcCount, info.softwareVersion)
        )
        val ctx = requireContext()
        protectionText.setTextIfChanged(ctx.protectionSummary(info))
        balanceText.setTextIfChanged(ctx.balanceSummary(info))
        temperaturesText.visibility = View.GONE
        temperaturesChipGroup.removeAllViews()
        temperaturesChipGroup.visibility = View.VISIBLE
        temperaturesChipGroup.addView(
            temperatureChip(
                getString(
                    R.string.temperature_mos_item,
                    AppSettings.displayTemperature(ctx, info.temperaturesC[0]),
                    AppSettings.temperatureUnitLabel(ctx)
                ),
                true
            )
        )
        for (i in info.temperaturesC.indices) {
            temperaturesChipGroup.addView(
                temperatureChip(
                    getString(
                        R.string.temperature_probe_item,
                        i + 1,
                        AppSettings.displayTemperature(ctx, info.temperaturesC[i]),
                        AppSettings.temperatureUnitLabel(ctx)
                    ),
                    false
                )
            )
        }
    }

    private fun renderCells(voltages: JbdCellVoltages) {
        cellStatsGrid.visibility = View.VISIBLE
        cellMinText.setTextIfChanged(getString(R.string.format_value_voltage_3, voltages.min))
        cellMaxText.setTextIfChanged(getString(R.string.format_value_voltage_3, voltages.max))
        cellDeltaText.setTextIfChanged(getString(R.string.format_value_voltage_3, voltages.delta))
        cellAverageText.setTextIfChanged(getString(R.string.format_value_voltage_3, voltages.average))
        if (cellList.childCount != voltages.cells.size) {
            cellList.removeAllViews()
            for (i in voltages.cells.indices) {
                cellList.addView(createCellVoltageRow(i + 1))
            }
        }
        for (i in voltages.cells.indices) {
            updateCellVoltageRow(cellList.getChildAt(i), i + 1, voltages.cells[i], voltages.min, voltages.max)
        }
    }

    private fun createCellVoltageRow(index: Int): View {
        val row = LayoutInflater.from(requireContext()).inflate(R.layout.row_cell_voltage, cellList, false)
        val label = row.findViewById<TextView>(R.id.txt_cell_label)
        label.text = getString(R.string.cell_label, index)
        return row
    }

    private fun updateCellVoltageRow(row: View, index: Int, voltage: Float, min: Float, max: Float) {
        val label = row.findViewById<TextView>(R.id.txt_cell_label)
        val value = row.findViewById<TextView>(R.id.txt_cell_value)
        val progress = row.findViewById<LinearProgressIndicator>(R.id.progress_cell)
        val basicInfo = lastRenderedBasicInfo
        val suffix = if (basicInfo != null && index <= basicInfo.balanceStates.size && basicInfo.balanceStates[index - 1])
            getString(R.string.cell_balancing_suffix) else ""
        label.setTextIfChanged(getString(R.string.cell_label, index) + suffix)
        value.setTextIfChanged(getString(R.string.format_value_voltage_3, voltage))
        val span = maxOf(0.001f, max - min)
        val scaled = maxOf(80, minOf(1000, (1000f * ((voltage - min) / span)).toInt()))
        progress.setProgressCompat(scaled, false)
        val color = when (voltage) {
            max -> cellColorAccent
            min -> cellColorPrimary
            else -> cellColorNormal
        }
        progress.setIndicatorColor(color)
    }

    private fun temperatureChip(text: String, primary: Boolean): Chip {
        val ctx = requireContext()
        val chip = Chip(ctx)
        chip.text = text
        chip.isCheckable = false
        chip.isClickable = false
        chip.setTextColor(ctx.getColor(if (primary) R.color.primary else R.color.text_primary))
        chip.chipBackgroundColor = ctx.getColorStateList(R.color.surface)
        chip.chipStrokeColor = ctx.getColorStateList(if (primary) R.color.primary else R.color.card_outline)
        chip.chipStrokeWidth = requireContext().dp(1).toFloat()
        chip.chipMinHeight = requireContext().dp(36).toFloat()
        chip.textSize = 14f
        return chip
    }

    private fun clearDeviceData() {
        voltageText.text = "--"
        currentText.text = "--"
        powerText.text = "--"
        socText.setText(R.string.placeholder_percent)
        socProgress.progress = 0
        capacityText.text = "--"
        cyclesText.text = "--"
        mosText.text = "--"
        statsText.text = "--"
        socNoteText.setText(R.string.local_ble_note)
        protectionText.text = "--"
        balanceText.text = "--"
        temperaturesText.visibility = View.VISIBLE
        temperaturesChipGroup.removeAllViews()
        temperaturesChipGroup.visibility = View.GONE
        cellMinText.text = "--"
        cellMaxText.text = "--"
        cellDeltaText.text = "--"
        cellAverageText.text = "--"
        cellStatsGrid.visibility = View.GONE
        lastRenderedBasicInfo = null
        showEmptyCells()
        setStatus(getString(R.string.status_select_bms))
    }

    private fun showEmptyCells() {
        cellList.removeAllViews()
        val emptyCells = TextView(requireContext())
        emptyCells.setText(R.string.empty_cells)
        emptyCells.setTextAppearance(R.style.TextAppearance_OpenJbd_EmptyBody)
        emptyCells.gravity = Gravity.CENTER
        val ctx = requireContext()
        emptyCells.setPadding(ctx.dp(14), ctx.dp(14), ctx.dp(14), ctx.dp(14))
        cellList.addView(wrapCard(emptyCells, 8))
    }

    private fun showConnectedContent() {
        placeholderConnect.visibility = View.GONE
        connectedOverviewContent.visibility = View.VISIBLE
    }

    private fun showEmptyContent() {
        connectedOverviewContent.visibility = View.GONE
        placeholderConnect.visibility = View.VISIBLE
    }

    private fun wrapCard(child: View, bottomMarginDp: Int): MaterialCardView {
        val ctx = requireContext()
        val card = MaterialCardView(ctx)
        card.setCardBackgroundColor(ctx.getColor(R.color.surface))
        card.radius = resources.getDimensionPixelSize(R.dimen.space_4).toFloat()
        card.strokeColor = ctx.getColor(R.color.card_outline)
        card.strokeWidth = 1
        card.cardElevation = 0f
        card.useCompatPadding = true
        card.addView(child)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.bottomMargin = ctx.dp(bottomMarginDp)
        card.layoutParams = params
        return card
    }

    private fun socNote(info: JbdBasicInfo): String {
        val absCurrent = Math.abs(info.current)
        if (absCurrent < 0.05f) return getString(R.string.local_ble_note)
        if (info.current < 0f) {
            return getString(R.string.estimate_discharging, formatDurationHours(info.remainingAh / absCurrent))
        }
        val fullCapacityAh = Math.max(info.remainingAh, info.learnedOrNominalAh)
        return getString(R.string.estimate_charging, formatDurationHours((fullCapacityAh - info.remainingAh) / absCurrent))
    }

    private fun formatDurationHours(hours: Float): String {
        if (hours.isNaN() || hours.isInfinite() || hours <= 0f) {
            return getString(R.string.estimate_duration_zero)
        }
        val totalMinutes = Math.round(hours * 60f)
        return getString(R.string.estimate_duration_hours_minutes, totalMinutes / 60, totalMinutes % 60)
    }

    private fun setStatus(status: String) {
        statusText.setTextIfChanged(status)
    }

    private fun renderReconnectAction(snapshot: BmsUiState) {
        showReconnectBanner(
            snapshot.connectionState == ConnectionState.WAITING_RECONNECT,
            snapshot.status
        )
    }

    private fun showReconnectBanner(show: Boolean, status: String?) {
        if (show) {
            reconnectBannerBody.setTextIfChanged(status ?: "")
            if (reconnectBannerVisible && reconnectBanner.visibility == View.VISIBLE) return
            cancelReconnectBannerAnimation()
            reconnectBannerVisible = true
            animateReconnectBannerIn()
            return
        }
        if (!reconnectBannerVisible) {
            if (reconnectBanner.visibility != View.VISIBLE) reconnectBanner.visibility = View.GONE
            return
        }
        cancelReconnectBannerAnimation()
        reconnectBannerVisible = false
        animateReconnectBannerOut()
    }

    private fun cancelReconnectBannerAnimation() {
        reconnectBannerAnimator?.cancel()
        reconnectBannerAnimator = null
        reconnectBanner.animate().cancel()
    }

    private fun animateReconnectBannerIn() {
        reconnectBanner.visibility = View.VISIBLE
        val targetHeight = measureReconnectBannerHeight()
        if (targetHeight <= 0) {
            val p = reconnectBanner.layoutParams
            p.height = ViewGroup.LayoutParams.WRAP_CONTENT
            reconnectBanner.layoutParams = p
            reconnectBanner.alpha = 1f
            reconnectBanner.translationY = 0f
            return
        }
        val params = reconnectBanner.layoutParams
        val startHeight = maxOf(0, reconnectBanner.height)
        params.height = startHeight
        reconnectBanner.layoutParams = params
        reconnectBanner.alpha = if (startHeight > 0) reconnectBanner.alpha else 0f
        reconnectBanner.translationY = if (startHeight > 0) reconnectBanner.translationY else -requireContext().dp(8).toFloat()

        val animator = ValueAnimator.ofInt(startHeight, targetHeight).apply {
            duration = RECONNECT_BANNER_ENTER_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { a -> params.height = a.animatedValue as Int; reconnectBanner.layoutParams = params }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (reconnectBannerVisible) {
                        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                        reconnectBanner.layoutParams = params
                    }
                    reconnectBannerAnimator = null
                }
            })
        }
        reconnectBannerAnimator = animator
        reconnectBanner.animate()
            .alpha(1f).translationY(0f)
            .setDuration(RECONNECT_BANNER_ENTER_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
        animator.start()
    }

    private fun animateReconnectBannerOut() {
        val startHeight = reconnectBanner.height
        if (startHeight <= 0) { hideReconnectBannerImmediately(); return }
        val params = reconnectBanner.layoutParams
        params.height = startHeight
        reconnectBanner.layoutParams = params
        val animator = ValueAnimator.ofInt(startHeight, 0).apply {
            duration = RECONNECT_BANNER_EXIT_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { a -> params.height = a.animatedValue as Int; reconnectBanner.layoutParams = params }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (!reconnectBannerVisible) hideReconnectBannerImmediately()
                    reconnectBannerAnimator = null
                }
            })
        }
        reconnectBannerAnimator = animator
        reconnectBanner.animate()
            .alpha(0f).translationY(-requireContext().dp(8).toFloat())
            .setDuration(RECONNECT_BANNER_EXIT_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
        animator.start()
    }

    private fun measureReconnectBannerHeight(): Int {
        val parent = reconnectBanner.parent as? View
        val width = parent?.width ?: 0
        val effectiveWidth = if (width <= 0) resources.displayMetrics.widthPixels else width
        val params = reconnectBanner.layoutParams
        val previousHeight = params.height
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        reconnectBanner.measure(
            View.MeasureSpec.makeMeasureSpec(effectiveWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        params.height = previousHeight
        return reconnectBanner.measuredHeight
    }

    private fun hideReconnectBannerImmediately() {
        val params = reconnectBanner.layoutParams
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        reconnectBanner.layoutParams = params
        reconnectBanner.visibility = View.GONE
        reconnectBanner.alpha = 1f
        reconnectBanner.translationY = 0f
        if (hideOverviewAfterReconnectBanner) {
            hideOverviewAfterReconnectBanner = false
            showEmptyContent()
        }
    }

    companion object {
        private const val RECONNECT_BANNER_ENTER_MS = 180L
        private const val RECONNECT_BANNER_EXIT_MS = 140L
    }
}
