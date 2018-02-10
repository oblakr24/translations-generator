package generator

import org.junit.Test

import java.io.File

import org.junit.Assert.assertEquals

class SettingsTest {

    /**
     * Test the settings parsing
     */
    @Test
    fun testSetting() {
        val settingsFile = File(javaClass.getResource("/settings.json").file)
        val settings = SettingsParser.parse(settingsFile)

        assertEquals(3, settings.targets.size)
        assertEquals("mainClient", settings.targets.first().clientName)
        assertEquals(true, settings.targets.first().doIOS)
        assertEquals(true, settings.targets.first().doAndroid)

        val clientTwoSettings = TargetSetting("clientOne", emptyList(), true, true, "English", null, "android/clientOne", "App/Clients/clientOne")

        assertEquals(clientTwoSettings, settings.targets[1])
        assertEquals("translations_clientTwo.csv", settings.targets.last().clientCSVFilename)
    }
}
