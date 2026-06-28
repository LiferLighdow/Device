package com.liferlighdow.device;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppDetailActivity {
    private Context context;
    private ScrollView rootLayout;
    private LinearLayout contentArea;

    public AppDetailActivity(Context context) {
        this.context = context;
        rootLayout = new ScrollView(context);
        contentArea = new LinearLayout(context);
        contentArea.setOrientation(LinearLayout.VERTICAL);
        contentArea.setPadding(40, 40, 40, 40);
        rootLayout.addView(contentArea);
    }

    public View getView(ApplicationsActivity.AppEntry app) {
        contentArea.removeAllViews();
        applyTheme();

        // Header with Back Button and Title
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, 40);

        TextView backButton = new TextView(context);
        backButton.setText("←");
        backButton.setTextSize(24);
        backButton.setPadding(30, 20, 30, 20);
        backButton.setClickable(true);
        backButton.setFocusable(true);
        ThemeManager.setSelectableBackground(backButton);
        backButton.setOnClickListener(v -> {
            if (context instanceof MainActivity) {
                ((MainActivity) context).showApps();
            }
        });
        backButton.setTextColor(ThemeManager.getTextColor(context));
        header.addView(backButton);

        TextView title = new TextView(context);
        title.setText("App Details");
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(ThemeManager.getTextColor(context));
        header.addView(title);
        contentArea.addView(header);

        // App Icon and Basic Info
        LinearLayout basicInfo = new LinearLayout(context);
        basicInfo.setOrientation(LinearLayout.HORIZONTAL);
        basicInfo.setGravity(Gravity.CENTER_VERTICAL);
        basicInfo.setPadding(0, 0, 0, 60);

        ImageView icon = new ImageView(context);
        icon.setImageDrawable(app.icon);
        basicInfo.addView(icon, new LinearLayout.LayoutParams(160, 160));

        LinearLayout namePkg = new LinearLayout(context);
        namePkg.setOrientation(LinearLayout.VERTICAL);
        namePkg.setPadding(40, 0, 0, 0);

        TextView nameText = new TextView(context);
        nameText.setText(app.name);
        nameText.setTextSize(20);
        nameText.setTypeface(null, Typeface.BOLD);
        nameText.setTextColor(ThemeManager.getTextColor(context));
        namePkg.addView(nameText);

        TextView pkgText = new TextView(context);
        pkgText.setText(app.packageName);
        pkgText.setTextSize(14);
        pkgText.setTextColor(ThemeManager.getTextColor(context));
        pkgText.setAlpha(0.7f);
        namePkg.addView(pkgText);

        basicInfo.addView(namePkg);
        contentArea.addView(basicInfo);

        // Details
        addDetailItem("Version", app.versionName + " (" + app.versionCode + ")");
        addDetailItem("Target SDK", String.valueOf(app.targetSdk));
        addDetailItem("Min SDK", String.valueOf(app.minSdk));
        addDetailItem("Size", formatSize(app.size));
        addDetailItem("Install Time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(app.installTime)));
        addDetailItem("System App", app.isSystem ? "Yes" : "No");
        addDetailItem("Source Path", app.sourceDir);

        return rootLayout;
    }

    private void addDetailItem(String label, String value) {
        LinearLayout item = new LinearLayout(context);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(0, 0, 0, 30);

        TextView labelText = new TextView(context);
        labelText.setText(label);
        labelText.setTextSize(12);
        labelText.setTextColor(ThemeManager.getTextColor(context));
        labelText.setAlpha(0.6f);
        item.addView(labelText);

        TextView valueText = new TextView(context);
        valueText.setText(value);
        valueText.setTextSize(15);
        valueText.setTextColor(ThemeManager.getTextColor(context));
        valueText.setPadding(0, 5, 0, 0);
        item.addView(valueText);

        contentArea.addView(item);
    }

    private void applyTheme() {
        rootLayout.setBackgroundColor(ThemeManager.getBackgroundColor(context));
    }

    private String formatSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
