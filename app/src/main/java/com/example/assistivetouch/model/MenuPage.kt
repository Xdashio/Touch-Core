package com.example.assistivetouch.model

/**
 * Represents a complete page of the Assistive Touch menu.
 * Contains radial slot items and an optional center item (such as a Back button for submenus).
 */
data class MenuPage(
    val id: String,
    val items: List<AssistiveItem>,
    val centerItem: AssistiveItem? = null
)
