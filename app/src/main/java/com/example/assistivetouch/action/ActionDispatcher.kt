package com.example.assistivetouch.action

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import com.example.assistivetouch.model.AssistiveAction
import com.example.assistivetouch.service.MyAccessibilityService
import com.example.assistivetouch.service.ScreenRecordingService
import com.example.assistivetouch.ui.FavoritesActivity
import com.example.assistivetouch.ui.SettingsActivity

/**
 * Executes all real features of the Assistive Touch application safely.
 */
class ActionDispatcher(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isTorchOn = false

    fun execute(
        action: AssistiveAction,
        onClosePanel: () -> Unit,
        onNavigateMenu: ((String) -> Unit)? = null
    ) {
        when (action) {
            is AssistiveAction.Home -> {
                onClosePanel()
                runWithAccessibility { it.performHomeAction() }
            }

            is AssistiveAction.Back -> {
                onClosePanel()
                runWithAccessibility { it.performBackAction() }
            }

            is AssistiveAction.Recents -> {
                onClosePanel()
                runWithAccessibility { it.performRecentsAction() }
            }

            is AssistiveAction.Notifications -> {
                onClosePanel()
                runWithAccessibility { it.openNotificationsPanel() }
            }

            is AssistiveAction.LockScreen -> {
                onClosePanel()
                runWithAccessibility { it.performLockScreenAction() }
            }

            is AssistiveAction.Flashlight -> {
                toggleFlashlight()
            }

            is AssistiveAction.Screenshot -> {
                onClosePanel()
                // Delayed screenshot so AssistiveTouch panel disappears before screenshot is captured
                mainHandler.postDelayed({
                    runWithAccessibility { it.performScreenshotAction() }
                }, 280L)
            }

            is AssistiveAction.ControlsMenu -> {
                onNavigateMenu?.invoke("controls")
            }

            is AssistiveAction.FavoritesMenu -> {
                onNavigateMenu?.invoke("favorites")
            }

            is AssistiveAction.BackToMain -> {
                onNavigateMenu?.invoke("main")
            }

            is AssistiveAction.Wifi -> {
                onClosePanel()
                openWifiPanel()
            }

            is AssistiveAction.Bluetooth -> {
                onClosePanel()
                openBluetoothSettings()
            }

            is AssistiveAction.ScreenRecord -> {
                onClosePanel()
                mainHandler.postDelayed({
                    toggleScreenRecording()
                }, 220L)
            }

            is AssistiveAction.RotateScreen -> {
                toggleRotation()
            }

            is AssistiveAction.PowerDialog -> {
                onClosePanel()
                runWithAccessibility { it.performPowerDialogAction() }
            }

            is AssistiveAction.LaunchApp -> {
                onClosePanel()
                launchApp(action.packageName)
            }

            is AssistiveAction.AddFavorite -> {
                onClosePanel()
                openFavoritesPicker()
            }

            is AssistiveAction.OpenSettings -> {
                onClosePanel()
                openSettings()
            }
        }
    }

    private inline fun runWithAccessibility(crossinline action: (MyAccessibilityService) -> Unit) {
        val service = MyAccessibilityService.getInstance()
        if (service != null) {
            action(service)
        } else {
            Toast.makeText(
                context,
                "Please enable Accessibility Service in Settings",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun toggleFlashlight() {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                try {
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                } catch (_: Exception) {
                    false
                }
            } ?: run {
                Toast.makeText(context, "No flash unit found on this camera", Toast.LENGTH_SHORT).show()
                return
            }
            isTorchOn = !isTorchOn
            cameraManager.setTorchMode(cameraId, isTorchOn)
            Toast.makeText(context, if (isTorchOn) "Torch ON" else "Torch OFF", Toast.LENGTH_SHORT).show()
        } catch (e: CameraAccessException) {
            Toast.makeText(context, "Flashlight unavailable", Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(context, "Camera permission needed for torch", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(context, "Flashlight not supported", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleScreenRecording() {
        if (ScreenRecordingService.isRecording) {
            ScreenRecordingService.requestStop(context)
        } else {
            ScreenRecordingService.launchPermissionFlow(context)
        }
    }

    private fun openWifiPanel() {
        val intentsToTry = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Intent(Settings.Panel.ACTION_WIFI))
            }
            add(Intent(Settings.ACTION_WIFI_SETTINGS))
            add(Intent(Settings.ACTION_SETTINGS))
        }

        for (intent in intentsToTry) {
            try {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
    }

    private fun openBluetoothSettings() {
        val intentsToTry = listOf(
            Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )

        for (intent in intentsToTry) {
            try {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
    }

    private fun toggleRotation() {
        val resolver = context.contentResolver
        if (Settings.System.canWrite(context)) {
            val current = try {
                Settings.System.getInt(resolver, Settings.System.ACCELEROMETER_ROTATION)
            } catch (_: Exception) { 0 }
            val next = if (current == 1) 0 else 1
            try {
                Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, next)
                Toast.makeText(context, if (next == 1) "Auto-rotate ON" else "Auto-rotate OFF", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to toggle auto-rotate", Toast.LENGTH_SHORT).show()
            }
        } else {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    private fun launchApp(packageName: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
        } catch (_: Exception) {}
    }

    private fun openFavoritesPicker() {
        try {
            val intent = Intent(context, FavoritesActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun openSettings() {
        try {
            val intent = Intent(context, SettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
