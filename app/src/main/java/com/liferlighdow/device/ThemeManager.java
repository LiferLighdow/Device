package com.liferlighdow.device;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public class ThemeManager {
    private static final String PREFS_NAME = "device_prefs";
    private static final String KEY_THEME = "theme_mode"; // 0: Dark, 1: Light, 2: System

    public static int getThemeMode(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_THEME, 0);
    }

    public static void setThemeMode(Context context, int mode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt(KEY_THEME, mode).apply();
    }

    public static boolean isLightMode(Context context) {
        int mode = getThemeMode(context);
        if (mode == 2) { // System
            int nightModeFlags = context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_NO;
        }
        return mode == 1;
    }

    public static int getBackgroundColor(Context context) {
        return isLightMode(context) ? Color.WHITE : Color.BLACK;
    }

    public static int getTextColor(Context context) {
        return isLightMode(context) ? Color.BLACK : Color.WHITE;
    }

    public static int getSecondaryTextColor() {
        return Color.parseColor("#9CA3AF");
    }

    public static int getAccentColor() {
        return Color.parseColor("#B2D7FF");
    }
}
