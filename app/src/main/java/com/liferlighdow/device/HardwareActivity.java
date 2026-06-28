package com.liferlighdow.device;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.ConsumerIrManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

/**
 * Providing Hardware Information UI logic.
 */
public class HardwareActivity {

    private final Context context;
    private final LinearLayout rootLayout;
    private final LinearLayout contentArea;
    private final LinearLayout menuContainer;
    private final HorizontalScrollView menuScroll;
    private String currentCategory = "DEVICE";

    private final Handler handler = new Handler(android.os.Looper.getMainLooper());
    private boolean isProcessorActive = false;

    public HardwareActivity(Context context) {
        this.context = context;

        rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);

        TextView mainTitle = new TextView(context);
        mainTitle.setText("Hardware");
        mainTitle.setTextSize(32);
        mainTitle.setTypeface(null, Typeface.BOLD);
        mainTitle.setPadding(40, 40, 40, 20);
        rootLayout.addView(mainTitle);

        menuScroll = new HorizontalScrollView(context);
        menuScroll.setHorizontalScrollBarEnabled(false);
        
        menuContainer = new LinearLayout(context);
        menuContainer.setOrientation(LinearLayout.HORIZONTAL);
        menuContainer.setPadding(20, 0, 20, 0);
        menuScroll.addView(menuContainer);

        rootLayout.addView(menuScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View separator = new View(context);
        separator.setBackgroundColor(Color.parseColor("#2E2F3E"));
        rootLayout.addView(separator, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));

        ScrollView contentScroll = new ScrollView(context);
        contentScroll.setFillViewport(true);
        contentArea = new LinearLayout(context);
        contentArea.setOrientation(LinearLayout.VERTICAL);
        contentArea.setPadding(40, 40, 40, 40);
        contentScroll.addView(contentArea);
        
        rootLayout.addView(contentScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        applyTheme();
        setupMenu();
        showCategory(currentCategory);
    }

    private final Runnable processorUpdateTask = new Runnable() {
        @Override
        public void run() {
            if (isProcessorActive && currentCategory.equalsIgnoreCase("PROCESSOR")) {
                refreshProcessorDynamicInfo();
                handler.postDelayed(this, 3000);
            }
        }
    };

    private void refreshProcessorDynamicInfo() {
        // We only want to update dynamic items, but for simplicity with our current setup, 
        // we'll just find and update them or clear/re-add. 
        // Given the requirement, let's just re-render the category if it's the active one.
        if (currentCategory.equalsIgnoreCase("PROCESSOR")) {
            showCategory("PROCESSOR");
        }
    }

    public void onPause() {
        isProcessorActive = false;
        handler.removeCallbacks(processorUpdateTask);
    }

    public void onResume() {
        if (currentCategory.equalsIgnoreCase("PROCESSOR")) {
            isProcessorActive = true;
            handler.post(processorUpdateTask);
        }
    }

    public void applyTheme() {
        rootLayout.setBackgroundColor(ThemeManager.getBackgroundColor(context));
        ((TextView)rootLayout.getChildAt(0)).setTextColor(ThemeManager.getTextColor(context));
        menuScroll.setBackgroundColor(ThemeManager.isLightMode(context) ? Color.parseColor("#F5F5F5") : Color.parseColor("#121212"));
        showCategory(currentCategory); // Refresh item colors
    }

    public View getView() {
        return rootLayout;
    }

    private void setupMenu() {
        menuContainer.removeAllViews();
        String[] categories = {"DEVICE", "SYSTEM", "PROCESSOR", "MEMORY", "STORAGE", "DISPLAY", "BATTERY", "CAMERA", "WIRELESS", "CELLULAR", "SENSOR"};
        for (String cat : categories) {
            TextView tv = new TextView(context);
            tv.setText(cat);
            tv.setTextColor(ThemeManager.getSecondaryTextColor());
            tv.setTextSize(14);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setPadding(40, 35, 40, 35);
            tv.setAllCaps(true);
            tv.setClickable(true);
            tv.setFocusable(true);
            ThemeManager.setSelectableBackground(tv);
            tv.setOnClickListener(v -> showCategory(cat));
            menuContainer.addView(tv);
        }
    }

