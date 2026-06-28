package com.liferlighdow.device;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TemperatureActivity {

    private final Context context;
    private final LinearLayout rootLayout;
    private final LinearLayout contentArea;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;

    private static final int UPDATE_INTERVAL = 5000;

    public TemperatureActivity(Context context) {
        this.context = context;

        rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        
        TextView mainTitle = new TextView(context);
        mainTitle.setText("Temperature");
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
        
        applyTheme();
    }

    public void applyTheme() {
        rootLayout.setBackgroundColor(ThemeManager.getBackgroundColor(context));
        ((TextView)rootLayout.getChildAt(0)).setTextColor(ThemeManager.getTextColor(context));
    }

    public View getView() {
        return rootLayout;
    }

    public void startUpdates() {
        if (!isRunning) {
            isRunning = true;
            handler.post(updateTask);
        }
    }

    public void stopUpdates() {
        isRunning = false;
        handler.removeCallbacks(updateTask);
    }

    private final Runnable updateTask = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) return;
            refreshTemperatures();
            handler.postDelayed(this, UPDATE_INTERVAL);
        }
    };

    private void refreshTemperatures() {
        contentArea.removeAllViews();

        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent != null) {
            int temp = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            addTempItem("Battery", temp / 10.0f);
        }

        List<ThermalZone> zones = getThermalZones();
        for (ThermalZone zone : zones) {
            if (zone.temp > -100 && zone.temp < 150) {
                addTempItem(zone.type, zone.temp);
            }
        }

        if (contentArea.getChildCount() <= 1) {
            TextView tv = new TextView(context);
            tv.setText("\nNo other thermal sensors detected or accessible.");
            tv.setTextColor(ThemeManager.getSecondaryTextColor());
            tv.setTextSize(14);
            contentArea.addView(tv);
        }
    }

    private void addTempItem(String label, float celsius) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 20, 0, 20);

        boolean isLikelyThreshold = (celsius == 99.0f || celsius == 100.0f || celsius == 105.0f || 
                                    celsius == 115.0f || celsius == 75.0f || 
                                    label.toLowerCase().contains("limit") || 
                                    label.toLowerCase().contains("crit"));

        TextView tvLabel = new TextView(context);
        tvLabel.setText(isLikelyThreshold ? label + " (Threshold)" : label);
        tvLabel.setTextColor(ThemeManager.getSecondaryTextColor());
        tvLabel.setTextSize(16);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f));

        TextView tvValue = new TextView(context);
        tvValue.setText(formatTemperature(celsius));
        
        if (isLikelyThreshold) {
            tvValue.setTextColor(Color.parseColor("#90A4AE")); // Greyish for limits
        } else if (celsius > 45) {
            tvValue.setTextColor(Color.parseColor("#FF5252")); // Red
        } else if (celsius > 35) {
            tvValue.setTextColor(Color.parseColor("#FFAB40")); // Orange
        } else {
            tvValue.setTextColor(Color.parseColor("#69F0AE")); // Green
        }

        tvValue.setTextSize(18);
        tvValue.setTypeface(null, Typeface.BOLD);
        tvValue.setGravity(Gravity.END);
        tvValue.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(tvLabel);
        row.addView(tvValue);
        contentArea.addView(row);
        
        View line = new View(context);
        line.setBackgroundColor(Color.parseColor("#2E2F3E"));
        contentArea.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
    }

    private String formatTemperature(float celsius) {
        int unit = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE).getInt("temp_unit", 0);
        if (unit == 1) { // Fahrenheit
            float f = (celsius * 9/5) + 32;
            return String.format(Locale.US, "%.1f °F", f);
        } else if (unit == 2) { // Kelvin
            float k = celsius + 273.15f;
            return String.format(Locale.US, "%.2f K", k);
        }
        return String.format(Locale.US, "%.1f °C", celsius);
    }

    private List<ThermalZone> getThermalZones() {
        List<ThermalZone> zones = new ArrayList<>();
        File dir = new File("/sys/class/thermal/");
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().startsWith("thermal_zone")) {
                    String type = readFile(new File(f, "type"));
                    String tempStr = readFile(new File(f, "temp"));
                    if (!type.isEmpty() && !tempStr.isEmpty()) {
                        try {
                            float t = Float.parseFloat(tempStr);
                            if (t > 1000 || t < -1000) t /= 1000;
                            zones.add(new ThermalZone(formatType(type), t));
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        return zones;
    }

    private String formatType(String type) {
        type = type.replace("-thermal", "").replace("_thermal", "");
        type = type.replace("-user", "").replace("-step", "");
        if (type.length() > 1) {
            return type.substring(0, 1).toUpperCase() + type.substring(1);
        }
        return type.toUpperCase();
    }

    private String readFile(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            return br.readLine();
        } catch (Exception e) {
            return "";
        }
    }

    private static class ThermalZone {
        String type;
        float temp;
        ThermalZone(String type, float temp) {
            this.type = type;
            this.temp = temp;
        }
    }
}
