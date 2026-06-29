package com.liferlighdow.device

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import coil.load
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

class ApplicationsActivity(private val context: Context) {

    private val rootLayout: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }
    private val listView: ListView
    private val masterList = mutableListOf<AppEntry>()
    private val displayList = mutableListOf<AppEntry>()
    private val adapter: AppAdapter
    private val progressBar: ProgressBar
    private val header: FrameLayout

    private var showSystemApps = false
    private var sortMethod = 0
    private val activityScope = CoroutineScope(Dispatchers.Main + Job())

    init {
        header = FrameLayout(context).apply {
            setPadding(40, 40, 40, 20)
        }

        val mainTitle = TextView(context).apply {
            text = "Applications"
            textSize = 32f
            setTypeface(null, Typeface.BOLD)
        }
        header.addView(mainTitle)

        val menuButton = TextView(context).apply {
            text = "⋮"
            textSize = 32f
            setPadding(20, 0, 20, 0)
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            }
            isClickable = true
            isFocusable = true
            ThemeManager.setSelectableBackground(this)
            setOnClickListener { showSettingsMenu() }
        }
        header.addView(menuButton)

        rootLayout.addView(header)

        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            visibility = View.GONE
        }
        rootLayout.addView(progressBar)

        listView = ListView(context).apply {
            dividerHeight = 1
        }
        rootLayout.addView(listView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        adapter = AppAdapter()
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            (context as? MainActivity)?.showAppDetail(displayList[position])
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            showAppOptions(displayList[position])
            true
        }

        applyTheme()
        loadApps()
    }

    fun applyTheme() {
        rootLayout.setBackgroundColor(ThemeManager.getBackgroundColor(context))
        (header.getChildAt(0) as TextView).setTextColor(ThemeManager.getTextColor(context))
        (header.getChildAt(1) as TextView).setTextColor(ThemeManager.getTextColor(context))
        listView.divider = ColorDrawable(if (ThemeManager.isLightMode(context)) Color.parseColor("#E0E0E0") else Color.parseColor("#2E2F3E"))
        adapter.notifyDataSetChanged()
    }

    fun getView(): View = rootLayout

    private fun loadApps() {
        progressBar.visibility = View.VISIBLE
        activityScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val packages = pm.getInstalledPackages(0)
                packages.map { pkg ->
                    val appInfo = pkg.applicationInfo!!
                    AppEntry(
                        name = appInfo.loadLabel(pm).toString(),
                        packageName = pkg.packageName,
                        versionName = pkg.versionName ?: "N/A",
                        versionCode = if (Build.VERSION.SDK_INT >= 28) pkg.longVersionCode else pkg.versionCode.toLong(),
                        targetSdk = appInfo.targetSdkVersion,
                        minSdk = if (Build.VERSION.SDK_INT >= 24) appInfo.minSdkVersion else 0,
                        installTime = pkg.firstInstallTime,
                        sourceDir = appInfo.sourceDir,
                        isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                        size = File(appInfo.sourceDir).length(),
                        appInfo = appInfo,
                        icon = appInfo.loadIcon(pm)
                    )
                }
            }
            masterList.clear()
            masterList.addAll(apps)
            applyFiltersAndSort()
        }
    }

    private fun applyFiltersAndSort() {
        displayList.clear()
        masterList.filterTo(displayList) { showSystemApps || !it.isSystem }

        val comparator = when (sortMethod) {
            1 -> compareByDescending<AppEntry> { it.size }
            2 -> compareBy<AppEntry> { it.packageName.lowercase() }
            3 -> compareByDescending<AppEntry> { it.targetSdk }
            4 -> compareByDescending<AppEntry> { it.minSdk }
            5 -> compareByDescending<AppEntry> { it.installTime }
            else -> compareBy<AppEntry> { it.name.lowercase() }
        }
        displayList.sortWith(comparator)

        adapter.notifyDataSetChanged()
        progressBar.visibility = View.GONE
    }

    private fun showSettingsMenu() {
        val options = arrayOf(
            (if (showSystemApps) "☑" else "☐") + " Show System Apps",
            "Sort by Name", "Sort by Size", "Sort by Package", "Sort by Target SDK", "Sort by Min SDK", "Sort by Install Time"
        )
        AlertDialog.Builder(context)
            .setTitle("Filter & Sort")
            .setItems(options) { _, which ->
                if (which == 0) showSystemApps = !showSystemApps
                else sortMethod = which - 1
                applyFiltersAndSort()
            }.show()
    }

    private fun showAppOptions(app: AppEntry) {
        val options = arrayOf("Open App", "App Info", "Share APK", "Extract APK", "Uninstall", "Freeze (Toggle State)")
        AlertDialog.Builder(context)
            .setTitle(app.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openApp(app)
                    1 -> openAppInfo(app)
                    2 -> shareApk(app)
                    3 -> extractApk(app)
                    4 -> uninstallApp(app)
                    5 -> toggleFreeze(app)
                }
            }.show()
    }

    private fun openApp(app: AppEntry) {
        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent != null) context.startActivity(intent)
        else Toast.makeText(context, "Cannot open this app", Toast.LENGTH_SHORT).show()
    }

    private fun shareApk(app: AppEntry) {
        try {
            val src = File(app.sourceDir)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().build())
                }
                putExtra(Intent.EXTRA_STREAM, Uri.fromFile(src))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share APK via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Sharing failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAppInfo(app: AppEntry) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${app.packageName}")
        }
        context.startActivity(intent)
    }

    private fun extractApk(app: AppEntry) {
        activityScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val src = File(app.sourceDir)
                    val destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!destDir.exists()) destDir.mkdirs()
                    val dest = File(destDir, "${app.packageName}_${app.versionName}.apk")
                    src.inputStream().use { input ->
                        dest.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    dest.name
                }
                Toast.makeText(context, "Extracted to: $result", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Extraction failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uninstallApp(app: AppEntry) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:${app.packageName}")
        }
        context.startActivity(intent)
    }

    private fun toggleFreeze(app: AppEntry) {
        Toast.makeText(context, "Freeze requires Root or Device Owner privileges", Toast.LENGTH_SHORT).show()
    }

    private inner class AppAdapter : BaseAdapter() {
        override fun getCount() = displayList.size
        override fun getItem(i: Int) = displayList[i]
        override fun getItemId(i: Int) = i.toLong()
        override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
            val layout: LinearLayout = (view as? LinearLayout) ?: LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(40, 30, 40, 30)
                gravity = Gravity.CENTER_VERTICAL
                
                val icon = ImageView(context)
                addView(icon, LinearLayout.LayoutParams(120, 120))
                
                val info = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(40, 0, 0, 0)
                    
                    val title = TextView(context).apply {
                        textSize = 16f
                        setTypeface(null, Typeface.BOLD)
                    }
                    addView(title)
                    
                    val sub = TextView(context).apply {
                        textSize = 12f
                    }
                    addView(sub)
                }
                addView(info, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            }

            val app = displayList[i]
            val icon = layout.getChildAt(0) as ImageView
            icon.setImageDrawable(app.icon) 

            val info = layout.getChildAt(1) as LinearLayout
            (info.getChildAt(0) as TextView).apply {
                text = app.name
                setTextColor(ThemeManager.getTextColor(context))
            }
            (info.getChildAt(1) as TextView).apply {
                setTextColor(ThemeManager.getSecondaryTextColor())
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                text = String.format(Locale.US, "%s | %s\nTarget: %d | Min: %d | %s", 
                    app.packageName, formatSize(app.size), app.targetSdk, app.minSdk, sdf.format(Date(app.installTime)))
            }
            return layout
        }
    }

    data class AppEntry(
        val name: String,
        val packageName: String,
        val versionName: String,
        val sourceDir: String,
        val versionCode: Long,
        val size: Long,
        val installTime: Long,
        val targetSdk: Int,
        val minSdk: Int,
        val isSystem: Boolean,
        val appInfo: ApplicationInfo,
        val icon: Drawable
    )

    private fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.US, "%.2f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
    }
}