    private void showCategory(String category) {
        currentCategory = category;
        
        // Handle background task for PROCESSOR
        if (category.equalsIgnoreCase("PROCESSOR")) {
            isProcessorActive = true;
            handler.removeCallbacks(processorUpdateTask);
            handler.postDelayed(processorUpdateTask, 3000);
        } else {
            isProcessorActive = false;
            handler.removeCallbacks(processorUpdateTask);
        }

        for (int i = 0; i < menuContainer.getChildCount(); i++) {
            TextView tv = (TextView) menuContainer.getChildAt(i);
            if (tv.getText().toString().equalsIgnoreCase(category)) {
                tv.setTextColor(ThemeManager.getAccentColor());
            } else {
                tv.setTextColor(ThemeManager.getSecondaryTextColor());
            }
        }

        contentArea.removeAllViews();

        switch (category.toUpperCase()) {
            case "DEVICE":
                addItem("Model", Build.MODEL);
                addItem("Manufacturer", Build.MANUFACTURER);
                addItem("Device", Build.DEVICE);
                addItem("Board", Build.BOARD);
                addItem("Hardware", Build.HARDWARE);
                addItem("Brand", Build.BRAND);
                addItem("Product", Build.PRODUCT);
                addItem("Bootloader", Build.BOOTLOADER);
                addItem("Radio Version", Build.getRadioVersion());
                addItem("Build ID", Build.ID);
                addItem("Display Build", Build.DISPLAY);
                break;
            case "SYSTEM":
                addItem("Android Version", Build.VERSION.RELEASE);
                addItem("API Level", String.valueOf(Build.VERSION.SDK_INT));
                
                // Security Providers (From Image)
                java.security.Provider[] providers = java.security.Security.getProviders();
                for (java.security.Provider p : providers) {
                    String name = p.getName();
                    if (name.equals("AndroidNSSP") || name.equals("AndroidOpenSSL") || 
                        name.equals("CertPathProvider") || name.equals("AndroidKeyStoreBCWorkaround") || 
                        name.equals("BC")) {
                        addItem(name, "v" + p.getVersion());
                    }
                }

                addItem("Kernel", System.getProperty("os.version"));
                addItem("Serial", (Build.VERSION.SDK_INT < 26) ? Build.SERIAL : "Protected");
                addItem("Language", java.util.Locale.getDefault().getDisplayName());
                addItem("Android ID", android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID));
                addItem("Rooted", isRooted() ? "Yes" : "No");
                
                // VM Info
                String vmName = System.getProperty("java.vm.name");
                addItem("VM (Virtual Machine)", (vmName != null && vmName.contains("Dalvik")) ? "Dalvik" : "ART");
                addItem("VM Version", System.getProperty("java.vm.version"));
                
                // Encryption & Security
                if (Build.VERSION.SDK_INT >= 24) {
                    addItem("Encrypted Storage", "Yes"); 
                }
                if (Build.VERSION.SDK_INT >= 28) {
                    addItem("StrongBox", context.getPackageManager().hasSystemFeature("android.hardware.strongbox_keystore") ? "Yes" : "No");
                }

                addItem("Build Tags", Build.TAGS);
                addItem("Build Type", Build.TYPE);
                addItem("Build User", Build.USER);
                addItem("Build Host", Build.HOST);
                addItem("VM Version", System.getProperty("java.vm.version"));
                addItem("TimeZone", java.util.TimeZone.getDefault().getID());
                addItem("Uptime", formatUptime());
                break;
            case "PROCESSOR":
                Map<String, String> cpuInfo = getFullCpuInfo();
                String procName = cpuInfo.get("Hardware");
                if (procName == null) procName = cpuInfo.get("model name");
                if (procName == null) procName = Build.HARDWARE;
                
                addSubTitle("CPU");
                addItem("Processor", procName);
                addItem("Architecture", System.getProperty("os.arch"));
                addItem("Cores", String.valueOf(Runtime.getRuntime().availableProcessors()));
                
                // Real-time Frequencies
                for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
                    String freq = readFileLine("/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_cur_freq");
                    if (!freq.isEmpty()) {
                        try {
                            long f = Long.parseLong(freq.trim());
                            addItem("  Core " + i, (f / 1000) + " MHz");
                        } catch (Exception e) {
                            addItem("  Core " + i, freq);
                        }
                    }
                }

