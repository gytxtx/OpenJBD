package com.gytxtx.openjbd

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class AboutActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppSettings.preferredContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettings.applyThemePreference(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        SystemBars.applyAppBars(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.about_top_app_bar)
        toolbar.setNavigationIconTint(getColor(R.color.on_primary))
        toolbar.setNavigationOnClickListener { finish() }

        configureAboutList()
    }

    private fun configureAboutList() {
        val versionItem = findViewById<View>(R.id.item_app_version)
        bindListItem(
            versionItem,
            R.drawable.ic_info_outline_24,
            getString(R.string.about_item_app_version),
            appVersionName(),
            false
        )

        val sourceCodeItem = findViewById<View>(R.id.item_source_code)
        bindListItem(
            sourceCodeItem,
            R.drawable.ic_code_24,
            getString(R.string.about_item_source_code),
            null,
            true
        )
        sourceCodeItem.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPOSITORY_URL)))
        }

        val licensesItem = findViewById<View>(R.id.item_licenses)
        bindListItem(
            licensesItem,
            R.drawable.ic_description_outline_24,
            getString(R.string.about_item_licenses),
            null,
            true
        )
        licensesItem.setOnClickListener { startActivity(Intent(this, LicensesActivity::class.java)) }
    }

    private fun bindListItem(
        item: View,
        iconRes: Int,
        titleText: String,
        subtitleText: String?,
        actionable: Boolean
    ) {
        val icon = item.findViewById<ImageView>(R.id.img_about_item_icon)
        val title = item.findViewById<TextView>(R.id.txt_about_item_title)
        val subtitle = item.findViewById<TextView>(R.id.txt_about_item_subtitle)
        val chevron = item.findViewById<ImageView>(R.id.img_about_item_chevron)
        icon.setImageResource(iconRes)
        icon.setColorFilter(getColor(R.color.icon_default))
        icon.contentDescription = titleText
        title.text = titleText
        if (subtitleText.isNullOrEmpty()) {
            subtitle.visibility = View.GONE
        } else {
            subtitle.text = subtitleText
            subtitle.visibility = View.VISIBLE
        }
        chevron.visibility = if (actionable) View.VISIBLE else View.GONE
        item.isClickable = actionable
    }

    private fun appVersionName(): String {
        return try {
            val info = packageManager.getPackageInfo(packageName, 0)
            info.versionName ?: getString(R.string.about_version_unknown)
        } catch (_: PackageManager.NameNotFoundException) {
            getString(R.string.about_version_unknown)
        }
    }

    companion object {
        private const val REPOSITORY_URL = "https://github.com/gytxtx/OpenJBD"
    }
}
