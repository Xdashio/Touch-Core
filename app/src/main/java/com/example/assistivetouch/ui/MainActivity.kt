package com.example.assistivetouch.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.assistivetouch.R
import com.example.assistivetouch.service.FloatingButtonService
import com.example.assistivetouch.service.MyAccessibilityService
import com.example.assistivetouch.ui.view.CapsuleSliderView
import com.example.assistivetouch.ui.view.PillSegmentedGroup
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar

    // Master Service Controls
    private lateinit var pillGroupMasterService: PillSegmentedGroup
    private lateinit var textServiceStatusSubtitle: TextView

    // Permission Cards & Indicators
    private lateinit var buttonOverlayPermission: View
    private lateinit var buttonAccessibility: View
    private lateinit var buttonWriteSettings: View
    private lateinit var iconOverlayStatusLarge: ImageView
    private lateinit var iconAccessibilityStatusLarge: ImageView
    private lateinit var iconWriteSettingsStatusLarge: ImageView

    // Dashboard Live Sandbox Sliders
    private lateinit var dashboardVolumeSlider: CapsuleSliderView
    private lateinit var dashboardBrightnessSlider: CapsuleSliderView

    // Navigation Cards
    private lateinit var cardNavSettings: View
    private lateinit var cardNavFavorites: View

    // Sidebar Destinations
    private lateinit var sideNavDashboard: View
    private lateinit var sideNavAppearance: View
    private lateinit var sideNavFavorites: View
    private lateinit var sideNavControls: View
    private lateinit var sideNavRestart: View

    private var isServiceActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupSidebarNavigation()
        setupMasterPillToggle()
        setupPermissionClicks()
        setupHardwareSandbox()
        setupQuickLinks()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        toolbar = findViewById(R.id.toolbar)

        pillGroupMasterService = findViewById(R.id.pillGroupMasterService)
        textServiceStatusSubtitle = findViewById(R.id.textServiceStatusSubtitle)

        buttonOverlayPermission = findViewById(R.id.buttonOverlayPermission)
        buttonAccessibility = findViewById(R.id.buttonAccessibility)
        buttonWriteSettings = findViewById(R.id.buttonWriteSettings)

        iconOverlayStatusLarge = findViewById(R.id.iconOverlayStatusLarge)
        iconAccessibilityStatusLarge = findViewById(R.id.iconAccessibilityStatusLarge)
        iconWriteSettingsStatusLarge = findViewById(R.id.iconWriteSettingsStatusLarge)

        dashboardVolumeSlider = findViewById(R.id.dashboardVolumeSlider)
        dashboardBrightnessSlider = findViewById(R.id.dashboardBrightnessSlider)

        cardNavSettings = findViewById(R.id.cardNavSettings)
        cardNavFavorites = findViewById(R.id.cardNavFavorites)

        sideNavDashboard = findViewById(R.id.sideNavDashboard)
        sideNavAppearance = findViewById(R.id.sideNavAppearance)
        sideNavFavorites = findViewById(R.id.sideNavFavorites)
        sideNavControls = findViewById(R.id.sideNavControls)
        sideNavRestart = findViewById(R.id.sideNavRestart)
    }

    private fun setupSidebarNavigation() {
        // Toolbar hamburger button opens sidebar drawer
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        sideNavDashboard.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        sideNavAppearance.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            drawerLayout.closeDrawer(GravityCompat.START)
            openSettingsActivity()
        }

        sideNavFavorites.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            drawerLayout.closeDrawer(GravityCompat.START)
            openFavoritesActivity()
        }

        sideNavControls.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            drawerLayout.closeDrawer(GravityCompat.START)
            val scrollView = findViewById<androidx.core.widget.NestedScrollView>(R.id.nestedScrollView)
            val sectionControls = findViewById<View>(R.id.sectionControlsTitle)
            scrollView.smoothScrollTo(0, sectionControls.top)
        }

        sideNavRestart.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            drawerLayout.closeDrawer(GravityCompat.START)
            restartFloatingService()
        }
    }

    private fun setupMasterPillToggle() {
        pillGroupMasterService.setSegments(
            listOf(
                PillSegmentedGroup.Segment("STANDBY", "Standby"),
                PillSegmentedGroup.Segment("ACTIVE", "Active")
            ),
            initialIndex = 0
        )

        pillGroupMasterService.onSegmentSelected = { _, id ->
            if (id == "ACTIVE") {
                if (hasOverlayPermission()) {
                    startFloatingService()
                    isServiceActive = true
                    textServiceStatusSubtitle.text = "Floating Overlay Active"
                    textServiceStatusSubtitle.setTextColor(ContextCompat.getColor(this, R.color.white))
                } else {
                    Toast.makeText(
                        this,
                        "Please allow 'Display over other apps' to start overlay",
                        Toast.LENGTH_SHORT
                    ).show()
                    requestOverlayPermission()
                    pillGroupMasterService.selectSegmentById("STANDBY", notify = false)
                    isServiceActive = false
                }
            } else {
                stopFloatingService()
                isServiceActive = false
                textServiceStatusSubtitle.text = "Overlay Standby"
                textServiceStatusSubtitle.setTextColor(android.graphics.Color.parseColor("#8E8E93"))
            }
        }
    }

    private fun setupPermissionClicks() {
        buttonOverlayPermission.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            requestOverlayPermission()
        }

        buttonAccessibility.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            openAccessibilitySettings()
        }

        buttonWriteSettings.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            openWriteSettings()
        }
    }

    private fun setupHardwareSandbox() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = try {
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        } catch (_: Exception) { 15 }
        val currentVol = try {
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        } catch (_: Exception) { 7 }

        dashboardVolumeSlider.apply {
            minValue = 0
            maxValue = maxVol.coerceAtLeast(1)
            stepSize = 1
            showPercentage = false
            customFormat = { "VOL $it / $maxValue" }
            setIconResource(if (currentVol == 0) R.drawable.ic_lucide_volume_x else R.drawable.ic_lucide_volume_2)
            setValue(currentVol)

            onValueChanged = { vol, fromUser ->
                if (fromUser) {
                    try {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0)
                    } catch (_: Exception) {}
                    setIconResource(if (vol == 0) R.drawable.ic_lucide_volume_x else R.drawable.ic_lucide_volume_2)
                }
            }
        }

        val maxBrightness = 255
        val currentBrightness = try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (_: Exception) {
            128
        }

        dashboardBrightnessSlider.apply {
            minValue = 0
            maxValue = maxBrightness
            stepSize = 5
            showPercentage = true
            setIconResource(R.drawable.ic_lucide_sun)
            setValue(currentBrightness)

            onValueChanged = { b, fromUser ->
                if (fromUser) {
                    if (Settings.System.canWrite(this@MainActivity)) {
                        try {
                            Settings.System.putInt(
                                contentResolver,
                                Settings.System.SCREEN_BRIGHTNESS_MODE,
                                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                            )
                            Settings.System.putInt(
                                contentResolver,
                                Settings.System.SCREEN_BRIGHTNESS,
                                b
                            )
                        } catch (_: Exception) {}
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Allow modify system settings to change brightness",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun setupQuickLinks() {
        cardNavSettings.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            openSettingsActivity()
        }

        cardNavFavorites.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            openFavoritesActivity()
        }
    }

    private fun openSettingsActivity() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out)
    }

    private fun openFavoritesActivity() {
        val intent = Intent(this, FavoritesActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out)
    }

    override fun onResume() {
        super.onResume()
        updatePermissionsStatus()
    }

    private fun updatePermissionsStatus() {
        val hasOverlay = hasOverlayPermission()
        val hasAccessibility = MyAccessibilityService.isEnabled(this)
        val hasWriteSettings = Settings.System.canWrite(this)

        val successColor = android.graphics.Color.parseColor("#34C759") // iOS vibrant green
        val dimColor = android.graphics.Color.parseColor("#48484A")

        iconOverlayStatusLarge.setColorFilter(if (hasOverlay) successColor else dimColor)
        iconAccessibilityStatusLarge.setColorFilter(if (hasAccessibility) successColor else dimColor)
        iconWriteSettingsStatusLarge.setColorFilter(if (hasWriteSettings) successColor else dimColor)

        if (hasOverlay) {
            if (!isServiceActive) {
                startFloatingService()
                isServiceActive = true
            }
            pillGroupMasterService.selectSegmentById("ACTIVE", notify = false)
            textServiceStatusSubtitle.text = "Floating Overlay Active"
            textServiceStatusSubtitle.setTextColor(ContextCompat.getColor(this, R.color.white))
        } else {
            pillGroupMasterService.selectSegmentById("STANDBY", notify = false)
            isServiceActive = false
            textServiceStatusSubtitle.text = "Overlay Permission Required"
            textServiceStatusSubtitle.setTextColor(android.graphics.Color.parseColor("#FF453A"))
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Draw over apps permission already granted", Toast.LENGTH_SHORT).show()
                return
            }
            try {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
        }
    }

    private fun openAccessibilitySettings() {
        if (MyAccessibilityService.isEnabled(this)) {
            Toast.makeText(this, "Accessibility service already enabled", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Enable Accessibility Service")
            .setMessage("To perform Home, Back, Recents, and Screen Capture:\n\n" +
                    "1. Tap 'Open Settings'\n" +
                    "2. Select 'TouchCore'\n" +
                    "3. Turn switch ON.\n\n" +
                    "(If restricted on Android 13: App Info > Three Dots > Allow restricted settings)")
            .setPositiveButton("Open Settings") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openWriteSettings() {
        if (Settings.System.canWrite(this)) {
            Toast.makeText(this, "Modify system settings already granted", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        }
    }

    private fun startFloatingService() {
        try {
            val intent = Intent(this, FloatingButtonService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                @Suppress("DEPRECATION")
                startService(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not start overlay: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopFloatingService() {
        try {
            val intent = Intent(this, FloatingButtonService::class.java)
            stopService(intent)
        } catch (_: Exception) {}
    }

    private fun restartFloatingService() {
        stopFloatingService()
        window.decorView.postDelayed({
            if (hasOverlayPermission()) {
                startFloatingService()
                Toast.makeText(this, "Overlay refreshed", Toast.LENGTH_SHORT).show()
            }
        }, 300L)
    }
}
