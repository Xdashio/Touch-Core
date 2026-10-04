package com.example.assistivetouch

import com.example.assistivetouch.model.AssistiveAction
import com.example.assistivetouch.model.MenuSlot
import com.example.assistivetouch.repository.MenuRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuRepositoryTest {

    @Test
    fun testMainMenuContainsRealFeatures() {
        val page = MenuRepository.getMainMenu()

        assertEquals(MenuRepository.PAGE_MAIN, page.id)
        assertNotNull("Center item must be Home", page.centerItem)
        assertTrue("Center item must be Home action", page.centerItem?.action is AssistiveAction.Home)
        assertEquals(7, page.items.size)

        val actions = page.items.map { it.action }
        assertTrue(actions.contains(AssistiveAction.Notifications))
        assertTrue(actions.contains(AssistiveAction.Flashlight))
        assertTrue(actions.contains(AssistiveAction.Recents))
        assertTrue(actions.contains(AssistiveAction.ControlsMenu))
        assertTrue(actions.contains(AssistiveAction.FavoritesMenu))
        assertTrue(actions.contains(AssistiveAction.Back))
        assertTrue(actions.contains(AssistiveAction.LockScreen))
    }

    @Test
    fun testControlsMenuContainsSystemControlsAndBack() {
        val page = MenuRepository.getControlsMenu()

        assertEquals(MenuRepository.PAGE_CONTROLS, page.id)
        assertNotNull("Center item must exist to navigate back to main", page.centerItem)
        assertTrue(page.centerItem?.action is AssistiveAction.BackToMain)

        val actions = page.items.map { it.action }
        assertTrue(actions.contains(AssistiveAction.Screenshot))
        assertTrue(actions.contains(AssistiveAction.Bluetooth))
        assertTrue(actions.contains(AssistiveAction.ScreenRecord))
        assertTrue(actions.contains(AssistiveAction.RotateScreen))
        assertTrue(actions.contains(AssistiveAction.Wifi))
        assertTrue(actions.contains(AssistiveAction.PowerDialog))
    }

    @Test
    fun testMenuSlotRadialOffsets() {
        val radius = 100f

        // TOP should be (0, -radius)
        val (topX, topY) = MenuSlot.TOP.computeOffset(radius)
        assertEquals(0f, topX, 0.01f)
        assertEquals(-radius, topY, 0.01f)

        // RIGHT should be (radius, 0)
        val (rightX, rightY) = MenuSlot.RIGHT.computeOffset(radius)
        assertEquals(radius, rightX, 0.01f)
        assertEquals(0f, rightY, 0.01f)

        // BOTTOM should be (0, radius)
        val (bottomX, bottomY) = MenuSlot.BOTTOM.computeOffset(radius)
        assertEquals(0f, bottomX, 0.01f)
        assertEquals(radius, bottomY, 0.01f)

        // LEFT should be (-radius, 0)
        val (leftX, leftY) = MenuSlot.LEFT.computeOffset(radius)
        assertEquals(-radius, leftX, 0.01f)
        assertEquals(0f, leftY, 0.01f)

        // CENTER should be (0, 0)
        val (centerX, centerY) = MenuSlot.CENTER.computeOffset(radius)
        assertEquals(0f, centerX, 0.01f)
        assertEquals(0f, centerY, 0.01f)
    }
}
