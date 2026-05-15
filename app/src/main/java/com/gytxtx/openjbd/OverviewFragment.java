package com.gytxtx.openjbd;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.gytxtx.openjbd.protocol.JbdBasicInfo;
import com.gytxtx.openjbd.protocol.JbdCellVoltages;

import java.util.Locale;

public final class OverviewFragment extends Fragment implements BmsStateStore.Listener {
    private LinearLayout placeholderConnect;
    private LinearLayout connectedOverviewContent;
    private LinearLayout cellStatsGrid;
    private LinearLayout cellList;
    private TextView statusText;
    private MaterialButton cancelReconnectButton;
    private TextView voltageText;
    private TextView currentText;
    private TextView powerText;
    private TextView socText;
    private TextView capacityText;
    private TextView cyclesText;
    private TextView mosText;
    private TextView socNoteText;
    private TextView statsText;
    private TextView temperaturesText;
    private TextView protectionText;
    private TextView balanceText;
    private ChipGroup temperaturesChipGroup;
    private TextView cellMinText;
    private TextView cellMaxText;
    private TextView cellDeltaText;
    private TextView cellAverageText;
    private LinearProgressIndicator socProgress;
    private JbdBasicInfo lastRenderedBasicInfo;
    private String lastTemperatureSignature = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_overview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        placeholderConnect = view.findViewById(R.id.placeholder_connect);
        connectedOverviewContent = view.findViewById(R.id.content_connected_overview);
        statusText = view.findViewById(R.id.txt_status);
        cancelReconnectButton = view.findViewById(R.id.btn_cancel_reconnect);
        cancelReconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View button) {
                BmsConnectionManager.getInstance(requireContext()).cancelReconnect();
            }
        });
        socText = view.findViewById(R.id.txt_soc);
        socProgress = view.findViewById(R.id.progress_soc);
        voltageText = view.findViewById(R.id.txt_voltage);
        currentText = view.findViewById(R.id.txt_current);
        powerText = view.findViewById(R.id.txt_power);
        capacityText = view.findViewById(R.id.txt_capacity);
        cyclesText = view.findViewById(R.id.txt_cycles);
        statsText = view.findViewById(R.id.txt_stats);
        mosText = view.findViewById(R.id.txt_mos);
        socNoteText = view.findViewById(R.id.txt_soc_note);
        temperaturesText = view.findViewById(R.id.txt_temperatures);
        protectionText = view.findViewById(R.id.txt_protection);
        balanceText = view.findViewById(R.id.txt_balance);
        temperaturesChipGroup = view.findViewById(R.id.chips_temperatures);
        cellStatsGrid = view.findViewById(R.id.cell_stats_grid);
        cellMinText = view.findViewById(R.id.txt_cell_min);
        cellMaxText = view.findViewById(R.id.txt_cell_max);
        cellDeltaText = view.findViewById(R.id.txt_cell_delta);
        cellAverageText = view.findViewById(R.id.txt_cell_average);
        cellList = view.findViewById(R.id.list_cells);
        clearDeviceData();
    }

    @Override
    public void onStart() {
        super.onStart();
        BmsStateStore.addListener(this);
        renderState(BmsStateStore.getSnapshot());
    }

    @Override
    public void onStop() {
        BmsStateStore.removeListener(this);
        super.onStop();
    }

    @Override
    public void onBmsStateChanged(final BmsStateStore.Snapshot snapshot) {
        if (getActivity() == null) {
            return;
        }
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                renderState(snapshot);
            }
        });
    }

    private void renderState(BmsStateStore.Snapshot snapshot) {
        if (statusText == null) {
            return;
        }
        renderReconnectAction(snapshot);
        if (snapshot.basicInfo == null) {
            clearDeviceData();
            renderReconnectAction(snapshot);
            if (snapshot.status != null && snapshot.status.length() > 0) {
                setStatus(snapshot.status);
            }
            if (snapshot.connected || snapshot.deviceName != null) {
                showConnectedContent();
            }
            return;
        }
        showConnectedContent();
        setStatus(snapshot.status);
        if (snapshot.basicInfo != lastRenderedBasicInfo) {
            renderBasicInfo(snapshot.basicInfo);
            lastRenderedBasicInfo = snapshot.basicInfo;
        }
        if (snapshot.cellVoltages != null) {
            renderCells(snapshot.cellVoltages);
        }
    }

    private void renderBasicInfo(JbdBasicInfo info) {
        setTextIfChanged(voltageText, String.format(Locale.US, "%.2f V", info.totalVoltage));
        setTextIfChanged(currentText, String.format(Locale.US, "%.2f A", info.current));
        setTextIfChanged(powerText, String.format(Locale.US, "%.1f W", info.totalVoltage * info.current));
        setTextIfChanged(socText, info.soc + "%");
        socProgress.setProgressCompat(info.soc, false);
        setTextIfChanged(capacityText, String.format(Locale.US, "%.2f / %.2f Ah", info.remainingAh, info.learnedOrNominalAh()));
        setTextIfChanged(cyclesText, String.format(Locale.US, "%d", info.cycleCount));
        setTextIfChanged(mosText, getString(R.string.label_charge) + " " + onOff(info.chargeEnabled) + "  /  " + getString(R.string.label_discharge) + " " + onOff(info.dischargeEnabled));
        setTextIfChanged(socNoteText, socNote(info));
        setTextIfChanged(statsText, getString(R.string.pack_stats, info.cellCount, info.ntcCount, info.softwareVersion));
        setTextIfChanged(protectionText, protectionSummary(info));
        setTextIfChanged(balanceText, balanceSummary(info));
        String temperatureSignature = temperatureSignature(info);
        if (temperatureSignature.equals(lastTemperatureSignature)) {
            return;
        }
        lastTemperatureSignature = temperatureSignature;
        if (!info.temperaturesC.isEmpty()) {
            temperaturesText.setVisibility(View.GONE);
            temperaturesChipGroup.removeAllViews();
            temperaturesChipGroup.setVisibility(View.VISIBLE);
            temperaturesChipGroup.addView(temperatureChip(String.format(Locale.US, getString(R.string.temperature_mos_item), 0, AppSettings.displayTemperature(requireContext(), info.temperaturesC.get(0)), AppSettings.temperatureUnitLabel(requireContext())), true));
            for (int i = 0; i < info.temperaturesC.size(); i++) {
                temperaturesChipGroup.addView(temperatureChip(String.format(Locale.US, getString(R.string.temperature_probe_item), i + 1, AppSettings.displayTemperature(requireContext(), info.temperaturesC.get(i)), AppSettings.temperatureUnitLabel(requireContext())), false));
            }
        } else {
            temperaturesChipGroup.removeAllViews();
            temperaturesChipGroup.setVisibility(View.GONE);
            temperaturesText.setVisibility(View.VISIBLE);
            temperaturesText.setText(getString(R.string.temperature_none));
        }
    }

    private void renderCells(JbdCellVoltages voltages) {
        cellStatsGrid.setVisibility(View.VISIBLE);
        setTextIfChanged(cellMinText, String.format(Locale.US, "%.3f V", voltages.min));
        setTextIfChanged(cellMaxText, String.format(Locale.US, "%.3f V", voltages.max));
        setTextIfChanged(cellDeltaText, String.format(Locale.US, "%.3f V", voltages.delta));
        setTextIfChanged(cellAverageText, String.format(Locale.US, "%.3f V", voltages.average));
        if (cellList.getChildCount() != voltages.cells.size()) {
            cellList.removeAllViews();
            for (int i = 0; i < voltages.cells.size(); i++) {
                cellList.addView(createCellVoltageRow(i + 1));
            }
        }
        for (int i = 0; i < voltages.cells.size(); i++) {
            updateCellVoltageRow(cellList.getChildAt(i), i + 1, voltages.cells.get(i), voltages.min, voltages.max);
        }
    }

    private View createCellVoltageRow(int index) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.row_cell_voltage, cellList, false);
        TextView label = row.findViewById(R.id.txt_cell_label);
        label.setText(String.format(Locale.US, getString(R.string.cell_label), index));
        return row;
    }

    private void updateCellVoltageRow(View row, int index, float voltage, float min, float max) {
        TextView label = row.findViewById(R.id.txt_cell_label);
        TextView value = row.findViewById(R.id.txt_cell_value);
        LinearProgressIndicator progress = row.findViewById(R.id.progress_cell);
        String labelText = String.format(Locale.US, getString(R.string.cell_label), index);
        JbdBasicInfo basicInfo = lastRenderedBasicInfo;
        if (basicInfo != null && index <= basicInfo.balanceStates.length && basicInfo.balanceStates[index - 1]) {
            labelText += getString(R.string.cell_balancing_suffix);
        }
        setTextIfChanged(label, labelText);
        setTextIfChanged(value, String.format(Locale.US, "%.3f V", voltage));
        float span = Math.max(0.001f, max - min);
        int scaled = Math.max(80, Math.min(1000, (int) (1000f * ((voltage - min) / span))));
        progress.setProgressCompat(scaled, false);
        int color = voltage == max ? Color.rgb(0, 166, 118) : voltage == min ? Color.rgb(21, 101, 192) : Color.rgb(117, 125, 138);
        progress.setIndicatorColor(color);
    }

    private Chip temperatureChip(String text, boolean primary) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCheckable(false);
        chip.setClickable(false);
        chip.setTextColor(requireContext().getColor(primary ? R.color.primary : R.color.text_primary));
        chip.setChipBackgroundColorResource(R.color.surface);
        chip.setChipStrokeColorResource(primary ? R.color.primary : R.color.card_stroke);
        chip.setChipStrokeWidth(dp(1));
        chip.setChipMinHeight(dp(36));
        chip.setTextSize(14);
        chip.setEnsureMinTouchTargetSize(false);
        return chip;
    }

    private void clearDeviceData() {
        cancelReconnectButton.setVisibility(View.GONE);
        voltageText.setText("--");
        currentText.setText("--");
        powerText.setText("--");
        socText.setText("--%");
        socProgress.setProgress(0);
        capacityText.setText("--");
        cyclesText.setText("--");
        mosText.setText("--");
        statsText.setText("--");
        socNoteText.setText(R.string.local_ble_note);
        protectionText.setText("--");
        balanceText.setText("--");
        temperaturesText.setText("--");
        temperaturesText.setVisibility(View.VISIBLE);
        temperaturesChipGroup.removeAllViews();
        temperaturesChipGroup.setVisibility(View.GONE);
        cellMinText.setText("--");
        cellMaxText.setText("--");
        cellDeltaText.setText("--");
        cellAverageText.setText("--");
        cellStatsGrid.setVisibility(View.GONE);
        lastRenderedBasicInfo = null;
        lastTemperatureSignature = "";
        showEmptyCells();
        setStatus(getString(R.string.status_select_bms));
        connectedOverviewContent.setVisibility(View.GONE);
        placeholderConnect.setVisibility(View.VISIBLE);
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

    private TextView text(String value, int sp, int color, int style) {
        TextView view = new TextView(requireContext());
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        return view;
    }

    private MaterialCardView wrapCard(View child, int bottomMarginDp) {
        MaterialCardView card = new MaterialCardView(requireContext());
        card.setCardBackgroundColor(requireContext().getColor(R.color.surface));
        card.setRadius(dp(8));
        card.setStrokeColor(requireContext().getColor(R.color.card_stroke));
        card.setStrokeWidth(1);
        card.setCardElevation(dp(2));
        card.setUseCompatPadding(true);
        card.addView(child);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(bottomMarginDp);
        card.setLayoutParams(params);
        return card;
    }

    private String onOff(boolean value) {
        return value ? getString(R.string.label_on) : getString(R.string.label_off);
    }

    private String protectionSummary(JbdBasicInfo info) {
        int[] labels = protectionLabelIds();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < info.protectionStates.length && i < labels.length; i++) {
            if (!info.protectionStates[i]) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" / ");
            }
            builder.append(getString(labels[i]));
        }
        return builder.length() == 0 ? getString(R.string.protection_none) : builder.toString();
    }

    private int[] protectionLabelIds() {
        return new int[]{
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
        };
    }

    private String balanceSummary(JbdBasicInfo info) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < info.balanceStates.length; i++) {
            if (!info.balanceStates[i]) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(i + 1);
        }
        return builder.length() == 0 ? getString(R.string.balance_none) : getString(R.string.balance_cells, builder.toString());
    }

    private String socNote(JbdBasicInfo info) {
        float absCurrent = Math.abs(info.current);
        if (absCurrent < 0.05f) {
            return getString(R.string.local_ble_note);
        }
        if (info.current < 0f) {
            return getString(R.string.estimate_discharging, formatDurationHours(info.remainingAh / absCurrent));
        }
        float fullCapacityAh = Math.max(info.remainingAh, info.learnedOrNominalAh());
        return getString(R.string.estimate_charging, formatDurationHours((fullCapacityAh - info.remainingAh) / absCurrent));
    }

    private String formatDurationHours(float hours) {
        if (Float.isNaN(hours) || Float.isInfinite(hours) || hours <= 0f) {
            return getString(R.string.estimate_duration_zero);
        }
        long totalMinutes = Math.round(hours * 60f);
        return getString(R.string.estimate_duration_hours_minutes, totalMinutes / 60, totalMinutes % 60);
    }

    private void setStatus(String status) {
        setTextIfChanged(statusText, status);
    }

    private void renderReconnectAction(BmsStateStore.Snapshot snapshot) {
        cancelReconnectButton.setVisibility(snapshot.connectionState == BmsStateStore.ConnectionState.WAITING_RECONNECT ? View.VISIBLE : View.GONE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void setTextIfChanged(TextView view, String value) {
        if (!value.contentEquals(view.getText())) {
            view.setText(value);
        }
    }

    private String temperatureSignature(JbdBasicInfo info) {
        if (info.temperaturesC.isEmpty()) {
            return "none";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(AppSettings.temperatureUnitLabel(requireContext()));
        for (Float temperature : info.temperaturesC) {
            builder.append('|').append(String.format(Locale.US, "%.1f", AppSettings.displayTemperature(requireContext(), temperature)));
        }
        return builder.toString();
    }
}
