package com.liferlighdow.device;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private FrameLayout contentFrame;
    private LinearLayout bottomNav;
    private HardwareActivity hardwareActivity;
    private ApplicationsActivity appsActivity;
    private TemperatureActivity tempActivity;
    private SettingsActivity settingsActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View rootLayout = findViewById(R.id.root_layout);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            rootLayout.setOnApplyWindowInsetsListener((v, insets) -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    v.setPadding(insets.getInsets(android.view.WindowInsets.Type.systemBars()).left,
                            insets.getInsets(android.view.WindowInsets.Type.systemBars()).top,
                            insets.getInsets(android.view.WindowInsets.Type.systemBars()).right,
                            insets.getInsets(android.view.WindowInsets.Type.systemBars()).bottom);
                } else {
                    v.setPadding(insets.getSystemWindowInsetLeft(),
                            insets.getSystemWindowInsetTop(),
                            insets.getSystemWindowInsetRight(),
                            insets.getSystemWindowInsetBottom());
                }
                return insets;
            });
        }

        contentFrame = findViewById(R.id.content_frame);
        bottomNav = findViewById(R.id.bottom_nav);

        hardwareActivity = new HardwareActivity(this);
        appsActivity = new ApplicationsActivity(this);
        tempActivity = new TemperatureActivity(this);
        settingsActivity = new SettingsActivity(this, new SettingsActivity.OnSettingsChangedListener() {
            @Override
            public void onThemeChanged() {
                applyTheme();
            }

            @Override
            public void onUnitChanged() {
                // Temperature values will refresh on next 5s cycle
            }

            @Override
            public void onQuickTestRequested(String type) {
                if ("SCREEN".equals(type)) {
                    runScreenTest();
                }
            }
        });

        findViewById(R.id.nav_hardware).setOnClickListener(v -> showHardware());
        findViewById(R.id.nav_apps).setOnClickListener(v -> showApps());
        findViewById(R.id.nav_temp).setOnClickListener(v -> showTemp());
        findViewById(R.id.nav_settings).setOnClickListener(v -> showSettings());

        applyTheme();
        showHardware();
    }

    private void applyTheme() {
        int bgColor = ThemeManager.getBackgroundColor(this);
        int textColor = ThemeManager.getTextColor(this);
        int navBg = ThemeManager.isLightMode(this) ? Color.parseColor("#F5F5F5") : Color.BLACK;

        contentFrame.setBackgroundColor(bgColor);
        bottomNav.setBackgroundColor(navBg);

        for (int i = 0; i < bottomNav.getChildCount(); i++) {
            View v = bottomNav.getChildAt(i);
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                tv.setTextColor(textColor);
                // Refresh drawables if needed, but they are white which works okay on dark, 
                // on light we might want them black.
                if (ThemeManager.isLightMode(this)) {
                    tv.getCompoundDrawables()[1].setColorFilter(Color.BLACK, android.graphics.PorterDuff.Mode.SRC_IN);
                } else {
                    tv.getCompoundDrawables()[1].clearColorFilter();
                }
            }
        }

        hardwareActivity.applyTheme();
        appsActivity.applyTheme();
        tempActivity.applyTheme();
        settingsActivity.refreshUI();
    }

    private void showHardware() {
        tempActivity.stopUpdates();
        contentFrame.removeAllViews();
        contentFrame.addView(hardwareActivity.getView());
    }

    private void showApps() {
        tempActivity.stopUpdates();
        contentFrame.removeAllViews();
        contentFrame.addView(appsActivity.getView());
    }

    private void showTemp() {
        contentFrame.removeAllViews();
        contentFrame.addView(tempActivity.getView());
        tempActivity.startUpdates();
    }

    private void showSettings() {
        tempActivity.stopUpdates();
        bottomNav.setVisibility(View.VISIBLE);
        contentFrame.removeAllViews();
        contentFrame.addView(settingsActivity.getView());
    }

    private int screenTestStep = 0;
    private void runScreenTest() {
        tempActivity.stopUpdates();
        bottomNav.setVisibility(View.GONE);
        contentFrame.removeAllViews();
        
        View testView = new View(this);
        int[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.WHITE, Color.BLACK};
        testView.setBackgroundColor(colors[0]);
        screenTestStep = 0;

        testView.setOnClickListener(v -> {
            screenTestStep++;
            if (screenTestStep < colors.length) {
                v.setBackgroundColor(colors[screenTestStep]);
            } else {
                bottomNav.setVisibility(View.VISIBLE);
                showSettings(); // Exit test
                android.widget.Toast.makeText(this, "Screen test finished", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        contentFrame.addView(testView);
        
        // Use a simple full screen flag if available
        if (Build.VERSION.SDK_INT >= 19) {
            testView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }

        android.widget.Toast.makeText(this, "Tap to cycle colors", android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        tempActivity.stopUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tempActivity.stopUpdates();
    }
}
