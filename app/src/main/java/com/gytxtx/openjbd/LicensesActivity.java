package com.gytxtx.openjbd;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class LicensesActivity extends AppCompatActivity {
    private LicenseItem[] licenseItems;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppSettings.preferredContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyThemePreference(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licenses);
        SystemBars.applyAppBars(this);

        MaterialToolbar toolbar = findViewById(R.id.licenses_top_app_bar);
        toolbar.setNavigationIconTint(getColor(R.color.on_primary));
        toolbar.setNavigationOnClickListener(view -> finish());

        licenseItems = new LicenseItem[]{
                new LicenseItem(getString(R.string.license_item_openjbd), getString(R.string.license_mit), getString(R.string.license_detail_openjbd)),
                new LicenseItem(getString(R.string.license_item_androidx_fragment), getString(R.string.license_apache_2), getString(R.string.license_detail_androidx_fragment)),
                new LicenseItem(getString(R.string.license_item_material_components), getString(R.string.license_apache_2), getString(R.string.license_detail_material_components)),
                new LicenseItem(getString(R.string.license_item_swiperefreshlayout), getString(R.string.license_apache_2), getString(R.string.license_detail_swiperefreshlayout))
        };
        renderLicenseList();
    }

    private void renderLicenseList() {
        LinearLayout list = findViewById(R.id.licenses_list);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < licenseItems.length; i++) {
            LicenseItem licenseItem = licenseItems[i];
            View row = inflater.inflate(R.layout.row_about_list_item, list, false);
            bindLicenseRow(row, licenseItem);
            list.addView(row);
            if (i < licenseItems.length - 1) {
                list.addView(divider());
            }
        }
    }

    private void bindLicenseRow(View row, LicenseItem licenseItem) {
        ImageView icon = row.findViewById(R.id.img_about_item_icon);
        TextView title = row.findViewById(R.id.txt_about_item_title);
        TextView subtitle = row.findViewById(R.id.txt_about_item_subtitle);
        ImageView chevron = row.findViewById(R.id.img_about_item_chevron);

        icon.setImageResource(R.drawable.ic_description_outline_24);
        icon.setColorFilter(getColor(R.color.icon_default));
        icon.setContentDescription(licenseItem.name);
        title.setText(licenseItem.name);
        subtitle.setText(licenseItem.licenseName);
        subtitle.setVisibility(View.VISIBLE);
        chevron.setVisibility(View.VISIBLE);
        row.setOnClickListener(view -> showLicenseDetail(licenseItem));
    }

    private View divider() {
        View divider = new View(this);
        divider.setBackgroundColor(getColor(R.color.card_stroke));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1);
        params.setMarginStart(dp(56));
        divider.setLayoutParams(params);
        return divider;
    }

    private void showLicenseDetail(LicenseItem licenseItem) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(licenseItem.name)
                .setMessage(licenseItem.detail)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class LicenseItem {
        final String name;
        final String licenseName;
        final String detail;

        LicenseItem(String name, String licenseName, String detail) {
            this.name = name;
            this.licenseName = licenseName;
            this.detail = detail;
        }
    }
}
