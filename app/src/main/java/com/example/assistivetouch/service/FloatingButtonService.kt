package com.example.assistivetouch.service

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.media.AudioManager
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.assistivetouch.R
import com.example.assistivetouch.action.ActionDispatcher
import com.example.assistivetouch.model.AssistiveAction
import com.example.assistivetouch.model.AssistiveItem
import com.example.assistivetouch.repository.MenuRepository
import com.example.assistivetouch.ui.MainActivity
import com.example.assistivetouch.ui.SettingsActivity
import com.example.assistivetouch.ui.view.AssistiveRadialMenuView
import com.example.assistivetouch.ui.view.CapsuleSliderView
import kotlin.math.abs

/**
 * Foreground service hosting the Apple AssistiveTouch floating button and radial menu overlay.
 * Rebuilt from scratch with robust window lifecycle management, smooth Apple physics,
 * idle auto-dimming, and dynamic data-driven menus.
 */
class FloatingButtonService : Service() {

    enum class PanelState {
        HIDDEN,
        OPENING,
        VISIBLE,
        CLOSING
    }

    private lateinit var windowManager: WindowManager
    private lateinit var actionDispatcher: ActionDispatcher

    private var floatingView: View? = null
    private var floatingLayoutParams: WindowManager.LayoutParams? = null

    private var panelView: View? = null
    private var panelLayoutParams: WindowManager.LayoutParams? = null
    private var panelState: PanelState = PanelState.HIDDEN
    private var currentMenuPageId: String = MenuRepository.PAGE_MAIN

    private val mainHandler = Handler(Looper.getMainLooper())

    // Touch & Drag state
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialParamX = 0
    private var initialParamY = 0
    private var downTime = 0L
    private var isDragging = false
    private var isLongPressed = false
    private var touchSlop = 0

    // Auto-dim idle timer (Apple AssistiveTouch behavior)
    private val idleDimRunnable = Runnable {
        dimFloatingButtonToIdle()
    }

    private val longPressRunnable = Runnable {
        isLongPressed = true
        handleLongPress()
    }

