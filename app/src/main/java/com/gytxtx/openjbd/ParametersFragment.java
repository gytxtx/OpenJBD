package com.gytxtx.openjbd;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.gytxtx.openjbd.protocol.JbdBasicInfo;
import com.gytxtx.openjbd.protocol.JbdDeviceInfo;

public final class ParametersFragment extends Fragment implements BmsStateStore.Listener {
    private LinearLayout placeholderParameters;
    private MaterialCardView parametersCard;
    private LinearLayout parametersList;
    private ParametersAdapter parametersAdapter;
    private BmsStateStore.Snapshot snapshot;
    private boolean lastRenderedHasData;
    private JbdBasicInfo lastRenderedBasicInfo;
    private JbdDeviceInfo lastRenderedDeviceInfo;
    private String lastRenderedDeviceName;
    private String lastRenderedDeviceAddress;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parameters, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        placeholderParameters = view.findViewById(R.id.placeholder_parameters);
        parametersCard = view.findViewById(R.id.card_parameters);
        parametersList = view.findViewById(R.id.list_parameters);
        parametersAdapter = new ParametersAdapter();
        snapshot = BmsStateStore.getSnapshot();
        updateParametersPage();
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
        this.snapshot = snapshot;
        updateParametersPage();
    }

    private void updateParametersPage() {
        if (placeholderParameters == null || parametersCard == null || parametersAdapter == null) {
            return;
        }
        boolean hasData = snapshot != null && snapshot.basicInfo != null;
        String deviceName = snapshot == null ? null : snapshot.deviceName;
        String deviceAddress = snapshot == null ? null : snapshot.deviceAddress;
        if (hasData == lastRenderedHasData
                && same(lastRenderedBasicInfo, snapshot == null ? null : snapshot.basicInfo)
                && same(lastRenderedDeviceInfo, snapshot == null ? null : snapshot.deviceInfo)
                && same(lastRenderedDeviceName, deviceName)
                && same(lastRenderedDeviceAddress, deviceAddress)) {
            return;
        }
        lastRenderedHasData = hasData;
        lastRenderedBasicInfo = snapshot == null ? null : snapshot.basicInfo;
        lastRenderedDeviceInfo = snapshot == null ? null : snapshot.deviceInfo;
        lastRenderedDeviceName = deviceName;
        lastRenderedDeviceAddress = deviceAddress;
        placeholderParameters.setVisibility(hasData ? View.GONE : View.VISIBLE);
        parametersCard.setVisibility(hasData ? View.VISIBLE : View.GONE);
        renderParameterRows();
    }

    private void renderParameterRows() {
        while (parametersList.getChildCount() < parametersAdapter.getCount()) {
            parametersList.addView(parametersAdapter.getView(parametersList.getChildCount(), null, parametersList));
        }
        while (parametersList.getChildCount() > parametersAdapter.getCount()) {
            parametersList.removeViewAt(parametersList.getChildCount() - 1);
        }
        for (int i = 0; i < parametersAdapter.getCount(); i++) {
            parametersAdapter.getView(i, parametersList.getChildAt(i), parametersList);
        }
    }

    private boolean same(Object first, Object second) {
        return first == second || (first != null && first.equals(second));
    }

    private final class ParametersAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return 26;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = LayoutInflater.from(requireContext()).inflate(R.layout.row_setting_item, parent, false);
            }
            ImageView icon = row.findViewById(R.id.img_setting_icon);
            TextView title = row.findViewById(R.id.txt_setting_title);
            TextView subtitle = row.findViewById(R.id.txt_setting_subtitle);
            View settingSwitch = row.findViewById(R.id.switch_setting_action);
            settingSwitch.setVisibility(View.GONE);
            icon.setImageResource(parameterIcon(position));
            icon.setColorFilter(requireContext().getColor(R.color.icon_default));
            title.setText(parameterTitle(position));
            setTextIfChanged(subtitle, parameterValue(position));
            return row;
        }

        private int parameterIcon(int position) {
            if (position == 0 || position == 1) {
                return R.drawable.ic_bluetooth_searching_24;
            }
            if (position == 4 || position == 8 || position == 9 || position == 20 || position == 23 || position == 24) {
                return R.drawable.ic_battery_4_bar_24;
            }
            if (position == 25) {
                return R.drawable.ic_widgets_outline_24;
            }
            if (position == 10 || position == 11) {
                return R.drawable.ic_bolt_24;
            }
            if (position == 12 || position == 13 || position == 14 || position == 25) {
                return R.drawable.ic_dashboard_24;
            }
            return R.drawable.ic_info_24;
        }

        private int parameterTitle(int position) {
            switch (position) {
                case 0:
                    return R.string.param_bluetooth_name;
                case 1:
                    return R.string.param_device_address;
                case 2:
                    return R.string.param_bms_version;
                case 3:
                    return R.string.param_manufacturing_date;
                case 4:
                    return R.string.param_nominal_capacity;
                case 5:
                    return R.string.param_remaining_capacity;
                case 6:
                    return R.string.param_soc;
                case 7:
                    return R.string.param_cycle_count;
                case 8:
                    return R.string.param_cell_count;
                case 9:
                    return R.string.param_ntc_count;
                case 10:
                    return R.string.param_charge_mos;
                case 11:
                    return R.string.param_discharge_mos;
                case 12:
                    return R.string.param_pack_voltage;
                case 13:
                    return R.string.param_pack_current;
                case 14:
                    return R.string.param_pack_power;
                case 15:
                    return R.string.param_serial_number;
                case 16:
                    return R.string.param_barcode;
                case 17:
                    return R.string.param_battery_model;
                case 18:
                    return R.string.param_manufacturer;
                case 20:
                    return R.string.param_learn_capacity;
                case 21:
                    return R.string.param_extension_marker;
                case 22:
                    return R.string.param_alter;
                case 23:
                    return R.string.param_balance_current;
                case 24:
                    return R.string.param_balance_state;
                case 25:
                    return R.string.param_protection_state;
                default:
                    return R.string.param_bms_model;
            }
        }

        private String parameterValue(int position) {
            String unread = getString(R.string.param_unread);
            if (position == 0) {
                return snapshot == null || snapshot.deviceName == null || snapshot.deviceName.length() == 0 ? unread : snapshot.deviceName;
            }
            if (position == 1) {
                return snapshot == null || snapshot.deviceAddress == null || snapshot.deviceAddress.length() == 0 ? unread : snapshot.deviceAddress;
            }
            JbdBasicInfo info = snapshot == null ? null : snapshot.basicInfo;
            JbdDeviceInfo deviceInfo = snapshot == null ? null : snapshot.deviceInfo;
            if (info == null) {
                return unread;
            }
            switch (position) {
                case 2:
                    return info.softwareVersion;
                case 3:
                    return info.productionDate;
                case 4:
                    return getString(R.string.format_value_capacity_2, info.nominalAh);
                case 5:
                    return getString(R.string.format_value_capacity_2, info.remainingAh);
                case 6:
                    return getString(R.string.format_value_percent, info.soc);
                case 7:
                    return getString(R.string.format_value_integer, info.cycleCount);
                case 8:
                    return getString(R.string.format_value_integer, info.cellCount);
                case 9:
                    return getString(R.string.format_value_integer, info.ntcCount);
                case 10:
                    return onOff(info.chargeEnabled);
                case 11:
                    return onOff(info.dischargeEnabled);
                case 12:
                    return getString(R.string.format_value_voltage_2, info.totalVoltage);
                case 13:
                    return getString(R.string.format_value_current_2, info.current);
                case 14:
                    return getString(R.string.format_value_power_1, info.totalVoltage * info.current);
                case 15:
                    return fieldOrUnread(deviceInfo == null ? null : deviceInfo.serialNumber, unread);
                case 16:
                    return fieldOrUnread(deviceInfo == null ? null : deviceInfo.barcode, unread);
                case 17:
                    return fieldOrUnread(deviceInfo == null ? null : deviceInfo.batteryModel, unread);
                case 18:
                    return fieldOrUnread(deviceInfo == null ? null : deviceInfo.manufacturer, unread);
                case 19:
                    return fieldOrUnread(deviceInfo == null ? null : deviceInfo.bmsModel, unread);
                case 20:
                    return info.hasLearnCapacity ? getString(R.string.format_value_capacity_2, info.learnCapacityAh) : unread;
                case 21:
                    return info.hasExtendedInfo ? getString(R.string.format_value_integer, info.extensionMarker) : unread;
                case 22:
                    return info.hasExtendedInfo ? getString(R.string.format_value_integer, info.alter) : unread;
                case 23:
                    return info.hasBalanceCurrent ? getString(R.string.format_value_current_2, info.balanceCurrentA) : unread;
                case 24:
                    return balanceSummary(info);
                case 25:
                    return protectionSummary(info);
                default:
                    return unread;
            }
        }

        private String fieldOrUnread(String value, String unread) {
            return value == null || value.length() == 0 ? unread : value;
        }
    }

    private String onOff(boolean value) {
        return value ? getString(R.string.label_on) : getString(R.string.label_off);
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

    private void setTextIfChanged(TextView view, String value) {
        if (!value.contentEquals(view.getText())) {
            view.setText(value);
        }
    }
}
