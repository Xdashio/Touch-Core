package com.example.assistivetouch.repository

import android.content.Context
import android.content.pm.PackageManager
import com.example.assistivetouch.R
import com.example.assistivetouch.model.AssistiveAction
import com.example.assistivetouch.model.AssistiveItem
import com.example.assistivetouch.model.MenuPage
import com.example.assistivetouch.model.MenuSlot
import com.example.assistivetouch.prefs.FavoritesManager

/**
 * Dynamic repository that produces the menus for the app's real feature set.
 * No hardcoded Siri, Control Center, or Device placeholders.
 */
object MenuRepository {

    const val PAGE_MAIN = "main"
    const val PAGE_CONTROLS = "controls"
    const val PAGE_FAVORITES = "favorites"

    /**
     * Main Core Menu:
     * - Center: Home (concentric circles)
     * - Top: Notifications
     * - Top-Right: Torch (Flashlight)
     * - Right: Recents (App Switcher)
     * - Bottom-Right: Controls (Sliders & toggles)
     * - Bottom-Left: Favorites (User's custom apps)
     * - Left: Back
     * - Top-Left: Lock Screen
     */
    fun getMainMenu(): MenuPage {
        val items = listOf(
            AssistiveItem(
                id = "notifications",
                titleResId = R.string.menu_notifications,
                iconRes = R.drawable.ic_lucide_bell,
                action = AssistiveAction.Notifications,
                slot = MenuSlot.TOP
            ),
            AssistiveItem(
                id = "flashlight",
                titleResId = R.string.menu_flashlight,
                iconRes = R.drawable.ic_lucide_flashlight,
                action = AssistiveAction.Flashlight,
                slot = MenuSlot.TOP_RIGHT
            ),
            AssistiveItem(
                id = "recents",
                titleResId = R.string.menu_recents,
                iconRes = R.drawable.ic_lucide_layers,
                action = AssistiveAction.Recents,
                slot = MenuSlot.RIGHT
            ),
            AssistiveItem(
                id = "controls",
                titleResId = R.string.menu_controls,
                iconRes = R.drawable.ic_lucide_sliders,
                action = AssistiveAction.ControlsMenu,
                slot = MenuSlot.BOTTOM_RIGHT
            ),
            AssistiveItem(
                id = "favorites",
                titleResId = R.string.menu_favorites,
                iconRes = R.drawable.ic_lucide_star,
                action = AssistiveAction.FavoritesMenu,
                slot = MenuSlot.BOTTOM_LEFT
            ),
            AssistiveItem(
                id = "back",
                titleResId = R.string.menu_back,
                iconRes = R.drawable.ic_lucide_arrow_left,
                action = AssistiveAction.Back,
                slot = MenuSlot.LEFT
            ),
            AssistiveItem(
                id = "lock_screen",
                titleResId = R.string.menu_lock_screen,
                iconRes = R.drawable.ic_lucide_lock,
                action = AssistiveAction.LockScreen,
                slot = MenuSlot.TOP_LEFT
            )
        )

        val centerItem = AssistiveItem(
            id = "home",
            titleResId = R.string.menu_home,
            iconRes = R.drawable.ic_lucide_home,
            action = AssistiveAction.Home,
            slot = MenuSlot.CENTER
        )

        return MenuPage(
            id = PAGE_MAIN,
            items = items,
            centerItem = centerItem
        )
    }