    private val prefs by lazy {
        getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val settingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SettingsActivity.ACTION_SETTINGS_CHANGED) {
                applyButtonAppearance()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        actionDispatcher = ActionDispatcher(this)
        touchSlop = ViewConfiguration.get(this).scaledTouchSlop

        createNotificationChannel()
        startServiceInForeground()

        addFloatingButton()

        val filter = IntentFilter(SettingsActivity.ACTION_SETTINGS_CHANGED)
        try {
            androidx.core.content.ContextCompat.registerReceiver(
                this,
                settingsReceiver,
                filter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (_: Exception) {
            try {
                registerReceiver(settingsReceiver, filter)
            } catch (_: Exception) {}
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startServiceInForeground()
        if (floatingView == null) {
            addFloatingButton()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)

        panelState = PanelState.CLOSING
        safeRemoveView(panelView)
        panelView = null
        panelLayoutParams = null
        panelState = PanelState.HIDDEN

        safeRemoveView(floatingView)
        floatingView = null
        floatingLayoutParams = null

        try {
            unregisterReceiver(settingsReceiver)
        } catch (_: Exception) {}
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Screen orientation changed; re-clamp floating button within safe bounds
        floatingView?.let { view ->
            floatingLayoutParams?.let { params ->
                val displayMetrics = resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels
                val btnW = view.width.coerceAtLeast(1)
                val btnH = view.height.coerceAtLeast(1)

                val snapToLeft = params.x < screenWidth / 2
                params.x = if (snapToLeft) 16 else (screenWidth - btnW - 16)
                params.y = params.y.coerceIn(40, (screenHeight - btnH - 60).coerceAtLeast(40))

                try {
                    windowManager.updateViewLayout(view, params)
                } catch (_: Exception) {}
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startServiceInForeground() {
        try {
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } catch (_: Exception) {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {}
    }

    // =========================================================================
    // Floating Button Management
    // =========================================================================

    private fun addFloatingButton() {
        if (floatingView != null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return

        val themedContext = androidx.appcompat.view.ContextThemeWrapper(this, R.style.Theme_AssistiveTouch)
        val inflater = LayoutInflater.from(themedContext)
        val view = try {
            inflater.inflate(R.layout.view_floating_button, null)
        } catch (_: Exception) {
            return
        }

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val savedX = prefs.getInt(PREF_KEY_X, Int.MIN_VALUE)
            val savedY = prefs.getInt(PREF_KEY_Y, Int.MIN_VALUE)
            if (savedX != Int.MIN_VALUE && savedY != Int.MIN_VALUE) {
                x = savedX
                y = savedY
            } else {
                x = screenWidth - (64 * displayMetrics.density).toInt() - 16
                y = screenHeight / 2
            }
        }

        setupFloatingTouchListener(view, params)

        floatingLayoutParams = params
        try {
            windowManager.addView(view, params)
            floatingView = view
            applyButtonAppearance()
            resetIdleDimTimer()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to display AssistiveTouch: ${e.message}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun setupFloatingTouchListener(view: View, params: WindowManager.LayoutParams) {
        val core = view.findViewById<View>(R.id.floatingButtonCore)

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Brighten immediately on touch
                    cancelIdleDimTimer()
                    restoreActiveAlpha()

                    downTime = SystemClock.elapsedRealtime()
                    initialParamX = params.x
                    initialParamY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    isLongPressed = false

                    mainHandler.removeCallbacks(longPressRunnable)
                    mainHandler.postDelayed(longPressRunnable, LONG_PRESS_THRESHOLD)

                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    core?.animate()?.scaleX(0.92f)?.scaleY(0.92f)?.setDuration(90)?.start()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()

                    if (!isDragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                        isDragging = true
                        mainHandler.removeCallbacks(longPressRunnable)
                    }

                    if (isDragging) {
                        params.x = initialParamX + dx
                        params.y = initialParamY + dy
                        try {
                            windowManager.updateViewLayout(view, params)
                        } catch (_: Exception) {}
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    mainHandler.removeCallbacks(longPressRunnable)
                    core?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(120)?.start()

                    val duration = SystemClock.elapsedRealtime() - downTime
                    val totalDx = abs(event.rawX - initialTouchX)
                    val totalDy = abs(event.rawY - initialTouchY)
                    val isClick = !isDragging && duration < CLICK_THRESHOLD && totalDx < touchSlop && totalDy < touchSlop

                    if (isLongPressed) {
                        resetIdleDimTimer()
                        true
                    } else if (isClick) {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        togglePanel()
                        resetIdleDimTimer()
                        true
                    } else {
                        snapFloatingButtonToEdge(params)
                        resetIdleDimTimer()
                        true
                    }
                }
                else -> false
            }
        }
    }

    private fun snapFloatingButtonToEdge(params: WindowManager.LayoutParams) {
        val view = floatingView ?: return
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val btnWidth = view.width.coerceAtLeast(1)
        val btnHeight = view.height.coerceAtLeast(1)
        val margin = (12 * displayMetrics.density).toInt()

        val centerX = params.x + btnWidth / 2
        val targetX = if (centerX < screenWidth / 2) margin else (screenWidth - btnWidth - margin)
        val minY = (40 * displayMetrics.density).toInt()
        val maxY = screenHeight - btnHeight - (60 * displayMetrics.density).toInt()
        val targetY = params.y.coerceIn(minY, maxY)

        val startX = params.x
        val startY = params.y

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 240
            interpolator = DecelerateInterpolator()
            addUpdateListener { va ->
                val fraction = va.animatedFraction
                params.x = (startX + (targetX - startX) * fraction).toInt()
                params.y = (startY + (targetY - startY) * fraction).toInt()
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (_: Exception) {}
            }
        }
        animator.start()

        prefs.edit()
            .putInt(PREF_KEY_X, targetX)
            .putInt(PREF_KEY_Y, targetY)
            .apply()
    }

    private fun cancelIdleDimTimer() {
        mainHandler.removeCallbacks(idleDimRunnable)
    }

    private fun resetIdleDimTimer() {
        mainHandler.removeCallbacks(idleDimRunnable)
        mainHandler.postDelayed(idleDimRunnable, IDLE_DIM_DELAY)
    }

    private fun restoreActiveAlpha() {
        val alphaPercent = prefs.getInt(SettingsActivity.KEY_BUTTON_ALPHA, 100)
        val targetAlpha = (alphaPercent.coerceIn(40, 100) / 100f)
        floatingView?.animate()?.alpha(targetAlpha)?.setDuration(120)?.start()
    }

    private fun dimFloatingButtonToIdle() {
        // Dims to 40% of its base opacity when idle (authentic Apple AssistiveTouch behavior)
        val alphaPercent = prefs.getInt(SettingsActivity.KEY_BUTTON_ALPHA, 100)
        val baseAlpha = (alphaPercent.coerceIn(40, 100) / 100f)
        val idleAlpha = (baseAlpha * 0.40f).coerceAtLeast(0.20f)
        floatingView?.animate()?.alpha(idleAlpha)?.setDuration(400)?.start()
    }

    private fun applyButtonAppearance() {
        val core = floatingView?.findViewById<View>(R.id.floatingButtonCore) ?: return
        val sizeDp = prefs.getInt(SettingsActivity.KEY_BUTTON_SIZE_DP, 56).coerceIn(40, 80)
        val density = resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt()

        core.layoutParams = core.layoutParams.apply {
            width = sizePx
            height = sizePx
        }
        restoreActiveAlpha()
        floatingView?.requestLayout()
    }

    private fun handleLongPress() {
        val actionKey = prefs.getString(
            SettingsActivity.KEY_LONG_PRESS_ACTION,
            SettingsActivity.ACTION_OPEN_SETTINGS
        )
        when (actionKey) {
            SettingsActivity.ACTION_LOCK_SCREEN -> {
                actionDispatcher.execute(AssistiveAction.LockScreen, {})
            }
            SettingsActivity.ACTION_SCREENSHOT -> {
                actionDispatcher.execute(AssistiveAction.Screenshot, {})
            }
            else -> {
                actionDispatcher.execute(AssistiveAction.OpenSettings, {})
            }
        }
    }

    // =========================================================================
    // Radial AssistiveTouch Menu Panel Management
    // =========================================================================

    private fun togglePanel() {
        when (panelState) {
            PanelState.HIDDEN -> openPanel()
            PanelState.VISIBLE -> closePanel()
            PanelState.OPENING, PanelState.CLOSING -> {
                // Ignore rapid spam during state transition
            }
        }
    }

    private fun openPanel() {
        if (panelState != PanelState.HIDDEN || floatingView == null) return
        panelState = PanelState.OPENING

        val themedContext = androidx.appcompat.view.ContextThemeWrapper(this, R.style.Theme_AssistiveTouch)
        val inflater = LayoutInflater.from(themedContext)
        val view = try {
            inflater.inflate(R.layout.view_action_panel_wrapper, null)
        } catch (e: Exception) {
            panelState = PanelState.HIDDEN
            return
        }

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Full screen backdrop to capture outside touch without stealing focus
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        val panelCard = view.findViewById<View>(R.id.panelCard)
        val backdrop = view.findViewById<View>(R.id.panelBackdrop)
        val radialMenuView = view.findViewById<AssistiveRadialMenuView>(R.id.radialMenuView)

        // Close when clicking outside on the backdrop
        backdrop.setOnClickListener {
            closePanel()
        }

        // Set up dynamic item click handling
        radialMenuView.setOnItemClickListener { item ->
            handleMenuItemClick(item)
        }

        // Set up controls panel
        setupControlsView(view)

        // Load the initial core main menu
        currentMenuPageId = MenuRepository.PAGE_MAIN
        radialMenuView.setMenuPage(MenuRepository.getMainMenu(), animate = false)

        // Animate entrance: scrim fades in, card zooms in with spring overshoot
        view.alpha = 0f
        panelCard.alpha = 0f
        panelCard.scaleX = 0.85f
        panelCard.scaleY = 0.85f

        try {
            windowManager.addView(view, params)
            panelView = view
            panelLayoutParams = params

            view.animate().alpha(1f).setDuration(180).start()
            panelCard.animate()
                .alpha(1f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(240)
                .setInterpolator(OvershootInterpolator(1.15f))
                .withEndAction {
                    panelState = PanelState.VISIBLE
                }
                .start()
        } catch (e: Exception) {
            panelState = PanelState.HIDDEN
            safeRemoveView(view)
            panelView = null
            Toast.makeText(this, "Failed to open menu: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupControlsView(view: View) {
        val controlsBack = view.findViewById<View>(R.id.buttonControlsBack)
        controlsBack?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            showRadialMenuView()
        }

        // Quick toggles with Lucide icons
        view.findViewById<View>(R.id.controlWifi)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            closePanel()
            actionDispatcher.execute(AssistiveAction.Wifi, {})
        }
        view.findViewById<View>(R.id.controlBluetooth)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            closePanel()
            actionDispatcher.execute(AssistiveAction.Bluetooth, {})
        }
        view.findViewById<View>(R.id.controlScreenshot)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            closePanel()
            mainHandler.postDelayed({
                actionDispatcher.execute(AssistiveAction.Screenshot, {})
            }, 280L)
        }
        view.findViewById<View>(R.id.controlRecord)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            closePanel()
            mainHandler.postDelayed({
                actionDispatcher.execute(AssistiveAction.Recents, {})
            }, 120L)
        }

        // Live Volume Capsule Slider
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val volumeSlider = view.findViewById<CapsuleSliderView>(R.id.capsuleVolumeSlider)
        val maxVol = try {
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        } catch (_: Exception) { 15 }
        val currentVol = try {
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        } catch (_: Exception) { 7 }

        volumeSlider?.apply {
            minValue = 0
            maxValue = maxVol.coerceAtLeast(1)
            stepSize = 1
            showPercentage = false
            customFormat = { vol -> "VOL $vol / $maxValue" }
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

        // Live Brightness Capsule Slider
        val brightnessSlider = view.findViewById<CapsuleSliderView>(R.id.capsuleBrightnessSlider)
        val resolver = contentResolver
        val maxBrightness = 255
        val currentBrightness = try {
            Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (_: Exception) {
            128
        }

        brightnessSlider?.apply {
            minValue = 0
            maxValue = maxBrightness
            stepSize = 5
            showPercentage = true
            setIconResource(R.drawable.ic_lucide_sun)
            setValue(currentBrightness)

            onValueChanged = { b, fromUser ->
                if (fromUser) {
                    if (Settings.System.canWrite(this@FloatingButtonService)) {
                        try {
                            Settings.System.putInt(
                                resolver,
                                Settings.System.SCREEN_BRIGHTNESS_MODE,
                                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                            )
                            Settings.System.putInt(
                                resolver,
                                Settings.System.SCREEN_BRIGHTNESS,
                                b
                            )
                        } catch (_: Exception) {}
                    } else {
                        Toast.makeText(
                            this@FloatingButtonService,
                            "Allow modify system settings to change brightness",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun showControlsView() {
        val panel = panelView ?: return
        val radialMenu = panel.findViewById<View>(R.id.radialMenuView) ?: return
        val controlsLayout = panel.findViewById<View>(R.id.layoutControlsView) ?: return

        currentMenuPageId = MenuRepository.PAGE_CONTROLS

        radialMenu.animate().alpha(0f).setDuration(120).withEndAction {
            radialMenu.visibility = View.GONE
            controlsLayout.alpha = 0f
            controlsLayout.visibility = View.VISIBLE
            controlsLayout.animate().alpha(1f).setDuration(160).start()
        }.start()
    }

    private fun showRadialMenuView() {
        val panel = panelView ?: return
        val radialMenu = panel.findViewById<AssistiveRadialMenuView>(R.id.radialMenuView) ?: return
        val controlsLayout = panel.findViewById<View>(R.id.layoutControlsView) ?: return

        currentMenuPageId = MenuRepository.PAGE_MAIN
        radialMenu.setMenuPage(MenuRepository.getMainMenu(), animate = false)

        controlsLayout.animate().alpha(0f).setDuration(120).withEndAction {
            controlsLayout.visibility = View.GONE
            radialMenu.alpha = 0f
            radialMenu.visibility = View.VISIBLE
            radialMenu.animate().alpha(1f).setDuration(160).start()
        }.start()
    }

    private fun handleMenuItemClick(item: AssistiveItem) {
        actionDispatcher.execute(
            action = item.action,
            onClosePanel = { closePanel() },
            onNavigateMenu = { targetPageId ->
                navigateToMenuPage(targetPageId)
            }
        )
    }

    private fun navigateToMenuPage(pageId: String) {
        if (pageId == MenuRepository.PAGE_CONTROLS) {
            showControlsView()
            return
        }

        val panel = panelView ?: return
        val controlsLayout = panel.findViewById<View>(R.id.layoutControlsView)
        val radialMenu = panel.findViewById<AssistiveRadialMenuView>(R.id.radialMenuView) ?: return

        if (controlsLayout?.visibility == View.VISIBLE) {
            controlsLayout.visibility = View.GONE
            radialMenu.visibility = View.VISIBLE
            radialMenu.alpha = 1f
        }

        currentMenuPageId = pageId
        val page = when (pageId) {
            MenuRepository.PAGE_FAVORITES -> MenuRepository.getFavoritesMenu(this)
            else -> MenuRepository.getMainMenu()
        }
        radialMenu.setMenuPage(page, animate = true)
    }

    private fun closePanel() {
        if (panelState != PanelState.VISIBLE && panelState != PanelState.OPENING) return
        val view = panelView ?: run {
            panelState = PanelState.HIDDEN
            return
        }
        panelState = PanelState.CLOSING

        val panelCard = view.findViewById<View>(R.id.panelCard)

        view.animate().alpha(0f).setDuration(140).start()
        panelCard?.animate()
            ?.alpha(0f)
            ?.scaleX(0.85f)
            ?.scaleY(0.85f)
            ?.setDuration(160)
            ?.withEndAction {
                safeRemoveView(view)
                panelView = null
                panelLayoutParams = null
                currentMenuPageId = MenuRepository.PAGE_MAIN
                panelState = PanelState.HIDDEN
            }
            ?.start() ?: run {
                safeRemoveView(view)
                panelView = null
                panelLayoutParams = null
                currentMenuPageId = MenuRepository.PAGE_MAIN
                panelState = PanelState.HIDDEN
            }
    }

    private fun safeRemoveView(view: View?) {
        if (view != null) {
            try {
                windowManager.removeViewImmediate(view)
            } catch (_: Exception) {
                try {
                    windowManager.removeView(view)
                } catch (_: Exception) {}
            }
        }
    }

    // =========================================================================
    // Notification & Channel
    // =========================================================================

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TouchCore Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the TouchCore floating button active"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun createNotificationLargeIcon(): Bitmap {
        val size = (64 * resources.displayMetrics.density).toInt().coerceAtLeast(128)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cx = size / 2f
        val cy = size / 2f
        val rOuter = size * 0.46f

        // 1. Dark translucent glass disc background
        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(235, 22, 24, 32)
        }
        canvas.drawCircle(cx, cy, rOuter, outerPaint)

        // Subtle outer glass stroke
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.02f
            color = Color.argb(60, 255, 255, 255)
        }
        canvas.drawCircle(cx, cy, rOuter, borderPaint)

        // 2. Middle concentric white ring
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.045f
            color = Color.argb(190, 255, 255, 255)
        }
        canvas.drawCircle(cx, cy, size * 0.29f, ringPaint)

        // 3. Inner solid white core disc
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        canvas.drawCircle(cx, cy, size * 0.15f, corePaint)

        return bitmap
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags)

        val roundIcon = createNotificationLargeIcon()

        return androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TouchCore")
            .setContentText("TouchCore is active")
            .setSmallIcon(R.drawable.ic_notification_assistive_touch)
            .setLargeIcon(roundIcon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "touchcore_service_channel"
        private const val NOTIFICATION_ID = 1001

        private const val CLICK_THRESHOLD = 220L
        private const val LONG_PRESS_THRESHOLD = 500L
        private const val IDLE_DIM_DELAY = 2600L

        private const val PREF_KEY_X = "floating_x"
        private const val PREF_KEY_Y = "floating_y"
    }
}
