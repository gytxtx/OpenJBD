package com.gytxtx.openjbd

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LicensesActivity : AppCompatActivity() {
    private lateinit var licenseItems: Array<LicenseItem>

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppSettings.preferredContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettings.applyThemePreference(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_licenses)
        SystemBars.applyAppBars(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.licenses_top_app_bar)
        toolbar.setNavigationIconTint(getColor(R.color.on_primary))
        toolbar.setNavigationOnClickListener { finish() }

        licenseItems = arrayOf(
            LicenseItem(
                getString(R.string.license_item_openjbd),
                getString(R.string.license_mit),
                R.raw.license_mit
            ),
            LicenseItem(
                getString(R.string.license_item_compose),
                getString(R.string.license_apache_2),
                R.raw.license_apache_20
            ),
            LicenseItem(
                getString(R.string.license_item_material_components),
                getString(R.string.license_apache_2),
                R.raw.license_apache_20
            ),
            LicenseItem(
                getString(R.string.license_item_navigation),
                getString(R.string.license_apache_2),
                R.raw.license_apache_20
            ),
            LicenseItem(
                getString(R.string.license_item_lifecycle),
                getString(R.string.license_apache_2),
                R.raw.license_apache_20
            ),
            LicenseItem(
                getString(R.string.license_item_hilt),
                getString(R.string.license_apache_2),
                R.raw.license_apache_20
            ),
            LicenseItem(
                getString(R.string.license_item_core_ktx),
                getString(R.string.license_apache_2),
                R.raw.license_apache_20
            ),
            LicenseItem(
                getString(R.string.license_item_appcompat),
                getString(R.string.license_apache_2),
                R.raw.license_apache_20
            ),
            LicenseItem(
                getString(R.string.license_item_datastore),
                getString(R.string.license_apache_2),
                R.raw.license_apache_20
            ),
            LicenseItem(
                getString(R.string.license_item_kotlinx_serialization),
                getString(R.string.license_apache_2),
                R.raw.license_apache_20
            ),
            LicenseItem(
                getString(R.string.license_item_fragment),
                getString(R.string.license_apache_2),
                R.raw.license_apache_20
            ),
            LicenseItem(
                getString(R.string.license_item_swiperefreshlayout),
                getString(R.string.license_apache_2),
                R.raw.license_apache_20
            )
        )
        renderLicenseList()
    }

    private fun renderLicenseList() {
        val list = findViewById<LinearLayout>(R.id.licenses_list)
        val inflater = LayoutInflater.from(this)
        for (i in licenseItems.indices) {
            val licenseItem = licenseItems[i]
            val row = inflater.inflate(R.layout.row_about_list_item, list, false)
            bindLicenseRow(row, licenseItem)
            list.addView(row)
            if (i < licenseItems.size - 1) {
                list.addView(divider())
            }
        }
    }

    private fun bindLicenseRow(row: View, licenseItem: LicenseItem) {
        val icon = row.findViewById<ImageView>(R.id.img_about_item_icon)
        val title = row.findViewById<TextView>(R.id.txt_about_item_title)
        val subtitle = row.findViewById<TextView>(R.id.txt_about_item_subtitle)
        val chevron = row.findViewById<ImageView>(R.id.img_about_item_chevron)

        icon.setImageResource(R.drawable.ic_description_outline_24)
        icon.setColorFilter(getColor(R.color.icon_default))
        icon.contentDescription = licenseItem.name
        title.text = licenseItem.name
        subtitle.text = licenseItem.licenseName
        subtitle.visibility = View.VISIBLE
        chevron.visibility = View.VISIBLE
        row.setOnClickListener { showLicenseDetail(licenseItem) }
    }

    private fun divider(): View {
        val divider = View(this)
        divider.setBackgroundColor(getColor(R.color.card_stroke))
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
        params.marginStart = dp(56)
        divider.layoutParams = params
        return divider
    }

    private fun showLicenseDetail(licenseItem: LicenseItem) {
        val text = readRawText(licenseItem.rawResId)
        MaterialAlertDialogBuilder(this)
            .setTitle(licenseItem.name)
            .setMessage(text)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun readRawText(resId: Int): String {
        return try {
            val inputStream = resources.openRawResource(resId)
            inputStream.bufferedReader().use { it.readText() }
        } catch (_: java.io.IOException) {
            ""
        }
    }

    private fun dp(value: Int): Int =
        Math.round(value * resources.displayMetrics.density)

    private class LicenseItem(
        val name: String,
        val licenseName: String,
        val rawResId: Int
    )
}
