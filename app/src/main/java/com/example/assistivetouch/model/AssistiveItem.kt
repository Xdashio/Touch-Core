package com.example.assistivetouch.model

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

/**
 * Data model for an AssistiveTouch menu item.
 * Nothing is hardcoded in the view; all labels, icons, positions, and actions
 * are resolved dynamically from this data structure.
 */
data class AssistiveItem(
    val id: String,
    @StringRes val titleResId: Int? = null,
    val titleText: String? = null,
    @DrawableRes val iconRes: Int? = null,
    val iconDrawable: Drawable? = null,
    val action: AssistiveAction,
    val slot: MenuSlot
) {
    fun getDisplayTitle(context: Context): String {
        return titleText ?: titleResId?.let { context.getString(it) } ?: ""
    }

    fun getResolvedDrawable(context: Context): Drawable? {
        return iconDrawable ?: iconRes?.let { ContextCompat.getDrawable(context, it) }
    }
}
