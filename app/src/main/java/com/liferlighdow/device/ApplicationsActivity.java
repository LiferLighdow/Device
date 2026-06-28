package com.liferlighdow.device;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ApplicationsActivity {

    private final Context context;
    private final LinearLayout rootLayout;
    private final ListView listView;
    private final List<AppEntry> masterList = new ArrayList<>();
    private final List<AppEntry> displayList = new ArrayList<>();
    private final AppAdapter adapter;
    private final ProgressBar progressBar;
    private final FrameLayout header;

    private boolean showSystemApps = false;
    private int sortMethod = 0;

    public ApplicationsActivity(Context context) {
        this.context = context;

        rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);

        header = new FrameLayout(context);
        header.setPadding(40, 40, 40, 20);

        TextView mainTitle = new TextView(context);
        mainTitle.setText("Applications");
        mainTitle.setTextSize(32);
        mainTitle.setTypeface(null, Typeface.BOLD);
        header.addView(mainTitle);

        TextView menuButton = new TextView(context);
        menuButton.setText("⋮");
        menuButton.setTextSize(32);
        menuButton.setPadding(20, 0, 20, 0);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        menuButton.setLayoutParams(lp);
        menuButton.setClickable(true);
        menuButton.setFocusable(true);
        ThemeManager.setSelectableBackground(menuButton);
        menuButton.setOnClickListener(v -> showSettingsMenu());
        header.addView(menuButton);

        rootLayout.addView(header);

        progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        rootLayout.addView(progressBar);

        listView = new ListView(context);
        listView.setDividerHeight(1);
        rootLayout.addView(listView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        adapter = new AppAdapter();
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (context instanceof MainActivity) {
                ((MainActivity) context).showAppDetail(displayList.get(position));
            }
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showAppOptions(displayList.get(position));
            return true;
        });

        applyTheme();
        loadApps();
    }

    public void applyTheme() {
        rootLayout.setBackgroundColor(ThemeManager.getBackgroundColor(context));
        ((TextView)header.getChildAt(0)).setTextColor(ThemeManager.getTextColor(context));
        ((TextView)header.getChildAt(1)).setTextColor(ThemeManager.getTextColor(context));
        listView.setDivider(new android.graphics.drawable.ColorDrawable(ThemeManager.isLightMode(context) ? Color.parseColor("#E0E0E0") : Color.parseColor("#2E2F3E")));
        adapter.notifyDataSetChanged();
    }

    public View getView() {
        return rootLayout;
    }

    private void loadApps() {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            PackageManager pm = context.getPackageManager();
            List<PackageInfo> packages = pm.getInstalledPackages(0);
            masterList.clear();
            for (PackageInfo pkg : packages) {
                AppEntry entry = new AppEntry();
                entry.name = pkg.applicationInfo.loadLabel(pm).toString();
                entry.packageName = pkg.packageName;
                entry.versionName = pkg.versionName != null ? pkg.versionName : "N/A";
                entry.versionCode = (Build.VERSION.SDK_INT >= 28) ? pkg.getLongVersionCode() : pkg.versionCode;
                entry.targetSdk = pkg.applicationInfo.targetSdkVersion;
                if (Build.VERSION.SDK_INT >= 24) {
                    entry.minSdk = pkg.applicationInfo.minSdkVersion;
                }
                entry.installTime = pkg.firstInstallTime;
                entry.sourceDir = pkg.applicationInfo.sourceDir;
                entry.icon = pkg.applicationInfo.loadIcon(pm);
                entry.isSystem = (pkg.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                
                File file = new File(entry.sourceDir);
                entry.size = file.length();
                
                masterList.add(entry);
            }
            applyFiltersAndSort();
        }).start();
    }

    private void applyFiltersAndSort() {
        displayList.clear();
        for (AppEntry entry : masterList) {
            if (showSystemApps || !entry.isSystem) displayList.add(entry);
        }

        Comparator<AppEntry> comparator;
        switch (sortMethod) {
            case 1: comparator = (a, b) -> Long.compare(b.size, a.size); break;
            case 2: comparator = (a, b) -> a.packageName.compareToIgnoreCase(b.packageName); break;
            case 3: comparator = (a, b) -> Integer.compare(b.targetSdk, a.targetSdk); break;
            case 4: comparator = (a, b) -> Integer.compare(b.minSdk, a.minSdk); break;
            case 5: comparator = (a, b) -> Long.compare(b.installTime, a.installTime); break;
            default: comparator = (a, b) -> a.name.compareToIgnoreCase(b.name); break;
        }
        Collections.sort(displayList, comparator);

        ((android.app.Activity) context).runOnUiThread(() -> {
            adapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE);
        });
    }

    private void showSettingsMenu() {
        String[] options = { (showSystemApps ? "☑" : "☐") + " Show System Apps", "Sort by Name", "Sort by Size", "Sort by Package", "Sort by Target SDK", "Sort by Min SDK", "Sort by Install Time" };
        new AlertDialog.Builder(context)
                .setTitle("Filter & Sort")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showSystemApps = !showSystemApps;
                    else sortMethod = which - 1;
                    applyFiltersAndSort();
                }).show();
    }

    private void showAppOptions(AppEntry app) {
        String[] options = {"Open App", "App Info", "Share APK", "Extract APK", "Uninstall", "Freeze (Toggle State)"};
        new AlertDialog.Builder(context)
                .setTitle(app.name)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: openApp(app); break;
                        case 1: openAppInfo(app); break;
                        case 2: shareApk(app); break;
                        case 3: extractApk(app); break;
                        case 4: uninstallApp(app); break;
                        case 5: toggleFreeze(app); break;
                    }
                }).show();
    }

    private void openApp(AppEntry app) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(app.packageName);
        if (intent != null) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "Cannot open this app", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareApk(AppEntry app) {
        try {
            File src = new File(app.sourceDir);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/vnd.android.package-archive");
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
            }
            
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(src));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, "Share APK via"));
        } catch (Exception e) {
            Toast.makeText(context, "Sharing failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openAppInfo(AppEntry app) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + app.packageName));
        context.startActivity(intent);
    }

    private void extractApk(AppEntry app) {
        new Thread(() -> {
            try {
                File src = new File(app.sourceDir);
                File destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!destDir.exists()) destDir.mkdirs();
                File dest = new File(destDir, app.packageName + "_" + app.versionName + ".apk");
                try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dest)) {
                    byte[] buf = new byte[1024 * 64];
                    int len;
                    while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                }
                ((android.app.Activity) context).runOnUiThread(() -> Toast.makeText(context, "Extracted to: " + dest.getName(), Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                ((android.app.Activity) context).runOnUiThread(() -> Toast.makeText(context, "Extraction failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void uninstallApp(AppEntry app) {
        Intent intent = new Intent(Intent.ACTION_DELETE);
        intent.setData(Uri.parse("package:" + app.packageName));
        context.startActivity(intent);
    }

    private void toggleFreeze(AppEntry app) {
        Toast.makeText(context, "Freeze requires Root or Device Owner privileges", Toast.LENGTH_SHORT).show();
    }

    private class AppAdapter extends BaseAdapter {
        @Override
        public int getCount() { return displayList.size(); }
        @Override
        public Object getItem(int i) { return displayList.get(i); }
        @Override
        public long getItemId(int i) { return i; }
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            LinearLayout layout;
            if (view == null) {
                layout = new LinearLayout(context);
                layout.setOrientation(LinearLayout.HORIZONTAL);
                layout.setPadding(40, 30, 40, 30);
                layout.setGravity(Gravity.CENTER_VERTICAL);
                ImageView icon = new ImageView(context);
                layout.addView(icon, new LinearLayout.LayoutParams(120, 120));
                LinearLayout info = new LinearLayout(context);
                info.setOrientation(LinearLayout.VERTICAL);
                info.setPadding(40, 0, 0, 0);
                TextView title = new TextView(context);
                title.setTextSize(16);
                title.setTypeface(null, Typeface.BOLD);
                info.addView(title);
                TextView sub = new TextView(context);
                sub.setTextSize(12);
                info.addView(sub);
                layout.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            } else { layout = (LinearLayout) view; }

            AppEntry app = displayList.get(i);
            ((ImageView) layout.getChildAt(0)).setImageDrawable(app.icon);
            LinearLayout info = (LinearLayout) layout.getChildAt(1);
            TextView title = (TextView) info.getChildAt(0);
            title.setText(app.name);
            title.setTextColor(ThemeManager.getTextColor(context));
            TextView sub = (TextView) info.getChildAt(1);
            sub.setTextColor(ThemeManager.getSecondaryTextColor());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sub.setText(String.format(Locale.US, "%s | %s\nTarget: %d | Min: %d | %s", app.packageName, formatSize(app.size), app.targetSdk, app.minSdk, sdf.format(new Date(app.installTime))));
            return layout;
        }
    }

    public static class AppEntry {
        public String name, packageName, versionName, sourceDir;
        public long versionCode, size, installTime;
        public int targetSdk, minSdk;
        public boolean isSystem;
        public Drawable icon;
    }

    private String formatSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format(Locale.US, "%.2f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
