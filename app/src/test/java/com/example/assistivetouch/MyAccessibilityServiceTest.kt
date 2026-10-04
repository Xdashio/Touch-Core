package com.example.assistivetouch

import android.content.Context
import android.provider.Settings
import com.example.assistivetouch.service.MyAccessibilityService
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner.Silent::class)
class MyAccessibilityServiceTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var contentResolver: android.content.ContentResolver

    private lateinit var mockedSettings: MockedStatic<Settings.Secure>

    @Before
    fun setUp() {
        `when`(context.contentResolver).thenReturn(contentResolver)
        `when`(context.packageName).thenReturn("com.example.assistivetouch")
        mockedSettings = mockStatic(Settings.Secure::class.java)
    }

    @After
    fun tearDown() {
        mockedSettings.close()
    }

    @Test
    fun testIsEnabled_whenServiceEnabled_returnsTrue() {
        val serviceId = "com.example.assistivetouch/com.example.assistivetouch.service.MyAccessibilityService"
        mockedSettings.`when`<String> { Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) }
            .thenReturn(serviceId)

        val result = MyAccessibilityService.isEnabled(context)

        assertTrue(result)
    }

    @Test
    fun testIsEnabled_whenServiceNotEnabled_returnsFalse() {
        mockedSettings.`when`<String> { Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) }
            .thenReturn("com.other.package/com.other.Service")

        val result = MyAccessibilityService.isEnabled(context)

        assertFalse(result)
    }

    @Test
    fun testIsEnabled_whenNoServicesEnabled_returnsFalse() {
        mockedSettings.`when`<String> { Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) }
            .thenReturn(null)

        val result = MyAccessibilityService.isEnabled(context)

        assertFalse(result)
    }

    @Test
    fun testIsEnabled_whenMultipleServicesEnabled_returnsTrue() {
        val serviceId = "com.example.assistivetouch/com.example.assistivetouch.service.MyAccessibilityService"
        val otherService = "com.other.package/com.other.Service"
        mockedSettings.`when`<String> { Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) }
            .thenReturn("$serviceId:$otherService")

        val result = MyAccessibilityService.isEnabled(context)

        assertTrue(result)
    }

    @Test
    fun testIsEnabled_whenServiceIdHasWrongClass_returnsFalse() {
        val serviceId = "com.example.assistivetouch/com.example.assistivetouch.service.OtherService"
        mockedSettings.`when`<String> { Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) }
            .thenReturn(serviceId)

        val result = MyAccessibilityService.isEnabled(context)

        assertFalse(result)
    }
}
