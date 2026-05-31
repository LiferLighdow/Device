package com.liferlighdow.device;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class SettingsActivity {

    private final Context context;
    private final LinearLayout rootLayout;
    private LinearLayout contentArea;
    private final OnSettingsChangedListener listener;

    public interface OnSettingsChangedListener {
        void onThemeChanged();
        void onUnitChanged();
        void onQuickTestRequested(String type);
    }

    public SettingsActivity(Context context, OnSettingsChangedListener listener) {
        this.context = context;
        this.listener = listener;

        rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        
        refreshUI();
    }

    public void refreshUI() {
        rootLayout.removeAllViews();
        rootLayout.setBackgroundColor(ThemeManager.getBackgroundColor(context));

        TextView mainTitle = new TextView(context);
        mainTitle.setText("Settings");
        mainTitle.setTextColor(ThemeManager.getTextColor(context));
        mainTitle.setTextSize(32);
        mainTitle.setTypeface(null, Typeface.BOLD);
        mainTitle.setPadding(40, 40, 40, 20);
        rootLayout.addView(mainTitle);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        contentArea = new LinearLayout(context);
        contentArea.setOrientation(LinearLayout.VERTICAL);
        contentArea.setPadding(40, 20, 40, 20);
        scrollView.addView(contentArea);

        rootLayout.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setupOptions();
    }

    private void setupOptions() {
        addOption("Theme", getThemeName(), () -> {
            String[] themes = {"Dark", "Light", "System"};
            new AlertDialog.Builder(context, ThemeManager.isLightMode(context) ? AlertDialog.THEME_HOLO_LIGHT : AlertDialog.THEME_HOLO_DARK)
                    .setTitle("Select Theme")
                    .setItems(themes, (dialog, which) -> {
                        ThemeManager.setThemeMode(context, which);
                        refreshUI();
                        listener.onThemeChanged();
                    }).show();
        });

        addOption("Temperature Unit", getUnitName(), () -> {
            String[] units = {"Celsius (°C)", "Fahrenheit (°F)", "Kelvin (K)"};
            new AlertDialog.Builder(context, ThemeManager.isLightMode(context) ? AlertDialog.THEME_HOLO_LIGHT : AlertDialog.THEME_HOLO_DARK)
                    .setTitle("Select Unit")
                    .setItems(units, (dialog, which) -> {
                        setUnitMode(which);
                        refreshUI();
                        listener.onUnitChanged();
                    }).show();
        });

        addOption("Root Status", isRooted() ? "Rooted" : "Not Rooted", () -> {
            if (isRooted()) {
                Toast.makeText(context, "Root access detected. Hardware detection enhanced.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "No Root access. Some advanced data may be restricted.", Toast.LENGTH_SHORT).show();
            }
        });

        addOption("Quick Test", "Run simple hardware diagnostics", () -> {
            String[] tests = {"Vibration Test", "Flashlight Test", "Screen Pixel Test"};
            new AlertDialog.Builder(context, ThemeManager.isLightMode(context) ? AlertDialog.THEME_HOLO_LIGHT : AlertDialog.THEME_HOLO_DARK)
                    .setTitle("Hardware Quick Test")
                    .setItems(tests, (dialog, which) -> {
                        switch (which) {
                            case 0: runVibrationTest(); break;
                            case 1: runFlashlightTest(); break;
                            case 2: listener.onQuickTestRequested("SCREEN"); break;
                        }
                    }).show();
        });
    }

    private void runVibrationTest() {
        android.os.Vibrator v = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(android.os.VibrationEffect.createOneShot(1000, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(1000);
            }
            Toast.makeText(context, "Vibrating...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Vibrator not found", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isFlashOn = false;
    private void runFlashlightTest() {
        try {
            android.hardware.camera2.CameraManager cm = (android.hardware.camera2.CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String cameraId = cm.getCameraIdList()[0];
            isFlashOn = !isFlashOn;
            cm.setTorchMode(cameraId, isFlashOn);
            Toast.makeText(context, "Flash: " + (isFlashOn ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "Flashlight error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addOption(String title, String summary, Runnable action) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, 30, 0, 30);
        layout.setClickable(true);
        layout.setFocusable(true);
        layout.setBackgroundResource(android.R.drawable.list_selector_background);
        layout.setOnClickListener(v -> action.run());

        TextView tvTitle = new TextView(context);
        tvTitle.setText(title);
        tvTitle.setTextColor(ThemeManager.getTextColor(context));
        tvTitle.setTextSize(18);
        layout.addView(tvTitle);

        TextView tvSummary = new TextView(context);
        tvSummary.setText(summary);
        tvSummary.setTextColor(ThemeManager.getSecondaryTextColor());
        tvSummary.setTextSize(14);
        layout.addView(tvSummary);

        contentArea.addView(layout);
        
        View line = new View(context);
        line.setBackgroundColor(Color.parseColor("#2E2F3E"));
        contentArea.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
    }

    private String getThemeName() {
        int mode = ThemeManager.getThemeMode(context);
        if (mode == 1) return "Light";
        if (mode == 2) return "System";
        return "Dark";
    }

    private String getUnitName() {
        int unit = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE).getInt("temp_unit", 0);
        if (unit == 1) return "Fahrenheit (°F)";
        if (unit == 2) return "Kelvin (K)";
        return "Celsius (°C)";
    }

    private void setUnitMode(int unit) {
        context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE).edit().putInt("temp_unit", unit).apply();
    }

    private boolean isRooted() {
        String[] paths = { "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
                "/system/bin/failsafe/su", "/data/local/su" };
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    public View getView() {
        return rootLayout;
    }
}
