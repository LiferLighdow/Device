package com.liferlighdow.device

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import com.liferlighdow.device.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val hardwareViewModel: HardwareViewModel by viewModels()
    
    private lateinit var hardwareActivity: HardwareActivity
    private lateinit var appsActivity: ApplicationsActivity
    private lateinit var appDetailActivity: AppDetailActivity
    private lateinit var tempActivity: TemperatureActivity
    private lateinit var settingsActivity: SettingsActivity
    
    private var isDetailView = false
    private var lastBackPressTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle Window Insets for edge-to-edge
        binding.root.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val systemBars = insets.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            } else {
                @Suppress("DEPRECATION")
                v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            insets
        }

        hardwareActivity = HardwareActivity(this, hardwareViewModel)
        appsActivity = ApplicationsActivity(this)
        appDetailActivity = AppDetailActivity(this)
        tempActivity = TemperatureActivity(this)
        settingsActivity = SettingsActivity(this, object : SettingsActivity.OnSettingsChangedListener {
            override fun onThemeChanged() {
                applyTheme()
            }

            override fun onUnitChanged() {
                // Refresh logic if needed
            }

            override fun onQuickTestRequested(type: String) {
                if (type == "SCREEN") runScreenTest()
            }
        })

        binding.navHardware.setOnClickListener { showHardware() }
        binding.navApps.setOnClickListener { showApps() }
        binding.navTemp.setOnClickListener { showTemp() }
        binding.navSettings.setOnClickListener { showSettings() }

        applyTheme()
        showHardware()
    }

    private fun applyTheme() {
        val bgColor = ThemeManager.getBackgroundColor(this)
        val textColor = ThemeManager.getTextColor(this)
        val navBg = if (ThemeManager.isLightMode(this)) Color.parseColor("#F5F5F5") else Color.BLACK

        binding.contentFrame.setBackgroundColor(bgColor)
        binding.bottomNav.setBackgroundColor(navBg)

        for (i in 0 until binding.bottomNav.childCount) {
            val v = binding.bottomNav.getChildAt(i)
            if (v is android.widget.TextView) {
                v.setTextColor(textColor)
                if (ThemeManager.isLightMode(this)) {
                    v.compoundDrawables[1]?.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
                } else {
                    v.compoundDrawables[1]?.clearColorFilter()
                }
            }
        }

        hardwareActivity.applyTheme()
        appsActivity.applyTheme()
        tempActivity.applyTheme()
        settingsActivity.refreshUI()
    }

    fun showHardware() {
        isDetailView = false
        tempActivity.stopUpdates()
        binding.contentFrame.removeAllViews()
        binding.contentFrame.addView(hardwareActivity.getView())
    }

    fun showApps() {
        isDetailView = false
        tempActivity.stopUpdates()
        binding.contentFrame.removeAllViews()
        binding.contentFrame.addView(appsActivity.getView())
    }

    fun showAppDetail(app: ApplicationsActivity.AppEntry) {
        isDetailView = true
        tempActivity.stopUpdates()
        binding.contentFrame.removeAllViews()
        binding.contentFrame.addView(appDetailActivity.getView(app))
    }

    fun showTemp() {
        isDetailView = false
        binding.contentFrame.removeAllViews()
        binding.contentFrame.addView(tempActivity.getView())
        tempActivity.startUpdates()
    }

    fun showSettings() {
        isDetailView = false
        tempActivity.stopUpdates()
        binding.bottomNav.visibility = View.VISIBLE
        binding.contentFrame.removeAllViews()
        binding.contentFrame.addView(settingsActivity.getView())
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        @Suppress("DEPRECATION")
        if (isDetailView) {
            showApps()
        } else {
            if (lastBackPressTime + 2000 > System.currentTimeMillis()) {
                super.onBackPressed()
            } else {
                Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()
                lastBackPressTime = System.currentTimeMillis()
            }
        }
    }

    private var screenTestStep = 0
    private fun runScreenTest() {
        tempActivity.stopUpdates()
        binding.bottomNav.visibility = View.GONE
        binding.contentFrame.removeAllViews()
        
        val testView = View(this)
        val colors = intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.WHITE, Color.BLACK)
        testView.setBackgroundColor(colors[0])
        screenTestStep = 0

        testView.setOnClickListener { v ->
            screenTestStep++
            if (screenTestStep < colors.size) {
                v.setBackgroundColor(colors[screenTestStep])
            } else {
                binding.bottomNav.visibility = View.VISIBLE
                showSettings()
                Toast.makeText(this, "Screen test finished", Toast.LENGTH_SHORT).show()
            }
        }

        binding.contentFrame.addView(testView)
        
        @Suppress("DEPRECATION")
        testView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)

        Toast.makeText(this, "Tap to cycle colors", Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        tempActivity.stopUpdates()
        hardwareActivity.onPause()
    }

    override fun onResume() {
        super.onResume()
        hardwareActivity.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        tempActivity.stopUpdates()
        hardwareActivity.destroy()
    }
}
