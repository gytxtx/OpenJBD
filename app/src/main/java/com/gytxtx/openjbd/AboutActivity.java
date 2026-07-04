package com.gytxtx.openjbd;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public final class AboutActivity extends AppCompatActivity {
    private static final String REPOSITORY_URL = "https://github.com/gytxtx/OpenJBD";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppSettings.preferredContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyThemePreference(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        SystemBars.applyAppBars(this);

        MaterialToolbar toolbar = findViewById(R.id.about_top_app_bar);
        toolbar.setNavigationIconTint(getColor(R.color.on_primary));
        toolbar.setOnMenuItemClickListener(new MaterialToolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return false;
            }
        });
        toolbar.setNavigationOnClickListener(view -> finish());

        configureAboutList();
    }

    private void configureAboutList() {
        View versionItem = findViewById(R.id.item_app_version);
        bindListItem(versionItem, R.drawable.ic_info_outline_24, getString(R.string.about_item_app_version), appVersionName(), false);

        View sourceCodeItem = findViewById(R.id.item_source_code);
        bindListItem(sourceCodeItem, R.drawable.ic_code_24, getString(R.string.about_item_source_code), null, true);
        sourceCodeItem.setOnClickListener(view -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(REPOSITORY_URL))));

        View licensesItem = findViewById(R.id.item_licenses);
        bindListItem(licensesItem, R.drawable.ic_description_outline_24, getString(R.string.about_item_licenses), null, true);
        licensesItem.setOnClickListener(view -> startActivity(new Intent(this, LicensesActivity.class)));
    }

    private void bindListItem(View item, int iconRes, String titleText, String subtitleText, boolean actionable) {
        ImageView icon = item.findViewById(R.id.img_about_item_icon);
        TextView title = item.findViewById(R.id.txt_about_item_title);
        TextView subtitle = item.findViewById(R.id.txt_about_item_subtitle);
        ImageView chevron = item.findViewById(R.id.img_about_item_chevron);
        icon.setImageResource(iconRes);
        icon.setColorFilter(getColor(R.color.icon_default));
        icon.setContentDescription(titleText);
        title.setText(titleText);
        if (subtitleText == null || subtitleText.length() == 0) {
            subtitle.setVisibility(View.GONE);
        } else {
            subtitle.setText(subtitleText);
            subtitle.setVisibility(View.VISIBLE);
        }
        chevron.setVisibility(actionable ? View.VISIBLE : View.GONE);
        item.setClickable(actionable);
    }

    private String appVersionName() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName == null ? getString(R.string.about_version_unknown) : info.versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
            return getString(R.string.about_version_unknown);
        }
    }
}
