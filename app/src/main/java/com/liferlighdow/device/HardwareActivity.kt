package com.liferlighdow.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.wifi.WifiManager
import android.os.*
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlinx.coroutines.*
import java.util.*
import kotlin.math.pow

class HardwareActivity(private val context: Context, private val viewModel: HardwareViewModel) {

    private val rootLayout: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }
    private val contentArea: LinearLayout
    private val menuContainer: LinearLayout
    private val menuScroll: HorizontalScrollView
    private var currentCategory = "DEVICE"

    private var isProcessorActive = false
    private val activityScope = CoroutineScope(Dispatchers.Main + Job())
    
    // Store references to CPU core TextViews for smooth updates
    private val coreFreqTextViews = mutableListOf<TextView>()

    init {
        val mainTitle = TextView(context).apply {
            text = "Hardware"
            textSize = 32f
            setTypeface(null, Typeface.BOLD)
            setPadding(40, 40, 40, 20)
        }
        rootLayout.addView(mainTitle)

        menuScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
        }
        
        menuContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 0, 20, 0)
        }
        menuScroll.addView(menuContainer)
        rootLayout.addView(menuScroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val separator = View(context).apply {
            setBackgroundColor(Color.parseColor("#2E2F3E"))
        }
        rootLayout.addView(separator, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2))

        val contentScroll = ScrollView(context).apply {
            isFillViewport = true
        }
        contentArea = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }
        contentScroll.addView(contentArea)
        rootLayout.addView(contentScroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        applyTheme()
        setupMenu()
        showCategory(currentCategory)
        
        // Observe CPU frequencies
        activityScope.launch {
            viewModel.cpuFrequencies.collect { freqs ->
                if (currentCategory.equals("PROCESSOR", ignoreCase = true)) {
                    freqs.forEachIndexed { index, freq ->
                        if (index < coreFreqTextViews.size) {
                            coreFreqTextViews[index].text = freq
                        }
                    }
                }
            }
        }
    }

    fun onPause() {
        isProcessorActive = false
        viewModel.stopFrequencyUpdates()
    }

    fun onResume() {
        if (currentCategory.equals("PROCESSOR", ignoreCase = true)) {
            isProcessorActive = true
            viewModel.startFrequencyUpdates()
        }
    }

    fun applyTheme() {
        rootLayout.setBackgroundColor(ThemeManager.getBackgroundColor(context))
        (rootLayout.getChildAt(0) as TextView).setTextColor(ThemeManager.getTextColor(context))
        menuScroll.setBackgroundColor(if (ThemeManager.isLightMode(context)) Color.parseColor("#F5F5F5") else Color.parseColor("#121212"))
        showCategory(currentCategory)
    }

    fun getView(): View = rootLayout

    private fun setupMenu() {
        menuContainer.removeAllViews()
        val categories = arrayOf("DEVICE", "SYSTEM", "PROCESSOR", "MEMORY", "STORAGE", "DISPLAY", "BATTERY", "CAMERA", "WIRELESS", "CELLULAR", "SENSOR")
        for (cat in categories) {
            val tv = TextView(context).apply {
                text = cat
                setTextColor(ThemeManager.getSecondaryTextColor())
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setPadding(40, 35, 40, 35)
                isAllCaps = true
                isClickable = true
                isFocusable = true
                ThemeManager.setSelectableBackground(this)
                setOnClickListener { showCategory(cat) }
            }
            menuContainer.addView(tv)
        }
    }

    private fun showCategory(category: String) {
        currentCategory = category
        
        isProcessorActive = category.equals("PROCESSOR", ignoreCase = true)
        if (isProcessorActive) {
            viewModel.startFrequencyUpdates()
        } else {
            viewModel.stopFrequencyUpdates()
        }

        for (i in 0 until menuContainer.childCount) {
            val tv = menuContainer.getChildAt(i) as TextView
            if (tv.text.toString().equals(category, ignoreCase = true)) {
                tv.setTextColor(ThemeManager.getAccentColor())
            } else {
                tv.setTextColor(ThemeManager.getSecondaryTextColor())
            }
        }

        contentArea.removeAllViews()

        activityScope.launch {
            when (category.uppercase()) {
                "DEVICE" -> loadDeviceCategory()
                "SYSTEM" -> loadSystemCategory()
                "PROCESSOR" -> loadProcessorCategory()
                "MEMORY" -> loadMemoryCategory()
                "STORAGE" -> loadStorageCategory()
                "DISPLAY" -> loadDisplayCategory()
                "BATTERY" -> loadBatteryCategory()
                "CAMERA" -> loadCameraCategory()
                "WIRELESS" -> loadWirelessCategory()
                "CELLULAR" -> loadCellularCategory()
                "SENSOR" -> loadSensorCategory()
            }
        }
    }

    private fun loadDeviceCategory() {
        addItem("Model", Build.MODEL)
        addItem("Manufacturer", Build.MANUFACTURER)
        addItem("Device", Build.DEVICE)
        addItem("Board", Build.BOARD)
        addItem("Hardware", Build.HARDWARE)
        addItem("Brand", Build.BRAND)
        addItem("Product", Build.PRODUCT)
        addItem("Bootloader", Build.BOOTLOADER)
        addItem("Radio Version", Build.getRadioVersion())
        addItem("Build ID", Build.ID)
        addItem("Display Build", Build.DISPLAY)
    }

    private suspend fun loadSystemCategory() {
        addItem("Android Version", Build.VERSION.RELEASE)
        addItem("API Level", Build.VERSION.SDK_INT.toString())
        
        java.security.Security.getProviders().forEach { p ->
            val name = p.name
            if (name in listOf("AndroidNSSP", "AndroidOpenSSL", "CertPathProvider", "AndroidKeyStoreBCWorkaround", "BC")) {
                addItem(name, "v${p.version}")
            }
        }

        addItem("Kernel", System.getProperty("os.version"))
        addItem("Serial", if (Build.VERSION.SDK_INT < 26) Build.SERIAL else "Protected")
        addItem("Language", Locale.getDefault().displayName)
        addItem("Android ID", android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID))
        addItem("Rooted", if (isRooted()) "Yes" else "No")
        
        val vmName = System.getProperty("java.vm.name")
        addItem("VM (Virtual Machine)", if (Build.VERSION.SDK_INT >= 21) "ART" else if (vmName?.contains("Dalvik") == true) "Dalvik" else "ART")
        addItem("VM Version", System.getProperty("java.vm.version"))
        
        if (Build.VERSION.SDK_INT >= 24) addItem("Encrypted Storage", "Yes")
        if (Build.VERSION.SDK_INT >= 28) {
            addItem("StrongBox", if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)) "Yes" else "No")
        }

        addItem("Build Tags", Build.TAGS)
        addItem("Build Type", Build.TYPE)
        addItem("Build User", Build.USER)
        addItem("Build Host", Build.HOST)
        addItem("TimeZone", TimeZone.getDefault().id)
        addItem("Uptime", formatUptime())
    }

    private suspend fun loadProcessorCategory() {
        val cpuInfo = HardwareProvider.getCpuInfo()
        val procName = cpuInfo["Hardware"] ?: cpuInfo["model name"] ?: Build.HARDWARE
        
        addSubTitle("CPU")
        addItem("Processor", procName)
        addItem("Architecture", System.getProperty("os.arch"))
        addItem("Cores", Runtime.getRuntime().availableProcessors().toString())
        
        coreFreqTextViews.clear()
        for (i in 0 until Runtime.getRuntime().availableProcessors()) {
            val tvValue = addItem("  Core $i", "...")
            coreFreqTextViews.add(tvValue)
        }

        addItem("Instruction Sets", Build.SUPPORTED_ABIS.joinToString(", "))
        val features = cpuInfo["Features"] ?: ""
        addItem("Arm Neon", if (features.contains("neon") || features.contains("asimd")) "Yes" else "No")
        addItem("BogoMIPS", cpuInfo["BogoMIPS"] ?: "N/A")
        addItem("Implementer", cpuInfo["CPU implementer"] ?: "N/A")
        addItem("Part", cpuInfo["CPU part"] ?: "N/A")
        addItem("Revision", cpuInfo["CPU revision"] ?: "N/A")
        
        addSubTitle("GPU")
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        am?.deviceConfigurationInfo?.let { info ->
            addItem("Vendor", HardwareProvider.getSystemProperty("ro.hardware.egl.vendor"))
            addItem("GL Version", info.glEsVersion)
            addItem("Renderer", HardwareProvider.getSystemProperty("ro.hardware.egl"))
            if (Build.VERSION.SDK_INT >= 24) {
                addItem("Vulkan Support", if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL, 0)) "Yes" else "No")
            }
            val ext = HardwareProvider.getSystemProperty("ro.opengles.extensions")
            if (ext.isNotEmpty()) addItem("Extensions", ext)
        }

        val npu = HardwareProvider.getSystemProperty("ro.hardware.npu")
        if (npu.isNotEmpty()) {
            addSubTitle("NPU")
            addItem("Model", npu)
        }
        
        val tpu = HardwareProvider.getSystemProperty("ro.hardware.tpu")
        if (tpu.isNotEmpty() || Build.MANUFACTURER.equals("Google", ignoreCase = true)) {
            addSubTitle("TPU")
            addItem("Model", if (tpu.isNotEmpty()) tpu else "Google Tensor TPU")
        }
    }

    private suspend fun loadMemoryCategory() {
        val mi = ActivityManager.MemoryInfo()
        val amMem = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        amMem?.getMemoryInfo(mi)
        addItem("Total RAM", formatSize(mi.totalMem))
        addItem("Available RAM", formatSize(mi.availMem))
        addItem("Threshold", formatSize(mi.threshold))
        addItem("Low Memory", mi.lowMemory.toString())

        val memInfo = HardwareProvider.getMemInfo()
        addItem("Cached", memInfo["Cached"] ?: "N/A")
        addItem("Active", memInfo["Active"] ?: "N/A")
        addItem("Inactive", memInfo["Inactive"] ?: "N/A")
        addItem("Swap Total", memInfo["SwapTotal"] ?: "N/A")
        addItem("Swap Free", memInfo["SwapFree"] ?: "N/A")
        addItem("Zram", memInfo["Zram"] ?: "N/A")
        addItem("Vmalloc Total", memInfo["VmallocTotal"] ?: "N/A")
        addItem("RAM Type", getRamType())
        addItem("Manufacturer", getRamManufacturer())
        addItem("Clock Speed", getRamFrequency())
    }

    private suspend fun loadStorageCategory() {
        addItem("Data Partition", getPartitionInfo(Environment.getDataDirectory()))
        addItem("System Partition", getPartitionInfo(Environment.getRootDirectory()))
        addSdCardInfo()
        addItem("Download Dir", getPartitionInfo(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)))
        addItem("Storage Type", getStorageType())
        addItem("Disk Name", getStorageHardwareInfo("name"))
        addItem("Manufacturer ID", getStorageHardwareInfo("manfid"))
        addItem("Revision", getStorageHardwareInfo("rev"))
        addItem("Primary FS", getFilesystemType("/data"))
    }

    private fun loadDisplayCategory() {
        val dm = context.resources.displayMetrics
        addItem("Resolution", "${dm.widthPixels} x ${dm.heightPixels}")
        addItem("Screen class", getScreenClass())
        addItem("Density class", getDensityClass(dm.densityDpi))
        addItem("Density DPI", "${dm.densityDpi} dpi")
        addItem("Width", "${dm.widthPixels} px")
        addItem("Height", "${dm.heightPixels} px")
        addItem("Dp Width", (dm.widthPixels / dm.density).toInt().toString() + " dp")
        addItem("Dp Height", (dm.heightPixels / dm.density).toInt().toString() + " dp")
        addItem("Density", dm.density.toString())

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager
        wm?.defaultDisplay?.let { display ->
            val realSize = android.graphics.Point()
            display.getRealSize(realSize)
            addItem("Absolute Width", "${realSize.x} px")
            addItem("Absolute Height", "${realSize.y} px")
            addItem("Refresh rate", String.format(Locale.US, "%.1f Hz", display.refreshRate))
            addItem("Orientation", getOrientationString(display.rotation))

            if (Build.VERSION.SDK_INT >= 24) {
                display.hdrCapabilities?.let { hdr ->
                    addItem("HDR Support", if (hdr.supportedHdrTypes.isNotEmpty()) "Yes" else "No")
                }
            }
            if (Build.VERSION.SDK_INT >= 26) addItem("Wide Color Gamut", if (display.isWideColorGamut) "Yes" else "No")
        }

        addItem("XDPI / YDPI", "${dm.xdpi} / ${dm.ydpi}")
        val xInches = (dm.widthPixels / dm.xdpi).toDouble().pow(2.0)
        val yInches = (dm.heightPixels / dm.ydpi).toDouble().pow(2.0)
        addItem("Physical Size", String.format(Locale.US, "%.2f inches", Math.sqrt(xInches + yInches)))
        addItem("Screen State", getScreenState())
    }

    private fun loadBatteryCategory() {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        batteryIntent?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            addItem("Level", "${level * 100.0f / scale}%")
            addItem("Health", getBatteryHealth(intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)))
            addItem("Voltage", "${intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) / 1000.0f}V")
            addItem("Temperature", formatTemperature(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10.0f))
            addItem("Capacity", "${getBatteryCapacity()}mAh")
            addItem("Technology", intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "N/A")
            addItem("Present", if (intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false)) "Yes" else "No")
            
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            addItem("Battery state", getBatteryStatus(status))
            addItem("Is charging", if (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL) "Yes" else "No")
            addItem("Charging type", getPluggedType(intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)))

            if (Build.VERSION.SDK_INT >= 26) {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                addItem("Cycle count", bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER).toString())
                addItem("Current now", "${bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000.0f}mA")
                addItem("Current average", "${bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE) / 1000.0f}mA")
                addItem("Remaining charge", "${bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) / 1000.0f}mAh")
            }
        }
    }

    private fun loadCameraCategory() {
        try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val ids = manager.cameraIdList
            addItem("Amount", ids.size.toString())
            ids.forEach { id ->
                val characteristics = manager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                addItem("Camera $id", "")
                addItem("  Type", if (facing == CameraCharacteristics.LENS_FACING_FRONT) "Front" else "Back")
                
                characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)?.let { pixelSize ->
                    val mp = (pixelSize.width * pixelSize.height) / 1000000.0f
                    addItem("  Resolution", String.format(Locale.US, "%.1f MP (%dx%d)", mp, pixelSize.width, pixelSize.height))
                }

                addItem("  Orientation", characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)?.toString() ?: "N/A")
                characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.takeIf { it.isNotEmpty() }?.let {
                    addItem("  Focal Length", "${it[0]} mm")
                }
                characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.takeIf { it.isNotEmpty() }?.let {
                    addItem("  Aperture", "f/${it[0]}")
                }
                characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)?.let { iso ->
                    addItem("  ISO Range", "${iso.lower} - ${iso.upper}")
                }
                addItem("  Flash", if (characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) "Yes" else "No")

                if (Build.VERSION.SDK_INT >= 28) {
                    addItem("  Number of lenses", (characteristics.physicalCameraIds.size.takeIf { it > 0 } ?: 1).toString())
                }
            }
        } catch (e: Exception) {
            addItem("Camera", "Access Denied")
        }
    }

    private fun loadWirelessCategory() {
        addItem("Bluetooth", if (hasFeature(PackageManager.FEATURE_BLUETOOTH)) "Yes" else "No")
        addItem("Bluetooth LE", if (hasFeature(PackageManager.FEATURE_BLUETOOTH_LE)) "Yes" else "No")
        addItem("GPS", if (hasFeature(PackageManager.FEATURE_LOCATION_GPS)) "Yes" else "No")
        addItem("NFC", if (hasFeature(PackageManager.FEATURE_NFC)) "Yes" else "No")
        addItem("NFC Card Emulation", if (hasFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) "Yes" else "No")

        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        wm?.let {
            addItem("Wi-Fi", if (it.isWifiEnabled) "Yes" else "No")
            if (Build.VERSION.SDK_INT >= 26) addItem("Wi-Fi Aware", if (hasFeature(PackageManager.FEATURE_WIFI_AWARE)) "Yes" else "No")
            addItem("Wi-Fi Direct", if (hasFeature(PackageManager.FEATURE_WIFI_DIRECT)) "Yes" else "No")
            if (Build.VERSION.SDK_INT >= 21) {
                addItem("Wi-Fi Passpoint", if (hasFeature(PackageManager.FEATURE_WIFI_PASSPOINT)) "Yes" else "No")
                addItem("Wi-Fi 5GHz", if (it.is5GHzBandSupported) "Yes" else "No")
            }
            if (Build.VERSION.SDK_INT >= 30) addItem("Wi-Fi 6GHz (6E)", if (it.is6GHzBandSupported) "Yes" else "No")
            if (Build.VERSION.SDK_INT >= 31) addItem("Wi-Fi 60GHz", if (hasFeature("android.hardware.wifi.60ghz")) "Yes" else "No")
        }
        addItem("IR Emitter", if (hasFeature(PackageManager.FEATURE_CONSUMER_IR)) "Yes" else "No")
    }

    private fun loadCellularCategory() {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        tm?.let {
            addItem("Phone type", getPhoneType(it.phoneType))
            addItem("Operator", it.networkOperatorName)
            addItem("SIM operator", it.simOperatorName)
            addItem("SIM state", getSimState(it.simState))
            addItem("Network country", it.networkCountryIso.uppercase())
            addItem("SIM country", it.simCountryIso.uppercase())
            
            it.networkOperator?.takeIf { op -> op.length >= 3 }?.let { op ->
                addItem("MCC", op.substring(0, 3))
                addItem("MNC", op.substring(3))
            }

            addItem("Data state", getDataState(it.dataState))
            addItem("Network type", getNetworkTypeDisplay(it))
            if (Build.VERSION.SDK_INT >= 23) addItem("SIM count", it.phoneCount.toString())
            if (Build.VERSION.SDK_INT >= 28) addItem("eSIM", if (hasFeature(PackageManager.FEATURE_TELEPHONY_EUICC)) "Yes" else "No")
            addItem("5G", if (is5GSupported(it)) "Yes" else "No")
        }
    }

    private fun loadSensorCategory() {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? android.hardware.SensorManager
        sm?.let {
            val sensors = it.getSensorList(android.hardware.Sensor.TYPE_ALL)
            addItem("Total Sensors", sensors.size.toString())
            sensors.forEach { s -> addItem(s.name, "${s.vendor} (v${s.version})") }
        }
    }

    private fun hasFeature(feature: String) = context.packageManager.hasSystemFeature(feature)
    
    private fun getPhoneType(type: Int) = when (type) {
        TelephonyManager.PHONE_TYPE_GSM -> "GSM"
        TelephonyManager.PHONE_TYPE_CDMA -> "CDMA"
        TelephonyManager.PHONE_TYPE_SIP -> "SIP"
        else -> "None"
    }

    private fun getSimState(state: Int) = when (state) {
        TelephonyManager.SIM_STATE_ABSENT -> "Absent"
        TelephonyManager.SIM_STATE_READY -> "Ready"
        TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN Required"
        TelephonyManager.SIM_STATE_PUK_REQUIRED -> "PUK Required"
        TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "Locked"
        else -> "Unknown"
    }

    private fun getDataState(state: Int) = when (state) {
        TelephonyManager.DATA_CONNECTED -> "Connected"
        TelephonyManager.DATA_CONNECTING -> "Connecting"
        TelephonyManager.DATA_DISCONNECTED -> "Disconnected"
        TelephonyManager.DATA_SUSPENDED -> "Suspended"
        else -> "Unknown"
    }

    private fun getNetworkTypeDisplay(tm: TelephonyManager): String {
        return try {
            val type = tm.networkType
            when (type) {
                TelephonyManager.NETWORK_TYPE_LTE -> "LTE (4G)"
                TelephonyManager.NETWORK_TYPE_NR -> "NR (5G)"
                else -> "Other"
            }
        } catch (e: SecurityException) { "N/A" }
    }

    private fun is5GSupported(tm: TelephonyManager): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {
            return try {
                val bitmaskNR = 1L shl 20
                val method = TelephonyManager::class.java.getMethod("getSupportedRadioAccessFamily")
                val supported = method.invoke(tm) as Long
                (supported and bitmaskNR) != 0L
            } catch (e: Exception) { false }
        }
        return false
    }

    private fun getBatteryHealth(health: Int) = when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
        BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
        else -> "Unknown"
    }

    private fun getBatteryStatus(status: Int) = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        BatteryManager.BATTERY_STATUS_FULL -> "Full"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
        else -> "Unknown"
    }

    private fun getPluggedType(plugged: Int) = when (plugged) {
        BatteryManager.BATTERY_PLUGGED_AC -> "AC"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
        else -> "None"
    }

    private fun getBatteryCapacity(): Double {
        return try {
            val powerProfileClass = "com.android.internal.os.PowerProfile"
            val mPowerProfile = Class.forName(powerProfileClass).getConstructor(Context::class.java).newInstance(context)
            Class.forName(powerProfileClass).getMethod("getBatteryCapacity").invoke(mPowerProfile) as Double
        } catch (e: Exception) { 0.0 }
    }

    private fun addItem(label: String, value: String?): TextView {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 15, 0, 15)
        }
        val tvLabel = TextView(context).apply {
            text = label
            setTextColor(ThemeManager.getSecondaryTextColor())
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvValue = TextView(context).apply {
            text = if (value.isNullOrEmpty()) "N/A" else value
            setTextColor(ThemeManager.getTextColor(context))
            textSize = 15f
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.8f)
        }
        row.addView(tvLabel)
        row.addView(tvValue)
        contentArea.addView(row)
        return tvValue
    }

    private fun addSubTitle(title: String) {
        val tv = TextView(context).apply {
            text = title
            setTextColor(ThemeManager.getAccentColor())
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 30, 0, 10)
        }
        contentArea.addView(tv)
        val line = View(context).apply {
            setBackgroundColor(Color.parseColor("#2E2F3E"))
        }
        contentArea.addView(line, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2))
    }

    private fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.US, "%.2f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
    }

    private fun formatTemperature(celsius: Float): String {
        val unit = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE).getInt("temp_unit", 0)
        return when (unit) {
            1 -> String.format(Locale.US, "%.1f °F", (celsius * 9 / 5) + 32)
            2 -> String.format(Locale.US, "%.2f K", celsius + 273.15f)
            else -> String.format(Locale.US, "%.1f °C", celsius)
        }
    }

    private fun formatUptime(): String {
        val uptime = SystemClock.elapsedRealtime()
        return String.format(Locale.US, "%d hours, %d mins", uptime / 3600000, (uptime % 3600000) / 60000)
    }

    private suspend fun getRamType() = HardwareProvider.getSystemProperty("ro.boot.ddr_type").takeIf { it.isNotEmpty() } ?: "LPDDRx"
    private suspend fun getRamManufacturer() = HardwareProvider.getSystemProperty("ro.boot.ddr_manf").takeIf { it.isNotEmpty() } ?: "Unknown"
    private suspend fun getRamFrequency(): String = "N/A"

    private fun getPartitionInfo(path: java.io.File): String {
        return try {
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            formatSize(stat.availableBlocksLong * blockSize) + " free / " + formatSize(stat.blockCountLong * blockSize)
        } catch (e: Exception) { "N/A" }
    }

    private fun getStorageType() = if (java.io.File("/sys/block/sda/device").exists()) "UFS" else "eMMC"
    
    private suspend fun getStorageHardwareInfo(property: String): String {
        val paths = arrayOf("/sys/block/sda/device/", "/sys/block/mmcblk0/device/", "/sys/class/block/mmcblk0/device/")
        for (path in paths) {
            val valStr = HardwareProvider.readFileLine(path + property)
            if (valStr.isNotEmpty()) return valStr.trim()
        }
        return "N/A"
    }

    private fun getFilesystemType(path: String): String = "N/A"
    private fun getScreenClass() = "Normal"
    private fun getDensityClass(dpi: Int) = "HDPI"
    private fun getOrientationString(rotation: Int) = "Portrait"
    private fun getScreenState() = "On"
    private fun isRooted() = HardwareProvider.isRooted()

    private fun addSdCardInfo() {
        val dirs = context.getExternalFilesDirs(null)
        dirs?.forEach { dir ->
            if (dir != null && Environment.isExternalStorageRemovable(dir)) {
                val path = dir.absolutePath.split("/Android")[0]
                addItem("SD Card", getPartitionInfo(java.io.File(path)))
                addItem("SD Path", path)
                return
            }
        }
    }

    fun destroy() {
        activityScope.cancel()
    }
}
