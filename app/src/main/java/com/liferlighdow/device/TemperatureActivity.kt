package com.liferlighdow.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.os.BatteryManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlinx.coroutines.*
import java.io.File
import java.util.Locale

class TemperatureActivity(private val context: Context) {

    private val rootLayout: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }
    private val contentArea: LinearLayout
    private var isRunning = false
    private var updateJob: Job? = null
    private val activityScope = CoroutineScope(Dispatchers.Main + Job())

    init {
        val mainTitle = TextView(context).apply {
            text = "Temperature"
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
        rootLayout.addView(scrollView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        
        applyTheme()
    }

    fun applyTheme() {
        rootLayout.setBackgroundColor(ThemeManager.getBackgroundColor(context))
        (rootLayout.getChildAt(0) as TextView).setTextColor(ThemeManager.getTextColor(context))
    }

    fun getView(): View = rootLayout

    fun startUpdates() {
        if (!isRunning) {
            isRunning = true
            updateJob?.cancel()
            updateJob = activityScope.launch {
                while (isActive && isRunning) {
                    refreshTemperatures()
                    delay(5000)
                }
            }
        }
    }

    fun stopUpdates() {
        isRunning = false
        updateJob?.cancel()
    }

    private suspend fun refreshTemperatures() {
        val thermalData = withContext(Dispatchers.IO) {
            val zones = mutableListOf<ThermalZone>()
            
            // Battery Temp
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val batteryTemp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)?.let { it / 10.0f } ?: -100f
            
            // Thermal Zones
            val dir = File("/sys/class/thermal/")
            dir.listFiles()?.filter { it.name.startsWith("thermal_zone") }?.forEach { f ->
                val type = readFile(File(f, "type"))
                val tempStr = readFile(File(f, "temp"))
                if (type.isNotEmpty() && tempStr.isNotEmpty()) {
                    try {
                        var t = tempStr.toFloat()
                        if (t > 1000 || t < -1000) t /= 1000
                        zones.add(ThermalZone(formatType(type), t))
                    } catch (ignored: Exception) {}
                }
            }
            Pair(batteryTemp, zones)
        }

        contentArea.removeAllViews()
        
        if (thermalData.first > -100f) {
            addTempItem("Battery", thermalData.first)
        }

        thermalData.second.forEach { zone ->
            if (zone.temp > -100 && zone.temp < 150) {
                addTempItem(zone.type, zone.temp)
            }
        }

        if (contentArea.childCount <= 1) {
            val tv = TextView(context).apply {
                text = "\nNo other thermal sensors detected or accessible."
                setTextColor(ThemeManager.getSecondaryTextColor())
                textSize = 14f
            }
            contentArea.addView(tv)
        }
    }

    private fun addTempItem(label: String, celsius: Float) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 20, 0, 20)
        }

        val lowerLabel = label.lowercase()
        val isLikelyThreshold = (celsius == 99.0f || celsius == 100.0f || celsius == 105.0f || 
                                celsius == 115.0f || celsius == 75.0f || 
                                lowerLabel.contains("limit") || lowerLabel.contains("crit"))

        val tvLabel = TextView(context).apply {
            text = if (isLikelyThreshold) "$label (Threshold)" else label
            setTextColor(ThemeManager.getSecondaryTextColor())
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f)
        }

        val tvValue = TextView(context).apply {
            text = formatTemperature(celsius)
            setTextColor(when {
                isLikelyThreshold -> Color.parseColor("#90A4AE")
                celsius > 45 -> Color.parseColor("#FF5252")
                celsius > 35 -> Color.parseColor("#FFAB40")
                else -> Color.parseColor("#69F0AE")
            })
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        row.addView(tvLabel)
        row.addView(tvValue)
        contentArea.addView(row)
        
        val line = View(context).apply {
            setBackgroundColor(Color.parseColor("#2E2F3E"))
        }
        contentArea.addView(line, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2))
    }

    private fun formatTemperature(celsius: Float): String {
        val unit = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE).getInt("temp_unit", 0)
        return when (unit) {
            1 -> String.format(Locale.US, "%.1f °F", (celsius * 9 / 5) + 32)
            2 -> String.format(Locale.US, "%.2f K", celsius + 273.15f)
            else -> String.format(Locale.US, "%.1f °C", celsius)
        }
    }

    private fun formatType(type: String): String {
        val formatted = type.replace("-thermal", "").replace("_thermal", "")
            .replace("-user", "").replace("-step", "")
        return if (formatted.length > 1) {
            formatted.substring(0, 1).uppercase() + formatted.substring(1)
        } else {
            formatted.uppercase()
        }
    }

    private fun readFile(file: File): String {
        return try {
            file.bufferedReader().use { it.readLine() ?: "" }
        } catch (e: Exception) {
            ""
        }
    }

    private data class ThermalZone(val type: String, val temp: Float)
}
