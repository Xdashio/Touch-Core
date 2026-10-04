package com.example.assistivetouch.model

/**
 * Clean, decoupled actions representing the exact features of the application.
 * No hardcoded Siri, Control Center, or Device placeholders.
 */
sealed class AssistiveAction {
    // Primary core actions
    object Home : AssistiveAction()
    object Back : AssistiveAction()
    object Recents : AssistiveAction()
    object Notifications : AssistiveAction()
    object LockScreen : AssistiveAction()
    object Flashlight : AssistiveAction()
    object Screenshot : AssistiveAction()
    object ControlsMenu : AssistiveAction()
    object FavoritesMenu : AssistiveAction()
    object BackToMain : AssistiveAction()

    // Controls sub-actions
    object Wifi : AssistiveAction()
    object Bluetooth : AssistiveAction()
    object ScreenRecord : AssistiveAction()
    object RotateScreen : AssistiveAction()
    object PowerDialog : AssistiveAction()

    // Favorites sub-actions
    data class LaunchApp(val packageName: String, val appLabel: String? = null) : AssistiveAction()
    object AddFavorite : AssistiveAction()
    object OpenSettings : AssistiveAction()
}
