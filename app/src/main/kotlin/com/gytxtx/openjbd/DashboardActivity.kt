package com.gytxtx.openjbd

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity(), BmsStateStore.Listener {
    private lateinit var socValue: TextView
    private lateinit var voltageValue: TextView
    private lateinit var currentValue: TextView
    private lateinit var powerValue: TextView
    private lateinit var statusValue: TextView

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppSettings.preferredContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettings.applyThemePreference(this)
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_dashboard)
        socValue = findViewById(R.id.txt_dashboard_soc)
        voltageValue = findViewById(R.id.txt_dashboard_voltage)
        currentValue = findViewById(R.id.txt_dashboard_current)
        powerValue = findViewById(R.id.txt_dashboard_power)
        statusValue = findViewById(R.id.txt_dashboard_status)
        val backButton = findViewById<ImageButton>(R.id.btn_dashboard_back)
        backButton.setOnClickListener { finish() }
        SystemBars.applyFullscreen(this)
        render(BmsStateStore.getSnapshot())
    }

    override fun onStart() {
        super.onStart()
        SystemBars.applyFullscreen(this)
        BmsStateStore.addListener(this)
        render(BmsStateStore.getSnapshot())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            SystemBars.applyFullscreen(this)
        }
    }

    override fun onStop() {
        BmsStateStore.removeListener(this)
        super.onStop()
    }

    override fun onBmsStateChanged(snapshot: BmsStateStore.Snapshot) {
        runOnUiThread { render(snapshot) }
    }

    private fun render(snapshot: BmsStateStore.Snapshot?) {
        if (snapshot == null || snapshot.basicInfo == null) {
            socValue.setText(R.string.placeholder_percent)
            voltageValue.setText(R.string.placeholder_voltage)
            currentValue.setText(R.string.placeholder_current)
            powerValue.setText(R.string.placeholder_power)
            statusValue.setText(
                if (snapshot != null && !snapshot.status.isNullOrEmpty())
                    snapshot.status
                else
                    getString(R.string.dashboard_waiting_data)
            )
            return
        }
        socValue.text = getString(R.string.format_value_percent, snapshot.basicInfo!!.soc)
        voltageValue.text = getString(R.string.format_value_voltage_2, snapshot.basicInfo!!.totalVoltage)
        currentValue.text = getString(R.string.format_value_current_2, snapshot.basicInfo!!.current)
        powerValue.text = getString(
            R.string.format_value_power_1,
            snapshot.basicInfo!!.totalVoltage * snapshot.basicInfo!!.current
        )
        statusValue.setText(R.string.dashboard_live)
    }
}
