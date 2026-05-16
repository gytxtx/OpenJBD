package com.gytxtx.openjbd;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public final class DashboardActivity extends AppCompatActivity implements BmsStateStore.Listener {
    private TextView socValue;
    private TextView voltageValue;
    private TextView currentValue;
    private TextView powerValue;
    private TextView statusValue;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppSettings.preferredContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyThemePreference(this);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_dashboard);
        socValue = findViewById(R.id.txt_dashboard_soc);
        voltageValue = findViewById(R.id.txt_dashboard_voltage);
        currentValue = findViewById(R.id.txt_dashboard_current);
        powerValue = findViewById(R.id.txt_dashboard_power);
        statusValue = findViewById(R.id.txt_dashboard_status);
        ImageButton backButton = findViewById(R.id.btn_dashboard_back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        SystemBars.applyFullscreen(this);
        render(BmsStateStore.getSnapshot());
    }

    @Override
    protected void onStart() {
        super.onStart();
        SystemBars.applyFullscreen(this);
        BmsStateStore.addListener(this);
        render(BmsStateStore.getSnapshot());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            SystemBars.applyFullscreen(this);
        }
    }

    @Override
    protected void onStop() {
        BmsStateStore.removeListener(this);
        super.onStop();
    }

    @Override
    public void onBmsStateChanged(final BmsStateStore.Snapshot snapshot) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                render(snapshot);
            }
        });
    }

    private void render(BmsStateStore.Snapshot snapshot) {
        if (snapshot == null || snapshot.basicInfo == null) {
            socValue.setText(R.string.placeholder_percent);
            voltageValue.setText(R.string.placeholder_voltage);
            currentValue.setText(R.string.placeholder_current);
            powerValue.setText(R.string.placeholder_power);
            statusValue.setText(snapshot != null && snapshot.status != null && snapshot.status.length() > 0 ? snapshot.status : getString(R.string.dashboard_waiting_data));
            return;
        }
        socValue.setText(getString(R.string.format_value_percent, snapshot.basicInfo.soc));
        voltageValue.setText(getString(R.string.format_value_voltage_2, snapshot.basicInfo.totalVoltage));
        currentValue.setText(getString(R.string.format_value_current_2, snapshot.basicInfo.current));
        powerValue.setText(getString(R.string.format_value_power_1, snapshot.basicInfo.totalVoltage * snapshot.basicInfo.current));
        statusValue.setText(R.string.dashboard_live);
    }
}
