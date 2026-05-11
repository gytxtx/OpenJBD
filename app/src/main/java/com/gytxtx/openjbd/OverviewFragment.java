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
    private LinearProgressIndicator socProgress;

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
        socText = view.findViewById(R.id.txt_soc);
        socProgress = view.findViewById(R.id.progress_soc);
        voltageText = view.findViewById(R.id.txt_voltage);
        currentText = view.findViewById(R.id.txt_current);
        powerText = view.findViewById(R.id.txt_power);
        capacityText = view.findViewById(R.id.txt_capacity);
        cyclesText = view.findViewById(R.id.txt_cycles);
        statsText = view.findViewById(R.id.txt_stats);
        mosText = view.findViewById(R.id.txt_mos);
        temperaturesText = view.findViewById(R.id.txt_temperatures);
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
        if (snapshot.basicInfo == null) {
            clearDeviceData();
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
        renderBasicInfo(snapshot.basicInfo);
        if (snapshot.cellVoltages != null) {
            renderCells(snapshot.cellVoltages);
        }
    }

    private void renderBasicInfo(JbdBasicInfo info) {
        voltageText.setText(String.format(Locale.US, "%.2f V", info.totalVoltage));
        currentText.setText(String.format(Locale.US, "%.2f A", info.current));
        powerText.setText(String.format(Locale.US, "%.1f W", info.totalVoltage * info.current));
        socText.setText(info.soc + "%");
        socProgress.setProgress(info.soc);
        capacityText.setText(String.format(Locale.US, "%.2f / %.2f Ah", info.remainingAh, info.nominalAh));
        cyclesText.setText(String.format(Locale.US, "%d", info.cycleCount));
        mosText.setText(getString(R.string.label_charge) + " " + onOff(info.chargeEnabled) + "  /  " + getString(R.string.label_discharge) + " " + onOff(info.dischargeEnabled));
        statsText.setText(getString(R.string.pack_stats, info.cellCount, info.ntcCount, info.softwareVersion));
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
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.row_cell_voltage, cellList, false);
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

    private void setStatus(String status) {
        statusText.setText(status);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
