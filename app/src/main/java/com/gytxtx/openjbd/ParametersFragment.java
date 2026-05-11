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

import java.util.Locale;

public final class ParametersFragment extends Fragment implements BmsStateStore.Listener {
    private LinearLayout placeholderParameters;
    private MaterialCardView parametersCard;
    private LinearLayout parametersList;
    private ParametersAdapter parametersAdapter;
    private BmsStateStore.Snapshot snapshot;

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
        placeholderParameters.setVisibility(hasData ? View.GONE : View.VISIBLE);
        parametersCard.setVisibility(hasData ? View.VISIBLE : View.GONE);
        renderParameterRows();
    }

    private void renderParameterRows() {
        parametersList.removeAllViews();
        for (int i = 0; i < parametersAdapter.getCount(); i++) {
            parametersList.addView(parametersAdapter.getView(i, null, parametersList));
        }
    }

    private final class ParametersAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return 20;
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
            icon.setColorFilter(requireContext().getColor(R.color.text_secondary));
            title.setText(parameterTitle(position));
            subtitle.setText(parameterValue(position));
            return row;
        }

        private int parameterIcon(int position) {
            if (position == 0 || position == 1) {
                return R.drawable.ic_bluetooth_searching_24;
            }
            if (position == 8 || position == 9) {
                return R.drawable.ic_battery_4_bar_24;
            }
            if (position == 10 || position == 11) {
                return R.drawable.ic_bolt_24;
            }
            if (position == 12 || position == 13 || position == 14) {
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
            if (info == null) {
                return unread;
            }
            switch (position) {
                case 2:
                    return info.softwareVersion;
                case 3:
                    return info.productionDate;
                case 4:
                    return String.format(Locale.US, "%.2f Ah", info.nominalAh);
                case 5:
                    return String.format(Locale.US, "%.2f Ah", info.remainingAh);
                case 6:
                    return info.soc + "%";
                case 7:
                    return String.format(Locale.US, "%d", info.cycleCount);
                case 8:
                    return String.format(Locale.US, "%d", info.cellCount);
                case 9:
                    return String.format(Locale.US, "%d", info.ntcCount);
                case 10:
                    return onOff(info.chargeEnabled);
                case 11:
                    return onOff(info.dischargeEnabled);
                case 12:
                    return String.format(Locale.US, "%.2f V", info.totalVoltage);
                case 13:
                    return String.format(Locale.US, "%.2f A", info.current);
                case 14:
                    return String.format(Locale.US, "%.1f W", info.totalVoltage * info.current);
                default:
                    return unread;
            }
        }
    }

    private String onOff(boolean value) {
        return value ? getString(R.string.label_on) : getString(R.string.label_off);
    }
}
