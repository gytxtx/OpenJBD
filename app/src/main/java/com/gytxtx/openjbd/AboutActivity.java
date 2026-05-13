package com.gytxtx.openjbd;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

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
        toolbar.setNavigationIconTint(getColor(R.color.text_primary));
        toolbar.setOnMenuItemClickListener(new MaterialToolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return false;
            }
        });
        toolbar.setNavigationOnClickListener(view -> finish());

        MaterialButton repositoryButton = findViewById(R.id.btn_github_repository);
        repositoryButton.setOnClickListener(view -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(REPOSITORY_URL))));
    }
}
