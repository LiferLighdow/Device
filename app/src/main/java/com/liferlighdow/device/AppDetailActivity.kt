package com.liferlighdow.device

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import coil.load
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow

class AppDetailActivity(private val context: Context) {
    private val rootLayout: ScrollView = ScrollView(context)
    private val contentArea: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(40, 40, 40, 40)
    }

    init {
        rootLayout.addView(contentArea)
    }

    fun getView(app: ApplicationsActivity.AppEntry): View {
        contentArea.removeAllViews()
        applyTheme()

        // Header
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 40)
        }

        val backButton = TextView(context).apply {
            text = "←"
            textSize = 24f
            setPadding(30, 20, 30, 20)
            isClickable = true
            isFocusable = true
            ThemeManager.setSelectableBackground(this)
            setOnClickListener {
                (context as? MainActivity)?.showApps()
            }
            setTextColor(ThemeManager.getTextColor(context))
        }
        header.addView(backButton)

        val title = TextView(context).apply {
            text = "App Details"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ThemeManager.getTextColor(context))
        }
        header.addView(title)
        contentArea.addView(header)

        // App Icon and Basic Info
        val basicInfo = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 60)
        }

        val icon = ImageView(context)
        icon.setImageDrawable(app.icon)
        basicInfo.addView(icon, LinearLayout.LayoutParams(160, 160))

        val namePkg = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 0, 0, 0)
        }

        val nameText = TextView(context).apply {
            text = app.name
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ThemeManager.getTextColor(context))
        }
        namePkg.addView(nameText)

        val pkgText = TextView(context).apply {
            text = app.packageName
            textSize = 14f
            setTextColor(ThemeManager.getTextColor(context))
            alpha = 0.7f
        }
        namePkg.addView(pkgText)

        basicInfo.addView(namePkg)
        contentArea.addView(basicInfo)

        // Details
        addDetailItem("Version", "${app.versionName} (${app.versionCode})")
        addDetailItem("Target SDK", app.targetSdk.toString())
        addDetailItem("Min SDK", app.minSdk.toString())
        addDetailItem("Size", formatSize(app.size))
        addDetailItem("Install Time", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(app.installTime)))
        addDetailItem("System App", if (app.isSystem) "Yes" else "No")
        addDetailItem("Source Path", app.sourceDir)

        return rootLayout
    }

    private fun addDetailItem(label: String, value: String) {
        val item = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 30)
        }

        val labelText = TextView(context).apply {
            text = label
            textSize = 12f
            setTextColor(ThemeManager.getTextColor(context))
            alpha = 0.6f
        }
        item.addView(labelText)

        val valueText = TextView(context).apply {
            text = value
            textSize = 15f
            setTextColor(ThemeManager.getTextColor(context))
            setPadding(0, 5, 0, 0)
        }
        item.addView(valueText)

        contentArea.addView(item)
    }

    private fun applyTheme() {
        rootLayout.setBackgroundColor(ThemeManager.getBackgroundColor(context))
    }

    private fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.US, "%.2f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
    }
}
