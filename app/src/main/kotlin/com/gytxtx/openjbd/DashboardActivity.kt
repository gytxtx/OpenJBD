package com.gytxtx.openjbd

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gytxtx.openjbd.data.BmsRepository
import com.gytxtx.openjbd.data.BmsUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {
    private lateinit var socValue: TextView
    private lateinit var voltageValue: TextView
    private lateinit var currentValue: TextView
    private lateinit var powerValue: TextView
    private lateinit var statusValue: TextView

    @Inject lateinit var repository: BmsRepository

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
        findViewById<ImageButton>(R.id.btn_dashboard_back).setOnClickListener { finish() }
        SystemBars.applyFullscreen(this)
        render(repository.getSnapshot())
    }

    override fun onStart() {
        super.onStart()
        SystemBars.applyFullscreen(this)
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                repository.uiState.collect { render(it) }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) SystemBars.applyFullscreen(this)
    }

    override fun onStop() {
        super.onStop()
    }

    private fun render(snapshot: BmsUiState?) {
        val info = snapshot?.basicInfo
        if (info == null) {
            socValue.setText(R.string.placeholder_percent)
            voltageValue.setText(R.string.placeholder_voltage)
            currentValue.setText(R.string.placeholder_current)
            powerValue.setText(R.string.placeholder_power)
            val status = snapshot?.status
            statusValue.setText(if (status.isNullOrEmpty()) getString(R.string.dashboard_waiting_data) else status)
            return
        }
        socValue.text = getString(R.string.format_value_percent, info.soc)
        voltageValue.text = getString(R.string.format_value_voltage_2, info.totalVoltage)
        currentValue.text = getString(R.string.format_value_current_2, info.current)
        powerValue.text = getString(R.string.format_value_power_1, info.totalVoltage * info.current)
        statusValue.setText(R.string.dashboard_live)
    }
}