    /**
     * Controls Submenu:
     * Wi-Fi, Bluetooth, Screen Record, Screenshot, Rotate Screen, Power,
     * with center button returning to Main.
     */
    fun getControlsMenu(): MenuPage {
        val items = listOf(
            AssistiveItem(
                id = "screenshot",
                titleResId = R.string.menu_screenshot,
                iconRes = R.drawable.ic_lucide_camera,
                action = AssistiveAction.Screenshot,
                slot = MenuSlot.TOP
            ),
            AssistiveItem(
                id = "bluetooth",
                titleResId = R.string.menu_bluetooth,
                iconRes = R.drawable.ic_lucide_bluetooth,
                action = AssistiveAction.Bluetooth,
                slot = MenuSlot.RIGHT
            ),
            AssistiveItem(
                id = "record",
                titleResId = R.string.menu_record,
                iconRes = R.drawable.ic_lucide_record,
                action = AssistiveAction.ScreenRecord,
                slot = MenuSlot.BOTTOM_RIGHT
            ),
            AssistiveItem(
                id = "rotate",
                titleResId = R.string.menu_rotate_screen,
                iconRes = R.drawable.ic_lucide_rotate,
                action = AssistiveAction.RotateScreen,
                slot = MenuSlot.BOTTOM_LEFT
            ),
            AssistiveItem(
                id = "wifi",
                titleResId = R.string.menu_wifi,
                iconRes = R.drawable.ic_lucide_wifi,
                action = AssistiveAction.Wifi,
                slot = MenuSlot.LEFT
            ),
            AssistiveItem(
                id = "power",
                titleResId = R.string.menu_power,
                iconRes = R.drawable.ic_lucide_power,
                action = AssistiveAction.PowerDialog,
                slot = MenuSlot.TOP_LEFT
            )
        )

        val centerItem = AssistiveItem(
            id = "back_to_main",
            titleResId = R.string.menu_back,
            iconRes = R.drawable.ic_lucide_arrow_left,
            action = AssistiveAction.BackToMain,
            slot = MenuSlot.CENTER
        )

        return MenuPage(
            id = PAGE_CONTROLS,
            items = items,
            centerItem = centerItem
        )
    }

    /**
     * Favorites Submenu:
     * Populated dynamically with user's favorite apps, + Add shortcut, and Settings shortcut.
     */
    fun getFavoritesMenu(context: Context): MenuPage {
        val packageManager = context.packageManager
        val favoritePackages = FavoritesManager.getFavoritePackages(context).toList()
        val availableSlots = listOf(
            MenuSlot.TOP,
            MenuSlot.TOP_RIGHT,
            MenuSlot.RIGHT,
            MenuSlot.BOTTOM_RIGHT,
            MenuSlot.BOTTOM_LEFT,
            MenuSlot.LEFT,
            MenuSlot.TOP_LEFT
        )

        val items = mutableListOf<AssistiveItem>()

        for (i in favoritePackages.indices) {
            if (i >= availableSlots.size - 2) break
            val pkg = favoritePackages[i]
            try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                val appLabel = packageManager.getApplicationLabel(appInfo).toString()
                val appIcon = packageManager.getApplicationIcon(appInfo)
                items.add(
                    AssistiveItem(
                        id = "app_$pkg",
                        titleText = appLabel,
                        iconDrawable = appIcon,
                        action = AssistiveAction.LaunchApp(pkg, appLabel),
                        slot = availableSlots[i]
                    )
                )
            } catch (_: PackageManager.NameNotFoundException) {}
        }

        // Add "+ Add" shortcut
        val nextIndex = items.size
        if (nextIndex < availableSlots.size) {
            items.add(
                AssistiveItem(
                    id = "add_favorite",
                    titleResId = R.string.menu_add_favorite,
                    iconRes = R.drawable.ic_lucide_plus,
                    action = AssistiveAction.AddFavorite,
                    slot = availableSlots[nextIndex]
                )
            )
        }

        // Add Settings shortcut
        val settingsIndex = items.size
        if (settingsIndex < availableSlots.size) {
            items.add(
                AssistiveItem(
                    id = "open_settings",
                    titleResId = R.string.open_settings,
                    iconRes = R.drawable.ic_lucide_settings,
                    action = AssistiveAction.OpenSettings,
                    slot = availableSlots[settingsIndex]
                )
            )
        }

        val centerItem = AssistiveItem(
            id = "back_to_main_fav",
            titleResId = R.string.menu_back,
            iconRes = R.drawable.ic_lucide_arrow_left,
            action = AssistiveAction.BackToMain,
            slot = MenuSlot.CENTER
        )

        return MenuPage(
            id = PAGE_FAVORITES,
            items = items,
            centerItem = centerItem
        )
    }
}
