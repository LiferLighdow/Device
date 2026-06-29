package com.liferlighdow.device

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.View

object ThemeManager {
    private const val PREFS_NAME = "device_prefs"
    private const val KEY_THEME = "theme_mode" // 0: Dark, 1: Light, 2: System

    fun getThemeMode(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_THEME, 0)
    }

    fun setThemeMode(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt(KEY_THEME, mode).apply()
    }

    fun isLightMode(context: Context): Boolean {
        val mode = getThemeMode(context)
        if (mode == 2) { // System
            val nightModeFlags = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_NO
        }
        return mode == 1
    }

    fun getBackgroundColor(context: Context): Int {
        return if (isLightMode(context)) Color.WHITE else Color.BLACK
    }

    fun getTextColor(context: Context): Int {
        return if (isLightMode(context)) Color.BLACK else Color.WHITE
    }

    fun getSecondaryTextColor(): Int {
        return Color.parseColor("#9CA3AF")
    }

    fun getAccentColor(): Int {
        return Color.parseColor("#B2D7FF")
    }

    fun setSelectableBackground(view: View) {
        val outValue = TypedValue()
        view.context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        view.setBackgroundResource(outValue.resourceId)
    }
}