                addItem("Instruction Sets", getABIs());
                
                String features = cpuInfo.get("Features");
                if (features == null) features = "";
                addItem("Arm Neon", (features.contains("neon") || features.contains("asimd")) ? "Yes" : "No");

                addItem("BogoMIPS", getValue(cpuInfo, "BogoMIPS"));
                addItem("Implementer", getValue(cpuInfo, "CPU implementer"));
                addItem("Part", getValue(cpuInfo, "CPU part"));
                addItem("Revision", getValue(cpuInfo, "CPU revision"));
                
                addSubTitle("GPU");
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    ConfigurationInfo info = am.getDeviceConfigurationInfo();
                    addItem("Vendor", getSystemProperty("ro.hardware.egl.vendor"));
                    addItem("GL Version", info.getGlEsVersion());
                    addItem("Renderer", getSystemProperty("ro.hardware.egl"));
                    
                    // Vulkan check
                    if (Build.VERSION.SDK_INT >= 24) {
                        addItem("Vulkan Support", context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL, 0) ? "Yes" : "No");
                    }
                    
                    // Extensions placeholder (typically require GL context to list properly, 
                    // using system props for vendor specific list if available)
                    String ext = getSystemProperty("ro.opengles.extensions");
                    if (!ext.isEmpty()) addItem("Extensions", ext);
                }

                String npu = getSystemProperty("ro.hardware.npu");
                if (!npu.isEmpty()) {
                    addSubTitle("NPU");
                    addItem("Model", npu);
                }
                
                String tpu = getSystemProperty("ro.hardware.tpu");
                if (!tpu.isEmpty() || Build.MANUFACTURER.equalsIgnoreCase("Google")) {
                    addSubTitle("TPU");
                    addItem("Model", !tpu.isEmpty() ? tpu : "Google Tensor TPU");
                }
                break;
            case "MEMORY":
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                ActivityManager amMem = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                if (amMem != null) {
                    amMem.getMemoryInfo(mi);
                    addItem("Total RAM", formatSize(mi.totalMem));
                    addItem("Available RAM", formatSize(mi.availMem));
                    addItem("Threshold", formatSize(mi.threshold));
                    addItem("Low Memory", String.valueOf(mi.lowMemory));
                }
                Map<String, String> memInfo = getProcMemInfo();
                addItem("Cached", getValue(memInfo, "Cached"));
                addItem("Active", getValue(memInfo, "Active"));
                addItem("Inactive", getValue(memInfo, "Inactive"));
                addItem("Swap Total", getValue(memInfo, "SwapTotal"));
                addItem("Swap Free", getValue(memInfo, "SwapFree"));
                addItem("Zram", getValue(memInfo, "Zram"));
                addItem("Vmalloc Total", getValue(memInfo, "VmallocTotal"));
                addItem("RAM Type", getRamType());
                addItem("Manufacturer", getRamManufacturer());
                addItem("Clock Speed", getRamFrequency());
                break;
            case "STORAGE":
                addItem("Data Partition", getPartitionInfo(Environment.getDataDirectory()));
                addItem("System Partition", getPartitionInfo(Environment.getRootDirectory()));
                addSdCardInfo();
                addItem("Download Dir", getPartitionInfo(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)));
                addItem("Storage Type", getStorageType());
                addItem("Disk Name", getStorageHardwareInfo("name"));
                addItem("Manufacturer ID", getStorageHardwareInfo("manfid"));
                addItem("Revision", getStorageHardwareInfo("rev"));
                addItem("Primary FS", getFilesystemType("/data"));
                break;
            case "DISPLAY":
                DisplayMetrics dm = context.getResources().getDisplayMetrics();
                addItem("Resolution", dm.widthPixels + " x " + dm.heightPixels);
                
                // From Image
                addItem("Screen class", getScreenClass());
                addItem("Density class", getDensityClass(dm.densityDpi));
                addItem("Density DPI", dm.densityDpi + " dpi");
                addItem("Width", dm.widthPixels + " px");
                addItem("Height", dm.heightPixels + " px");
                addItem("Dp Width", (int)(dm.widthPixels / dm.density) + " dp");
                addItem("Dp Height", (int)(dm.heightPixels / dm.density) + " dp");
                addItem("Density", String.valueOf(dm.density));

                android.view.WindowManager wm = (android.view.WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                if (wm != null) {
                    android.view.Display display = wm.getDefaultDisplay();
                    
                    // Absolute size (includes system bars)
                    android.graphics.Point realSize = new android.graphics.Point();
                    display.getRealSize(realSize);
                    addItem("Absolute Width", realSize.x + " px");
                    addItem("Absolute Height", realSize.y + " px");
                    
                    addItem("Refresh rate", String.format(Locale.US, "%.1f Hz", display.getRefreshRate()));
                    addItem("Orientation", getOrientationString(display.getRotation()));

                    if (Build.VERSION.SDK_INT >= 24) {
                        android.view.Display.HdrCapabilities hdr = display.getHdrCapabilities();
                        if (hdr != null) addItem("HDR Support", hdr.getSupportedHdrTypes().length > 0 ? "Yes" : "No");
                    }
                    if (Build.VERSION.SDK_INT >= 26) addItem("Wide Color Gamut", display.isWideColorGamut() ? "Yes" : "No");
                }

                // Existing extra info
                addItem("XDPI / YDPI", dm.xdpi + " / " + dm.ydpi);
                double x_inches = Math.pow(dm.widthPixels / dm.xdpi, 2);
                double y_inches = Math.pow(dm.heightPixels / dm.ydpi, 2);
                addItem("Physical Size", String.format(Locale.US, "%.2f inches", Math.sqrt(x_inches + y_inches)));
                addItem("Screen State", getScreenState());
                break;
            case "BATTERY":
                Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (batteryIntent != null) {
                    int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    addItem("Level", (level * 100.0f / scale) + "%");
                    
                    int health = batteryIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
                    addItem("Health", getBatteryHealth(health));

                    int volt = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
                    addItem("Voltage", (volt / 1000.0f) + "V");

                    int temp = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                    addItem("Temperature", formatTemperature(temp / 10.0f));

                    addItem("Capacity", getBatteryCapacity() + "mAh");
                    addItem("Technology", batteryIntent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY));
                    addItem("Present", batteryIntent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false) ? "Yes" : "No");
                    
                    int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                    addItem("Battery state", getBatteryStatus(status));
                    addItem("Is charging", (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL) ? "Yes" : "No");
                    
                    int plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                    addItem("Charging type", getPluggedType(plugged));

                    if (Build.VERSION.SDK_INT >= 26) {
                        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
                        addItem("Cycle count", String.valueOf(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER))); // Approx or actual if supported
                        addItem("Current now", (bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000.0f) + "mA");
                        addItem("Current average", (bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE) / 1000.0f) + "mA");
                        addItem("Remaining charge", (bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) / 1000.0f) + "mAh");
                    }
                }
                break;
            case "CAMERA":
                addCameraInfo();
                break;
            case "WIRELESS":
                addWirelessInfo();
                break;
            case "CELLULAR":
                addCellularInfo();
                break;
            case "SENSOR":
                android.hardware.SensorManager sm = (android.hardware.SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
                if (sm != null) {
                    java.util.List<android.hardware.Sensor> sensors = sm.getSensorList(android.hardware.Sensor.TYPE_ALL);
                    addItem("Total Sensors", String.valueOf(sensors.size()));
                    for (android.hardware.Sensor s : sensors) {
                        addItem(s.getName(), s.getVendor() + " (v" + s.getVersion() + ")");
                    }
                }
                break;
        }
    }

    private void addCameraInfo() {
        try {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String[] ids = manager.getCameraIdList();
            addItem("Amount", String.valueOf(ids.length));
            for (String id : ids) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                
                addItem("Camera " + id, "");
                addItem("  Type", (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT ? "Front" : "Back"));
                
                // Restore & Add more details
                android.util.Size pixelSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
                if (pixelSize != null) {
                    float mp = (pixelSize.getWidth() * pixelSize.getHeight()) / 1000000.0f;
                    addItem("  Resolution", String.format(Locale.US, "%.1f MP (%dx%d)", mp, pixelSize.getWidth(), pixelSize.getHeight()));
                }

                Integer orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                addItem("  Orientation", orientation != null ? orientation.toString() : "N/A");

                float[] focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                if (focalLengths != null && focalLengths.length > 0) {
                    addItem("  Focal Length", focalLengths[0] + " mm");
                }

                float[] apertures = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES);
                if (apertures != null && apertures.length > 0) {
                    addItem("  Aperture", "f/" + apertures[0]);
                }

                android.util.Range<Integer> isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
                if (isoRange != null) {
                    addItem("  ISO Range", isoRange.getLower() + " - " + isoRange.getUpper());
                }

                Boolean hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                addItem("  Flash", (hasFlash != null && hasFlash) ? "Yes" : "No");

                if (Build.VERSION.SDK_INT >= 28) {
                    java.util.Set<String> physicalIds = characteristics.getPhysicalCameraIds();
                    addItem("  Number of lenses", String.valueOf(physicalIds != null ? physicalIds.size() : 1));
                }
            }
        } catch (Exception e) {
            addItem("Camera", "Access Denied");
        }
    }

    private void addWirelessInfo() {
        addItem("Bluetooth", hasFeature(PackageManager.FEATURE_BLUETOOTH) ? "Yes" : "No");
        addItem("Bluetooth LE", hasFeature(PackageManager.FEATURE_BLUETOOTH_LE) ? "Yes" : "No");
        addItem("GPS", hasFeature(PackageManager.FEATURE_LOCATION_GPS) ? "Yes" : "No");
        addItem("NFC", hasFeature(PackageManager.FEATURE_NFC) ? "Yes" : "No");
        addItem("NFC Card Emulation", hasFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION) ? "Yes" : "No");

        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            addItem("Wi-Fi", wm.isWifiEnabled() ? "Yes" : "No");
            if (Build.VERSION.SDK_INT >= 26) addItem("Wi-Fi Aware", hasFeature(PackageManager.FEATURE_WIFI_AWARE) ? "Yes" : "No");
            addItem("Wi-Fi Direct", hasFeature(PackageManager.FEATURE_WIFI_DIRECT) ? "Yes" : "No");
            if (Build.VERSION.SDK_INT >= 21) addItem("Wi-Fi Passpoint", hasFeature(PackageManager.FEATURE_WIFI_PASSPOINT) ? "Yes" : "No");
            addItem("Wi-Fi P2P", hasFeature(PackageManager.FEATURE_WIFI_DIRECT) ? "Yes" : "No");
            
            addItem("Wi-Fi 2.4GHz", "Yes");
            if (Build.VERSION.SDK_INT >= 21) {
                addItem("Wi-Fi 5GHz", wm.is5GHzBandSupported() ? "Yes" : "No");
            }
            if (Build.VERSION.SDK_INT >= 30) {
                addItem("Wi-Fi 6GHz (6E)", wm.is6GHzBandSupported() ? "Yes" : "No");
            }
            if (Build.VERSION.SDK_INT >= 31) {
                addItem("Wi-Fi 60GHz", hasFeature("android.hardware.wifi.60ghz") ? "Yes" : "No");
            }
        }
        addItem("IR Emitter", hasFeature(PackageManager.FEATURE_CONSUMER_IR) ? "Yes" : "No");
    }

    private boolean hasFeature(String feature) {
        return context.getPackageManager().hasSystemFeature(feature);
    }

    private void addCellularInfo() {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm != null) {
            addItem("Phone type", getPhoneType(tm.getPhoneType()));
            addItem("Operator", tm.getNetworkOperatorName());
            addItem("SIM operator", tm.getSimOperatorName());
            addItem("SIM state", getSimState(tm.getSimState()));
            addItem("Network country", tm.getNetworkCountryIso().toUpperCase());
            addItem("SIM country", tm.getSimCountryIso().toUpperCase());
            
            String networkOperator = tm.getNetworkOperator();
            if (networkOperator != null && networkOperator.length() >= 3) {
                addItem("MCC", networkOperator.substring(0, 3));
                addItem("MNC", networkOperator.substring(3));
            }

            addItem("Data state", getDataState(tm.getDataState()));
            addItem("Network type", getNetworkTypeDisplay(tm));
            
            if (Build.VERSION.SDK_INT >= 23) {
                addItem("SIM count", String.valueOf(tm.getPhoneCount()));
            }

            if (Build.VERSION.SDK_INT >= 28) {
                addItem("eSIM", hasFeature(PackageManager.FEATURE_TELEPHONY_EUICC) ? "Yes" : "No");
            }
            
            addItem("5G", is5GSupported(tm) ? "Yes" : "No");
        }
    }

    private String getPhoneType(int type) {
        switch (type) {
            case TelephonyManager.PHONE_TYPE_GSM: return "GSM";
            case TelephonyManager.PHONE_TYPE_CDMA: return "CDMA";
            case TelephonyManager.PHONE_TYPE_SIP: return "SIP";
            default: return "None";
        }
    }

    private String getDataState(int state) {
        switch (state) {
            case TelephonyManager.DATA_CONNECTED: return "Connected";
            case TelephonyManager.DATA_CONNECTING: return "Connecting";
            case TelephonyManager.DATA_DISCONNECTED: return "Disconnected";
            case TelephonyManager.DATA_SUSPENDED: return "Suspended";
            default: return "Unknown";
        }
    }

    private String getNetworkTypeDisplay(TelephonyManager tm) {
        try {
            int type = tm.getNetworkType();
            String label = getDataNetworkType(type);
            switch (type) {
                case TelephonyManager.NETWORK_TYPE_LTE: return "LTE (4G)";
                case TelephonyManager.NETWORK_TYPE_NR: return "NR (5G)";
                default: return label;
            }
        } catch (SecurityException e) { return "N/A"; }
    }

    private boolean is5GSupported(TelephonyManager tm) {
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                // Using reflection for bitmask to avoid compilation issues if SDK 33 symbols are weirdly resolved
                long bitmaskNR = 1L << 20; // NETWORK_TYPE_BITMASK_NR
                Method method = TelephonyManager.class.getMethod("getSupportedRadioAccessFamily");
                long supported = (long) method.invoke(tm);
                return (supported & bitmaskNR) != 0;
            } catch (Exception e) { return false; }
        }
        return false;
    }

    private String getBatteryHealth(int health) {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "Good";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "Overheat";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "Dead";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "Over Voltage";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE: return "Unspecified Failure";
            case BatteryManager.BATTERY_HEALTH_COLD: return "Cold";
            default: return "Unknown";
        }
    }

    private String getPluggedType(int plugged) {
        switch (plugged) {
            case BatteryManager.BATTERY_PLUGGED_AC: return "AC";
            case BatteryManager.BATTERY_PLUGGED_USB: return "USB";
            case BatteryManager.BATTERY_PLUGGED_WIRELESS: return "Wireless";
            default: return "None";
        }
    }

    private double getBatteryCapacity() {
        Object mPowerProfile;
        double batteryCapacity = 0;
        final String POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile";
        try {
            mPowerProfile = Class.forName(POWER_PROFILE_CLASS).getConstructor(Context.class).newInstance(context);
            batteryCapacity = (Double) Class.forName(POWER_PROFILE_CLASS).getMethod("getBatteryCapacity").invoke(mPowerProfile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return batteryCapacity;
    }

    private String getSimState(int state) {
        switch (state) {
            case TelephonyManager.SIM_STATE_ABSENT: return "Absent";
            case TelephonyManager.SIM_STATE_READY: return "Ready";
            case TelephonyManager.SIM_STATE_PIN_REQUIRED: return "PIN Required";
            case TelephonyManager.SIM_STATE_PUK_REQUIRED: return "PUK Required";
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED: return "Locked";
            default: return "Unknown";
        }
    }

    private String getDataNetworkType(int type) {
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN: return "2G";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP: return "3G";
            case TelephonyManager.NETWORK_TYPE_LTE: return "4G";
            case TelephonyManager.NETWORK_TYPE_NR: return "5G";
            default: return "Unknown";
        }
    }

    private void addSubTitle(String title) {
        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextColor(ThemeManager.getAccentColor());
        tv.setTextSize(18);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, 30, 0, 10);
        contentArea.addView(tv);

        View line = new View(context);
        line.setBackgroundColor(Color.parseColor("#2E2F3E"));
        contentArea.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
    }

    private String getScreenClass() {
        int size = context.getResources().getConfiguration().screenLayout & android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK;
        switch (size) {
            case android.content.res.Configuration.SCREENLAYOUT_SIZE_SMALL: return "Small";
            case android.content.res.Configuration.SCREENLAYOUT_SIZE_NORMAL: return "Normal";
            case android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE: return "Large";
            case android.content.res.Configuration.SCREENLAYOUT_SIZE_XLARGE: return "XLarge";
            default: return "Unknown";
        }
    }

    private String getDensityClass(int dpi) {
        if (dpi <= DisplayMetrics.DENSITY_LOW) return "LDPI";
        if (dpi <= DisplayMetrics.DENSITY_MEDIUM) return "MDPI";
        if (dpi <= DisplayMetrics.DENSITY_HIGH) return "HDPI";
        if (dpi <= DisplayMetrics.DENSITY_XHIGH) return "XHDPI";
        if (dpi <= DisplayMetrics.DENSITY_XXHIGH) return "XXHDPI";
        if (dpi <= DisplayMetrics.DENSITY_XXXHIGH) return "XXXHDPI";
        return "Unknown";
    }

    private String getOrientationString(int rotation) {
        switch (rotation) {
            case android.view.Surface.ROTATION_0: return "Portrait (0°)";
            case android.view.Surface.ROTATION_90: return "Landscape (90°)";
            case android.view.Surface.ROTATION_180: return "Reverse Portrait (180°)";
            case android.view.Surface.ROTATION_270: return "Reverse Landscape (270°)";
            default: return "Unknown";
        }
    }

    private boolean isRooted() {
        String[] paths = { "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
                "/system/bin/failsafe/su", "/data/local/su" };
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    private String formatTemperature(float celsius) {
        int unit = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE).getInt("temp_unit", 0);
        if (unit == 1) return String.format(Locale.US, "%.1f °F", (celsius * 9/5) + 32);
        if (unit == 2) return String.format(Locale.US, "%.2f K", celsius + 273.15f);
        return String.format(Locale.US, "%.1f °C", celsius);
    }

    private void addItem(String label, String value) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 15, 0, 15);

        TextView tvLabel = new TextView(context);
        tvLabel.setText(label);
        tvLabel.setTextColor(ThemeManager.getSecondaryTextColor());
        tvLabel.setTextSize(15);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvValue = new TextView(context);
        tvValue.setText(value != null && !value.isEmpty() ? value : "N/A");
        tvValue.setTextColor(ThemeManager.getTextColor(context));
        tvValue.setTextSize(15);
        tvValue.setGravity(Gravity.END);
        tvValue.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.8f));

        row.addView(tvLabel);
        row.addView(tvValue);
        contentArea.addView(row);
    }

    private String getScreenState() {
        android.view.WindowManager wm = (android.view.WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            int state = wm.getDefaultDisplay().getState();
            switch (state) {
                case android.view.Display.STATE_OFF: return "Off";
                case android.view.Display.STATE_ON: return "On";
                case android.view.Display.STATE_DOZE: return "Doze";
                case android.view.Display.STATE_DOZE_SUSPEND: return "Doze Suspend";
                case android.view.Display.STATE_VR: return "VR";
                default: return "Unknown";
            }
        }
        return "N/A";
    }

    private Map<String, String> getProcMemInfo() {
        Map<String, String> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length > 1) {
                    map.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException ignored) {}
        return map;
    }

    private String getRamType() {
        String type = getSystemProperty("ro.boot.ddr_type");
        if (type.isEmpty()) type = readFileLine("/sys/class/devfreq/soc:qcom,cpu-bw/ddr_type");
        return type.isEmpty() ? "LPDDRx" : type;
    }

    private String getRamManufacturer() {
        String man = getSystemProperty("ro.boot.ddr_manf");
        if (man.isEmpty()) man = getSystemProperty("ro.vendor.ddr_manf");
        return man.isEmpty() ? "Unknown" : man;
    }

    private String getRamFrequency() {
        String[] paths = { "/sys/class/devfreq/soc:qcom,cpu-bw/cur_freq", "/sys/class/devfreq/dmc/cur_freq", "/sys/kernel/debug/clk/measure_clk", "/sys/class/devfreq/10012000.memory_controller/cur_freq" };
        for (String path : paths) {
            String freq = readFileLine(path);
            if (!freq.isEmpty()) {
                try {
                    long f = Long.parseLong(freq.trim());
                    if (f > 1000000) return (f / 1000000) + " MHz";
                    if (f > 1000) return (f / 1000) + " MHz";
                    return f + " Hz";
                } catch (Exception e) { return freq; }
            }
        }
        return "N/A";
    }

    private void addSdCardInfo() {
        File[] dirs = context.getExternalFilesDirs(null);
        if (dirs != null) {
            for (File dir : dirs) {
                if (dir != null && Environment.isExternalStorageRemovable(dir)) {
                    String path = dir.getAbsolutePath().split("/Android")[0];
                    addItem("SD Card", getPartitionInfo(new File(path)));
                    addItem("SD Path", path);
                    return;
                }
            }
        }
    }

    private String getPartitionInfo(File path) {
        try {
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSizeLong();
            return formatSize(stat.getAvailableBlocksLong() * blockSize) + " free / " + formatSize(stat.getBlockCountLong() * blockSize);
        } catch (Exception e) { return "N/A"; }
    }

    private String getStorageType() {
        if (new File("/sys/block/sda/device").exists()) return "UFS";
        if (new File("/sys/block/mmcblk0/device").exists()) return "eMMC";
        return "Unknown";
    }

    private String getStorageHardwareInfo(String property) {
        String[] paths = { "/sys/block/sda/device/", "/sys/block/mmcblk0/device/", "/sys/class/block/mmcblk0/device/" };
        for (String path : paths) {
            String val = readFileLine(path + property);
            if (!val.isEmpty()) return val.trim();
        }
        return "N/A";
    }

    private String getFilesystemType(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/mounts"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(path)) {
                    String[] parts = line.split("\\s+");
                    if (parts.length > 2) return parts[2];
                }
            }
        } catch (IOException ignored) {}
        return "N/A";
    }

    private String readFileLine(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) { return br.readLine(); } catch (Exception e) { return ""; }
    }

    private String formatUptime() {
        long uptime = SystemClock.elapsedRealtime();
        return String.format(Locale.US, "%d hours, %d mins", uptime / 3600000, (uptime % 3600000) / 60000);
    }

    private Map<String, String> getFullCpuInfo() {
        Map<String, String> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length > 1) map.put(parts[0].trim(), parts[1].trim());
            }
        } catch (IOException ignored) {}
        return map;
    }

    private String getABIs() {
        StringBuilder sb = new StringBuilder();
        for (String abi : Build.SUPPORTED_ABIS) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(abi);
        }
        return sb.toString();
    }

    private String getBatteryStatus(int status) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING: return "Charging";
            case BatteryManager.BATTERY_STATUS_DISCHARGING: return "Discharging";
            case BatteryManager.BATTERY_STATUS_FULL: return "Full";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING: return "Not Charging";
            default: return "Unknown";
        }
    }

    private String getSystemProperty(String key) {
        try {
            Process p = Runtime.getRuntime().exec("getprop " + key);
            BufferedReader br = new BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
            String line = br.readLine();
            return (line != null) ? line : "";
        } catch (Exception e) { return ""; }
    }

    private String getValue(Map<String, String> map, String key) {
        String val = map.get(key);
        return val != null ? val : "N/A";
    }

    private String formatSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format(Locale.US, "%.2f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
