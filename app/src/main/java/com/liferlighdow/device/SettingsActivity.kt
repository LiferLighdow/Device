package com.liferlighdow.device

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.hardware.camera2.CameraManager
import android.os.*
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File

class SettingsActivity(private val context: Context, private val listener: OnSettingsChangedListener) {

    private val rootLayout: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }
    private lateinit var contentArea: LinearLayout

    interface OnSettingsChangedListener {
        fun onThemeChanged()
        fun onUnitChanged()
        fun onQuickTestRequested(type: String)
    }

    init {
        refreshUI()
    }

    fun refreshUI() {
        rootLayout.removeAllViews()
        rootLayout.setBackgroundColor(ThemeManager.getBackgroundColor(context))

        val mainTitle = TextView(context).apply {
            text = "Settings"
            setTextColor(ThemeManager.getTextColor(context))
            textSize = 32f
            setTypeface(null, Typeface.BOLD)
            setPadding(40, 40, 40, 20)
        }
        rootLayout.addView(mainTitle)

        val scrollView = ScrollView(context).apply {
            isFillViewport = true
        }
        contentArea = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }
        scrollView.addView(contentArea)

        rootLayout.addView(scrollView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        setupOptions()
    }

    private fun setupOptions() {
        addOption("Theme", getThemeName()) {
            val themes = arrayOf("Dark", "Light", "System")
            AlertDialog.Builder(context)
                .setTitle("Select Theme")
                .setItems(themes) { _, which ->
                    ThemeManager.setThemeMode(context, which)
                    refreshUI()
                    listener.onThemeChanged()
                }.show()
        }

        addOption("Temperature Unit", getUnitName()) {
            val units = arrayOf("Celsius (°C)", "Fahrenheit (°F)", "Kelvin (K)")
            AlertDialog.Builder(context)
                .setTitle("Select Unit")
                .setItems(units) { _, which ->
                    setUnitMode(which)
                    refreshUI()
                    listener.onUnitChanged()
                }.show()
        }

        addOption("Root Status", if (isRooted()) "Rooted" else "Not Rooted") {
            if (isRooted()) {
                Toast.makeText(context, "Root access detected. Hardware detection enhanced.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "No Root access. Some advanced data may be restricted.", Toast.LENGTH_SHORT).show()
            }
        }

        addOption("Quick Test", "Run simple hardware diagnostics") {
            val tests = arrayOf("Vibration Test", "Flashlight Test", "Screen Pixel Test")
            AlertDialog.Builder(context)
                .setTitle("Hardware Quick Test")
                .setItems(tests) { _, which ->
                    when (which) {
                        0 -> runVibrationTest()
                        1 -> runFlashlightTest()
                        2 -> listener.onQuickTestRequested("SCREEN")
                    }
                }.show()
        }
    }

    private fun runVibrationTest() {
        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (v != null && v.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(1000)
            }
            Toast.makeText(context, "Vibrating...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Vibrator not found", Toast.LENGTH_SHORT).show()
        }
    }

    private var isFlashOn = false
    private fun runFlashlightTest() {
        try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cm.cameraIdList[0]
            isFlashOn = !isFlashOn
            cm.setTorchMode(cameraId, isFlashOn)
            Toast.makeText(context, "Flash: ${if (isFlashOn) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Flashlight error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addOption(title: String, summary: String, action: () -> Unit) {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 30, 0, 30)
            isClickable = true
            isFocusable = true
            ThemeManager.setSelectableBackground(this)
            setOnClickListener { action() }
        }

        val tvTitle = TextView(context).apply {
            text = title
            setTextColor(ThemeManager.getTextColor(context))
            textSize = 18f
            layout.addView(this)
        }

        val tvSummary = TextView(context).apply {
            text = summary
            setTextColor(ThemeManager.getSecondaryTextColor())
            textSize = 14f
            layout.addView(this)
        }

        contentArea.addView(layout)
        
        val line = View(context).apply {
            setBackgroundColor(Color.parseColor("#2E2F3E"))
        }
        contentArea.addView(line, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1))
    }

    private fun getThemeName(): String {
        return when (ThemeManager.getThemeMode(context)) {
            1 -> "Light"
            2 -> "System"
            else -> "Dark"
        }
    }

    private fun getUnitName(): String {
        val unit = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE).getInt("temp_unit", 0)
        return when (unit) {
            1 -> "Fahrenheit (°F)"
            2 -> "Kelvin (K)"
            else -> "Celsius (°C)"
        }
    }

    private fun setUnitMode(unit: Int) {
        context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE).edit().putInt("temp_unit", unit).apply()
    }

    private fun isRooted(): Boolean = HardwareProvider.isRooted()

    fun getView(): View = rootLayout
}
