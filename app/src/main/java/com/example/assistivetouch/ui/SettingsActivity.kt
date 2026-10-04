package com.example.assistivetouch.ui

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.assistivetouch.R
import com.example.assistivetouch.ui.view.CapsuleSliderView
import com.example.assistivetouch.ui.view.PillSegmentedGroup
import com.google.android.material.appbar.MaterialToolbar

class SettingsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var sizeSlider: CapsuleSliderView
    private lateinit var alphaSlider: CapsuleSliderView
    private lateinit var longPressGroup: PillSegmentedGroup
    private lateinit var colorGroup: PillSegmentedGroup
    private lateinit var applyButton: View
    private lateinit var previewButton: View

    private var currentSizeDp = 56
    private var currentAlphaPercent = 100
    private var currentColor = COLOR_BLUE
    private var currentLongPress = ACTION_OPEN_SETTINGS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_out_left, R.anim.fade_in)
        }

        sizeSlider = findViewById(R.id.capsuleSeekSize)
        alphaSlider = findViewById(R.id.capsuleSeekAlpha)
        longPressGroup = findViewById(R.id.pillGroupLongPress)
        colorGroup = findViewById(R.id.pillGroupColor)
        applyButton = findViewById(R.id.buttonApplySettings)
        previewButton = findViewById(R.id.previewButton)

        setupSegmentedGroups()
        loadPrefs()
        setupSliders()
        updatePreview()

        applyButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            savePrefs()
            sendBroadcast(Intent(ACTION_SETTINGS_CHANGED))
            finish()
            overridePendingTransition(R.anim.slide_out_left, R.anim.fade_in)
        }
    }

    private fun setupSegmentedGroups() {
        longPressGroup.setSegments(
            listOf(
                PillSegmentedGroup.Segment(ACTION_OPEN_SETTINGS, "Settings"),
                PillSegmentedGroup.Segment(ACTION_LOCK_SCREEN, "Lock Screen"),
                PillSegmentedGroup.Segment(ACTION_SCREENSHOT, "Capture")
            )
        )
        longPressGroup.onSegmentSelected = { _, id ->
            currentLongPress = id
        }

        colorGroup.setSegments(
            listOf(
                PillSegmentedGroup.Segment(COLOR_BLUE, "Monochrome"),
                PillSegmentedGroup.Segment(COLOR_RED, "Crimson"),
                PillSegmentedGroup.Segment(COLOR_GREEN, "Emerald")
            )
        )
        colorGroup.onSegmentSelected = { _, id ->
            currentColor = id
            updatePreview()
        }
    }

    private fun setupSliders() {
        sizeSlider.apply {
            minValue = 40
            maxValue = 88
            stepSize = 2
            showPercentage = false
            customFormat = { "${it}dp" }
            setIconResource(R.drawable.ic_lucide_sliders)
            setValue(currentSizeDp)

            onValueChanged = { value, _ ->
                currentSizeDp = value
                updatePreview()
            }
        }

        alphaSlider.apply {
            minValue = 30
            maxValue = 100
            stepSize = 5
            showPercentage = true
            setIconResource(R.drawable.ic_lucide_sun)
            setValue(currentAlphaPercent)

            onValueChanged = { value, _ ->
                currentAlphaPercent = value
                updatePreview()
            }
        }
    }

    private fun loadPrefs() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        currentSizeDp = prefs.getInt(KEY_BUTTON_SIZE_DP, 56).coerceIn(40, 88)
        currentAlphaPercent = prefs.getInt(KEY_BUTTON_ALPHA, 100).coerceIn(30, 100)
        currentColor = prefs.getString(KEY_BUTTON_COLOR, COLOR_BLUE) ?: COLOR_BLUE
        currentLongPress = prefs.getString(KEY_LONG_PRESS_ACTION, ACTION_OPEN_SETTINGS) ?: ACTION_OPEN_SETTINGS

        longPressGroup.selectSegmentById(currentLongPress, notify = false)
        colorGroup.selectSegmentById(currentColor, notify = false)
    }

    private fun savePrefs() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_BUTTON_SIZE_DP, currentSizeDp)
            .putInt(KEY_BUTTON_ALPHA, currentAlphaPercent)
            .putString(KEY_BUTTON_COLOR, currentColor)
            .putString(KEY_LONG_PRESS_ACTION, currentLongPress)
            .apply()
    }

    private fun updatePreview() {
        val density = resources.displayMetrics.density
        val sizePx = (currentSizeDp * density).toInt()

        previewButton.layoutParams = previewButton.layoutParams.apply {
            width = sizePx
            height = sizePx
        }
        previewButton.alpha = (currentAlphaPercent.coerceIn(30, 100) / 100f)
        previewButton.requestLayout()
    }

    companion object {
        const val PREFS_NAME = "assistive_touch_prefs"
        const val KEY_BUTTON_SIZE_DP = "button_size_dp"
        const val KEY_BUTTON_ALPHA = "button_alpha"
        const val KEY_BUTTON_COLOR = "button_color"
        const val KEY_PANEL_THEME = "panel_theme"
        const val KEY_LONG_PRESS_ACTION = "long_press_action"

        const val COLOR_BLUE = "blue"
        const val COLOR_RED = "red"
        const val COLOR_GREEN = "green"

        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

        const val ACTION_OPEN_SETTINGS = "open_settings"
        const val ACTION_LOCK_SCREEN = "lock_screen"
        const val ACTION_SCREENSHOT = "screenshot"

        const val ACTION_SETTINGS_CHANGED = "com.example.assistivetouch.ACTION_SETTINGS_CHANGED"
    }
}
